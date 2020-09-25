package org.lynxz.forwardsms.validation

import org.lynxz.forwardsms.bean.SmsDetail
import org.lynxz.forwardsms.para.TimeValidationParaManager

/**
 * 验证当前是否在可转发的时间段内
 * */
object TimeVerify : IForwardVerify {
    override fun verify(smsDetail: SmsDetail) = TimeValidationParaManager.isInTimeDuration()
}

/**
 * 可转发的信息类型(包名)判定
 * */
object SrcTypeVerify : IForwardVerify {
    // 存储包名和是否需要转发, key为srcType,通常为包名, value-true/false false表示不转发
    private val srcTypeStateMap = mutableMapOf<String, Boolean>()

    /**
     * 设置指定渠道是否允许转发
     * */
    fun setMessageSrcType(srcType: String, enable: Boolean) {
        srcTypeStateMap[srcType] = enable
    }

    fun getSrcTypeState() = srcTypeStateMap

    override fun verify(smsDetail: SmsDetail) = srcTypeStateMap[smsDetail.srcType] == true
}