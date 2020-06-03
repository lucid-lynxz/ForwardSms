package org.lynxz.imdingding.bean

data class CommonResponse(val errcode: Long, val errMsg: String?) {
    companion object {
        const val codeTokenExpired = 40014L // token过去错误码
    }
}