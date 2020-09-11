package org.lynxz.forwardsms.ui.fragment.forwardsetting

import androidx.lifecycle.ViewModel
import org.lynxz.baseimlib.IMManager
import org.lynxz.forwardsms.para.GlobalImSettingPara
import org.lynxz.forwardsms.viewmodel.GlobalParaUtil

class ForwardSettingViewModel : ViewModel() {

    /**
     * 启用或者禁用某个im转发功能
     * */
    fun activeIm(imType: String, active: Boolean = true) {
        GlobalImSettingPara.updateImSetting(imType) { setting ->
            setting.enable = active
        }

        if (active) {
            GlobalParaUtil.activeIm(imType)
        } else {
            IMManager.unregisterIm(imType)
        }
    }
}