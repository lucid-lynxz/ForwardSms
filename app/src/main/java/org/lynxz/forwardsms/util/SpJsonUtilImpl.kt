package org.lynxz.forwardsms.util

import org.lynxz.securitysp.ISpJsonUtil

/**
 * 用于 SecuritySp 进行普通对象的解析
 * */
object SpJsonUtilImpl : ISpJsonUtil {
    override fun <T> parseJson(json: String, cls: Class<out T?>?) =
        StringUtil.parseJson<T>(json, cls)

    override fun toJson(obj: Any?) = StringUtil.toJson(obj)
}