package org.lynxz.version.util;


import java.io.BufferedWriter;
import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;

public class FileUtilSimple {
    private static final String TAG = "FileUtilSimple";

    private static boolean isNullOrEmpty(String info) {
        return info == null || info.isEmpty();
    }

    private static void printLog(String tag, String msg) {
        System.out.println(tag + " " + msg);
    }

    /**
     * 检查 文件 / 文件夹 是否存在
     *
     * @param filepath 文件绝对路径
     */
    public static boolean checkFileExists(String filepath) {
        return !isNullOrEmpty(filepath) && new File(filepath).exists();
    }

    /**
     * 创建文件夹,若已存在则不重新创建
     *
     * @param dirpath 路径
     */
    public static boolean createDIR(String dirpath) {
        return createDIR(dirpath, false);
    }

    /**
     * 创建文件夹
     * forceRecreate 若文件存在,但非目录,则删除重建
     * 参考 {@link #createDIR(File, boolean)}
     */
    public static boolean createDIR(String dirpath, boolean forceRecreate) {
        return createDIR(new File(dirpath), forceRecreate);
    }

    /**
     * 创建文件夹
     * 若文件存在,但非目录,则删除重建
     *
     * @param targetFile    要创建的目标目录文件
     * @param forceRecreate 若目录已存在,是否要强制重新闯进(删除后,新建)
     * @return 是否创建成功
     */
    public static boolean createDIR(File targetFile, boolean forceRecreate) {
        if (targetFile == null) {
            return false;
        }

        boolean result = true;
        if (targetFile.exists()) { // 存在同名文件
            boolean isDir = targetFile.isDirectory();
            if (!isDir) { // 非目录,删除以便创建目录
                result = targetFile.delete();
            } else if (forceRecreate) { // 强制删除目录
                result = deleteDir(targetFile);
            } else { // 目录存在
                return true;
            }
        }

        if (!result) {
            printLog(TAG, "删除目录相关文件失败,创建失败,请排查");
            return false;
        }

        return targetFile.mkdirs();
    }


    public static boolean createFile(String filepath) {
        return createFile(new File(filepath));
    }

    /**
     * 创建文件
     * 若存在同名文件/目录,则直接返回 true
     *
     * @return 创建文件结果
     */
    public static boolean createFile(File file) {
        if (file == null) {
            return false;
        }

        if (file.exists()) {
            return true;
        }
        boolean result = false;
        try {
            createDIR(file.getParent());

            result = file.createNewFile();
        } catch (Exception e) {
            e.printStackTrace();
        }
        // printLog(TAG, "createFile " + file.getAbsolutePath() + " , result = " + result);
        return result;
    }


    public static void writeToFile(String msg, String fileRelPathAndName, boolean append) {
        writeToFile(msg, fileRelPathAndName, append, true);
    }

    /**
     * 写文件
     *
     * @param msg         写入的内容
     * @param filePath    绝对路径,如: /sdcard/amapauto20/aa/bb
     * @param append      是否是追加模式
     * @param autoAddCTRL 自动在结尾添加回测换行符
     */
    public static void writeToFile(String msg, String filePath, boolean append, boolean autoAddCTRL) {
        if (isNullOrEmpty(filePath)) {
            return;
        }

        filePath = filePath
                .replace("\\", "/")
                .replace("//", "/");

        createFile(filePath);

        if (msg == null) {
            msg = "";
        }

        if (autoAddCTRL) {
            msg += "\r\n";
        }

        FileWriter fileWriter = null;
        BufferedWriter bufferedWriter = null;
        try {
            File file = new File(filePath);
            fileWriter = new FileWriter(file, append);
            bufferedWriter = new BufferedWriter(fileWriter);
            bufferedWriter.write(msg);
            bufferedWriter.flush();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            safetyClose(bufferedWriter);
            safetyClose(fileWriter);
        }
    }

    /**
     * 复制文件到指定位置
     *
     * @param srcPath 源文件路径,可以是目录
     * @param dstPath 目标位置
     * @return 复制成功或失败(对单文件复制结果准确)
     */
    public static boolean copyFile(String srcPath, String dstPath) {
        if (!checkFileExists(srcPath)) {
            return false;
        }

        File srcFile = new File(srcPath);
        return copyFile(srcFile, new File(dstPath));
    }

