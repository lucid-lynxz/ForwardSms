package org.lynxz.version.util

import com.google.gson.Gson
import com.google.gson.GsonBuilder

object GsonUtilSimple {
    private const val TAG = "GsonUtilSimple"

    private val mGson: Gson by lazy {
        MyObjectTypeAdapter.assign2Gson(
            GsonBuilder()
                .disableHtmlEscaping() // private数据若不打印, 影响较多,如espresso用例执行结果等,暂不处理
                // .excludeFieldsWithModifiers(Modifier.PRIVATE, Modifier.STATIC, Modifier.TRANSIENT)
                .create()
        )
    }

    /**
     * 直接序列化(不做格式缩进)
     */
    fun toJson(obj: Any?): String {
        return toJsonInternal(mGson, obj)
    }

    /**
     * 序列化输出指定对象
     */
    private fun toJsonInternal(gson: Gson, obj: Any?): String {
        return try {
            gson.toJson(obj)
        } catch (e: Exception) {
            println("toJsonInternal() fail:" + e.message)
            obj.toString()
        }
    }
}