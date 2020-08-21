package org.lynxz.forwardsms.ui.fragment.othersetting

import android.view.View
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import kotlinx.android.synthetic.main.fragment_other_setting.*
import org.lynxz.forwardsms.R
import org.lynxz.forwardsms.ui.BaseFragment

/**
 * 扩展设置项
 * 如: 低电量提醒,来电提醒等
 * */
class OtherSettingFragment : BaseFragment() {
    private val otherSettingViewModel by lazy {
        ViewModelProviders.of(this).get(OtherSettingViewModel::class.java)
    }

    override fun getLayoutRes() = R.layout.fragment_other_setting

    override fun afterViewCreated(view: View) {
        otherSettingViewModel.text.observe(viewLifecycleOwner, Observer {
            text_slideshow.text = it
        })
    }
}