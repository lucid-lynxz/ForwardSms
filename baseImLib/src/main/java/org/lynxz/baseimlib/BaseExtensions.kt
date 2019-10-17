package org.lynxz.baseimlib

import com.google.gson.Gson
import java.text.SimpleDateFormat
import java.util.*

private val mGson = Gson()
/**
 * 将当前时间戳转换为指定格式的日期字符串
 * */
fun msec2date(format: String = "yyyy-MM-dd HH:mm:ss") =
    SimpleDateFormat(format).format(Date(System.currentTimeMillis()))


/**
 * 反序列化
 */
fun <T> convert2Obj(src: String?, clz: Class<T>): T? {
    if (src.isNullOrBlank()) {
        println("convertBody sis is null ,clz is  ${clz.simpleName}")
        return null
    }

    try {
        println("${msec2date()} request body is: $src")
        return mGson.fromJson(src, clz)
    } catch (e: Exception) {
        e.printStackTrace()
        println("${msec2date()}  error occurs when convertBody :\n${e.message} ")
    }
    return null
}

fun convert2Str(obj: Any?): String? {
    return if (obj == null) "" else mGson.toJson(obj)
}