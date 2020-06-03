package org.lynxz.forwardsms.util;

import android.app.ActivityManager;
import android.app.Application;
import android.text.TextUtils;
import android.util.Log;
import android.util.SparseArray;

import androidx.annotation.NonNull;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

/**
 * 日志持久化工具
 * 注意: 本类中的日志语句不能使用 LoggerUtil , 避免递归循环调用
 * 使用方式:
 * <pre>
 *  // 1. 在 application 中初始化相关配置
 *  LogPersistenceUtil.getInstance()
 *      .addPersistenceTag(tagXXX) // 可选, 包含指定tag时,持久化记录日志(与日志级别无关).可指定多个tag
 *      .setLogFolderPath(logPath) // 可选, 设置日志文件所在目录(若需要权限,需自行申请)
 *      .setLogLengthForAutoFlush(500) // 可选, 日志msg内容缓冲长度超过时,自动flush日志,默认每条都进行写入
 *      .setPersistenceLevel(LoggerUtil.LEVEL_WARN) // 可选,指定级别以上的日志均做持久化(与tag内容无关),默认为 warn 级别
 *      .init(application); // 必填, 使配置生效
 *
 *  // 2. 在需要记录日志的调用
 *  // 当前已自动在 LoggerUtil 中调用,请使用 LoggerUtil 即可
 *  LogPersistenceUtil.getInstance().filterPersistenceLog(logLevel, tag, msg);
 *
 *  // 3. setLogLengthForAutoFlush() 传入大于0的值时, 在适当的时候主动flush,将日志写入文件
 *  // 目前demo在 TestActivity 关闭时以及java层发生崩溃时,均自动flush
 *  LogPersistenceUtil.getInstance().flush();
 *
 *  // 4. 关闭相关writer
 *  LogPersistenceUtil.getInstance().close();
 * </pre>
 */
public class LogPersistenceUtil {
    private static final String TAG = "LogPersistenceUtil";
    private static final String LOG_SUFFIX = ".txt"; // 日志文件名后缀

    // 最终生成的日志文件所在目录路径,默认在: /data/data/{pkgName}/files/LogPersistence/
    private String logFolderPath = null;

    // 需要记录到日志文件中的tag和级别,满足其中一个即可进行持久化记录
    private static Set<String> mPersistenceTag = new HashSet<>();
    private static int mPersistenceLevel = LoggerUtil.LEVEL_WARN; // 默认warn级别以上才写入
    private static StringBuilder mPersistenceSB = new StringBuilder(); // 日志缓存

    private static final Object mLockObj = new Object();
    private boolean isInited = false;

    private static final SparseArray<String> mLogLevelNames = new SparseArray<>(); // log级别与名称的映射关系
    private SimpleDateFormat logFileSdf = new SimpleDateFormat("yyyyMMddHHmmssSSS", Locale.CHINA);
    private SimpleDateFormat logSdf = new SimpleDateFormat("HH:mm:ss.SSS", Locale.CHINA);

    private String logPath = null; // 日志所在路径
    private FileWriter logFileWriter = null;
    private long curLogMsgLength = 0;// 当前已缓存的日志长度,超过一定长度自动flush写入日志
    private long logLengthForAutoFlush = 0;// 超过指定长度,则自动flush日志,默认每条都直接写入

    private static class LogPersistenceUtilHolder {
        private static LogPersistenceUtil mInstance = new LogPersistenceUtil();
    }

    private LogPersistenceUtil() {

    }

    public static LogPersistenceUtil getInstance() {
        return LogPersistenceUtilHolder.mInstance;
    }

    public LogPersistenceUtil init(@NonNull Application application) {
        if (TextUtils.isEmpty(logFolderPath)) {
            logFolderPath = application.getFilesDir().getAbsolutePath() + "/LogPersistence/";
        }

        mLogLevelNames.put(LoggerUtil.LEVEL_VERBOSE, "V");
        mLogLevelNames.put(LoggerUtil.LEVEL_DEBUG, "D");
        mLogLevelNames.put(LoggerUtil.LEVEL_INFO, "I");
        mLogLevelNames.put(LoggerUtil.LEVEL_WARN, "W");
        mLogLevelNames.put(LoggerUtil.LEVEL_ERROR, "E");

        File logFolder = new File(logFolderPath);
        File[] files = logFolder.listFiles();

        int len = files == null ? 0 : files.length;
        if (len == 0) {
            createNewLogFile();
        } else {
            File lastLog = files[len - 1];
            Log.d(TAG, "last demoLog size: " + lastLog.length() + ",path:" + lastLog.getAbsolutePath());
            if (lastLog.length() > 1024 * 1024) { // 若文件大于1M(大概), 则自动新建一个,否则继续使用
                createNewLogFile();
            } else {
                flush();
                close();
                logPath = lastLog.getAbsolutePath();
            }
        }

        mPersistenceSB.append("\n\n").append(getDate()).append("\tLogPersistenceUtil init...")
                .append("\nisUserAMonkey:").append(ActivityManager.isUserAMonkey());
        isInited = true;
        return this;
    }


    public String getDate() {
        return logSdf.format(new Date(System.currentTimeMillis()));
    }

