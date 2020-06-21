package org.lynxz.imfeishu.bean

/**
 * 飞书通用response
 * */
data class CommonResponse<T>(
    val code: Long, // 返回码，非 0 表示失败
    val msg: String?, // 返回码描述
    val data: T?
) {
    // 是否成功
    fun isSuccess() = code == 0L

    companion object {
        const val codeTokenExpired = 99991663L // token过时错误码
    }
}