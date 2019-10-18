package org.lynxz.baseimlib

import kotlinx.coroutines.*
import org.lynxz.baseimlib.actions.IIMAction
import retrofit2.Call
import java.io.Closeable
import java.io.IOException
import java.net.ConnectException
import kotlin.coroutines.CoroutineContext

/**
 * https://juejin.im/post/5cfb38f96fb9a07eeb139a00
 * */
fun <ResultType> CoroutineScope.retrofit(dsl: RetrofitCoroutineDsl<ResultType>.() -> Unit) {
//    launch(Dispatchers.Default) {
    launch(Dispatchers.Main) {
        println("currentThread: ${Thread.currentThread().name}")
        val retrofitCoroutine = RetrofitCoroutineDsl<ResultType>()
        retrofitCoroutine.dsl()
        val api = retrofitCoroutine.api
//        if (api == null) {
//            retrofitCoroutine.onFailed?.invoke("api不存在", -101)
//
//        } else {
//        retrofitCoroutine.api?.let { api ->
        val work = async(Dispatchers.IO) {
            try {
                api?.execute()
            } catch (e: ConnectException) {
                retrofitCoroutine.onFailed?.invoke(e.message, -100)
                null
            } catch (e: IOException) {
                retrofitCoroutine.onFailed?.invoke(e.message, -1)
                null
            }
        }

        work.invokeOnCompletion {
            if (work.isCancelled) {
                api?.cancel()
                retrofitCoroutine.onComplete?.invoke(true)
                retrofitCoroutine.clean()
            }
        }

        val response = work.await()

        response?.let {
            if (response.isSuccessful) {
                retrofitCoroutine.onSuccess?.invoke(response.body())
            } else {
//                    when (response.code()) {
//                        401 -> {
//                        }
//                        500 -> {
//                        }
//                    }
                retrofitCoroutine.onFailed?.invoke(
                    response.errorBody().toString(),
                    response.code()
                )
            }
        }

        retrofitCoroutine.onComplete?.invoke(false)
    }
//    }
}

class RetrofitCoroutineDsl<ResultType> {
    var api: (Call<ResultType>)? = null

    // http 请求正常返回(http状态码:2xx,3xx)时回调
    internal var onSuccess: ((ResultType?) -> Unit)? = null
        private set

    internal var onFailed: ((error: String?, code: Int) -> Unit)? = null
        private set

    // Boolean表示是否取消
    // 确保执行结束后一定会调用一次
    internal var onComplete: ((Boolean) -> Unit)? = null
        private set

    internal fun clean() {
        onSuccess = null
        onComplete = null
        onFailed = null
    }

    fun onSuccess(block: (ResultType?) -> Unit) {
        this.onSuccess = block
    }

    fun onComplete(block: (Boolean) -> Unit) {
        this.onComplete = block
    }

    fun onFailed(block: (error: String?, code: Int) -> Unit) {
        this.onFailed = block
    }
}

//private const val JOB_KEY = "androidx.lifecycle.ViewModelCoroutineScope.JOB_KEY"
val requestScope: CoroutineScope
    get() {
        return CloseableCoroutineScope(SupervisorJob() + Dispatchers.IO)
//        val scope: CoroutineScope? = this.getTag(JOB_KEY)
//        if (scope != null) {
//            return scope
//        }
//        return setTagIfAbsent(JOB_KEY,
//            CloseableCoroutineScope(SupervisorJob() + Dispatchers.Main))
    }

internal class CloseableCoroutineScope(context: CoroutineContext) : Closeable, CoroutineScope {
    override val coroutineContext: CoroutineContext = context

    override fun close() {
        coroutineContext.cancel()
    }
}
