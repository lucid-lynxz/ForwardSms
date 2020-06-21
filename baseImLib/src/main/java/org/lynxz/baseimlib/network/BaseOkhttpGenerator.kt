package org.lynxz.baseimlib.network

import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import org.lynxz.baseimlib.BaseKeyNames
import java.util.concurrent.TimeUnit

/**
 * okhttp通用参数设定
 * @param extHeaderMap 其他需要统一添加到所有请求中的head数据
 * */
class BaseOkhttpGenerator(extHeaderMap: Map<String, String>? = null) {
    // 给请求添加统一的header参数:Content-Type
    private val headerInterceptor = Interceptor { chain ->
        val request = chain.request().newBuilder()
            .addHeader(BaseKeyNames.HEADER_KEY_CONTENT_TYPE, "application/json")
            .also { builder ->
                extHeaderMap?.forEach {
                    builder.addHeader(it.key, it.value)
                }
            }
            .build()
        chain.proceed(request)
    }

    // 显示请求日志,可选
    private val bodyLogInterceptor =
        HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BODY }
    private val headLogInterceptor =
        HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.HEADERS }

    // 返回builder，便于调用方按需添加其他拦截器等功能
    // 由于tg需要科学上网, 而科学上网时,访问钉钉又容易超时,因此增大各超时时间
    val clientBuilder: OkHttpClient.Builder = OkHttpClient()
        .newBuilder()
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .writeTimeout(10, TimeUnit.SECONDS)
        .addInterceptor(headerInterceptor)
        .addInterceptor(bodyLogInterceptor)
        .addInterceptor(headLogInterceptor)
}