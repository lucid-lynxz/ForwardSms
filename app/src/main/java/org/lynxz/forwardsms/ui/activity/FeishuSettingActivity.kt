package org.lynxz.forwardsms.ui.activity

import kotlinx.android.synthetic.main.activity_feishu_setting.*
import kotlinx.android.synthetic.main.activity_telegram_setting.edt_target_name
import org.lynxz.baseimlib.bean.ImType
import org.lynxz.forwardsms.R
import org.lynxz.forwardsms.bean.ImSetting
import org.lynxz.forwardsms.databinding.ActivityFeishuSettingBinding
import org.lynxz.forwardsms.para.GlobalImSettingPara
import org.lynxz.forwardsms.showToast

/**
 * 飞书参数配置
 * */
class FeishuSettingActivity : BaseImSettingBindingActivity<ActivityFeishuSettingBinding>() {
    override fun getLayoutRes() = R.layout.activity_feishu_setting
    override fun getPageTitle() = "飞书"

    override fun initImSettingInfo() {
        val imSetting = GlobalImSettingPara.imSettingMapLiveData().value?.get(ImType.FeiShu)
        dataBinding.imSetting = imSetting as? ImSetting.FeishuImSetting
    }

    override fun saveImSetting() {
        // 到 飞书开放平台 创建和查看应用: https://open.feishu.cn/app/
        val appId = getAndCheckInputValid(edt_appid, "请输入飞书应用id") ?: return
        val appSecret = getAndCheckInputValid(edt_app_secret, "请输入飞书应用密钥") ?: return
        val targetUserName = getAndCheckInputValid(edt_target_name, "请输入接收人姓名") ?: return

        GlobalImSettingPara.updateImSetting(ImType.FeiShu) {
            val ddSetting = it as ImSetting.FeishuImSetting
            ddSetting.appId = appId
            ddSetting.appSecret = appSecret
            ddSetting.targetUserName = targetUserName
            showToast("保存配置成功")
            finish()
        }
    }
}
