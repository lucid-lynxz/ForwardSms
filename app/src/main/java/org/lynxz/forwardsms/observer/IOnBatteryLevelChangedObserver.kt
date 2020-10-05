package org.lynxz.forwardsms.observer

import org.lynxz.forwardsms.bean.BatteryInfoBean

/**
 * 电池电量变化是通知
 * */
interface IOnBatteryLevelChangedObserver {
    fun onBatteryChanged(batteryInfo: BatteryInfoBean)
}