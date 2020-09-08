package org.lynxz.forwardsms.bean

import org.lynxz.baseimlib.bean.ImType

/**
 * 用于发送钉钉消息给指定人员或者群组的请求
 * */
data class SendMessageReqBean(
    var name: String? = null, // 发送给指定人员的姓名,tg消息则对应其userName
    var content: String = "", // 消息内容
    var imType: String = ImType.DingDing, // 发送消息的im,默认发送到钉钉
    var mobile: String? = null,// 钉钉用户手机号,匹配优先级高于姓名
    var departmentName: String? = "", // 钉钉部门名称, 若 name 和 mobile 均为空,则发送给group所有人
    var tgBotToken: String = "" // tg接收消息的bot token
)