package org.lynxz.forwardsms.ui.fragment.forwardsetting

import android.content.Intent
import android.view.View
import android.widget.TextView
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.LinearLayoutManager
import com.angcyo.dsladapter.DslAdapter
import com.angcyo.dsladapter.DslAdapterStatusItem
import com.angcyo.dsladapter.DslItemDecoration
import com.angcyo.dsladapter.dslItem
import com.suke.widget.SwitchButton
import kotlinx.android.synthetic.main.fragment_forward_setting.*
import org.lynxz.baseimlib.bean.ImType
import org.lynxz.forwardsms.R
import org.lynxz.forwardsms.bean.ImSetting
import org.lynxz.forwardsms.para.GlobalImSettingPara
import org.lynxz.forwardsms.showToast
import org.lynxz.forwardsms.ui.BaseFragment
import org.lynxz.forwardsms.ui.activity.DingdingSettingActivity
import org.lynxz.forwardsms.ui.activity.FeishuSettingActivity
import org.lynxz.forwardsms.ui.activity.TelegramSettingActivity
import org.lynxz.forwardsms.ui.widget.AppAdapterStatusItem

/**
 * 消息转发配置
 * 支持: 邮箱/飞书/钉钉/telegram bot...
 * */
class ForwardSettingFragment : BaseFragment() {
    private val forwardSettingViewModel by lazy {
        ViewModelProviders.of(this).get(ForwardSettingViewModel::class.java)
    }

    private val dslAdapter =
        DslAdapter().apply {
            dslAdapterStatusItem = AppAdapterStatusItem()
            this.setAdapterStatus(DslAdapterStatusItem.ADAPTER_STATUS_EMPTY)
        }

    override fun getLayoutRes() = R.layout.fragment_forward_setting

    override fun afterViewCreated(view: View) {
//        forwardSettingViewModel.text.observe(viewLifecycleOwner, Observer {
//            text_gallery.text = it
//        })

        rv_config.apply {
            addItemDecoration(DslItemDecoration())
            setHasFixedSize(true)
            layoutManager = LinearLayoutManager(activity)
            adapter = dslAdapter
        }

        GlobalImSettingPara.imSettingMapLiveData().observe(activity!!,
            Observer<MutableMap<String, ImSetting?>> { settingMap ->
                dslAdapter.resetItem(listOf())
                dslAdapter.setAdapterStatus(if (settingMap.isNullOrEmpty()) DslAdapterStatusItem.ADAPTER_STATUS_EMPTY else DslAdapterStatusItem.ADAPTER_STATUS_NONE)
                settingMap.forEach {
                    val imType = it.key
                    it.value?.let { setting ->
                        dslAdapter.dslItem(R.layout.item_im_setting_list) {
                            itemBindOverride = { itemHolder, itemPosition, _, _ ->

                                // 设置平台信息和接收人信息
                                itemHolder.v<TextView>(R.id.tv_platform_name)?.text = setting.imType
                                itemHolder.v<TextView>(R.id.tv_target_user_name)?.text =
                                    setting.targetUserName

                                // 切换启用状态监听
                                itemHolder.v<SwitchButton>(R.id.sb_enable_status)?.apply {
                                    isChecked = setting.enable
                                    setOnCheckedChangeListener { _, isChecked ->
                                        GlobalImSettingPara.updateImSetting(imType) { setting ->
                                            setting.enable = isChecked
                                        }
                                    }
                                }

                                // 行点击跳转修改页面
                                itemHolder.clickItem { _ ->
                                    val targetClz = when (imType) {
                                        ImType.DingDing -> DingdingSettingActivity::class.java
                                        ImType.TG -> TelegramSettingActivity::class.java
                                        ImType.FeiShu -> FeishuSettingActivity::class.java
                                        else -> null
                                    }
                                    targetClz?.let { clz -> startActivity(Intent(activity, clz)) }
                                }
                            }
                        }
                    }
                }
                showToast("配置发生变化")
            })

        fab_add_config.setOnClickListener {
//            showFragment(SetupDingdingFragment())
//            startActivity(Intent(activity, DingdingSettingActivity::class.java))
            startActivity(Intent(activity, TelegramSettingActivity::class.java))
//            startActivity(Intent(activity, FeishuSettingActivity::class.java))
        }
    }
}