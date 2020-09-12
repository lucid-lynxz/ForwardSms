package org.lynxz.forwardsms.ui.fragment.forwardsetting

import androidx.lifecycle.ViewModel
import org.lynxz.baseimlib.IMManager
import org.lynxz.forwardsms.para.ImSettingManager
import org.lynxz.forwardsms.viewmodel.GlobalParaUtil

class ForwardSettingViewModel : ViewModel() {

    /**
     * 启用或者禁用某个im转发功能
     * */
    fun activeIm(imType: String, active: Boolean = true) {
        ImSettingManager.updateImSetting(imType) { setting ->
            setting.enable = active
        }

        if (active) {
            GlobalParaUtil.activeIm(imType)
        } else {
            IMManager.unregisterIm(imType)
        }
    }
}