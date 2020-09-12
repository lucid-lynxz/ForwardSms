package org.lynxz.forwardsms.ui.activity

import kotlinx.android.synthetic.main.activity_telegram_setting.*
import org.lynxz.baseimlib.bean.ImType
import org.lynxz.forwardsms.R
import org.lynxz.forwardsms.bean.ImSetting
import org.lynxz.forwardsms.databinding.ActivityTelegramSettingBinding
import org.lynxz.forwardsms.para.ImSettingManager
import org.lynxz.forwardsms.showToast

/**
 * Telegram参数配置
 * */
class TelegramSettingActivity : BaseImSettingBindingActivity<ActivityTelegramSettingBinding>() {
    override fun getLayoutRes() = R.layout.activity_telegram_setting
    override fun getPageTitle() = "Telegram"

    override fun initImSettingInfo() {
        val imSetting = ImSettingManager.imSettingMapLiveData().value?.get(ImType.TG)
        dataBinding.imSetting = imSetting as? ImSetting.TGImSetting
    }

    override fun saveImSetting() {
        val botToken = getAndCheckInputValid(edt_bot_token, "请输入电报机器人token") ?: return
        val targetUserName =
            getAndCheckInputValid(edt_target_name, "请输入telegram昵称或者userName") ?: return

        ImSettingManager.updateImSetting(ImType.TG) {
            val setting = it as ImSetting.TGImSetting
            setting.botToken = botToken
            setting.targetUserName = targetUserName
            showToast("保存配置成功")
            finish()
        }
    }
}
