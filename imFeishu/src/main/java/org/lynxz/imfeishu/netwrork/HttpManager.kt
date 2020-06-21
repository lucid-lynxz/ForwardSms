package org.lynxz.imfeishu.netwrork

import com.jakewharton.retrofit2.adapter.kotlin.coroutines.CoroutineCallAdapterFactory
import kotlinx.coroutines.*
import okhttp3.Interceptor
import okhttp3.Response
import org.lynxz.baseimlib.bean.SendMessageReqBean
import org.lynxz.baseimlib.msec2date
import org.lynxz.baseimlib.network.BaseOkhttpGenerator
import org.lynxz.baseimlib.network.interceptor.RetryInterceptor
import org.lynxz.imfeishu.bean.*
import org.lynxz.imfeishu.para.ConstantsPara
import org.lynxz.imfeishu.para.FeishuKeyNames
import org.lynxz.imfeishu.para.MessageType
import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory


/**
 * Created by lynxz on 25/08/2017.
 * 网络访问具体处理类
 */
object HttpManager {
    // 给请求添加统一的head参数
    private val tenantTokenInterceptor = Interceptor { chain ->
        var oriRequest =
            chain.request()
                .newBuilder().also {
                    if (!ConstantsPara.tenantToken.isNullOrBlank()) {
                        it.header(
                            FeishuKeyNames.HEAD_AUTHORIZATION,
                            "Bearer ${ConstantsPara.tenantToken}"
                        )
                    }
                }
                .build()

        var oriResponse = chain.proceed(chain.request())

        // todo 若token不合法,报错: xxx
        if (isTokenExpired(oriResponse)) {
            runBlocking {
                val tenantTokenBean = refreshAccessTokenAsync().await()
                if (!tenantTokenBean.tenant_access_token.isNullOrBlank()) {
                    ConstantsPara.tenantToken = tenantTokenBean.tenant_access_token ?: ""

                    // token刷新后尝试重新请求一次
                    oriRequest = oriRequest.newBuilder()
                        .header(
                            FeishuKeyNames.HEAD_AUTHORIZATION,
                            "Bearer ${ConstantsPara.tenantToken}"
                        )
                        .build()

                    oriResponse = chain.proceed(chain.request())
                }
            }
        }

        oriResponse
    }

    /**
     * 判断token是否过期
     * */
    private fun isTokenExpired(response: Response): Boolean {
        return false // todo 后续补充判定
//        // 若token不合法,报错: {"errcode":40014,"errmsg":"不合法的access_token"}
//        val oriRespBody = response.body
//        val source = oriRespBody?.source()
//        source?.request(Long.MAX_VALUE)
//        val buffer = source?.buffer
//        val contentType = oriRespBody?.contentType()
//        val charset = contentType?.charset() ?: Charset.forName("UTF-8")
//        val respStr = buffer?.clone()?.readString(charset)
//        val commonResp = convert2Obj(respStr, CommonResponse::class.java)
//        return commonResp?.errcode == CommonResponse.codeTokenExpired
    }


    private val feishuRetrofit: Retrofit = Retrofit.Builder()
        .client(
            BaseOkhttpGenerator()
                .clientBuilder.apply {
                    addInterceptor(tenantTokenInterceptor)
                    addInterceptor(RetryInterceptor())
                    retryOnConnectionFailure(false)
                }
                .build()
        )
        .baseUrl(ConstantsPara.SERVER_URL)
        .addConverterFactory(GsonConverterFactory.create())
        .addCallAdapterFactory(CoroutineCallAdapterFactory())
        .build()

    private val apiService: ApiService = feishuRetrofit.create(ApiService::class.java)


    /**
     * 刷新accessToken
     * */
    fun refreshAccessTokenAsync(): Deferred<TenantTokenBean> {
        return apiService.getTenantToken(ConstantsPara.appId, ConstantsPara.appSecret)
    }


    /**
     * 获取通讯录授权范围,主要是获取根部门id
     * */
    fun getContactScope(): Deferred<CommonResponse<GetContactScopeResponse>> {
        return apiService.getContactScope()
    }

    /**
     * [获取部门用户详情](https://open.feishu.cn/document/ukTMukTMukTM/uYzN3QjL2czN04iN3cDN)
     * 返回当前部门中的员工详细信息列表,包含名字和手机号等
     * 此处未做分页,最多100个员工
     * */
    fun getDepartmentMemberDetailListAsync(
        departmentId: String  // 部门id,默认为跟部门
    ): Deferred<CommonResponse<GetDepartmentMemberListResponse>> {
        return apiService.getDepartmentMemberDetailList(departmentId)
    }

    /**
     * 向指定用户发送文本内容
     * @param oriBean HMI传递过来的抽象后的消息体
     * 需要转换为飞书对应的requestBody
     * */
    fun sendTextMessageAsync(oriBean: SendMessageReqBean): Call<CommonResponse<SendMessageResponse>> {
        // 要发送的消息body


        val messageBean = MessageTextBean().apply {
            /*
                val open_id: String, // 给用户发私聊消息，只需要填 open_id、email、user_id 中的一个即可，向群里发消息使用群的 chat_id
    val content: Content, // 息的内容
    val msg_type: String = MessageType.RICH_TEXT, // 消息类型,参考 MessageType 类,富文本固定为: post
    val root_id: String? = "" // 可选, 如果需要回复某条消息，填对应消息的消息 ID
            * */
            // todo 通过 name/mobile 查询 openId

            // 拼接消息内容
            // 飞书暂时无此需要: 按需在消息体后面添加时间戳,避免重复消息被拒发送
//            oriBean.content.apply {
//                if (!contains("服务器时间"))
//                    "$this\n服务器时间: ${msec2date()}"
//            }
            val contentRichTextList = listOf(mutableListOf(ContentX(text = oriBean.content)))
            content = Content().apply {
                post = Post(zh_cn = ZhCn(contentRichTextList))
            }
        }


//        val messageBean = SendMessageReqBean().apply {
//            agentid = ConstantsPara.dd_agent_id
//            msgtype = MessageType.RICH_TEXT
//
//            // 自动添加时间信息,避免被钉钉服务器拦截
//            val message = body.content
//            text = MessageTextBean.TextBean().apply {
//                content = message
////                content =
////                    if (message.contains("服务器时间")) message
////                    else "$message\n服务器时间: ${msec2date()}"
//            }
//        }

//         由于钉钉部门名称不支持 "-" ,因此自动替换为 "_",创建通讯录时请注意
//        body.departmentName = body.departmentName?.replace("-", "_")

//        var departmentId = 1 // 默认为根部门
//        ConstantsPara.departmentNameMap.forEach {
//            if (it.value == body.departmentName) {
//                departmentId = it.key
//                return@forEach
//            }
//        }

        // 查找部门相关人员信息;匹配手机号和姓名(备注可当做别名)
//        ConstantsPara.departmentMemberDetailMap[departmentId]?.filter {
//            body.mobile.isNullOrBlank() or body.mobile.equals(it.mobile, true)
//        }?.firstOrNull {
//            body.name.isNullOrBlank() or
//                    body.name.equals(it.name, true) or
//                    body.name.equals(it.remark, true)
//        }?.let { messageBean.touser = it.userid }
        return apiService.sendTextMessage(messageBean)
    }
}
