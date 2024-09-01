package org.lynxz.version.transform;

import com.android.build.api.transform.DirectoryInput;
import com.android.build.api.transform.Format;
import com.android.build.api.transform.JarInput;
import com.android.build.api.transform.QualifiedContent;
import com.android.build.api.transform.Status;
import com.android.build.api.transform.Transform;
import com.android.build.api.transform.TransformInput;
import com.android.build.api.transform.TransformInvocation;
import com.android.build.api.transform.TransformOutputProvider;
import com.android.build.gradle.internal.pipeline.TransformManager;

import org.lynxz.version.util.FileUtilSimple;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Map;
import java.util.Set;

/**
 * transform基类, 默认只处理 .class 文件(所有模块), 具体可通过 {@link #getInputTypes()} 修改
 * 子类需实现: {@link #modifyClassCode(File, File)} 对字节码文件进行修改
 * 其他方法说明:
 * 1. {@link #onTransformStart()} 开始transform时回调, 返回boolean值, 表示是否可以继续执行
 * 2. {@link #onTransformFinished()} transform结束后回调, 可以进行收尾操作
 * 3. {@link #getName()} 返回transform的名称, 默认为子类名, 最终可在编译build面板中看到日志: Task :app:transformClassesWith{名称}ForDebug
 */
public abstract class BaseTransform extends Transform {
    // 本地调试用, 记录日志到本地
    private static final boolean enableSaveLog = false;
    private static final String logPath = "D:\\transform_log.txt";

    protected static void printLog(String msg) {
        printLog(msg, enableSaveLog);
    }

    private static void printLog(String msg, boolean save2Log) {
//        System.out.println(msg);
//        if (save2Log) {
//            FileUtilSimple.writeToFile(msg, logPath, true);
//        }
    }

    /**
     * 指定transform的名称
     * 在build信息中可以看到: Task :app:transformClassesWith{名称}ForDebug
     */
    @Override
    public String getName() {
        return getClass().getSimpleName();
    }

    /**
     * 指定需要处理的输入类型
     */
    @Override
    public Set<QualifiedContent.ContentType> getInputTypes() {
        return TransformManager.CONTENT_CLASS; // 代表 javac 编译成的 class 文件，常用
    }

    /**
     * gradle支持多工程(模块)编译, 可通过本方法指定输入文件所属范围
     */
    @Override
    public Set<? super QualifiedContent.Scope> getScopes() {
        return TransformManager.SCOPE_FULL_PROJECT; // 所有模块
    }

    /**
     * 是否是增量编译
     */
    @Override
    public boolean isIncremental() {
        return false;
    }

    /**
     * @param transformInvocation the invocation object containing the transform inputs.
     */
    @Override
    public void transform(TransformInvocation transformInvocation) throws IOException {
        if (!onTransformStart()) {
            printLog("onTransformFinished immediately as onTransformStart return false");
            onTransformFinished();
            return;
        }

        boolean incremental = transformInvocation.isIncremental();
        // 获取输出，如果没有上一级的输入，输出可能也就是空的
        TransformOutputProvider outputProvider = transformInvocation.getOutputProvider();
        // 如果不支持增量编译，需要把之前生成的都删除掉，不缓存复用
        if (!incremental) {
            outputProvider.deleteAll();
        }
        // 当然任务也可以放在并发的线程池进行，等待任务结束
        for (TransformInput input : transformInvocation.getInputs()) {
            // 处理Jar
            Collection<JarInput> jarInputs = input.getJarInputs();
            if (jarInputs != null && jarInputs.size() > 0) {
                for (JarInput jarInput : jarInputs) {
                    processJarFile(jarInput, outputProvider, incremental);
                }
            }
            // 处理source
            Collection<DirectoryInput> directoryInputs = input.getDirectoryInputs();
            if (directoryInputs != null && directoryInputs.size() > 0) {
                for (DirectoryInput directoryInput : directoryInputs) {
                    processDirFile(directoryInput, outputProvider, incremental);
                }
            }
        }

        onTransformFinished();
    }

