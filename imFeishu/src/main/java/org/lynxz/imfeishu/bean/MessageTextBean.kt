package org.lynxz.imfeishu.bean

import org.lynxz.imfeishu.para.MessageType

/**
 * 发送富文本消息的requestBody
 * */
data class MessageTextBean(
    /**
     * 给用户发私聊消息，只需要填 open_id、email、user_id 中的一个即可
     * 向群里发消息使用群的 chat_id
     */
    var open_id: String? = "", // 给用户发私聊消息，只需要填 open_id、email、user_id 中的一个即可，向群里发消息使用群的 chat_id
    var content: Content? = null, // 息的内容
    var msg_type: String = MessageType.RICH_TEXT, // 消息类型,参考 MessageType 类,富文本固定为: post
    var root_id: String? = "" // 可选, 如果需要回复某条消息，填对应消息的消息 ID
)

data class Content(
    var post: Post? = null
)

data class Post(
    var zh_cn: ZhCn? = null // 中文消息体、需要国际化可以增加ja_jp（日语）、en_us（英文）
)

data class ZhCn(
    var content: List<List<ContentX>>, // 消息的内容
    var title: String? = null // 可选, 消息的标题
)

data class ContentX(
    var height: Int = 0,
    var href: String? = null,
    var image_key: String? = null,
    var tag: String = "text", // 默认为文本类型
    var text: String,
    var un_escape: Boolean = false,
    var user_id: String? = null,
    var width: Int = 0
)