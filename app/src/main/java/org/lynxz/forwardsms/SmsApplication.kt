package org.lynxz.forwardsms

import android.app.Application
import android.content.ComponentName
import android.content.ServiceConnection
import android.os.IBinder
import org.lynxz.forwardsms.para.ImSettingManager
import org.lynxz.forwardsms.ui.widget.OnePixelActManager
import org.lynxz.forwardsms.validation.MosaicVerify
//
import org.lynxz.forwardsms.validation.SrcTypeVerify
import org.lynxz.forwardsms.validation.TimeVerify
import org.lynxz.forwardsms.viewmodel.GlobalParaUtil
import org.lynxz.forwardsms.viewmodel.ScreenStateViewModel
import org.lynxz.utils.log.LogLevel
import org.lynxz.utils.log.LogPersistenceImpl
import org.lynxz.utils.log.LoggerUtil

class SmsApplication : Application() {

    companion object {
        lateinit var app: SmsApplication
        private const val TAG = "SmsLog"
    }

    override fun onCreate() {
        super.onCreate()
        app = this

        // 开启日志持久化
        val logDirPath = getExternalFilesDir(null)?.absolutePath ?: filesDir.absolutePath
        LoggerUtil.init(
            LogLevel.DEBUG, TAG,
            LogPersistenceImpl("$logDirPath/LogPersistence/")
                .setLevel(LogLevel.WARN)
        )
        LoggerUtil.w(TAG, "LogPersistence inited, dir path:$logDirPath")

        // 初始化IM配置信息
        ImSettingManager.initPara(this)

        // 初始化短信监听
        OnePixelActManager.init(this)

        // 短信内容过滤处理
        GlobalParaUtil.init(this)
            .addVerifyActionIfAbsent(TimeVerify)
            .addVerifyActionIfAbsent(SrcTypeVerify)
            .addVerifyActionIfAbsent(MosaicVerify)

        ScreenStateViewModel.init(this)

//        // 与application生命周期保持一致
//        bindService(
//            Intent(this, ForwardService::class.java),
//            smsServiceConn,
//            Service.BIND_AUTO_CREATE
//        )
//
//        // 通知栏监听
//        bindService(
//            Intent(this, SmsNotificationListenerService::class.java),
//            notificationServiceConn,
//            Service.BIND_AUTO_CREATE
//        )
    }

    private val smsServiceConn = object : ServiceConnection {
        override fun onServiceDisconnected(name: ComponentName?) {
            LoggerUtil.w(TAG, "smsApplication smsServiceConn onServiceDisconnected $name")
        }

        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            LoggerUtil.w(TAG, "smsApplication smsServiceConn onServiceConnected $name")
        }
    }


    private val notificationServiceConn = object : ServiceConnection {
        override fun onServiceDisconnected(name: ComponentName?) {
            LoggerUtil.w(TAG, "smsApplication notificationServiceConn onServiceDisconnected $name")
        }

        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            LoggerUtil.w(TAG, "smsApplication notificationServiceConn onServiceConnected $name")
        }
    }
}