package org.lynxz.forwardsms

import android.app.Application
import android.app.Service
import android.content.ComponentName
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import org.lynxz.forwardsms.util.Logger
import org.lynxz.forwardsms.viewmodel.ScreenStateViewModel
import org.lynxz.forwardsms.viewmodel.SmsViewModel
import org.lynxz.forwardsms.widget.OnePixelActManager
import org.lynxz.forwardsms.widget.SmsNotificationListenerService
import org.lynxz.forwardsms.widget.SmsService

class SmsApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        OnePixelActManager.init(this)
        SmsViewModel.init(this)
        ScreenStateViewModel.init(this)

        // 与application生命周期保持一致
        bindService(Intent(this, SmsService::class.java), smsServiceConn, Service.BIND_AUTO_CREATE)

        bindService(
            Intent(this, SmsNotificationListenerService::class.java),
            notificationServiceConn,
            Service.BIND_AUTO_CREATE
        )
    }

    private val smsServiceConn = object : ServiceConnection {
        override fun onServiceDisconnected(name: ComponentName?) {
            Logger.i("smsApplication smsServiceConn onServiceDisconnected $name")
        }

        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
        }
    }


    private val notificationServiceConn = object : ServiceConnection {
        override fun onServiceDisconnected(name: ComponentName?) {
            Logger.i("smsApplication notificationServiceConn onServiceDisconnected $name")
        }

        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
        }
    }
}