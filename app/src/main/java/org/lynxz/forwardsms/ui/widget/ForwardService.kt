package org.lynxz.forwardsms.ui.widget

import android.app.Service
import android.content.Intent
import android.os.IBinder
import androidx.lifecycle.Observer
import org.lynxz.baseimlib.IMManager
import org.lynxz.baseimlib.bean.ImType
import org.lynxz.baseimlib.bean.SendMessageReqBean
import org.lynxz.baseimlib.convert2Str
import org.lynxz.forwardsms.bean.SmsDetail
import org.lynxz.forwardsms.para.ImSettingManager
import org.lynxz.forwardsms.util.NotificationUtils
import org.lynxz.forwardsms.viewmodel.GlobalParaUtil
import org.lynxz.forwardsms.viewmodel.ScreenStateViewModel
import org.lynxz.utils.log.LoggerUtil

/**
 * 开启service用于转发消息到IM
 * */
class ForwardService : Service() {
    private val TAG = "ForwardService"


    // 收到新短信息事件监听
    private val smsReceiveLiveData by lazy { GlobalParaUtil.getReceivedSms() }


    // 屏幕锁屏开屏事件监听
    private val screenStateLiveData by lazy { ScreenStateViewModel.getScreenStateLiveData() }

    // 收到新短信息后转发给服务器
    private val smsObserver = Observer<SmsDetail> {
        if (!it.forward) { // 不符合转发条件,则不做转发操作
            return@Observer
        }

        val body = SendMessageReqBean().apply {
            content = it.format()
        }

        // 遍历所有支持的平台,尝试进行发送数据发送
        val platformMap = ImSettingManager.imSettingMapLiveData().value
        platformMap?.forEach { entry ->
            val type = entry.key
            val imSetting = entry.value

            if (imSetting?.enable == true) {
                IMManager.sendTextMessage(type, body.duplicate().apply {
                    name = ImSettingManager.getImSetting(type)?.targetUserName
                    imType = type
                }) { result ->
                    LoggerUtil.w(
                        TAG,
                        "sendTextMsg by $type ,result: ${convert2Str(result)}, ${it.body}"
                    )
                    NotificationUtils.getInstance(null)
                        ?.sendNotification("消息转发", "$type:${result.detail}", 100)
                }

//                when (type) {
//                    ImType.TG -> {
//                        IMManager.sendTextMessage(type, body.duplicate().apply {
//                            name = GlobalImSettingPara.getImSetting(type)?.targetUserName
//                            imType = type
//                        }) {
//                            LoggerUtil.w(TAG, "sendTextMsg by $type result: ${convert2Str(it)}")
//                        }
//                    }
//                    ImType.FeiShu -> {
//                        IMManager.sendTextMessage(type, body.duplicate().apply {
//                            name = GlobalImSettingPara.getImSetting(type)?.targetUserName
//                            imType = type
//                        }) {
//                            LoggerUtil.w(TAG, "sendTextMsg by $type result: ${convert2Str(it)}")
//                        }
//                    }
//                    ImType.DingDing -> {
//                        sendByDingding(body.duplicate().apply {
//                            name = GlobalImSettingPara.getImSetting(type)?.targetUserName
//                            imType = type
//                        }, 1, 3)
//                    }
//                }
            }
        }
    }

    /**
     * 通过钉钉发送消息,若发送失败,则尝试刷新钉钉token及通讯录后,再次尝试发送, 直到次数达到最大
     * @param body 钉钉消息请求体
     * @param curIndex 当前尝试次数,没法送一次递增1,直到达到 maxIndex
     * @param maxIndex 最大尝试次数,达到此次数后不再尝试,默认为3
     * */
    private fun sendByDingding(body: SendMessageReqBean, curIndex: Int, maxIndex: Int = 3) {
        if (curIndex >= maxIndex) {
            print("sendByDingding fail as curIndex($curIndex) reach maxIndex")
            return
        }

        IMManager.sendTextMessage(ImType.DingDing, body) { ddResult ->
            LoggerUtil.w(
                TAG,
                "sendTextMsg by dingding(curIndex=$curIndex) result: ${convert2Str(ddResult)}, body:${
                    convert2Str(
                        body
                    )
                }"
            )
            if (ddResult.ok) {
                return@sendTextMessage
            }
            // 刷新token并尝试重发该消息
            IMManager.refresh(ImType.DingDing) {
                sendByDingding(body, curIndex + 1, maxIndex)
            }
        }
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
        LoggerUtil.d(TAG, "sms service created")
    }

    override fun onDestroy() {
        super.onDestroy()
        smsReceiveLiveData.removeObserver(smsObserver)
//        screenStateLiveData.removeObserver(screenObserver)
        LoggerUtil.d(TAG, "sms service destroyed")
    }
}