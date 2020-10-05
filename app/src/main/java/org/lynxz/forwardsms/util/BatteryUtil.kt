package org.lynxz.forwardsms.util

import android.content.Context
import android.os.BatteryManager
import org.lynxz.forwardsms.SmsApplication


object BatteryUtil {
    private val manager by lazy { SmsApplication.app.getSystemService(Context.BATTERY_SERVICE) as BatteryManager }

    /**
     * 获取电池百分比, 如49,负数表示无效
     */
    fun getBatteryCapacity(): Int {
        return manager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY) //当前电量百分比
    }

    fun isCharging(context: Context = SmsApplication.app) {
//return manager.getpro
    }

}