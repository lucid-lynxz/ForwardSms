package org.lynxz.baseimlib.bean

import org.lynxz.baseimlib.actions.IPropertyAction

/**
 * 初始化参数
 * */
data class InitPara(
    var paraMap: Map<String, String>? = null,// 相关key定义在各具体实现模块中
    var propertyUtil: IPropertyAction? = null, // 数据持久化操作实现类
    var cxt: Any? = null // 按需传入上下文
) {

    inline fun <reified T> getProperty(key: String, defaultValue: T?): T? {
        val value = paraMap?.get(key) ?: return defaultValue
        return if (value is T) value else defaultValue
    }
}