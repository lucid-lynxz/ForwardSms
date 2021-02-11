package org.lynxz.forwardsms.para

import android.content.Intent
import android.content.IntentFilter
import androidx.databinding.Observable
import androidx.databinding.ObservableBoolean
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import org.lynxz.forwardsms.SmsApplication
import org.lynxz.forwardsms.bean.BatteryInfoBean
import org.lynxz.forwardsms.observer.IOnBatteryLevelChangedObserver
import org.lynxz.forwardsms.para.BatteryListenerManager.batteryInfoLiveData
import org.lynxz.forwardsms.para.BatteryListenerManager.batterySettingLiveData
import org.lynxz.forwardsms.para.BatteryListenerManager.updateAndSave
import org.lynxz.forwardsms.receiver.BatteryLevelChangeReceiver

import org.lynxz.utils.log.LoggerUtil

/**
 * 低电量监听
 *
 * 通过 [batterySettingLiveData] 来获取电量设置变化
 * 通过 [batteryInfoLiveData] 来获取低电量通知回调(未充电,且电量低于设定值时,每变化1%都会更新)
 * 通过 [updateAndSave] 来更新低电量设置信息
 * */
object BatteryListenerManager :
    AbsSpSettingInfoManager<BatteryListenerManager.BatterySettingBean>() {
    private const val TAG = "BatteryListenerManager"

    /**
     * 低电量监听
     * */
    data class BatterySettingBean(
        var enableFullyChargeNotify: ObservableBoolean = ObservableBoolean(false), // 是否电量充满电时提醒
        var enable: ObservableBoolean = ObservableBoolean(false), // 是否启用低电量监控
        var lowLevelStr: String = "20" // 低电量值,单位:%, 默认为 20%
    ) {
        fun getLowLevelValue() = lowLevelStr.toInt()
    }

    override fun getParaFromSp(paraKey: String) =
        getSecuritySp().getPreference(
            paraKey,
            BatterySettingBean::class.java,
            BatterySettingBean()
        )!!


    // 低电量监听配置信息
    private val innerBatterySettingLiveData = MutableLiveData<BatterySettingBean>(paraBean)
    val batterySettingLiveData = innerBatterySettingLiveData as LiveData<BatterySettingBean>

    // 当前电池信息
    private val innerBatteryInfoLiveData = MutableLiveData<BatteryInfoBean>()
    val batteryInfoLiveData = innerBatteryInfoLiveData as LiveData<BatteryInfoBean>

    // 电量变化监听
    // 注意写在 init{} 前方
    // 尽在未充电状态下提示
    private val batteryLevelChangeReceiver =
        BatteryLevelChangeReceiver(object : IOnBatteryLevelChangedObserver {
            override fun onBatteryChanged(batteryInfo: BatteryInfoBean) {
                // 低电量提醒: 自由启用时才会回调
                if (!batteryInfo.charging && batteryInfo.level <= paraBean.getLowLevelValue()) {
//                if (batteryInfo.level <= paraBean.getLowLevelValue()) {
                    innerBatteryInfoLiveData.value = batteryInfo
                }

                // 前电量充满时提醒
                if (paraBean.enableFullyChargeNotify.get() && batteryInfo.charging && batteryInfo.level >= 100) {
                    innerBatteryInfoLiveData.value = batteryInfo
                }
            }
        })

    init {
        // 首次根据sp配置启用广播监听,状态变化时同步启用/禁用receiver
        toggleBatterReceiver(paraBean.enable.get())
        paraBean.enable.addOnPropertyChangedCallback(object :
            Observable.OnPropertyChangedCallback() {
            override fun onPropertyChanged(sender: Observable?, propertyId: Int) {
                val enable = paraBean.enable.get()
                toggleBatterReceiver(enable)
                savePara()
            }
        })

        paraBean.enableFullyChargeNotify.addOnPropertyChangedCallback(object :
            Observable.OnPropertyChangedCallback() {
            override fun onPropertyChanged(sender: Observable?, propertyId: Int) {
                savePara()
            }
        })
    }

    /**
     * (更新后)保存参数到sp
     * */
    fun updateAndSave(recookSettingPara: RecookPara<BatterySettingBean>? = null) {
        recookSettingPara?.let {
            it.invoke(paraBean)
            innerBatterySettingLiveData.value = paraBean
            toggleBatterReceiver(paraBean.enable.get())
        }
        savePara()
    }

    // 是否已注册过电量变化广播receiver
    private var hasRegisterReceiver = false

    private fun toggleBatterReceiver(enable: Boolean) {
        LoggerUtil.d(
            TAG,
            "toggleBatterReceiver enable=$enable, hasRegisterReceiver=$hasRegisterReceiver"
        )
        if (enable && !hasRegisterReceiver) {
            SmsApplication.app.registerReceiver(
                batteryLevelChangeReceiver,
                IntentFilter(Intent.ACTION_BATTERY_CHANGED)
            )
            hasRegisterReceiver = true
        }

        if (!enable && hasRegisterReceiver) {
            SmsApplication.app.unregisterReceiver(batteryLevelChangeReceiver)
            hasRegisterReceiver = false
        }
    }
}