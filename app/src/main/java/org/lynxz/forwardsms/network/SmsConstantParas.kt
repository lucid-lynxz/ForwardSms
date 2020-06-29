package org.lynxz.forwardsms.network

/**
 * 使用到的一些key名称
 */
object SmsConstantParas {
    // 与钉钉服务器通讯时,需要在url中添加query参数: access_token
    const val QUERY_KEY_ACCESS_TOKEN = "access_token"
    const val HEADER_KEY_CONTENT_TYPE = "Content-Type"


    const val SpKeyTgUserName = "tgUserName" // 默认tg接收消息的用户名
    const val SpKeyDDUserName = "ddUserName"// 默认钉钉接收消息的用户名
    const val SpKeyFeishuUserName = "feishuUserName"// 默认飞书接收消息的用户名
    const val SpKeyPhoneTag = "phoneTag"// 本机号码或昵称等信息

    const val SpKeyEnableTg = "enableTg" // 是否启用tg发送
    const val SpKeyEnableDingDing = "enableDingDing" // 是否启用钉钉发送
    const val SpKeyEnableFeishu = "enableFeishu" // 是否启用飞书发送

    const val SpKeyForwardWechat = "forwardWechat" // 是否允许转发微信通知信息


    // tg消息接收人 userName或者昵称
    var tgUserNme = ""

    // 钉钉消息接收人姓名或者备注名
    var ddName = ""

    // 飞书消息接收人姓名
    var feishuName = ""

    // 本机号码或昵称等信息,默认为手机型号
    var phoneTag: String = android.os.Build.MODEL
}