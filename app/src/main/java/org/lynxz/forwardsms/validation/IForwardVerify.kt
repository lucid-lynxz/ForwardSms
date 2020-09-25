package org.lynxz.forwardsms.validation

import org.lynxz.forwardsms.bean.SmsDetail

/**
 * 是否可转发验证接口
 * */
interface IForwardVerify {

    /**
     * 校验接口, 返回true表示符合条件
     * */
    fun verify(smsDetail: SmsDetail): Boolean
}