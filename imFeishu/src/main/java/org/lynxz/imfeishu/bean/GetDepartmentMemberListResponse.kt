package org.lynxz.imfeishu.bean

/**
 * 获取部门用户详情请求返回内容
 * https://open.feishu.cn/document/ukTMukTMukTM/uYzN3QjL2czN04iN3cDN
 * */
data class GetDepartmentMemberListResponse(
    val has_more: Boolean, // 分页查询时返回，代表是否还有更多用户
    val page_token: String, // 分页标记，当 has_more 为 true 时返回该参数，下一次接口调用使用该参数可以获取到当前部门更多用户， has_more 为 false 时不返回
    val user_infos: List<UserInfo> // 用户详情列表
)

/**
 * 飞书当前添加通讯录成员时, 姓名和手机号是必填项
 * */
data class UserInfo(
    val avatar_url: String?, // 用户头像，原始大小
    val departments: List<String>?,
    val email: String?,
    val employee_id: String?, // 用户的 employee_id，申请了"获取用户 user_id"权限后返回
    val employee_no: String?, // 工号
    val employee_type: Int?, // 员工类型。1:正式员工；2:实习生；3:外包；4:劳务；5:顾问
    val en_name: String?, // 英文名
    val gender: Int?, // 性别，未设置不返回该字段。1:男；2:女
    val mobile: String, // 用户手机号，已申请"获取用户手机号"权限的企业自建应用返回该字段
    val name: String, // 用户名
    val name_py: String, // 用户名拼音
    val open_id: String,
    val status: Int, // 用户状态，bit0(最低位): 1冻结，0未冻结；bit1:1离职，0在职；bit2:1未激活，0已激活
    val union_id: String? // 用户的 union_id,申请了"获取用户统一ID"权限后返回
)