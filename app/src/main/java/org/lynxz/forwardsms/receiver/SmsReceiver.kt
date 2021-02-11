package org.lynxz.forwardsms.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.telephony.SmsMessage
import androidx.annotation.RequiresApi
import org.lynxz.baseimlib.convert2Str
import org.lynxz.forwardsms.bean.SmsDetail
import org.lynxz.forwardsms.isSdkGE
import org.lynxz.forwardsms.observer.ISmsReceiveObserver

import org.lynxz.utils.log.LoggerUtil

/**
 * 收到短信提醒receiver
 * */
class SmsReceiver(private val observer: ISmsReceiveObserver? = null) : BroadcastReceiver() {
    companion object {
        private const val TAG = "SmsReceiver"
    }

    @RequiresApi(Build.VERSION_CODES.M)
    override fun onReceive(context: Context?, intent: Intent?) {
        intent?.let {
            val format = it.getStringExtra("format")
            val smsDetail = SmsDetail()
            val pdus = it.extras?.get("pdus") as Array<Any>?
            pdus?.forEach { pub ->
                val bas = pub as ByteArray
                if (isSdkGE(23)) {
                    smsDetail.updateSmsMessage(SmsMessage.createFromPdu(bas, format))
                } else {
                    smsDetail.updateSmsMessage(android.telephony.gsm.SmsMessage.createFromPdu(bas))
                }
            }

            LoggerUtil.w(TAG, "receive new msg: ${convert2Str(smsDetail)}")
            observer?.onReceiveSms(smsDetail)
        }
    }
}