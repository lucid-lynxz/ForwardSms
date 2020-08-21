package org.lynxz.securitysp

/**
 * 用于sp存储任意对象时,进行序列化和反序列话操作
 * */
interface ISpJsonUtil {

    /**
     * 反序列化为对象
     * */
    fun <T> parseJson(json: String, cls: Class<out T?>?): T?

    /**
     * 序列化对象
     * */
    fun toJson(obj: Any?): String?
}