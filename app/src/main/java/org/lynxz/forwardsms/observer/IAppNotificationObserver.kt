package org.lynxz.forwardsms.observer

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification

/**
 * app通知栏推送消息内容监听
 * */
interface IAppNotificationObserver {
    fun onReceiveAppNotification(
        pkgName: String, // app包名
        sbn: StatusBarNotification?,
        rankingMap: NotificationListenerService.RankingMap?
    )
}