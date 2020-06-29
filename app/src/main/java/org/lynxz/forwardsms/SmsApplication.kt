package org.lynxz.forwardsms

import android.app.Application
import android.app.Service
import android.content.ComponentName
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import org.lynxz.forwardsms.util.LogPersistenceUtil
import org.lynxz.forwardsms.util.LoggerUtil
import org.lynxz.forwardsms.viewmodel.ScreenStateViewModel
import org.lynxz.forwardsms.viewmodel.SmsViewModel
import org.lynxz.forwardsms.widget.OnePixelActManager
import org.lynxz.forwardsms.widget.SmsNotificationListenerService
import org.lynxz.forwardsms.widget.SmsService

class SmsApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        // 开启日志持久化
        LogPersistenceUtil.getInstance()
            .setPersistenceLevel(LoggerUtil.LEVEL_WARN)
            .init(this)

        // 初始化短信监听
        OnePixelActManager.init(this)
        SmsViewModel.init(this)
        ScreenStateViewModel.init(this)

        // 与application生命周期保持一致
        bindService(Intent(this, SmsService::class.java), smsServiceConn, Service.BIND_AUTO_CREATE)

        // 通知栏监听
        bindService(
            Intent(this, SmsNotificationListenerService::class.java),
            notificationServiceConn,
            Service.BIND_AUTO_CREATE
        )
    }

    private val smsServiceConn = object : ServiceConnection {
        override fun onServiceDisconnected(name: ComponentName?) {
            LoggerUtil.i("smsApplication smsServiceConn onServiceDisconnected $name")
        }

        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
        }
    }


    private val notificationServiceConn = object : ServiceConnection {
        override fun onServiceDisconnected(name: ComponentName?) {
            LoggerUtil.i("smsApplication notificationServiceConn onServiceDisconnected $name")
        }

        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
        }
    }
}