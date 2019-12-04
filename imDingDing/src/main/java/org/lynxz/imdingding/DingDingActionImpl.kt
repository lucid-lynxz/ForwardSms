package org.lynxz.imdingding

import kotlinx.coroutines.*
import org.lynxz.baseimlib.actions.IPropertyAction
import org.lynxz.baseimlib.actions.IIMAction
import org.lynxz.baseimlib.bean.CommonResult
import org.lynxz.baseimlib.bean.ImInitPara
import org.lynxz.baseimlib.bean.ImType
import org.lynxz.baseimlib.bean.SendMessageReqBean
import org.lynxz.baseimlib.network.requestScope
import org.lynxz.baseimlib.network.retrofit
import org.lynxz.imdingding.bean.DepartmentMemberDetailListBean
import org.lynxz.imdingding.bean.MessageResponseBean
import org.lynxz.imdingding.network.HttpManager
import org.lynxz.imdingding.para.ConstantsPara

/**
 * 钉钉相关操作
 * todo 异常处理, 超时处理, 错误重试机制等
 * */
object DingDingActionImpl : IIMAction, CoroutineScope by requestScope {


    private var propertyUtil: IPropertyAction? = null // 数据持久化工具类

    override fun <T : ImInitPara> init(para: T): CommonResult {
        val valid: Boolean
        val detail: String
        when (para) {
            is ImInitPara.DDInitPara -> {
                ConstantsPara.dd_corp_id = para.corpid
                ConstantsPara.dd_corp_secret = para.corpsecret
                ConstantsPara.dd_agent_id = para.agentId

                valid = ConstantsPara.dd_corp_id.isNotBlank()
                        && ConstantsPara.dd_corp_secret.isNotBlank()
                        && ConstantsPara.dd_agent_id.isNotBlank()

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

    override fun refresh(doOnComplete: (CommonResult) -> Unit) {
        launch {
            val result = CommonResult()
            try {
                // todo 失败重试
                // 获取accessToken
                val accessToken = HttpManager.refreshAccessTokenAsync().await()
                print(accessToken.access_token)
                ConstantsPara.accessToken = accessToken.access_token ?: ""

                // 获取部门列表
                ConstantsPara.departmentList = HttpManager.getDepartmentInfoAsync().await()

                val detailDeferrs =
                    mutableListOf<Deferred<Deferred<DepartmentMemberDetailListBean>>>()

                val departmentList = ConstantsPara.departmentList?.department

                // 获取各部门成员详情信息
                departmentList?.forEach {
                    ConstantsPara.departmentNameMap[it.id] = it.name // 获取部门id和部门名称之间的对应关系

                    val detail =
                        async { HttpManager.getDepartmentMemberDetailListAsync(it.id) }
                    detailDeferrs.add(detail)
                }


                detailDeferrs.forEachIndexed { index, deferred ->
                    val bean = deferred.await().await()
                    ConstantsPara.departmentMemberDetailMap[departmentList!![index].id] =
                        bean.userlist
                }

                println("refresh dingding finish...")
                if (ConstantsPara.departmentMemberDetailMap.size == 0) {
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
     * 发送文本消息给指定钉钉用户
     * */
    override fun sendTextMessage(
        body: SendMessageReqBean,
        doOnComplete: (CommonResult) -> Unit
    ) {
        if (ConstantsPara.accessToken.isBlank()) {
            doOnComplete(CommonResult(false, "send fail as as accessToken is empty "))
            return
        }

        retrofit<MessageResponseBean> {
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
                        it.save(IIMAction.lastSendMsgImType, ImType.DingDing)
                        it.save(IIMAction.lastSendMsgTime, System.currentTimeMillis())
                    }
                }

                doOnComplete.invoke(result)
            }

            onSuccess {
                result.detail = "${it?.errmsg}"
            }

            onFailed { error, code ->
                result.ok = false
                result.detail = "$error"
            }
        }
    }
}
