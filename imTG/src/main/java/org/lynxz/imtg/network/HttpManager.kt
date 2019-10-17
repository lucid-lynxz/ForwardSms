package org.lynxz.imtg.network

import com.jakewharton.retrofit2.adapter.kotlin.coroutines.CoroutineCallAdapterFactory
import kotlinx.coroutines.Deferred
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import org.lynxz.imtg.bean.TgGetUpdateResponseBean
import org.lynxz.imtg.bean.TgSendMessageReqBean
import org.lynxz.imtg.bean.TgSendMessageRespBean
import org.lynxz.imtg.para.ConstantsPara
import org.lynxz.imtg.para.TGKeyNames
import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory


/**
 * Created by lynxz on 25/08/2017.
 * 网络访问具体处理类
 */
object HttpManager {

    // 给请求添加统一的query参数:access_token
    private val queryInterceptor = Interceptor { chain ->
        val original = chain.request()
        val url = original.url.newBuilder()
            // .addQueryParameter("access_token", "xxx")
            .build()

        val requestBuilder = original.newBuilder().url(url)
        chain.proceed(requestBuilder.build())
    }

    // 给请求添加统一的header参数:Content-Type
    private val headerInterceptor = Interceptor { chain ->
        val request = chain.request().newBuilder()
            .addHeader(TGKeyNames.HEADER_KEY_CONTENT_TYPE, "application/json")
            .build()
        chain.proceed(request)
    }

    // 显示请求日志,可选
    private val logInterceptor =
        HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BODY }

    private val okHttpClient: OkHttpClient = OkHttpClient()
        .newBuilder()
        .addInterceptor(headerInterceptor)
        .addInterceptor(queryInterceptor)
        .addInterceptor(logInterceptor)
        .build()

    private val tgRetrofit: Retrofit = Retrofit.Builder()
        .client(okHttpClient)
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
