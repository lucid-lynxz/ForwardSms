package org.lynxz.baseimlib.bean

/**
 * 用于发送钉钉消息给指定人员或者群组的请求
 * 或者发送消息给指定的tg bot, 通过bot发送给指定用户
 * */
data class SendMessageReqBean(
    /*
    * 可空,消息接收人名称,若为空,则发送给所有人
    * 钉钉:成员姓名
    * tg:userName or firstName
    * 飞书: 员工姓名
     */
    var name: String? = null,
    var content: String = "", // 必填,消息内容
    var imType: String = ImType.DingDing, // 必填,发送消息的im类型,默认发送到钉钉
    var mobile: String? = null,// 发送钉钉/飞书消息使用,匹配优先级高于姓名
    var departmentName: String? = "", // 发送钉钉消息使用,钉钉部门名称, 若 name 和 mobile 均为空,则发送给group所有人
    var tgBotToken: String = "" // 发送tg消息时使用,接收消息的 bot token
) {
    // 拷贝一个对象,属性与当前属性一致
    fun duplicate() = SendMessageReqBean().also {
        it.name = name
        it.content = content
        it.imType = imType
        it.mobile = mobile
        it.departmentName = departmentName
        it.tgBotToken = tgBotToken
    }
}

// 支持消息转发的im类型信息
object ImType {
    const val DingDing = "DingDing"
    const val TG = "Telegram"
    const val FeiShu = "FeiShu"
}