    public String getDate(long time, SimpleDateFormat format) {
        Date date = new Date(time);
        return format.format(date);
    }

    /**
     * 创建新日志
     */
    private void createNewLogFile() {
        flush();
        close();
        clearEmptyLogFile();

        logPath = logFolderPath + getDate(System.currentTimeMillis(), logFileSdf) + LOG_SUFFIX;
        FileUtils.createFile(logPath, false);
    }

    /**
     * 清除空白日志文件
     */
    private void clearEmptyLogFile() {
        File logFolder = new File(logFolderPath);
        File[] files = logFolder.listFiles();
        int len = files == null ? 0 : files.length;
        if (len == 0) return;
        for (File file : files) {
            if (file.length() <= 10) {
                file.delete();
            }
        }
    }

    /**
     * 设置需要进行日志持久化的日志tag
     */
    public LogPersistenceUtil addPersistenceTag(String tag) {
        if (!TextUtils.isEmpty(tag)) {
            mPersistenceTag.add(tag);
        }
        return this;
    }

    /**
     * 移除日志持久化的日志tag
     */
    public LogPersistenceUtil removePersistenceTag(String tag) {
        if (!TextUtils.isEmpty(tag)) {
            mPersistenceTag.remove(tag);
        }
        return this;
    }

    /**
     * 设置需要进行持久化的日志级别,默认为 LEVEL_WARN ,具体参看 {@link LoggerUtil}
     */
    public LogPersistenceUtil setPersistenceLevel(@LoggerUtil.LoggerLevel int logLevel) {
        mPersistenceLevel = logLevel;
        return this;
    }

    /**
     * 设置自动flush的日志长度, <=0,表示每次都flush
     */
    public LogPersistenceUtil setLogLengthForAutoFlush(long logLengthForAutoFlush) {
        this.logLengthForAutoFlush = logLengthForAutoFlush;
        return this;
    }

    /**
     * 获取日志写入前的缓冲区大小,默认为0,表示每条日志都写入
     */
    public long getLogLengthForAutoFlush() {
        return Math.max(0, logLengthForAutoFlush);
    }

    /**
     * 设置日志文件所在目录路径,默认为 amapauto20/bllog/demolog/ 下
     */
    public LogPersistenceUtil setLogFolderPath(String folderPath) {
        if (!TextUtils.isEmpty(folderPath)) {
            this.logFolderPath = folderPath.replace("\\", "/");
        }

        if (!logFolderPath.endsWith("/")) {
            logFolderPath += "/";
        }

        logFolderPath = logFolderPath.replace("//", "/");
        return this;
    }

    public boolean isInited() {
        return isInited;
    }

    /**
     * 判断需要进行日志持久化的信息并添加到缓冲stringBuilder中
     * 日志级别或者tag内容符合要求,即可持久化到日志文件
     *
     * @param logLevel 当前日志级别
     * @param tag      当前日志tag
     * @param msg      当前日志信息
     */
    public void filterPersistenceLog(@LoggerUtil.LoggerLevel int logLevel, String tag, String msg) {
        if (!isInited) {
            Log.d(TAG, "filterPersistenceLog fail as not inited");
            return;
        }

        if (logLevel >= mPersistenceLevel || mPersistenceTag.contains(tag)) {
            synchronized (mLockObj) {
                String logLevelName = mLogLevelNames.get(logLevel);
                if (logLevelName == null) {
                    logLevelName = "?";
                }

                mPersistenceSB.append(getDate()).append(" ")
                        .append(logLevelName).append(" ").append(tag).append("\t").append(msg).append("\n");


                curLogMsgLength += msg.length();
            }

            // 日志内容超过指定长度,则立即flush()写入到文件,
            if (curLogMsgLength > 0 && curLogMsgLength >= logLengthForAutoFlush) {
                flush();
            }
        }
    }

    /**
     * 将缓存的日志信息写入到文件中
     */
    public void flush() {
        synchronized (mLockObj) {
            int length = mPersistenceSB.length();

            if (length == 0) {
                return;
            }

            String msg = mPersistenceSB.toString();

            // 避免被人为删除后无法写入
            if (!FileUtils.checkFileExists(logPath)) {
                FileUtils.safetyClose(logFileWriter);
                logFileWriter = null;
                FileUtils.createFile(logPath, false);
            }

            if (logFileWriter == null) {
                try {
                    logFileWriter = new FileWriter(logPath, true);//SD卡中的路径
                } catch (IOException ignore) {
                    ignore.printStackTrace();
                }
            }

            if (logFileWriter != null) {
                try {
                    logFileWriter.write(msg);
                    logFileWriter.flush();
                    mPersistenceSB.setLength(0); // 写入成功则清空日志
                    curLogMsgLength = 0;
                } catch (Exception e) {
                    Log.e(TAG, "flush fail: " + e.getMessage());
                }
            } else {
                Log.e(TAG, "flush fail: logFileWriter is null, logPath = " + logPath);
            }
        }
    }

    public void close() {
        synchronized (mLockObj) {
            FileUtils.safetyClose(logFileWriter);
            mPersistenceSB.setLength(0);
            logFileWriter = null;
        }
    }
}
