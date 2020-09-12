package org.lynxz.baseimlib

import org.lynxz.baseimlib.actions.IIMAction
import org.lynxz.baseimlib.bean.CommonResult
import org.lynxz.baseimlib.bean.SendMessageReqBean

/**
 * 管理所有的IM 模块,统一用于发送消息接口
 * 备注: doOnComplete() 目前均在io线程回调
 * */
object IMManager {

    private data class ImImplStatusBean(var iimAction: IIMAction, var enable: Boolean)

    private val imImplMap = mutableMapOf<String, ImImplStatusBean>()

    /**
     * 注册添加当前支持的im
     * */
    fun registerIm(imType: String, impl: IIMAction) {
        imImplMap[imType] = ImImplStatusBean(impl, true)
    }

    /**
     * 取消注册指定im
     * */
    fun unregisterIm(imType: String) {
        imImplMap.remove(imType)
    }

    /**
     * 是否允许指定的im平台执行操作
     * @param enable true-允许执行操作, false-禁止后续操作执行
     * */
    fun setEnable(imType: String, enable: Boolean) {
        imImplMap[imType]?.enable = enable
    }

    fun release() {
        imImplMap.clear()
    }

    /**
     * 重新加载im配置
     * */
    fun refresh(imType: String, doOnComplete: (CommonResult) -> Unit = {}) {
        val imImplStatusBean = imImplMap[imType]
        val enable = imImplStatusBean?.enable == true
        if (enable) {
            imImplStatusBean?.iimAction?.refresh(doOnComplete)
                ?: doOnComplete(CommonResult(false, "not impl for im type $imType"))
        } else {
            doOnComplete(CommonResult(false, "im $imType disable  or not impl"))
        }
    }

    /**
     * 发送文本消息
     * todo 按循序尝试发送 tg-dd-wechat,发送后记录最后发送的短信id
     * todo 下次app启动后,将新信息一并发再次尝试发送
     * */
    fun sendTextMessage(
        imType: String,
        body: SendMessageReqBean,
        doOnComplete: (CommonResult) -> Unit = {}
    ) {
        val imImplStatusBean = imImplMap[imType]
        val enable = imImplStatusBean?.enable == true
        if (enable) {
            imImplStatusBean?.iimAction?.sendTextMessage(body, doOnComplete)
                ?: doOnComplete(CommonResult(false, "not impl for im type $imType"))
        } else {
            doOnComplete(CommonResult(false, "im $imType disable or not impl"))
        }
    }
}
