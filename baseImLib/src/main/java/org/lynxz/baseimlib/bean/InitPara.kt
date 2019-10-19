package org.lynxz.baseimlib.bean

import org.lynxz.baseimlib.actions.IPropertyAction


/**
 * 各im相关初始化参数,目前只支持 钉钉 和 telegram
 * 有默认值的为可选参数,否则为必传
 * */
sealed class ImInitPara(
    var propertyUtil: IPropertyAction? = null, // 可选,数据持久化操作实现类
    var extParaMap: Map<String, String>? = null,// 可选,其他相关key定义在各具体实现模块中
    var cxt: Any? = null // 按需传入上下文
) {
    data class DDInitPara( // 钉钉初始化参数
        var corpid: String, // 必填,企业在钉钉中的标识，每个企业拥有一个唯一的CorpID
        var corpsecret: String, // 必填,企业每个应用的凭证密钥
        var agentId: String // 必填,钉钉微应用id
    ) : ImInitPara()

    data class TGInitPara( // telegram初始化参数
        var botToken: String, // 必填,用于接收消息的tg机器人token,通过 @BotFather 创建和获取
        var defaultUserName: String = "" // 可选,,调用方若未指定消息接收人,则默认发给本用户
    ) : ImInitPara()
}