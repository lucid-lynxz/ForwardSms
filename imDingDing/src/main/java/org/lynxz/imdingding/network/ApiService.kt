package org.lynxz.imdingding.network

import kotlinx.coroutines.Deferred
import org.lynxz.imdingding.bean.*
import retrofit2.Call
import retrofit2.http.*

/**
 * Created by lynxz on 25/08/2017.
 * 钉钉相关接口请求
 */
internal interface ApiService {

    /**
     * [获取钉钉AccessToken](https://open-doc.dingtalk.com/docs/doc.htm?spm=a219a.7629140.0.0.dfrJ5p&treeId=172&articleId=104980&docType=1)
     * @param id        corpid 企业id
     * @param secret    corpsecret 企业应用的凭证密钥
     * */
    @GET("gettoken")
    fun getAccessToken(@Query("corpid") id: String, @Query("corpsecret") secret: String): Deferred<AccessTokenBean>

    /**
     * [获取部门列表信息](https://open-doc.dingtalk.com/docs/doc.htm?spm=a219a.7629140.0.0.xIVqtB&treeId=172&articleId=104979&docType=1#s0)
     * 只会返回 用户名 和 id
     */
    @GET("department/list")
    fun getDepartmentList(): Deferred<DepartmentListBean>

    /**
     * [获取部门用户详情](https://ding-doc.dingtalk.com/doc#/serverapi2/ege851)
     * 会返回成员用户的详细信息,包括手机号码,归属部门等
     * */
    @GET("user/listbypage")
    fun getDepartmentMemberDetailList(
        @Query("department_id") id: Int = 1, // 部门id,默认为跟部门
        @Query("offset") offset: Int = 0, // 偏移量
        @Query("size") size: Int = 40, // 分页大小,默认为20,最大100
        @Query("order") order: String = "entry_asc" // 列表排序规则, 默认按进入部门的时间升序
    ): Deferred<DepartmentMemberDetailListBean>

    /**
     * [向指定用户发送普通文本消息](https://open-doc.dingtalk.com/docs/doc.htm?spm=a219a.7629140.0.0.oavHEu&treeId=172&articleId=104973&docType=1#s2)
     */
    @POST("message/send")
    fun sendTextMessage(@Body bean: MessageTextBean): Call<MessageResponseBean>
}