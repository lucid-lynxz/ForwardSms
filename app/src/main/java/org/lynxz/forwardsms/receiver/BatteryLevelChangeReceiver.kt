package org.lynxz.forwardsms.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.BatteryManager
import org.lynxz.forwardsms.bean.BatteryInfoBean
import org.lynxz.forwardsms.observer.IOnBatteryLevelChangedObserver

/**
 * 电池电量变化监听
 * */
class BatteryLevelChangeReceiver(val observer: IOnBatteryLevelChangedObserver? = null) :
    BroadcastReceiver() {

    // 记录上次收到广播的电量信息,电量有变化时才回调
    private var lastLevel = -1

    override fun onReceive(context: Context?, intent: Intent?) {
        if (intent?.action == Intent.ACTION_BATTERY_CHANGED) {
            val extras = intent.extras ?: return
            val level = extras.getInt(BatteryManager.EXTRA_LEVEL) // 当前电量百分比
//            LoggerUtil.w("xxx batteryLevel=$level")
            if (level == lastLevel) return // 电量有变化时才回调,否则过于频繁
            lastLevel = level

            val temperature = extras.getInt(BatteryManager.EXTRA_TEMPERATURE) // 电池温度
            val status = extras.getInt(BatteryManager.EXTRA_STATUS) //电池状态
            val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING; // 是否正在充电

            observer?.onBatteryChanged(BatteryInfoBean(level, isCharging, temperature))
        }
    }
}