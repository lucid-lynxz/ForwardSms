package org.lynxz.forwardsms.ui.activity

import org.lynxz.baseimlib.bean.ImType
import org.lynxz.forwardsms.R
import org.lynxz.forwardsms.para.GlobalImSettingPara
import org.lynxz.forwardsms.showToast

/**
 * 钉钉参数配置
 * */
class DingdingSettingActivity : BaseImSettingActivity() {
    override fun getLayoutRes() = R.layout.activity_dingding_setting
    override fun getPageTitle() = "钉钉"
    override fun saveImSetting() {
        showToast("todo: 保存配置")
        GlobalImSettingPara.updateImSetting(ImType.DingDing) {
//            it.extPropMap[]
        }
    }
}
