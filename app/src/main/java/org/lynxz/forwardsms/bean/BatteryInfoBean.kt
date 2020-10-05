package org.lynxz.forwardsms.bean

/**
 * 当前电池状态信息
 * */
data class BatteryInfoBean(
    var level: Int = -1, // 当前电量百分比
    var charging: Boolean = false, // 是否正在充电
    var temperature: Int = -1 // 电池温度
)