    /**
     * 处理Jar
     */
    private void processJarFile(JarInput input, TransformOutputProvider outputProvider,
                                boolean incremental) {
        // 获取到输出文件
        File dest = outputProvider.getContentLocation(input.getFile().getAbsolutePath(),
                input.getContentTypes(), input.getScopes(), Format.JAR);
        File inputFile = input.getFile();
        if (!incremental) {
            transformJarFile(inputFile, dest);
        } else {
            switch (input.getStatus()) {
                case NOTCHANGED:
                    break;
                case ADDED:
                case CHANGED:
                    transformJarFile(inputFile, dest);
                    break;
                case REMOVED:
                    deleteIfExists(dest);
                    break;
            }
        }
    }

    /**
     * 处理source-file
     */
    private void processDirFile(DirectoryInput input, TransformOutputProvider outputProvider,
                                boolean incremental) {
        // 处理源文件
        File dest = outputProvider.getContentLocation(input.getFile().getAbsolutePath(),
                input.getContentTypes(), input.getScopes(), Format.DIRECTORY);
        // 创建文件夹
        FileUtilSimple.createDIR(dest, true);
        if (incremental) {
            // 输入的路径
            String inputDirPath = input.getFile().getAbsolutePath();
            // 输出的路径
            String destDirPath = dest.getAbsolutePath();
            // 获取更改的
            Map<File, Status> changedFileMap = input.getChangedFiles();
            // 继续遍历
            for (Map.Entry<File, Status> entry : changedFileMap.entrySet()) {
                File inputFile = entry.getKey();
                String destFilePath = inputFile.getAbsolutePath().replace(inputDirPath, destDirPath);
                File outputFile = new File(destFilePath);
                switch (entry.getValue()) {
                    case NOTCHANGED:
                        break;
                    case ADDED:
                    case CHANGED:
                        transformDirFile(inputFile, outputFile);
                        break;
                    case REMOVED:
                        deleteIfExists(outputFile);
                        break;
                }
            }
        } else {
            copyDir(input.getFile(), dest);
        }
    }

    private void copyDir(File input, File dest) {
        deleteIfExists(dest);
        String srcDirPath = input.getAbsolutePath();
        String destDirPath = dest.getAbsolutePath();
        File[] inputFiles = input.listFiles();
        if (inputFiles != null) {
            for (File file : inputFiles) {
                String destFilePath = file.getAbsolutePath().replace(srcDirPath, destDirPath);
                File destFile = new File(destFilePath);
                if (file.isDirectory()) {
                    copyDir(file, destFile);
                } else if (file.isFile()) {
                    transformDirFile(file, destFile);
                }
            }
        }
    }

    protected void transformDirFile(File inputFile, File outputFile) {
        FileUtilSimple.createFile(outputFile);

        // 注意: 只修改class文件，进行字节码插桩
        if (isClassFile(inputFile)) {
            modifyClassCode(inputFile, outputFile);
        } else {
            FileUtilSimple.copyFile(inputFile, outputFile);
        }
    }

    private boolean isClassFile(File file) {
        String name = file.getName();
        return name.endsWith(".class") && !name.equals("R.class")
                && !name.startsWith("R$") && !name.equals("BuildConfig.class")
                && !FileUtilSimple.isFileEmpty(file);
    }

    /**
     * 进行字节码插桩
     */
    protected abstract void modifyClassCode(File inputFile, File outputFile);

    // 开始transform时触发, 若返回false,则不进行具体的transform操作
    protected boolean onTransformStart() {
        return true;
    }

    // transform结束时触发
    protected void onTransformFinished() {

    }

    protected void transformJarFile(File inputFile, File outputFile) {
        FileUtilSimple.createFile(outputFile);

        // 注意: 只修改class文件，进行字节码插桩
        if (isClassFile(inputFile)) {
            modifyClassCode(inputFile, outputFile);
        } else {
            FileUtilSimple.copyFile(inputFile, outputFile);
        }
    }

    private void deleteIfExists(File file) {
        if (file.exists()) {
            FileUtilSimple.deleteFile(file.getAbsolutePath());
        }
    }
}
