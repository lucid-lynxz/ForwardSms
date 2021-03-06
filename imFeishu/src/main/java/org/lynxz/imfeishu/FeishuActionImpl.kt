package org.lynxz.imfeishu

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.lynxz.baseimlib.actions.IIMAction
import org.lynxz.baseimlib.actions.IPropertyAction
import org.lynxz.baseimlib.bean.CommonResult
import org.lynxz.baseimlib.bean.ImInitPara
import org.lynxz.baseimlib.bean.ImType
import org.lynxz.baseimlib.bean.SendMessageReqBean
import org.lynxz.baseimlib.network.requestScope
import org.lynxz.baseimlib.network.retrofit
import org.lynxz.imfeishu.bean.CommonResponse
import org.lynxz.imfeishu.bean.SendMessageResponse
import org.lynxz.imfeishu.netwrork.HttpManager
import org.lynxz.imfeishu.para.ConstantsPara

/**
 * 飞书信息发送功能实现类
 * */
object FeishuActionImpl : IIMAction, CoroutineScope by requestScope {

    private var propertyUtil: IPropertyAction? = null // 数据持久化工具类

    override fun <T : ImInitPara> init(para: T): CommonResult {
        val valid: Boolean
        val detail: String
        when (para) {
            is ImInitPara.FeiShuPara -> {
                ConstantsPara.appId = para.appId
                ConstantsPara.appSecret = para.appSecret

                valid = ConstantsPara.appId.isNotBlank()
                        && ConstantsPara.appSecret.isNotBlank()
                detail = if (valid) "数据有效" else "数据异常"
            }
            else -> {
                valid = false
                detail = "fail: initPara not instanceOf DDInitPara"
            }
        }
        propertyUtil = para.propertyUtil
        return CommonResult(valid, detail)
    }

    /**
     * 刷新钉钉token
     * 接口文档: https://ding-doc.dingtalk.com/doc#/serverapi2/eev437
     * 有效期 7200 秒
     * */
    private suspend fun refreshToken(): String {
        val accessToken = HttpManager.refreshAccessTokenAsync().await()
        print("get feishu accessToken=${accessToken.tenant_access_token}")
        return accessToken.tenant_access_token?.trim() ?: ""
    }

    override fun refresh(doOnComplete: (CommonResult) -> Unit) {
        launch {
            val result = CommonResult()
            try {
                // 获取accessToken
                for (i in 0..3) { // 最多重试次数 [0,3]
                    ConstantsPara.tenantToken = refreshToken()

                    if (!ConstantsPara.tenantToken.isNullOrBlank()) {
                        break
                    }
                    print("第 ${i + 1} 次尝试重新获取飞书token\n")
                }

                if (ConstantsPara.tenantToken.isNullOrBlank()) {
                    throw IllegalArgumentException("获取钉钉token失败,请检查")
                }

                val scope = HttpManager.getContactScope().await()
                if (scope.isSuccess()) {
                    ConstantsPara.rootDepartmentId = scope.data?.authed_departments?.get(0)
                }

                val departBean = HttpManager
                    .getDepartmentMemberDetailListAsync(ConstantsPara.rootDepartmentId ?: "")
                    .await()

                if (departBean.isSuccess()) {
                    departBean.data?.user_infos?.forEach {
                        ConstantsPara.userNameIdMap[it.name] = it.open_id
                        // 飞书手机号默认添加了 +86, 自动删除, 此处未考虑境外地区手机号问题
                        ConstantsPara.userMobileIdMap[it.mobile.replace("+86", "")] = it.open_id
                    }
                }

                println("refresh feishu finish...")
                if (ConstantsPara.userNameIdMap.isEmpty() && ConstantsPara.userMobileIdMap.isEmpty()) {
                    result.ok = false
                    result.detail = "获取部门成员列表失败,数据为空,请检查"
                }
            } catch (e: Exception) {
                e.printStackTrace()
                result.ok = false
                result.detail = "${e.message}"
            } finally {
                doOnComplete.invoke(result)
            }
        }
    }

    /**
     * 发送文本消息给指定飞书用户
     * */
    override fun sendTextMessage(
        body: SendMessageReqBean,
        doOnComplete: (CommonResult) -> Unit
    ) {
        if (ConstantsPara.tenantToken.isNullOrBlank()) {
            doOnComplete(CommonResult(false, "send fail as as accessToken is empty "))
            return
        }

        retrofit<CommonResponse<SendMessageResponse>> {
            val result = CommonResult()
            api = HttpManager.sendTextMessageAsync(body)

            onComplete { canceled ->
                if (canceled) {
                    result.ok = false
                    result.detail = "canceled"
                }

                // 保存最后发送成功的短信信息
                propertyUtil?.let {
                    if (result.ok) {
                        it.save(IIMAction.lastSendMsgInfo, body.content)
                        it.save(IIMAction.lastSendMsgImType, ImType.FeiShu)
                        it.save(IIMAction.lastSendMsgTime, System.currentTimeMillis())
                    }
                }

                doOnComplete.invoke(result)
            }

            onSuccess {
                result.detail = "${it?.msg}"
            }

            onFailed { error, code ->
                println("发送飞书消息失败(${ConstantsPara.tenantToken}): $code  $error")
                result.ok = false
                result.detail = "$error"
            }
        }
    }
}