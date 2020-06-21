package org.lynxz.imfeishu.netwrork

import kotlinx.coroutines.Deferred
import org.lynxz.imfeishu.bean.*
import org.lynxz.imfeishu.para.ConstantsPara
import org.lynxz.imfeishu.para.FeishuKeyNames
import retrofit2.Call
import retrofit2.http.*

/**
 * Created by lynxz on 25/08/2017.
 * 钉钉相关接口请求
 */
internal interface ApiService {

    /**
     * [获取 tenant_access_token（企业自建应用）](https://open.feishu.cn/document/ukTMukTMukTM/uIjNz4iM2MjLyYzM)
     * @param id        app_id 应用唯一标识，创建应用后获得
     * @param secret    app_secret 应用秘钥
     * */
    @POST("open-apis/auth/v3/tenant_access_token/internal/")
    fun getTenantToken(
        @Query("app_id") id: String,
        @Query("app_secret") secret: String
    ): Deferred<TenantTokenBean>

    /**
     * 获取通讯录授权范围
     * 包括可访问的部门列表及用户列表
     *
     * */
    @GET("open-apis/contact/v1/scope/get")
    fun getContactScope(@Header(FeishuKeyNames.HEAD_AUTHORIZATION) tokenHead: String = "Bearer ${ConstantsPara.tenantToken}"): Deferred<CommonResponse<GetContactScopeResponse>>

    /**
     * [获取部门用户详情](https://open.feishu.cn/document/ukTMukTMukTM/uYzN3QjL2czN04iN3cDN)
     * 返回当前部门中的员工详细信息列表,包含名字和手机号等
     * 此处未做分页,最多100个员工
     */
    @GET("open-apis/contact/v1/department/user/detail/list?page_size=100&fetch_child=true")
    fun getDepartmentMemberDetailList(
        @Query("department_id") department_id: String,
        @Header(FeishuKeyNames.HEAD_AUTHORIZATION) tokenHead: String = "Bearer ${ConstantsPara.tenantToken}"
    ): Deferred<CommonResponse<GetDepartmentMemberListResponse>>

    /**
     * [发送富文本消息](https://open.feishu.cn/document/ukTMukTMukTM/uMDMxEjLzATMx4yMwETM)
     */
    @POST("open-apis/message/v4/send/")
    fun sendTextMessage(
        @Body bean: MessageTextBean,
        @Header(FeishuKeyNames.HEAD_AUTHORIZATION) tokenHead: String = "Bearer ${ConstantsPara.tenantToken}"
    ): Call<CommonResponse<SendMessageResponse>>
}