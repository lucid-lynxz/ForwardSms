package org.lynxz.imfeishu.netwrork

import com.jakewharton.retrofit2.adapter.kotlin.coroutines.CoroutineCallAdapterFactory
import kotlinx.coroutines.*
import okhttp3.Interceptor
import okhttp3.Response
import org.lynxz.baseimlib.bean.SendMessageReqBean
import org.lynxz.baseimlib.convert2Obj
import org.lynxz.baseimlib.convert2Str
import org.lynxz.baseimlib.network.BaseOkhttpGenerator
import org.lynxz.baseimlib.network.interceptor.RetryInterceptor
import org.lynxz.imfeishu.bean.*
import org.lynxz.imfeishu.para.ConstantsPara
import org.lynxz.imfeishu.para.FeishuKeyNames
import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.nio.charset.Charset


/**
 * Created by lynxz on 25/08/2017.
 * 网络访问具体处理类
 */
object HttpManager {
    // token过期时拦截重试
    private val tenantTokenInterceptor = Interceptor { chain ->
        var oriRequest = chain.request()
        var oriResponse = chain.proceed(oriRequest)

        // 若token过期尝试刷新
        if (isTokenExpired(oriResponse)) {
            runBlocking {
                val tenantTokenBean = refreshAccessTokenAsync().await()
                println("重新获取到的token ${convert2Str(tenantTokenBean)} 原token:${ConstantsPara.tenantToken}")

                if (!tenantTokenBean.tenant_access_token.isNullOrBlank()
                    and !ConstantsPara.tenantToken.equals(tenantTokenBean.tenant_access_token)
                ) {
                    ConstantsPara.tenantToken = tenantTokenBean.tenant_access_token
                    // token刷新后尝试重新请求一次
                    oriRequest = chain.request().newBuilder()
                        .header(
                            FeishuKeyNames.HEAD_AUTHORIZATION,
                            "Bearer ${ConstantsPara.tenantToken}"
                        )
                        .build()

                    println("更新飞书token,并尝试重新发送消息: ${oriRequest.header(FeishuKeyNames.HEAD_AUTHORIZATION)} ")
                    oriResponse.close()
                    oriResponse = chain.proceed(oriRequest)
                }
            }
        }
        oriResponse
    }

    /**
     * 判断token是否过期
     * */
    private fun isTokenExpired(response: Response): Boolean {
        // 若token不合法,报错:  {"code":99991663,"msg":"tenant_access_token not valid:t-9b43cc80fddd89b7c8dfde95e1aff978d2886933"}
        val oriRespBody = response.body
        val source = oriRespBody?.source()
        source?.request(Long.MAX_VALUE)
        val buffer = source?.buffer
        val contentType = oriRespBody?.contentType()
        val charset = contentType?.charset() ?: Charset.forName("UTF-8")
        val respStr = buffer?.clone()?.readString(charset)
        val commonResp = convert2Obj(respStr, CommonResponse::class.java)
        return commonResp?.code == CommonResponse.codeTokenExpired
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
     * token失效时报错:  {"code":99991663,"msg":"tenant_access_token not valid:t-9b43cc80fddd89b7c8dfde95e1aff978d2886933"}
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
        val messageBean = MessageTextBean().apply {
            // 通过员工 name/mobile 查询 openId
            val mobile = oriBean.mobile
            open_id = ConstantsPara.userMobileIdMap[mobile]
            if (open_id.isNullOrBlank()) {
                open_id = ConstantsPara.userNameIdMap[oriBean.name]
            }

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

        println("发送飞书消息: ${convert2Str(messageBean)}")
        return apiService.sendTextMessage(messageBean)
    }
}
