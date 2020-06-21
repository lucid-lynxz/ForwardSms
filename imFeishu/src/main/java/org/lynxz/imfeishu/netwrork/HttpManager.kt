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
    private val feishuRetrofit: Retrofit = Retrofit.Builder()
        .client(
            BaseOkhttpGenerator()
                .clientBuilder.apply {
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
        return apiService.sendTextMessage(messageBean)
    }
}
