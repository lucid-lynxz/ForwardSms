package org.lynxz.baseimlib.bean

data class CommonResult(
    var ok: Boolean = true, // 执行结果是否正常
    var detail: String = "ok", // 异常说明
    var resultObj: Any? = null // 其他执行结果对象
)