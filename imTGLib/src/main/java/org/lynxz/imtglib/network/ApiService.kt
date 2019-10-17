package org.lynxz.imtglib.network

import kotlinx.coroutines.Deferred
import org.lynxz.imtglib.bean.TgGetUpdateResponseBean
import org.lynxz.imtglib.bean.TgSendMessageReqBean
import org.lynxz.imtglib.bean.TgSendMessageRespBean
import retrofit2.Call
import retrofit2.http.*

/**
 * Created by lynxz on 25/08/2017.
 * tg相关接口请求
 */
internal interface ApiService {

//    /**
//     * [tg getUpdates](https://core.telegram.org/bots/api#getupdates)
//     * */
//    @GET("bot{botToken}/getUpdates")
//    fun getUpdatesAsync(@Path("botToken") botToken: String): Deferred<TgGetUpdateResponseBean>

//    /**
//     * [向tg发送消息](https://core.telegram.org/bots/api#sendmessage)
//     * */
//    @POST("bot{botToken}/sendMessage")
//    fun sendTextMessageAsync(
//        @Path("botToken") botToken: String,
//        @Body bean: TgSendMessageReqBean
//    ): Deferred<TgSendMessageRespBean>

    @GET
    fun getUpdatesAsync(
        @Url url: String,
        @Query("offset") offset: String = "",
        @Query("limit") limit: Int = 10,
        @Query("allowed_updates") allowed_updates: Array<String> = arrayOf("message")
    )
            : Deferred<TgGetUpdateResponseBean>

    /**
     * [向tg发送消息](https://core.telegram.org/bots/api#sendmessage)
     * */
    @POST
    fun sendTextMessageAsync(
        @Url url: String,
        @Body bean: TgSendMessageReqBean
    ): Call<TgSendMessageRespBean>
}