package org.lynxz.baseimlib.actions

/**
 * 简单配置文件读写,主要是key-value形式
 * */
interface IPropertyAction {

    /**
     * 持久化保存数据
     * @return 是否保存成功
     * */
    fun save(key: String, obj: Any): Boolean

    fun remove(key: String): Boolean

    /**
     * 从文件中读取数据
     * */
    fun <T> get(key: String, defaultValue: T?): Any?

}