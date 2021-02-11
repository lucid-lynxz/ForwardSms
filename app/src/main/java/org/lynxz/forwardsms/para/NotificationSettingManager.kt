package org.lynxz.forwardsms.para

import android.app.Notification
import android.app.Service
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.text.TextUtils
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import org.lynxz.forwardsms.bean.SmsDetail
import org.lynxz.forwardsms.bean.emptyimpl.EmptyServiceConnection
import org.lynxz.forwardsms.observer.IAppNotificationObserver
import org.lynxz.forwardsms.ui.widget.SmsNotificationListenerService

import org.lynxz.forwardsms.validation.VerifyActionManager
import org.lynxz.forwardsms.viewmodel.GlobalParaUtil
import org.lynxz.utils.log.LoggerUtil

/**
 * 通知提醒设置,包括:
 * 1. 可见听的通知栏消息app包名信息
 * 2. 来电/未接来电提醒
 * */
class NotificationSettingManager :
    AbsSpSettingInfoManager<NotificationSettingManager.NotificationPara>(),
    IAppNotificationObserver {
    private val TAG = "NotificationSettingMana"

    data class NotificationPara(
        var enable: Boolean = false, // 是否启用
        var notificationListenerPkgMap: MutableMap<String, Boolean> = mutableMapOf() // 需要监听通知栏通知的app包名启用信息
    )

    override fun getParaFromSp(paraKey: String) = getSecuritySp().getPreference(
        paraKey, NotificationPara::class.java,
        NotificationPara()
    )!!

    // 功能配置信息
    private val notificationParaLiveData = MutableLiveData(paraBean)

    fun getNotificationParaLiveData(): LiveData<NotificationPara> = notificationParaLiveData


    /**
     * 注册通知栏监听
     * 需要自行申请权限
     * 通过 [isNotificationListenerEnabled] 进行判断是否授予该选项
     * 通过 [enableMonitorNotification] 跳转权限设置页面
     */
    fun start(context: Context) {
        SmsNotificationListenerService.registerAppObserver(this)
        context.applicationContext.apply {
            bindService(
                Intent(this, SmsNotificationListenerService::class.java),
                EmptyServiceConnection(),
                Service.BIND_AUTO_CREATE
            )
        }
    }

    /**
     * 判断是否打开了通知监听权限
     */
    fun isNotificationListenerEnabled(context: Context): Boolean {
        val flat = Settings.Secure.getString(
            context.contentResolver,
            "enabled_notification_listeners"
        )
        if (!flat.isNullOrBlank()) {
            val names = flat.split(":".toRegex()).toTypedArray()
            for (i in names.indices) {
                ComponentName.unflattenFromString(names[i])?.let {
                    if (TextUtils.equals(context.applicationInfo.packageName, it.packageName)) {
                        return true
                    }
                }
            }
        }
        return false
    }

    /**
     * 允许读取通知栏信息
     * */
    fun enableMonitorNotification(context: Context): Boolean {
        if (isNotificationListenerEnabled(context)) {
            return true
        }

        try {
            val intent: Intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
                Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
            } else {
                Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS")
            }
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.applicationContext.startActivity(intent)
            return true
        } catch (e: Exception) {
            e.printStackTrace()
            return false
        }
    }

    // 监听到通知栏新消息时回调
    override fun onReceiveAppNotification(
        pkgName: String,
        sbn: StatusBarNotification?,
        rankingMap: NotificationListenerService.RankingMap?
    ) {
        LoggerUtil.w(TAG, "获取到notification包名: $pkgName")
        val notification = sbn?.notification
        // 微信通知标题信息:
        // 群消息时表示群名称
        // 普通好友消息时表示好友名称
        val title = notification?.extras?.get(Notification.EXTRA_TITLE)
        notification?.tickerText?.toString()?.let {
            GlobalParaUtil.postSmsDetail(SmsDetail().apply {
                body = it
                from = "$title($pkgName)"
                srcType = pkgName
                forward = VerifyActionManager.isValid(this)
            })
        }
    }
}