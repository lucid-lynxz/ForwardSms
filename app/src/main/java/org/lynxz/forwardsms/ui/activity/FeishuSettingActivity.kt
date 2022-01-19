package org.lynxz.forwardsms.ui.activity

import org.lynxz.baseimlib.bean.ImType
import org.lynxz.forwardsms.R
import org.lynxz.forwardsms.bean.ImSetting
import org.lynxz.forwardsms.databinding.ActivityFeishuSettingBinding
import org.lynxz.forwardsms.para.ImSettingManager
import org.lynxz.forwardsms.showToast

/**
 * 飞书参数配置
 * */
class FeishuSettingActivity : BaseImSettingBindingActivity<ActivityFeishuSettingBinding>() {
    override fun getLayoutRes() = R.layout.activity_feishu_setting
    override fun getPageTitle() = "飞书"

    override fun initImSettingInfo() {
        val imSetting = ImSettingManager.imSettingMapLiveData().value?.get(ImType.FeiShu)
        dataBinding.imSetting = imSetting as? ImSetting.FeishuImSetting
    }

    override fun saveImSetting() {
        // 到 飞书开放平台 创建和查看应用: https://open.feishu.cn/app/
        val appId = getAndCheckInputValid(dataBinding.edtAppid, "请输入飞书应用id") ?: return
        val appSecret = getAndCheckInputValid(dataBinding.edtAppSecret, "请输入飞书应用密钥") ?: return
        val targetUserName = getAndCheckInputValid(dataBinding.edtTargetName, "请输入接收人姓名") ?: return

        ImSettingManager.updateImSetting(ImType.FeiShu) {
            val setting = it as ImSetting.FeishuImSetting
            setting.appId = appId
            setting.appSecret = appSecret
            setting.targetUserName = targetUserName
            showToast("保存配置成功")
            finish()
        }
    }
}
