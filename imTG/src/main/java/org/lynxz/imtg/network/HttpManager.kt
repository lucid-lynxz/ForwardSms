package org.lynxz.imtg.network

import com.jakewharton.retrofit2.adapter.kotlin.coroutines.CoroutineCallAdapterFactory
import kotlinx.coroutines.Deferred
import org.lynxz.baseimlib.network.BaseOkhttpGenerator
import org.lynxz.imtg.bean.TgGetUpdateResponseBean
import org.lynxz.imtg.bean.TgSendMessageReqBean
import org.lynxz.imtg.bean.TgSendMessageRespBean
import org.lynxz.imtg.para.ConstantsPara
import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory


/**
 * Created by lynxz on 25/08/2017.
 * 网络访问具体处理类
 */
object HttpManager {
    private val tgRetrofit: Retrofit = Retrofit.Builder()
        .client(BaseOkhttpGenerator().clientBuilder.build())
        .baseUrl(ConstantsPara.TG_SERVER_URL)
        .addConverterFactory(GsonConverterFactory.create())
        .addCallAdapterFactory(CoroutineCallAdapterFactory())
        .build()

    private val apiService: ApiService = tgRetrofit.create(ApiService::class.java)


    /**
     * 获取bot所有消息,提取chatId
     * */
    fun getTgUpdatesAsync(botToken: String = ConstantsPara.botToken)
            : Deferred<TgGetUpdateResponseBean> {
//        return apiService.getUpdatesAsync(botToken)
        return apiService.getUpdatesAsync("${ConstantsPara.TG_SERVER_URL}bot$botToken/getUpdates")
    }

    /**
     * 通过bot发送消息给指定的用户
     * */
    fun sendTextMessageAsync(
        bean: TgSendMessageReqBean,
        botToken: String = ConstantsPara.botToken
    ): Call<TgSendMessageRespBean> {
//        return apiService.sendTextMessageAsync(ConstantsPara.botToken, bean)
        return apiService.sendTextMessageAsync(
            "${ConstantsPara.TG_SERVER_URL}bot$botToken/sendMessage",
            bean
        )
    }
}
