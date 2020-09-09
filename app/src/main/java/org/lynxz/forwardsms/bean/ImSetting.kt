package org.lynxz.forwardsms.bean

import org.lynxz.baseimlib.bean.ImType

/**
 * 各转发平台配置参数信息
 * */
sealed class ImSetting(
    val imType: String, // im标志,参考 [ImType]
    var enable: Boolean = false, // 是否允许转发到该IM
    var targetUserName: String = "" // 接收该消息的用户名
) {
    // 钉钉配置参数
    data class DDImSetting(
        var corpId: String = "", // 企业的钉钉唯一标识
        var corpSecret: String = "", // 企业的凭据密钥
        var agentId: String = "" // 微应用id
    ) : ImSetting(ImType.DingDing)

    // 电报telegram配置参数
    data class TGImSetting(
        var botToken: String = "" // 必填,用于接收消息的tg机器人token,通过 @BotFather 创建和获取
    ) : ImSetting(ImType.TG)

    // 飞书配置参数
    data class FeishuImSetting(
        var appId: String = "",
        var appSecret: String = ""
    ) : ImSetting(ImType.FeiShu)

    companion object {
        /**
         * 生成默认的配置
         * */
        fun generateDefaultImSetting(imType: String) = when (imType) {
            ImType.DingDing -> DDImSetting()
            ImType.TG -> TGImSetting()
            ImType.FeiShu -> FeishuImSetting()
            else -> throw IllegalArgumentException("imType not support: $imType")
        }
    }
}

