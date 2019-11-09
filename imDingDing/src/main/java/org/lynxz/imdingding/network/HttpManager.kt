package org.lynxz.imdingding.network

import com.jakewharton.retrofit2.adapter.kotlin.coroutines.CoroutineCallAdapterFactory
import kotlinx.coroutines.*
import okhttp3.Interceptor
import org.lynxz.baseimlib.bean.SendMessageReqBean
import org.lynxz.baseimlib.network.BaseOkhttpGenerator
import org.lynxz.imdingding.bean.*
import org.lynxz.imdingding.para.ConstantsPara
import org.lynxz.imdingding.para.DDKeyNames
import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory


/**
 * Created by lynxz on 25/08/2017.
 * 网络访问具体处理类
 */
object HttpManager {

    // 给请求添加统一的query参数:access_token
    private val accessTokenInterceptor = Interceptor { chain ->
        val original = chain.request()
        val url = original.url.newBuilder()
            .addQueryParameter(DDKeyNames.QUERY_KEY_ACCESS_TOKEN, ConstantsPara.accessToken)
            .build()

        val requestBuilder = original.newBuilder().url(url)
        chain.proceed(requestBuilder.build())
    }


    private val ddRetrofit: Retrofit = Retrofit.Builder()
        .client(
            BaseOkhttpGenerator()
                .clientBuilder.apply { addInterceptor(accessTokenInterceptor) }
                .build()
        )
        .baseUrl(ConstantsPara.DINGDING_SERVER_URL)
        .addConverterFactory(GsonConverterFactory.create())
        .addCallAdapterFactory(CoroutineCallAdapterFactory())
        .build()

    private val apiService: ApiService = ddRetrofit.create(ApiService::class.java)


    /**
     * 刷新钉钉accessToken
     * */
    fun refreshAccessTokenAsync(): Deferred<AccessTokenBean> {
        return apiService.getAccessToken(ConstantsPara.dd_corp_id, ConstantsPara.dd_corp_secret)
    }


    /**
     * 获取部门列表信息以及各部门成员信息
     */
    fun getDepartmentInfoAsync(): Deferred<DepartmentListBean> {
        return apiService.getDepartmentList()
    }

    /**
     * [获取部门用户详情](https://ding-doc.dingtalk.com/doc#/serverapi2/ege851)
     * 会返回成员用户的详细信息,包括手机号码,归属部门等
     * */
    fun getDepartmentMemberDetailListAsync(
        id: Int = 1, // 部门id,默认为跟部门
        offset: Int = 0, // 偏移量
        size: Int = 40, // 分页大小,默认为20,最大100
        order: String = "entry_asc"
    ): Deferred<DepartmentMemberDetailListBean> {
        return apiService.getDepartmentMemberDetailList(id, offset, size, order)
    }

    /**
     * 根据gitlab返回的项目组别和名称,发送消息给对应的部门群成员
     * 钉钉部门名称与项目名称一致,切部门的上级部门与项目所在组的namespace一致,则可确定要通知的部门
     *
     * 如钉钉通讯录中有某群:  father/child , 部门名为 child, 上级部门为 father
     * 而gitlab中有某项目地址为: https://gitlab.lynxz.org/father/child
     * 则可完全确定所需通知的部门child
     * 备注: 本项目只支持两级
     * */
    fun sendTestMessageToDepartment(
        msg: String? = "",
        projectName: String? = "",
        projectNameSpace: String = ""
    ) {
        if (msg.isNullOrBlank() or projectName.isNullOrBlank()) {
            return
        }

//        // 由于钉钉部门名称不支持 "-" ,因此自动替换为 "_",创建通讯录时请注意
//        ConstantsPara.departmentList?.department?.toObservable()
//            ?.filter {
//                (projectName!!.replace("-", "_").equals(
//                    it.name,
//                    true
//                )) and (projectNameSpace.replace(
//                    "-",
//                    "_"
//                ).equals(ConstantsPara.departmentNameMap[it.parentid], true))
//            }
//            ?.flatMap { bean -> Observable.just(ConstantsPara.departmentMemberDetailMap[bean.id]) }
//            ?.flatMap { Observable.fromIterable(it) }
//            ?.doOnNext { sendTextMessage(it.name, null, msg!!) }
//            ?.doOnSubscribe { println("要群发给 $projectNameSpace/$projectName 的消息是:\n${toString()}") }
//            ?.subscribe()
    }

    /**
     * 向指定用户发送文本内容
     * */
    fun sendTextMessageAsync(body: SendMessageReqBean): Call<MessageResponseBean> {
        // 要发送的消息body
        val messageBean = MessageTextBean().apply {
            agentid = ConstantsPara.dd_agent_id
            msgtype = MessageType.TEXT

            // 自动添加时间信息,避免被钉钉服务器拦截
            val message = body.content
            text = MessageTextBean.TextBean().apply {
                content = message
//                content =
//                    if (message.contains("服务器时间")) message
//                    else "$message\n服务器时间: ${msec2date()}"
            }
        }

        // 由于钉钉部门名称不支持 "-" ,因此自动替换为 "_",创建通讯录时请注意
        body.departmentName = body.departmentName?.replace("-", "_")

        var departmentId = 1 // 默认为根部门
        ConstantsPara.departmentNameMap.forEach {
            if (it.value == body.departmentName) {
                departmentId = it.key
                return@forEach
            }
        }

        // 查找部门相关人员信息;匹配手机号和姓名(备注可当做别名)
        ConstantsPara.departmentMemberDetailMap[departmentId]?.filter {
            body.mobile.isNullOrBlank() or body.mobile.equals(it.mobile, true)
        }?.firstOrNull {
            body.name.isNullOrBlank() or
                    body.name.equals(it.name, true) or
                    body.name.equals(it.remark, true)
        }?.let { messageBean.touser = it.userid }
        return apiService.sendTextMessage(messageBean)
    }
}
