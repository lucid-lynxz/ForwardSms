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
import org.lynxz.baseimlib.bean.ImType
import org.lynxz.forwardsms.R
import org.lynxz.forwardsms.bean.ImSetting
import org.lynxz.forwardsms.databinding.FragmentForwardSettingBinding
import org.lynxz.forwardsms.para.ImSettingManager
import org.lynxz.forwardsms.ui.BaseBindingFragment
import org.lynxz.forwardsms.ui.activity.BaseImSettingBindingActivity
import org.lynxz.forwardsms.ui.activity.DingdingSettingActivity
import org.lynxz.forwardsms.ui.activity.FeishuSettingActivity
import org.lynxz.forwardsms.ui.activity.TelegramSettingActivity
import org.lynxz.forwardsms.ui.widget.AppAdapterStatusItem

/**
 * 消息转发配置
 * 支持: 邮箱/飞书/钉钉/telegram bot...
 * */
class ForwardSettingFragment : BaseBindingFragment<FragmentForwardSettingBinding>() {
    private val forwardSettingViewModel by lazy {
        ViewModelProviders.of(requireActivity()).get(ForwardSettingViewModel::class.java)
    }

    private val dslAdapter =
        DslAdapter().apply {
            dslAdapterStatusItem = AppAdapterStatusItem()
            this.setAdapterStatus(DslAdapterStatusItem.ADAPTER_STATUS_EMPTY)
        }

    override fun getLayoutRes() = R.layout.fragment_forward_setting

    override fun afterViewCreated(view: View) {
        dataBinding.rvConfig.apply {
            addItemDecoration(DslItemDecoration())
            setHasFixedSize(true)
            layoutManager = LinearLayoutManager(activity)
            adapter = dslAdapter
        }

        // 显示已添加的平台信息
        ImSettingManager.imSettingMapLiveData().observe(
            requireActivity(),
            Observer<MutableMap<String, ImSetting?>> { settingMap ->
                dslAdapter.resetItem(listOf())
                dslAdapter.setAdapterStatus(if (settingMap.isNullOrEmpty()) DslAdapterStatusItem.ADAPTER_STATUS_EMPTY else DslAdapterStatusItem.ADAPTER_STATUS_NONE)
                settingMap.forEach {
                    val imType = it.key
                    it.value?.let { setting ->
                        dslAdapter.dslItem(R.layout.item_im_setting_list) {
                            itemBindOverride = { itemHolder, _, _, _ ->

                                // 设置平台信息和接收人信息
                                itemHolder.v<TextView>(R.id.tv_platform_name)?.text = setting.imType
                                itemHolder.v<TextView>(R.id.tv_target_user_name)?.text =
                                    setting.targetUserName

                                // 切换启用状态监听
                                itemHolder.v<SwitchButton>(R.id.sb_enable_status)?.apply {
                                    isChecked = setting.enable
                                    setOnCheckedChangeListener { _, isChecked ->
                                        forwardSettingViewModel.activeIm(imType, isChecked)
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
            })

        // 右下角添加按钮列表
        dataBinding.fabDingding.setOnClickListener {
            dataBinding.menuPlatformAdd.close(true)
            targetSettingActivityPage = DingdingSettingActivity::class.java
        }
        dataBinding.fabFeishu.setOnClickListener {
            dataBinding.menuPlatformAdd.close(true)
            targetSettingActivityPage = FeishuSettingActivity::class.java
        }
        dataBinding.fabTelegram.setOnClickListener {
            dataBinding.menuPlatformAdd.close(true)
            targetSettingActivityPage = TelegramSettingActivity::class.java
        }

        // 动画结束后再跳转到对应的设置二面
        dataBinding.menuPlatformAdd.setClosedOnTouchOutside(true) // 点击非按钮区,关闭菜单
        dataBinding.menuPlatformAdd.setOnMenuToggleListener { opened ->
            if (!opened) {
                targetSettingActivityPage?.let {
                    startActivity(Intent(activity, it))
                }
            }
            targetSettingActivityPage = null
        }
    }

    // 目标设置页面
    private var targetSettingActivityPage: Class<out BaseImSettingBindingActivity<*>?>? = null
}