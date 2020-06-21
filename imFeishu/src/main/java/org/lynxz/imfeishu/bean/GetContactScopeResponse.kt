package org.lynxz.imfeishu.bean

/**
 * 获取通讯录范围的响应
 * */
data class GetContactScopeResponse(
    // 已授权部门列表，授权范围为全员可见时返回的是当前企业的所有一级部门列表
    val authed_departments: List<String>,

    // 已授权用户 employee_id 列表，应用申请了 获取用户user_id 权限时返回；当授权范围为全员可见时返回的是当前企业所有顶级部门用户列表
    val authed_employee_ids: List<String>,

    // 已授权用户 open_id 列表；当授权范围为全员可见时返回的是当前企业所有顶级部门用户列表
    val authed_open_ids: List<String>
)