package org.lynxz.forwardsms.widget

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import org.lynxz.forwardsms.bean.SmsDetail
import org.lynxz.forwardsms.observer.IAppNotificationObserver
import org.lynxz.forwardsms.observer.ISmsReceiveObserver
import org.lynxz.forwardsms.util.Logger

/**
 * 通过查看通知栏信息读取短信，但只能读取短信内容，发信人信息无
 * 使用方法:
 * 1. 在 manifest中注册service
 * <pre>
 *      <service
 *          android:name=".widget.SmsNotificationListenerService"
 *          android:label="@string/app_name"
 *          android:permission="android.permission.BIND_NOTIFICATION_LISTENER_SERVICE">
 *          <intent-filter>
 *              <action android:name="android.service.notification.NotificationListenerService" />
 *          </intent-filter>
 *      </service>
 * </pre>
 *
 * 2. 手动允许通知条访问权限
 * <pre>
 *      try {
 *          val intent: Intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
 *              Intent(ACTION_NOTIFICATION_LISTENER_SETTINGS)
 *          } else {
 *              Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS")
 *          }
 *          startActivity(intent)
 *      } catch (e: Exception) {
 *          e.printStackTrace()
 *          tv_info.text = e.message
 *      }
 * </pre>
 *
 * 3. 当 [onListenerConnected] 后， 通过 [onNotificationPosted] 获取制定应用的通知栏信息
 * */
class SmsNotificationListenerService : NotificationListenerService() {
    companion object {
        private const val TAG = "SmsNotificationListenerService"

        // 短信监听
        private var smsObserver: ISmsReceiveObserver? = null

        // 普通app通知栏信息监听(包括短信)
        private var appObserver: IAppNotificationObserver? = null

        /**
         * 注册短信内容回调监听器
         * */
        fun registerSmsObserver(observer: ISmsReceiveObserver?) {
            this.smsObserver = observer
        }

        /**
         * 注册普通app通知回调监听器
         * */
        fun registerAppObserver(observer: IAppNotificationObserver?) {
            this.appObserver = observer
        }
    }

    override fun onListenerConnected() {
        super.onListenerConnected()
        Logger.d(TAG, "onListenerConnected")
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?, rankingMap: RankingMap?) {
        super.onNotificationPosted(sbn, rankingMap)
//        微信 com.tencent.mm
//        短信 com.android.mms
//        sbn.packageName // 应用通知的包名

        val pkgName = sbn?.packageName
        Logger.d(TAG, "onNotificationPosted $pkgName $appObserver")
        if (pkgName.isNullOrBlank()) {
            return
        }

        appObserver?.onReceiveAppNotification(pkgName, sbn, rankingMap)
        if (pkgName != "com.android.mms") {
            return
        }
        sbn.postTime // 1573881489793
        Logger.d(TAG, "$sbn $rankingMap")
        val notification = sbn.notification
        notification?.tickerText?.toString()?.let {
            if (it.isNotBlank()) {
                Logger.d(TAG, "tickerText: $it")
                smsObserver?.onReceiveSms(SmsDetail().apply {
                    body = it
                })
            }
        }


//        if (content.isNullOrBlank()) {
//            val notificationInfo = getNotificationInfo(notification)
//            notificationInfo?.forEach {
//                Logger.d(TAG, "-- ${it.key} ==> ${it.value}")
//            }
//        }
    }

    /**
     * 反射获取通知栏消息
     * 参考文章: https://juejin.im/post/5b8cec04518825273f07d0c3#heading-1
     * */
    private fun getNotificationInfo(notification: Notification?): Map<String, Any>? {
        val resultMap = mutableMapOf<String, Any>()

        try {
            notification?.contentView?.let { view ->
                val secretClass: Class<Any> = view.javaClass

                var key = 0

                secretClass.declaredFields.forEach { outerField ->
                    if ("mActions" != outerField.name) {
                        return@forEach
                    }

                    outerField.isAccessible = true
                    val actions = outerField.get(view) as ArrayList<Any>
                    var value: Any? = null
                    var type = 0
                    actions.forEach { action ->
                        actions.javaClass.declaredFields.forEach { innerField ->
                            innerField.isAccessible = true
                            when (innerField.name) {
                                "value" -> value = innerField.get(action)
                                "type" -> type = innerField.getInt(action)
                            }
                        }

                        // 经验所得 type 等于9 10为短信title和内容，不排除其他厂商拿不到的情况
                        if (type == 9 || type == 10) {
                            when (key) {
                                0 -> resultMap["title"] = value ?: ""
                                1 -> resultMap["text"] = value ?: ""
                                else -> resultMap["$key"] = value ?: ""
                            }
                            key++
                        }
                    }
                    key = 0
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return resultMap

    }
}