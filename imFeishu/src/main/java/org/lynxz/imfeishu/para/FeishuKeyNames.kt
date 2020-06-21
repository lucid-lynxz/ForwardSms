package org.lynxz.imfeishu.para

/**
 * 飞书相关操作常量名
 * */
object FeishuKeyNames {
    const val appId = "app_id"  // 应用唯一标识，创建应用后获得
    const val appSecret = "app_secret" // 应用秘钥
//    const val agentId = "agentId" // 钉钉微应用id

    const val departmentId = "department_id"

    // 需要在url中添加query参数: access_token
    const val QUERY_KEY_ACCESS_TOKEN = "access_token"

    const val HEAD_AUTHORIZATION = "Authorization"
    const val HEAD_CONTENT_TYPE = "Content-Type"

    // 本地缓存数据key
    const val KEY_DEPARTMENT_INFO = "departmentInfo" // 部门信息列表
    const val KEY_DEPARTMENT_DETAIL_INFO = "departmentDetailInfo" // 部门成员详情
}
