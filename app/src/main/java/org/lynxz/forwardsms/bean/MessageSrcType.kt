package org.lynxz.forwardsms.bean

/**
 * 待转发的消息常见类型
 * */
object MessageSrcType {
    const val SMS = "com.android.mms" // 短信
    const val WECHAT = "com.tencent.mm" // 微信
    const val BATTERY_LISTENER = "battery_changed_listener" // 低电量监听
}