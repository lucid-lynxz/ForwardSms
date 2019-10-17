package org.lynxz.forwardsms.network

import com.jakewharton.retrofit2.adapter.kotlin.coroutines.CoroutineCallAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import org.lynxz.forwardsms.bean.ImType
import org.lynxz.forwardsms.bean.SendMessageReqBean
import org.lynxz.forwardsms.util.Logger
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory


/**
 * Created by lynxz on 25/08/2017.
 * 网络访问具体处理类
 */
object HttpManager {
    private const val TAG = "HttpManager"

    // 给请求添加统一的header参数:Content-Type
    private val headerInterceptor = Interceptor { chain ->
        val request = chain.request().newBuilder()
            .addHeader(SmsConstantParas.HEADER_KEY_CONTENT_TYPE, "application/json")
            .build()
        chain.proceed(request)
    }

    // 显示请求日志,可选
    private val logInterceptor =
        HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BODY }

    private val okHttpClient: OkHttpClient = OkHttpClient()
        .newBuilder()
        .addInterceptor(headerInterceptor)
        .addInterceptor(logInterceptor)
        .build()

    private val ddRetrofit: Retrofit = Retrofit.Builder()
        .client(okHttpClient)
        .baseUrl("http://www.justfun.ml:8080/message_server/")
        .addConverterFactory(GsonConverterFactory.create())
        .addCallAdapterFactory(CoroutineCallAdapterFactory())
        .build()

    private val apiService: ApiService = ddRetrofit.create(ApiService::class.java)

    fun sendMessage(msg: String, userName: String = "") {
        GlobalScope.launch {
            withContext(Dispatchers.IO) {
                apiService.sendMessage(SendMessageReqBean().apply {
                    name = userName
                    content = msg
                    imType = ImType.TG
                }).await()
            }
            Logger.d(TAG, "发送消息完成")
        }
    }
}