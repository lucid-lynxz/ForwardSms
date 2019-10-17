package org.lynxz.forwardsms.widget

import android.app.Service
import android.content.Intent
import android.database.ContentObserver
import android.net.Uri
import android.os.Handler
import android.os.IBinder
import android.os.Message
import androidx.lifecycle.Observer
import org.lynxz.baseimlib.IMManager
import org.lynxz.baseimlib.bean.ImType
import org.lynxz.baseimlib.bean.SendMessageReqBean
import org.lynxz.forwardsms.bean.SmsDetail
import org.lynxz.forwardsms.network.SmsConstantParas
import org.lynxz.forwardsms.util.Logger
import org.lynxz.forwardsms.viewmodel.ScreenStateViewModel
import org.lynxz.forwardsms.viewmodel.SmsViewModel

class SmsService : Service() {
    private val TAG = "SmsService"


    // 收到新短信息事件监听
    private val smsReceiveLiveData by lazy { SmsViewModel.getReceivedSms() }


    // 屏幕锁屏开屏事件监听
    private val screenStateLiveData by lazy { ScreenStateViewModel.getScreenStateLiveData() }

    // 收到新短信息后转发给服务器
    private val smsObserver = Observer<SmsDetail> {
        val body = SendMessageReqBean().apply {
            name = SmsConstantParas.tgUserNme
            content = it.format()
        }

        // tg发送失败则尝试使用钉钉发送
        IMManager.sendTextMessage(ImType.TG, body) {
            if (!it.ok) {
                IMManager.sendTextMessage(ImType.DingDing, body.apply {
                    name = SmsConstantParas.ddName
                })
            }
        }
//        HttpManager.sendMessage(it.format(), SmsConstantParas.tgUserNme)
    }

    // 锁屏时启动单像素页面保活, 开屏后关闭单像素页面,避免影响屏幕事件
//    private val screenObserver = Observer<Boolean> { screenOn ->
//        Logger.d(TAG, "screenObserver $screenOn")
//        if (screenOn) {
//            OnePixelActManager.finishActivity()
//        } else {
//            OnePixelActManager.startActivity()
//        }
//    }


    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onCreate() {
        super.onCreate()
        smsReceiveLiveData.observeForever(smsObserver)
//        screenStateLiveData.observeForever(screenObserver)
        Logger.d(TAG, "sms service created")
    }

    override fun onDestroy() {
        super.onDestroy()
        smsReceiveLiveData.removeObserver(smsObserver)
//        screenStateLiveData.removeObserver(screenObserver)
        Logger.d(TAG, "sms service destroyed")
    }
}