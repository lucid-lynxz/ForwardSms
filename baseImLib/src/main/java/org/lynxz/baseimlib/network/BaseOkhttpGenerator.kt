package org.lynxz.baseimlib.network

import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import org.lynxz.baseimlib.BaseKeyNames
import java.util.concurrent.TimeUnit

/**
 * okhttp通用参数设定
 * */
class BaseOkhttpGenerator {
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
            .addHeader(BaseKeyNames.HEADER_KEY_CONTENT_TYPE, "application/json")
            .build()
        chain.proceed(request)
    }

    // 显示请求日志,可选
    private val logInterceptor =
        HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BODY }

    // 返回builder，便于调用方按需添加其他拦截器等功能
    val clientBuilder: OkHttpClient.Builder = OkHttpClient()
        .newBuilder()
        .connectTimeout(3, TimeUnit.SECONDS)
        .readTimeout(3, TimeUnit.SECONDS)
        .writeTimeout(3, TimeUnit.SECONDS)
        .addInterceptor(headerInterceptor)
        .addInterceptor(queryInterceptor)
        .addInterceptor(logInterceptor)
}