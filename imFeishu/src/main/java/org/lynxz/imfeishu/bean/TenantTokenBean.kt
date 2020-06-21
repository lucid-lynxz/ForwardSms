package org.lynxz.imfeishu.bean

/**
 *  获取 tenant_access_token（企业自建应用）
 *  文档: https://open.feishu.cn/document/ukTMukTMukTM/uADN14CM0UjLwQTN
 * */
data class TenantTokenBean(
    val code: Int, // 错误码，非 0 表示失败
    val expire: Int, // app_access_token 过期时间,单位:s
    val msg: String?,// 错误描述
    val tenant_access_token: String? // 访问 token
)