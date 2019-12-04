package org.lynxz.baseimlib.actions

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import org.lynxz.baseimlib.bean.CommonResult
import org.lynxz.baseimlib.bean.ImInitPara
import org.lynxz.baseimlib.bean.SendMessageReqBean
import org.lynxz.baseimlib.network.requestScope

/**
 * IM库功能接口
 * */
interface IIMAction {
    companion object {
        // 通用参数
        const val spIm = "spIM"
        const val lastSendMsgInfo = "last_sms_content"
        const val lastSendMsgImType = "last_send_im_type"
        const val lastSendMsgTime = "last_send_sms_time"
    }

    /**
     * 初始化操作,用于设置
     * */
    fun <T : ImInitPara> init(para: T): CommonResult

    /**
     * 刷新IM基础内容信息,如重新读取配置,刷新token,部门列表,或者bot用户列表等
     * */
    fun refresh(doOnComplete: (CommonResult) -> Unit = {})

    /**
     * 发送文本消息给指定用户
     * */
    fun sendTextMessage(body: SendMessageReqBean, doOnComplete: (CommonResult) -> Unit = {})

    @ExperimentalCoroutinesApi
    fun release() {
        requestScope.cancel()
    }
}