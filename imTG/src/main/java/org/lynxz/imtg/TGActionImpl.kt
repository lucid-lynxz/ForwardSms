package org.lynxz.imtg

import kotlinx.coroutines.*
import org.lynxz.baseimlib.actions.IIMAction
import org.lynxz.baseimlib.actions.IPropertyAction
import org.lynxz.baseimlib.bean.CommonResult
import org.lynxz.baseimlib.bean.ImInitPara
import org.lynxz.baseimlib.bean.SendMessageReqBean
import org.lynxz.baseimlib.convert2Obj
import org.lynxz.baseimlib.requestScope
import org.lynxz.baseimlib.retrofit
import org.lynxz.imtg.bean.TgSendMessageReqBean
import org.lynxz.imtg.bean.TgSendMessageRespBean
import org.lynxz.imtg.network.HttpManager
import org.lynxz.imtg.para.ConstantsPara
import org.lynxz.imtg.para.TGKeyNames
import java.lang.Exception

/**
 * telegram信息发送功能实现类
 * */
object TGActionImpl : IIMAction, CoroutineScope by requestScope {

    private var propertyUtil: IPropertyAction? = null // 数据持久化工具类

    override fun <T : ImInitPara> init(para: T): CommonResult {

        val valid: Boolean
        val detail: String
        when (para) {
            is ImInitPara.TGInitPara -> {
                ConstantsPara.botToken = para.botToken
                ConstantsPara.defaultUserName = para.defaultUserName

                valid = para.botToken.isNotBlank()
                detail = if (valid) "ok: 数据有效" else "fail:botToken数据异常"
            }
            else -> {
                valid = false
                detail = "fail: initPara not instanceOf TGInitPara"
            }
        }

        // 尝试从文件缓存中提取聊天chat_id列表
        propertyUtil = para.propertyUtil
        val cacheStr = propertyUtil?.get(TGKeyNames.chatIdMap, "") as? String?
        val cacheMap = convert2Obj(cacheStr, MutableMap::class.java)
        cacheMap?.forEach {
            if (it.value is Number) {
                val chatId = (it.value as Number).toLong()
                ConstantsPara.chatInfoMap[it.key as String] = chatId
            }
        }

        return CommonResult(valid, detail)
    }

    override fun refresh(doOnComplete: (CommonResult) -> Unit) {
        launch {
            val result = CommonResult()
            try {
                val bean = HttpManager.getTgUpdatesAsync().await()
                result.ok = bean.ok

                if (bean.ok) {
                    val chatMap = mutableMapOf<String, Long>()

                    bean.result?.filter {
                        // 只取私聊,非channel/group机器人
                        it.message?.chat?.type == "private"
                    }?.forEach {
                        it.message?.chat?.let { chat ->
                            // userName可能为空,缓存userName和firstName,减少客户端对接成本
                            val firstName = chat.first_name
                            val userName = chat.username

                            if (!firstName.isNullOrBlank()) {
                                chatMap["${ConstantsPara.botToken}_$firstName"] = chat.id
                            }

                            if (!userName.isNullOrBlank()) {
                                chatMap["${ConstantsPara.botToken}_$userName"] = chat.id
                            }
                        }
                    }

                    // 由于getUpdates数据有时效,超过则不返回,因此优先使用缓存的chaId, 非空才更新
                    if (chatMap.isNotEmpty()) {
                        ConstantsPara.chatInfoMap = chatMap
                        // 缓存cha_id列表到文件
                        propertyUtil?.save(TGKeyNames.chatIdMap, chatMap)
                    }
                }
                result.detail = "chatId size: ${ConstantsPara.chatInfoMap.size}"
            } catch (e: Exception) {
                e.printStackTrace()
                result.ok = false
                result.detail = "${e.message}"
            } finally {
                doOnComplete.invoke(result)
            }
        }
    }

    override fun sendTextMessage(body: SendMessageReqBean, doOnComplete: (CommonResult) -> Unit) {
        val result = CommonResult()


        if (body.name.isNullOrBlank()) {
            body.name = ConstantsPara.defaultUserName
        }

        if (body.tgBotToken.isBlank()) {
            body.tgBotToken = ConstantsPara.botToken
        }

        val key = "${body.tgBotToken}_${body.name}"
        val doOnInnerComplete: (CommonResult) -> Unit = {
            retrofit<TgSendMessageRespBean> {
                val chatId = ConstantsPara.chatInfoMap[key]
                api = HttpManager.sendTextMessageAsync(
                    TgSendMessageReqBean(chatId, body.content),
                    body.tgBotToken
                )
                onComplete { canceled ->
                    if (canceled) {
                        result.ok = false
                        result.detail = "canceled"
                    }
                    doOnComplete.invoke(result)
                }

                onSuccess {
                    val ok = it?.ok ?: false
                    if (!ok) {
                        result.ok = false
                        result.detail = "${it?.description}"
                    }
                }

                onFailed { error, code ->
                    result.ok = false
                    result.detail = "$error"
                }
            }
        }

        // 本地找不到chatId,尝试刷新一次
        if (ConstantsPara.chatInfoMap[key] == null) {
            refresh(doOnInnerComplete)
        } else {
            doOnInnerComplete.invoke(result)
        }
    }
}
