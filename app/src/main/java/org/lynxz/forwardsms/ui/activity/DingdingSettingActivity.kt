package org.lynxz.forwardsms.ui.activity

import kotlinx.android.synthetic.main.activity_dingding_setting.*
import org.lynxz.baseimlib.bean.ImType
import org.lynxz.forwardsms.R
import org.lynxz.forwardsms.bean.ImSetting
import org.lynxz.forwardsms.databinding.ActivityDingdingSettingBinding
import org.lynxz.forwardsms.para.ImSettingManager
import org.lynxz.forwardsms.showToast

/**
 * 钉钉参数配置
 * */
class DingdingSettingActivity : BaseImSettingBindingActivity<ActivityDingdingSettingBinding>() {
    override fun getLayoutRes() = R.layout.activity_dingding_setting
    override fun getPageTitle() = "钉钉"

    override fun initImSettingInfo() {
        val imSetting = ImSettingManager.imSettingMapLiveData().value?.get(ImType.DingDing)
        dataBinding.imSetting = imSetting as? ImSetting.DDImSetting
    }

    override fun saveImSetting() {
        val corpId = getAndCheckInputValid(edt_corpid) ?: return
        val corpSecret = getAndCheckInputValid(edt_corp_secret) ?: return
        val agentId = getAndCheckInputValid(edt_agent_id) ?: return
        val targetUserName = getAndCheckInputValid(edt_target_name) ?: return

        ImSettingManager.updateImSetting(ImType.DingDing) {
            val setting = it as ImSetting.DDImSetting
            setting.corpId = corpId
            setting.corpSecret = corpSecret
            setting.agentId = agentId
            setting.targetUserName = targetUserName
            showToast("保存配置成功")
            finish()
        }
    }
}
