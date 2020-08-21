package org.lynxz.forwardsms.ui.fragment.forwardsetting

import android.view.View
import androidx.lifecycle.ViewModelProviders
import kotlinx.android.synthetic.main.fragment_forward_setting.*
import org.lynxz.forwardsms.R
import org.lynxz.forwardsms.ui.BaseFragment
import org.lynxz.forwardsms.ui.fragment.SetupDingdingFragment

/**
 * 消息转发配置
 * 支持: 邮箱/飞书/钉钉/telegram bot...
 * */
class ForwardSettingFragment : BaseFragment() {
    private val forwardSettingViewModel by lazy {
        ViewModelProviders.of(this).get(ForwardSettingViewModel::class.java)
    }

    override fun getLayoutRes() = R.layout.fragment_forward_setting

    override fun afterViewCreated(view: View) {
//        forwardSettingViewModel.text.observe(viewLifecycleOwner, Observer {
//            text_gallery.text = it
//        })

        fab_add_config.setOnClickListener {
            showFragment(SetupDingdingFragment())
        }
    }
}