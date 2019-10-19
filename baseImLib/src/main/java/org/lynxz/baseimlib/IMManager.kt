package org.lynxz.baseimlib

import org.lynxz.baseimlib.actions.IIMAction
import org.lynxz.baseimlib.bean.CommonResult
import org.lynxz.baseimlib.bean.ImType
import org.lynxz.baseimlib.bean.SendMessageReqBean

/**
 * 管理所有的IM 模块,统一用于发送消息接口
 * 备注: doOnComplete() 目前均在io线程回调
 * */
object IMManager {

    private val imImplMap = mutableMapOf<String, IIMAction>()

    /**
     * 注册添加当前支持的im
     * */
    fun registerIm(imType: String, impl: IIMAction) {
        imImplMap[imType] = impl
    }

    /**
     * 取消注册指定im
     * */
    fun unregisterIm(imType: String) {
        imImplMap.remove(imType)
    }

    fun release() {
        imImplMap.clear()
    }

    /**
     * 重新加载im配置
     * */
    fun refresh(imType: String, doOnComplete: (CommonResult) -> Unit = {}) {
        return imImplMap[imType]?.refresh(doOnComplete)
            ?: doOnComplete(CommonResult(false, "not impl for im type $imType"))
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
        return imImplMap[imType]?.sendTextMessage(body, doOnComplete)
            ?: doOnComplete(CommonResult(false, "not impl for im type $imType"))
    }
}