    public static boolean copyFile(File srcFile, File dstFile) {
        String dstPath = dstFile.getAbsolutePath();
        if (srcFile.isDirectory()) { // 目录-递归处理
            createDIR(dstFile, false);

            File[] files = srcFile.listFiles();
            int len = files == null ? 0 : files.length;
            for (int i = 0; i < len; i++) {
                File file = files[i];
                String dstFilePath = dstPath + "/" + file.getName();
                copyFile(file.getAbsolutePath(), dstFilePath.replace("//", "/"));
            }
        } else { // 文件-直接复制
            InputStream is = null;
            FileOutputStream os = null;

            if (dstPath.endsWith(File.separator)) { // 斜杠结尾表示目录
                dstPath += srcFile.getName();
            }
            // 若源文件与目标文件地址先沟通,则不做处理
            if (srcFile.getAbsolutePath().equalsIgnoreCase(dstFile.getAbsolutePath())) {
                return true;
            }

            createFile(dstFile);
            try {
                is = new FileInputStream(srcFile);
                os = new FileOutputStream(dstFile);

                byte[] buffer = new byte[2048];
                int readLen = is.read(buffer);

                while (readLen != -1) {
                    os.write(buffer, 0, readLen);
                    readLen = is.read(buffer);
                }
                os.flush();
                return true;
            } catch (Exception e) {
                e.printStackTrace();
                return false;
            } finally {
                safetyClose(is);
                safetyClose(os);
            }
        }

        return false;
    }

    /**
     * 删除文件或者目录
     *
     * @return true-目标文件不存在(包括原本就不存在以及删除成功两种情况)
     * false-目标文件仍存在
     */
    public static boolean deleteFile(String path) {
        return deleteFile(path, true);
    }

    /**
     * 删除文件或者目录
     *
     * @param deleteWhenIsDir path对应文件是目录时,是否要删除, true-删除 false-非目录时才删除
     * @return true-目标文件不存在(包括原本就不存在以及删除成功两种情况)
     * false-目标文件仍存在
     */
    public static boolean deleteFile(String path, boolean deleteWhenIsDir) {
        File file = new File(path);
        if (!file.exists()) {
            return true;
        }

        if (file.isFile()) {
            return file.delete();
        } else if (deleteWhenIsDir) {
            return deleteDir(file);
        } else {
            return true;
        }
    }


    /**
     * 删除指定目录
     * 若存在同名非目录文件,则不处理
     */
    public static boolean deleteDir(File dir) {
        if (dir == null || !dir.exists() || !dir.isDirectory()) {
            return true;
        }

        // 重命名下再删除, 避免出现  Device or resource busy 等问题
        String srcPath = dir.getAbsolutePath();
        String destPath = srcPath + "_" + System.currentTimeMillis();
        if (rename(srcPath, destPath)) {
            dir = new File(destPath);
        } else {
            printLog(TAG, "deleteDir dir fail: srcPath=" + srcPath + ",destPath=" + destPath);
        }
        for (File file : dir.listFiles()) {
            if (file.isFile()) {// 删除所有文件
                boolean delete = file.delete();
                if (!delete) {
                    printLog(TAG, "deleteDir sub file fail,destPath=" + destPath + ",name=" + file.getName());
                }
            } else if (file.isDirectory()) { // 递归删除子目录
                deleteDir(file);
            }
        }
        return dir.delete();// 删除空目录本身
    }

    public static void safetyClose(Closeable closeable) {
        if (closeable != null) {
            try {
                closeable.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * 移动文件到指定位置并重命名
     *
     * @param oriFilePath  源文件绝对路径
     * @param destFilePath 要移动到的目标位置绝对路径
     */
    public static boolean rename(String oriFilePath, String destFilePath) {
        File srcFile = new File(oriFilePath);
        if (!srcFile.exists()) {
            printLog(TAG, "rename fail as " + oriFilePath + " not exist");
            return false;
        }

        File dest = new File(destFilePath);
        dest.getParentFile().mkdirs();
        return srcFile.renameTo(dest);
    }

    /**
     * 获取指定文件的字节大小, 若文件不存在则返回0
     */
    public static long getFileLen(String filePath) {
        if (isNullOrEmpty(filePath)) {
            return 0;
        }
        File file = new File(filePath);
        return file.length();
    }


    public static String recookPath(String filePath) {
        if (filePath == null || filePath.isEmpty()) {
            return filePath;
        }
        return filePath.trim().replace("\\", "/").replace("//", "/");
    }

    public static boolean isFileEmpty(File file) {
        return !file.exists() || getFileLen(file.getAbsolutePath()) == 0;
    }
}
