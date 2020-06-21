package org.lynxz.imfeishu.para


/**
 * Created by lynxz on 25/08/2017.
 * 参数
 */
object ConstantsPara {
    // 飞书服务器地址
    val SERVER_URL = "https://open.feishu.cn/"

    // 飞书相关参数
    var appId = ""
    var appSecret = ""
    var tenantToken: String? = ""

    // 根部门id
    var rootDepartmentId: String? = ""

    /**
     * 用于记录部门id和部门名称之间的对应关系
     * */
    var departmentNameMap = hashMapOf<Int, String>()

    /**
     * 存储用户姓名及其id的对应关系,用于姓名找到用户,并发送文本消息
     * */
    val userNameIdMap = hashMapOf<String, String>()

    /**
     * 存储用户手机号及其id的对应关系,用于通过手机号找到用户,发送信息
     * */
    val userMobileIdMap = hashMapOf<String, String>()
}