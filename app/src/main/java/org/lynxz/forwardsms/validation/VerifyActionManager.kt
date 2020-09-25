package org.lynxz.forwardsms.validation

import org.lynxz.forwardsms.bean.SmsDetail
import org.lynxz.forwardsms.validation.VerifyActionManager.addVerifyActionIfAbsent
import org.lynxz.forwardsms.validation.VerifyActionManager.removeVerifyAction

/**
 * 是否可转发的验证条件管理器
 * 通过 [addVerifyActionIfAbsent] 和 [removeVerifyAction] 进行 添加/移除 验证器
 * */
object VerifyActionManager {
    // 其他条件
    private val verifyList = mutableListOf<IForwardVerify>()

    // 添加转发验证条件
    fun addVerifyActionIfAbsent(action: IForwardVerify): VerifyActionManager {
        if (!verifyList.contains(action)) {
            verifyList.add(action)
        }
        return this
    }

    // 移除转发验证条件
    fun removeVerifyAction(action: IForwardVerify): VerifyActionManager {
        verifyList.remove(action)
        return this
    }

    // 设置可转发的渠道(包名)信息
    fun setSrcTypeState(srcType: String, enable: Boolean) =
        SrcTypeVerify.setMessageSrcType(srcType, enable)

    // 是否所有转发过滤条件都满足
    fun isValid(smsDetail: SmsDetail): Boolean =
        verifyList.isNullOrEmpty() || verifyList.all { it.verify(smsDetail) }
}