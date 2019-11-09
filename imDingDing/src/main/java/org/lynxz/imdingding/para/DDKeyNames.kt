package org.lynxz.imdingding.para

/**
 * 钉钉相关操作常量名
 * */
object DDKeyNames {
    const val corpid = "corpid" // 企业在钉钉中的标识，每个企业拥有一个唯一的CorpID
    const val corpsecret = "corpsecret" // 企业每个应用的凭证密钥
    const val agentId = "agentId" // 钉钉微应用id

    const val departmentId = "department_id"

    // 需要在url中添加query参数: access_token
    const val QUERY_KEY_ACCESS_TOKEN = "access_token"

    // 本地缓存数据key
    const val KEY_DEPARTMENT_INFO = "departmentInfo" // 部门信息列表
    const val KEY_DEPARTMENT_DETAIL_INFO = "departmentDetailInfo" // 部门成员详情
}
