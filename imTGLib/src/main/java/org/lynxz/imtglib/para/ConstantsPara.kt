package org.lynxz.imtglib.para


/**
 * tg发送消息相关参数
 */
object ConstantsPara {

    // tg服务器地址
    val TG_SERVER_URL = "https://api.telegram.org/"

    /**
     * 默认发送的目标tg bot token信息
     * */
    var botToken = ""

    /**
     * 发送消息给bot时,默认的接收用户
     * */
    var defaultUserName = ""

    /**
     * 通过tg getUpdates接口获取其所有的chat id信息
     * key: bot_token + "_" + userName/first_name 以便通过接口快速将消息发送给指定人员的bot聊天窗口
     * value: chat_id
     * */
    var chatInfoMap = mutableMapOf<String, Long>()
}