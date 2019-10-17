package org.lynxz.forwardsms.network

import kotlinx.coroutines.Deferred
import org.lynxz.forwardsms.bean.SendMessageReqBean
import org.lynxz.forwardsms.bean.SendMessageRespBean
import retrofit2.http.Body
import retrofit2.http.POST

interface ApiService {

    /**
     * 发送消息给中转服务器,中转服务器转发给钉钉/tg目标用户
     * */
    @POST("action/send_msg")
    fun sendMessage(@Body body: SendMessageReqBean): Deferred<SendMessageRespBean>
}