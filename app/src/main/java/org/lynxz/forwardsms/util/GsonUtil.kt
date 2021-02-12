package org.lynxz.forwardsms.util

import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import org.lynxz.forwardsms.util.GsonUtil.parseJson
import org.lynxz.forwardsms.util.GsonUtil.toJson
import org.lynxz.utils.log.LoggerUtil

/**
 * 使用:
 * 1. 序列化:  [toJson]
 * 2. 反序列化: [parseJson]
 */
object GsonUtil {
    private const val TAG = "GsonUtil"

    // 普通gson对象, 不带格式化功能
    private val mGson by lazy {
        MyObjTypeAdapter.assign2Gson(GsonBuilder().disableHtmlEscaping().create())
    }

    // 带格式化功能的gson对象
    private val mFormatGson by lazy {
        MyObjTypeAdapter.assign2Gson(
            GsonBuilder().disableHtmlEscaping().setPrettyPrinting().create()
        )
    }

    /**
     * 反序列化json字符串为对象
     */
    fun <T> parseJson(json: String, cls: Class<out T?>?): T? =
        if (json.isBlank()) null else {
            try {
                mGson.fromJson(json, cls)
            } catch (e: Exception) {
                LoggerUtil.e(TAG, "parseJson(String,clz) fail, srcJson:$json,errorMsg:${e.message}")
                null
            }
        }

    /**
     * 反序列化json列表数据,也可直接使用 [parseJson]
     */
    fun <T> parseJson(json: String): T? =
        if (json.isBlank()) {
            null
        } else {
            val type = object : TypeToken<T>() {}.type
            try {
                mGson.fromJson(json, type)
            } catch (e: Exception) {
                LoggerUtil.e(TAG, "parseJson(String) fail, srcJson:$json,errorMsg:${e.message}")
                null
            }
        }

//    /**
//     * 反序列化json列表数据,也可直接使用 [parseJson]
//     */
//    fun <T> parseJson(json: String, type: Type): T? =
//        if (json.isBlank()) {
//            null
//        } else {
//            try {
//                mGson?.fromJson(json, type)
//            } catch (e: Exception) {
//                LoggerUtil.w(TAG, "parseJson(String) fail, srcJson:$json,errorMsg:${e.message}")
//                null
//            }
//        }

    /**
     * 序列化输出指定对象
     * @param obj Any? 待序列化的对象
     * @param pretty Boolean true-格式化输出
     * @return String
     */
    fun toJson(obj: Any?, pretty: Boolean = false) =
        try {
            val gson = if (pretty) mFormatGson else mGson
            gson.toJson(obj) ?: ""
        } catch (e: Exception) {
            LoggerUtil.e(TAG, "toJson() pretty=$pretty fail:${e.message}")
            obj.toString()
        }
}