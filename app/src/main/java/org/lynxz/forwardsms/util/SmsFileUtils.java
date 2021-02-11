package org.lynxz.forwardsms.util;

import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.FileProvider;

import org.lynxz.utils.log.LoggerUtil;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * V1.0 文件工具类
 * version : 2
 * date: 2018.8.20
 * <p>
 * 提供了文件的创建/删除/读/写/获取长度/获取修改时间/获取文件名/获取后缀名等功能
 * <p>
 * 1. 若需要从相册选择照片,下载文件等操作,请确保在 application 中调用了
 * {@link #setFileProviderAuthority(String)} 初始化 `FileProviderAuthority` ,且
 * 传入的 `fileProviderAuthority` 值需要跟 `AndroidManifest.xml` 中设置的值一致:<br>
 * 2. 通过 {@link #installApk(Context, String)} 来安装apk
 * <p>
 * 其他:
 * fileProvider的声明
 * 1. 在 `app/src/main/res/xml/` 中添加文件: `provider_paths.xml`
 * 内容如下:
 *
 * <pre>
 * <?xml version="1.0" encoding="utf-8"?>
 * <paths>
 *      <root-path
 *         name="root"
 *         path="."/>
 *     <!--<files-path-->
 *     <!--name="files"-->
 *     <!--path="."/>-->
 *     <!--<cache-path-->
 *     <!--name="cache"-->
 *     <!--path="."/>-->
 *     <!--<external-path-->
 *     <!--name="external"-->
 *     <!--path="."/>-->
 *     <!--<external-files-path-->
 *     <!--name="external_file_path"-->
 *     <!--path="."/>-->
 *     <!--<external-cache-path-->
 *     <!--name="external_cache_path"-->
 *     <!--path="."/>-->
 *     <external-path
 *         name="external_files"
 *         path="."/>
 *     <external-files-path
 *         name="external_storage_directory"
 *         path="."/>
 * </paths>
 * </pre>
 * <p>
 * <p>
 * 2. 修改 app/build.gradle 默认设置
 * <pre>
 *     // 修改 app/build.gradle后rebuild项目
 *     android{
 *         defaultConfig{
 *              String providerAuthority = applicationId + ".fileProvider"
 *              buildConfigField("String", "fileProviderAuthority", "\"" + providerAuthority + "\"")
 *              manifestPlaceholders = [app_provider_authority: providerAuthority]
 *         }
 *     }
 *
 *     // application 类的 onCreate() 方法中调用
 *     FileUtils.setFileProviderAuthority(BuildConfig.fileProviderAuthority);
 * </pre>
 * <p>
 * 3. 在 `AndroidManifest.xml` 中添加如下内容:
 * <pre>
 * <manifest>
 *     <uses-permission android:name="android.permission.INTERNET"/>
 *     <uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE"/>
 *     <uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE"/>
 *     <uses-permission android:name="android.permission.REQUEST_INSTALL_PACKAGES"/>
 *
 *     <aplication>
 *         <provider
 *             android:name="android.support.v4.content.FileProvider"
 *             android:authorities="${app_provider_authority}"
 *             android:exported="false"
 *             android:grantUriPermissions="true">
 *             <meta-data
 *                 android:name="android.support.FILE_PROVIDER_PATHS"
 *                 android:resource="@xml/provider_paths"/>
 *         </provider>
 *     </aplication>
 * </manifest>
 * </pre>
 */
public class SmsFileUtils {
    private static final String TAG = "FileUtils";
    public static final String CACHE = "cache";
    public static final String IMG = "img";
    public static final String ROOT = "custom_tool";

    private static String fileProviderAuthority;

    public static void setFileProviderAuthority(String provider) {
        fileProviderAuthority = provider;
    }

    public static String getFileProviderAuthority() {
        return fileProviderAuthority;
    }

    /**
     * 获取图片的缓存的路径
     */
    public static File getImgDir(Context context) {
        return getDir(context, IMG);
    }

    /**
     * 获取缓存路径
     */
    public static File getCacheDir(Context context) {
        return getDir(context, CACHE);
    }

    public static File getDir(Context context, String cache) {
        StringBuilder path = new StringBuilder();
        if (isSDAvailable()) {
            path.append(Environment.getExternalStorageDirectory().getAbsolutePath());
            path.append(File.separator);// '/'
            path.append(ROOT);// /mnt/sdcard/Uplusgo
            path.append(File.separator);
            path.append(cache);// /mnt/sdcard/Uplusgo/cache

        } else {
            File filesDir = context.getCacheDir(); // cache getFileDir file
            path.append(filesDir.getAbsolutePath());// /mData/mData/com.soundbus.uplusgo/cache
            path.append(File.separator);/// mData/mData/com.isoundbus.uplusgo/cache/
            path.append(cache);/// mData/mData/com.soundbus.uplusgo/cache/cache
        }
        File file = new File(path.toString());
        if (!file.exists() || !file.isDirectory()) {
            file.mkdirs();// 创建文件夹
        }
        return file;
    }

    private static boolean isSDAvailable() {
        return Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED);
    }

    public static FileInputStream openInputStream(File file) throws IOException {
        if (file.exists()) {
            if (file.isDirectory()) {
                throw new IOException("File '" + file + "' exists but is a directory");
            }
            if (!file.canRead()) {
                throw new IOException("File '" + file + "' cannot be read");
            }
        } else {
            throw new FileNotFoundException("File '" + file + "' does not exist");
        }
        return new FileInputStream(file);
    }

    public static FileOutputStream openOutputStream(File file) throws IOException {
        return openOutputStream(file, false);
    }

    public static FileOutputStream openOutputStream(File file, boolean append) throws IOException {
        if (file.exists()) {
            if (file.isDirectory()) {
                throw new IOException("File '" + file + "' exists but is a directory");
            }
            if (!file.canWrite()) {
                throw new IOException("File '" + file + "' cannot be written to");
            }
        } else {
            File parent = file.getParentFile();
            if (parent != null) {
                if (!parent.mkdirs() && !parent.isDirectory()) {
                    throw new IOException("Directory '" + parent + "' could not be created");
                }
            }
        }
        return new FileOutputStream(file, append);
    }

    public static void closeQuietly(InputStream input) {
        closeQuietly((Closeable) input);
    }

    public static void closeQuietly(Closeable closeable) {
        try {
            if (closeable != null) {
                closeable.close();
            }
        } catch (IOException ioe) {
            // ignore
        }
    }

    /**
     * 安装本机apk文件 注意,请确保: 1. {@link #setFileProviderAuthority(String)} 已经设置过了 2. 在
     * `AndroidManifest.xml` 中添加了权限: `<uses-permission android:name=
     * "android.permission.REQUEST_INSTALL_PACKAGES"/>`
     *
     * @param filePath 本地apk文件路径
     */
    public static void installApk(Context context, @Nullable String filePath) {
        if (TextUtils.isEmpty(filePath)) {
            return;
        }
        File file = new File(filePath);
        Intent intent = new Intent();
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.setAction(Intent.ACTION_VIEW);
        Uri uri;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            uri = FileProvider.getUriForFile(context, fileProviderAuthority, file);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        } else {
            uri = Uri.fromFile(file);
        }
        intent.setDataAndType(uri, "application/vnd.android.package-archive");
        context.startActivity(intent);
    }

    /**
     * 读取指定路径文件的原始字节信息
     */
    @Nullable
    public static byte[] File2byte(String filePath) {
        if (TextUtils.isEmpty(filePath)) {
            return null;
        }
        return File2byte(new File(filePath));
    }

    @Nullable
    public static byte[] File2byte(File file) {
        if (!file.exists()) {
            return null;
        }

        byte[] buffer = null;
        FileInputStream fis = null;
        ByteArrayOutputStream bos = null;
        try {
            fis = new FileInputStream(file);
            bos = new ByteArrayOutputStream();
            byte[] b = new byte[1024];
            int n;
            while ((n = fis.read(b)) != -1) {
                bos.write(b, 0, n);
            }
            buffer = bos.toByteArray();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            safetyClose(fis);
            safetyClose(bos);
        }
        LoggerUtil.d(TAG, "File2byte: path = " + file);
        return buffer;
    }

    /**
     * 检查 文件 / 文件夹 是否存在
     *
     * @param filepath 文件绝对路径
     */
    public static boolean checkFileExists(String filepath) {
        return new File(filepath).exists();
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
     * 若文件存在,但非目录,则删除重建
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

        if (targetFile.exists()) { // 存在同名文件
            boolean isDir = targetFile.isDirectory();
            if (!isDir) { // 非目录,删除以便创建目录
                boolean result = targetFile.delete();
                LoggerUtil.d(TAG, "dirPath:" + targetFile.getAbsolutePath() + " is a file, delete it, result=" + result);
            } else if (forceRecreate) { // 强制删除目录
                deleteDir(targetFile);
            } else { // 目录存在
                return true;
            }
        }

        return targetFile.mkdirs();
    }

    /**
     * 创建文件
     *
     * @param filepath      路径
     * @param forceRecreate 若目标文件存在,是否要删除然后新建
     * @return 创建文件结果
     */
    public static boolean createFile(String filepath, boolean forceRecreate) {
        return createFile(new File(filepath), forceRecreate);
    }

    /**
     * 创建文件
     * 自动创建中间父目录
     *
     * @param forceRecreate 若目标文件存在,是否要删除然后新建
     */
    public static boolean createFile(@NonNull File file, boolean forceRecreate) {
        if (file.exists()) {
            if (forceRecreate) {
                deleteFile(file);
            } else {
                return true;
            }
        }

        boolean result = false;
        try {
            createDIR(file.getParent());
            result = file.createNewFile();
        } catch (IOException e) {
            e.printStackTrace();
        }
        LoggerUtil.d(TAG, "createFile " + file.getAbsolutePath() + " , result = " + result);
        return result;
    }

    /**
     * 删除文件或者目录
     *
     * @return true-目标文件不存在(包括原本就不存在以及删除成功两种情况)
     * false-目标文件仍存在
     */
    public static boolean deleteFile(@NonNull String path) {
        return deleteFile(new File(path));
    }

    /**
     * 删除文件或者目录
     */
    public static boolean deleteFile(@NonNull File file) {
        if (!file.exists()) {
            return true;
        }

        if (file.isFile()) {
            return file.delete();
        } else {
            return deleteDir(file);
        }
    }

    /**
     * 删除目录
     */
    public static boolean deleteDir(String pPath) {
        return deleteDir(new File(pPath));
    }

    /**
     * 删除指定目录
     * 若存在同名非目录文件,则不处理
     */
    public static boolean deleteDir(File dir) {
        if (dir == null || !dir.exists() || !dir.isDirectory())
            return true;
        for (File file : dir.listFiles()) {
            if (file.isFile()) {// 删除所有文件
                file.delete();
            } else if (file.isDirectory()) { // 递归删除子目录
                deleteDir(file);
            }
        }
        return dir.delete();// 删除空目录本身
    }

    /**
     * 写入到文件
     *
     * @param msg            待写入的内容
     * @param targetFilePath 文件绝对路径
     * @param append         是否是追加模式
     * @return 是否写入成功
     */
    public static boolean writeToFile(String msg, String targetFilePath, boolean append) {
        if (TextUtils.isEmpty(targetFilePath)) {
            return false;
        }

        createFile(targetFilePath, false);
        FileWriter fileWriter = null;
        BufferedWriter bufferedWriter = null;
        try {
            File file = new File(targetFilePath);
            fileWriter = new FileWriter(file, append);
            bufferedWriter = new BufferedWriter(fileWriter);
            bufferedWriter.write(msg);
            bufferedWriter.flush();
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        } finally {
            safetyClose(bufferedWriter);
            safetyClose(fileWriter);
        }
    }

    /**
     * 存储二进制文件
     *
     * @param byteArr        待写入的byte数组
     * @param targetFilePath 要写入的文件绝对路径
     * @param append         true-追加 false-覆盖原文件内容
     * @return 是否写入成功
     */
    public static boolean writeToFile(@NonNull byte[] byteArr, @NonNull String targetFilePath, boolean append) {
        if (TextUtils.isEmpty(targetFilePath)) {
            return false;
        }

        createFile(targetFilePath, false);
        DataOutputStream dataOutputStream = null;
        try {
            dataOutputStream = new DataOutputStream(new FileOutputStream(targetFilePath, append));
            for (byte b : byteArr) {
                dataOutputStream.write(b);
            }
            dataOutputStream.flush();
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        } finally {
            safetyClose(dataOutputStream);
        }
    }

    /**
     * 保存图片到文件
     *
     * @param targetFilePath 要保存的绝对路径
     */
    public static void saveImage(Bitmap bitmap, String targetFilePath) {
        if (bitmap == null || TextUtils.isEmpty(targetFilePath)) {
            LoggerUtil.e(TAG, "保存图片失败,请检查参数后再试");
            return;
        }

        FileOutputStream out = null;
        try {
            File file = new File(targetFilePath);
            createFile(file, false);
            out = new FileOutputStream(file);

            Bitmap.CompressFormat format =
                    Bitmap.Config.ARGB_8888 == bitmap.getConfig() ? Bitmap.CompressFormat.PNG : Bitmap.CompressFormat.JPEG;
            bitmap.compress(format, 100, out);
            out.flush();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            safetyClose(out);
        }
    }


    /**
     * 参考 {@link #containAllInfo(File, String...)}
     */
    public static boolean containAllInfo(@NonNull String filePath, @Nullable String... expectText) {
        return containAllInfo(new File(filePath));
    }

    /**
     * 指定文件的内容需要包含所有expectText才算匹配
     * 参考 {@link #containInfo(File, boolean, String...)}
     */
    public static boolean containAllInfo(@Nullable File file, @Nullable String... expectText) {
        return containInfo(file, true, expectText);
    }

    /**
     * 验证文件是否存在,并且包含特定文本
     *
     * @param file       文件
     * @param matchAll   true-所有expectText都需匹配才算ok, false-只需匹配 expectText 的任意一个即算ok
     * @param expectText 需要包含的文本, 若为空,则表示仅判断文件是否存在
     * @return true asset文件存在并包含给定的文本, false-asset文件不存在或不包含给定的文本
     */
    public static boolean containInfo(@Nullable File file, boolean matchAll, @Nullable String... expectText) {

        if (file == null || !file.exists()) {
            return false;
        }

        int expectArrLength = expectText == null ? 0 : expectText.length;
        boolean[] resultArr = new boolean[expectArrLength];
        boolean finalResult = false;
        InputStream inputStream = null;
        try {
            inputStream = new FileInputStream(file);

            if (expectArrLength == 0) { // 无异常则文件存在,直接返回
                return true;
            }

            byte[] buff = new byte[1024];
            int len = inputStream.read(buff);
            while (len >= 0) {
                String line = new String(buff);
                finalResult = true;

                for (int i = 0; i < expectArrLength; i++) {
                    if (!resultArr[i]) {
                        if (line.contains(expectText[i])) {
                            resultArr[i] = true;

                            // 只需匹配其中任意一个字符串,则退出循环, 匹配成功
                            if (!matchAll) {
                                finalResult = true;
                                break;
                            }
                        }
                    }
                    finalResult = finalResult && resultArr[i];
                }

                if (finalResult) {
                    break;
                }
                len = inputStream.read(buff);
            }

            return finalResult;
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            safetyClose(inputStream);
        }
        return false;
    }

    /**
     * 按行读取文件内容
     * 参考 {@link #readAllLine(File)}
     */
    @NonNull
    public static ArrayList<String> readAllLine(@Nullable String filePath) {
        if (TextUtils.isEmpty(filePath)) {
            return new ArrayList<>();
        }
        return readAllLine(new File(filePath));
    }


    /**
     * 按行读取指定文件的所有内容
     * 若文件不存在,则返回空list
     */
    @NonNull
    public static ArrayList<String> readAllLine(@Nullable File file) {
        ArrayList<String> contentList = new ArrayList<>();
        if (file == null || !file.exists()) {
            return contentList;
        }

        FileReader fr = null;
        BufferedReader bfr = null;
        try {
            fr = new FileReader(file);
            bfr = new BufferedReader(fr);

            String line = bfr.readLine();
            while (line != null) {
                contentList.add(line);
                line = bfr.readLine();
            }

            return contentList;
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            safetyClose(bfr);
            safetyClose(fr);
        }
        return contentList;
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
    public static boolean rename(@NonNull String oriFilePath, @NonNull String destFilePath) {
        File srcFile = new File(oriFilePath);
        if (!srcFile.exists()) {
            LoggerUtil.e(TAG, "rename fail as " + oriFilePath + " not exist");
            return false;
        }

        File dest = new File(destFilePath);
        dest.getParentFile().mkdirs();
        return srcFile.renameTo(dest);
    }

    public static long getLastModified(String filePath) {
        return getLastModified(filePath, false).get(filePath);
    }

    /**
     * 获取指定文件的字节大小, 若文件不存在则返回0
     */
    public static long getFileLen(String filePath) {
        if (TextUtils.isEmpty(filePath)) {
            return 0;
        }
        File file = new File(filePath);
        return file.length();
    }

    /**
     * 获取文件名, 包括扩展名, 如: a.9.png
     * 以分隔符"/",切分得到最后一部分
     *
     * @param filePath 文件路径
     */
    @NonNull
    public static String getFileName(String filePath) {
        if (TextUtils.isEmpty(filePath)) {
            return "";
        }

        filePath = filePath.replace("\\", "/"); // 兼容windows路径格式,避免误传
        String[] arr = filePath.split("/");
        int len = arr.length;
        return arr[len - 1];
    }

    /**
     * @param fileName 文件名或者路径
     * @return 文件扩展名(不包括点.), 若是点9文件,则返回 "9.png"
     */
    @NonNull
    public static String getFileExt(String fileName) {
        String fileExt = "";
        if (TextUtils.isEmpty(fileName)) {
            return fileExt;
        }

        fileName = fileName.toLowerCase();
        String[] split = fileName.split("\\.");
        int len = split.length;
        if (len >= 2) {
            fileExt = split[len - 1];
        }

        if (len >= 3) { // 可能是点9文件, 如 .9.png, .9.avsg 等
            String subFileExt = split[len - 2];
            if ("9".equals(subFileExt)) {
                fileExt = "9." + fileExt;
            }
        }
        return fileExt;
    }

    /**
     * 获取指定文件及其子文件(若存在)的修改时间
     *
     * @param getSubFileModifiedTime 若有子文件,之后返回子文件修改时间(目录有效) true-返回目录子文件修改时间 false-返回指定文件(目录)修改时间
     */
    public static HashMap<String, Long> getLastModified(String filePath, boolean getSubFileModifiedTime) {
        HashMap<String, Long> ts = new HashMap<>();
        if (TextUtils.isEmpty(filePath)) {
            ts.put(filePath, 0L);
            return ts;
        }

        File file = new File(filePath);
        if (file.exists()) {
            ts.put(filePath, file.lastModified());

            if (getSubFileModifiedTime) { // 是否获取子文件时间
                File[] files = file.listFiles(); // 非目录时返回null
                int length = files == null ? 0 : files.length;

                if (length >= 1) {
                    for (int i = 0; i < length; i++) {
                        File subFile = files[i];
                        ts.put(subFile.getAbsolutePath(), subFile.lastModified());
                    }
                }
            }
        }
        return ts;
    }

    /**
     * 列出指定路径目录的所有子文件列表(只包含一级子文件)
     * 若所给路径并未表示目录, 则返回空数据
     *
     * @param folderPath 目录路径
     */
    @Nullable
    public static File[] listSubFiles(String folderPath) {
        if (TextUtils.isEmpty(folderPath)) return null;

        File folder = new File(folderPath);
        boolean isDir = folder.exists() && folder.isDirectory();
        if (!isDir) return null;

        return folder.listFiles();
    }

    /**
     * 根据图片文件名,提取文件的原始字节数据(不进行bitmap解析)
     * 参考 {@link #getResDrawableRawBytes(Application, String, int)}
     */
    @Nullable
    public static byte[] getResDrawableRawBytes(Application application, String imageName) {
        return getResDrawableRawBytes(application, imageName, 0);
    }

    /**
     * 根据图片文件名或者文件id,提取文件的原始字节数据(不进行bitmap解析)
     * 若 resId 不等于0,则直接根据resId进行文件读取
     * 若 resId 等于0,则根据文件名称解析得到 resId, 再进行文件读取
     *
     * @param imageName 文件名, 如: a.png , 则表示 res/drawable-xxx/a.png
     * @param resID     文件资源id, 0 表示无效
     */
    @Nullable
    public static byte[] getResDrawableRawBytes(Application application, String imageName, int resID) {
        String fileExt = SmsFileUtils.getFileExt(imageName);
        if (!TextUtils.isEmpty(fileExt)) { // 若带有扩展名,则需要先将扩展名去掉
            imageName = imageName.replace("." + fileExt, "");
        }

        Resources resources = application.getResources();
        if (resID == 0) {
            resID = resources.getIdentifier(imageName, "drawable", application.getPackageName());
        }

        byte[] buffer = null;
        InputStream fis = null;
        ByteArrayOutputStream bos = null;
        try {
            fis = resources.openRawResource(resID);
            bos = new ByteArrayOutputStream();
            byte[] b = new byte[1024];
            int readLen;
            while ((readLen = fis.read(b)) != -1) {
                bos.write(b, 0, readLen);
            }
            buffer = bos.toByteArray();
        } catch (Exception e) { // IOException不够, openRawResource() 可能抛出 NotFoundException
            e.printStackTrace();
            LoggerUtil.w(TAG, "getResDrawableRawBytes fail:" + e.getMessage());
        } finally {
            safetyClose(fis);
            safetyClose(bos);
        }
        return buffer;
    }

    /**
     * 根据图片文件名,提取图片文件bitmap(系统会自动缩放)
     *
     * @param imageName 文件名, 如: a.png , 则表示 res/drawable-xxx/a.png
     */
    @Nullable
    public static Bitmap getResDrawableBitmap(Application application, String imageName) {
//        if (imageName.contains(".9")) imageName = imageName.replaceAll(".9", "");
        String fileExt = SmsFileUtils.getFileExt(imageName);
        if (!TextUtils.isEmpty(fileExt)) { // 若带有扩展名,则需要先将扩展名去掉
            imageName = imageName.replace("." + fileExt, "");
        }

        Resources resources = application.getResources();
        int resID = resources.getIdentifier(imageName, "drawable", application.getPackageName());
        return BitmapFactory.decodeResource(resources, resID);
    }

    /**
     * 根据图片文件名,提取图片文件的内容字节数组(解析成bitmap,再提取bitmap的字节内容)
     * 注意: 读取到的并非原始文件的二进制数据, 系统会根据当前设备参数进行缩放处理
     * 若有需要获取原始文件字节数据,请调用 {@link #getResDrawableRawBytes(Application, String)}
     *
     * @param imageName 文件名, 如: a.png , 则表示 res/drawable-xxx/a.png
     */
    @Nullable
    public static byte[] getResDrawableBitmapBytes(Application application, String imageName) {
        Bitmap bitmap = getResDrawableBitmap(application, imageName);
        if (bitmap == null) {
            return null;
        }
        ByteBuffer dataBuffer = ByteBuffer.allocate(bitmap.getByteCount());
        bitmap.copyPixelsToBuffer(dataBuffer);
        byte[] bytes = dataBuffer.array();
        bitmap.recycle();
        return bytes;
    }

    /**
     * 复制 res/drawablexx/xx.png 到指定目录
     *
     * @param imageName      图片名称,若为包括扩展名, 则默认追加png扩展名
     * @param destFolderPath 要复制到的目标目录
     * @param forceCopy      true-即使存在也要再复制一遍, false-若最终文件已存在则直接返回
     * @return 最终文件路径, 若复制失败,则返回空
     */
    @NonNull
    public static String copyResDrawableTo(Application application, String imageName,
                                           String destFolderPath, boolean forceCopy) {
        String fileExt = getFileExt(imageName); // 文件扩展名, 不包括点.
        if (TextUtils.isEmpty(fileExt)) {
            fileExt = "png";
        } else {
            int len = imageName.length();
            imageName = imageName.substring(0, len - fileExt.length() - 1); // 点符号占1位, 要扣掉
        }

        // 按需创建目标目录
        createDIR(destFolderPath);

        // 最终文件路径
        String destFilePath = (destFolderPath + "/" + imageName + "." + fileExt).replace("//", "/");
        File destFile = new File(destFilePath);

        // 若目标文件已存在,且不强制复制,则直接返回
        if (destFile.exists() && !forceCopy) {
            return destFilePath;
        }

        byte[] rawBytes = getResDrawableRawBytes(application, imageName);
        int len = rawBytes == null ? 0 : rawBytes.length;
        if (len == 0) {
            return "";
        }

        writeToFile(rawBytes, destFilePath, false);
        return destFilePath;
    }
}