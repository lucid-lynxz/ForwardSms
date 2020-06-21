package org.lynxz.baseimlib.network.interceptor

import okhttp3.Interceptor
import okhttp3.Response
import java.io.IOException

/**
 * okhttp失败重试interceptor,默认最多重试3次
 * 需要关闭默认的重试方法 okHttpClient.retryOnConnectionFailure(false)
 * */
class RetryInterceptor(private val maxRetry: Int = 3) : Interceptor {
    private var retryNum = 0 //假如设置为3次重试的话，则最大可能请求4次（默认1次+3次重试）

    @Throws(IOException::class)
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        var response = chain.proceed(request)
        while (!response.isSuccessful && retryNum < maxRetry) {
            retryNum++
            response.close()
            response = chain.proceed(request)
        }
        return response
    }
}