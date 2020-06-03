package org.lynxz.forwardsms.util

import android.util.Log
import androidx.annotation.IntDef
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

/**
 * Created by lynxz on 31/01/2017.
 * V1.1
 * 默认不可打印,请通过设置 LoggerUtil.init(logLevel,Tag) 来设定可打印等级
 * 格式化打印日志,若是需要打印json,可使用 LoggerUtil.json(jsonStr),或者 LoggerUtil.json(tag,jsonStr)
 * LoggerUtil.i(tag,msg) 或 LoggerUtil.i(msg) 使用通用的tag值
 */
object LoggerUtil {
    const val LEVEL_VERBOSE = 0 //打印所有日志
    const val LEVEL_DEBUG = 1
    const val LEVEL_INFO = 2
    const val LEVEL_WARN = 3
    const val LEVEL_ERROR = 4
    const val LEVEL_NONE = 10 // 不打印任何级别日志

    @IntDef(LEVEL_VERBOSE, LEVEL_DEBUG, LEVEL_INFO, LEVEL_WARN, LEVEL_ERROR, LEVEL_NONE)
    annotation class LoggerLevel

    private const val JSON_INDENT = 2
    private const val MIN_STACK_OFFSET = 3

    private var TAG = "custom_logger"
    var logLevel = LEVEL_DEBUG // 需要打印的日志等级(大于等于该等级的日志会被打印)

    @JvmStatic
    fun init(level: Int, clazz: Class<*>) {
        TAG = clazz.simpleName
        logLevel = level
    }

    /**
     * 支持用户自己传tag，可扩展性更好
     */
    @JvmStatic
    fun init(level: Int, tag: String) {
        TAG = tag
        logLevel = level
    }

    @JvmStatic
    fun d(msg: String) {
        d(TAG, msg)
    }

    @JvmStatic
    fun i(msg: String) {
        i(TAG, msg)
    }

    @JvmStatic
    fun w(msg: String) {
        w(TAG, msg)
    }

    @JvmStatic
    fun e(msg: String) {
        e(TAG, msg)
    }

    @JvmStatic
    fun e(tag: String, msg: String) {
        if (LEVEL_ERROR >= logLevel) {
            if (msg.isNotBlank()) {
                val s = getMethodNames()
                Log.e(tag, String.format(s, msg))
                filterPersistenceLog(LEVEL_ERROR, tag, msg)
            }
        }
    }

    @JvmStatic
    fun w(tag: String, msg: String) {
        if (LEVEL_WARN >= logLevel) {
            if (msg.isNotBlank()) {
                val s = getMethodNames()
                Log.w(tag, String.format(s, msg))
                filterPersistenceLog(LEVEL_WARN, tag, msg)
            }
        }
    }

    @JvmStatic
    fun i(tag: String, msg: String) {
        if (LEVEL_INFO >= logLevel) {
            if (msg.isNotBlank()) {
                val s = getMethodNames()
                Log.i(tag, String.format(s, msg))
                filterPersistenceLog(LEVEL_INFO, tag, msg)
            }
        }
    }

    @JvmStatic
    fun d(tag: String, msg: String) {
        if (LEVEL_DEBUG >= logLevel) {
            if (msg.isNotBlank()) {
                val s = getMethodNames()
                Log.d(tag, String.format(s, msg))
                filterPersistenceLog(LEVEL_VERBOSE, tag, msg)
            }
        }
    }

    /**
     * 打印json格式化字符串,在log过滤条中使用关键字 "system.out" 来搜索查找
     * @param tag 当打印或解析出错时,打印日志用
     * */
    @JvmStatic
    fun json(tag: String, json: String) {
        var jsonT = json

        if (jsonT.isBlank()) {
            d("Empty/Null json content", tag)
            return
        }

        try {
            jsonT = jsonT.trim { it <= ' ' }
            if (jsonT.startsWith("{")) {
                val jsonObject = JSONObject(jsonT)
                var message = jsonObject.toString(JSON_INDENT)
                message = message.replace("\n".toRegex(), "\n║ ")
                val s = getMethodNames()
                println(String.format(s, message))
                return
            }
            if (jsonT.startsWith("[")) {
                val jsonArray = JSONArray(jsonT)
                var message = jsonArray.toString(JSON_INDENT)
                message = message.replace("\n".toRegex(), "\n║ ")
                val s = getMethodNames()
                println(String.format(s, message))
                return
            }
            e("Invalid Json", tag)
        } catch (e: JSONException) {
            e("Invalid Json", tag)
        }
    }

    /**
     * 获取程序执行的线程名,类名和方法名,以及行号等信息
     * */
    private fun getMethodNames(): String {
        val sElements = Thread.currentThread().stackTrace
        var stackOffset = getStackOffset(sElements)
        stackOffset++
        val builder = StringBuilder()

        //builder.append(Thread.currentThread().name).append(" ")
        builder.append(sElements[stackOffset].methodName)
            .append("(").append(sElements[stackOffset].fileName)
            .append(":").append(sElements[stackOffset].lineNumber)
            .append(") ").append("%s")
        return builder.toString()
    }

    fun getStackOffset(trace: Array<StackTraceElement>): Int {
        var i = MIN_STACK_OFFSET
        while (i < trace.size) {
            val e = trace[i]
            val name = e.className
            if (name != LoggerUtil::class.java.name) {
                return --i
            }
            i++
        }
        return -1
    }

    /**
     * 判断日志是否需要持久化
     */
    private fun filterPersistenceLog(
        @LoggerLevel logLevel: Int,
        tag: String,
        msg: String
    ) {
        LogPersistenceUtil.getInstance().filterPersistenceLog(logLevel, tag, msg);
    }
}