package org.lynxz.forwardsms.para

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData

/**
 * 通知提醒设置,包括:
 * 1. 可见听的通知栏消息app包名信息
 * 2. 来电/未接来电提醒
 * */
class NotificationSettingManager :
    AbsSpSettingInfoManager<NotificationSettingManager.NotificationPara>() {
    data class NotificationPara(
        var notificationListenerPkgMap: MutableMap<String, Boolean> = mutableMapOf() // 需要监听通知栏通知的app包名启用信息
    )

    override fun getParaFromSp(paraKey: String) = getSecuritySp().getPreference(
        paraKey, NotificationPara::class.java,
        NotificationPara()
    )!!

    private val notificationParaLiveData = MutableLiveData(paraBean)

    fun getNotificationParaLiveData(): LiveData<NotificationPara> = notificationParaLiveData
}