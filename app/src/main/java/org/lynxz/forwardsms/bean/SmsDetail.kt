package org.lynxz.forwardsms.bean

import android.telephony.SmsMessage
import org.lynxz.forwardsms.network.SmsConstantParas
import java.text.SimpleDateFormat


data class SmsDetail(
    var ts: Long = System.currentTimeMillis(),// 收到短信的时间戳
    var from: String? = "",// 原始发信人
    var to: String? = SmsConstantParas.phoneTag, // 短信接收人
    var displayFrom: String? = "",// 显示的发信人信息
    var body: String? = "", // 短信内容
    var status: Int = 0, //  短信状态 -1:接收 0:complete 64:pending 128:failed
    var type: Int = 1, // 1:接收的 2:发出的
    var read: Int = 0 // 0:未读 1:已读
) {
    val date: String?
        get() {
            return SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(ts)
        }

    // 是否已读
    fun hasRead() = read == 1

    override fun toString(): String {
        return "$date $from $displayFrom $type $status $read\n$body\n"
    }

    // 格式化短信显示格式
    // 内容在前,以便IM消息通知时,可以直接看到验证码等重要内容
    fun format(): String {
        // 非空且与from值不同时才显示
        val displayFromPlaceHolder = if (!displayFrom.isNullOrBlank() && from != displayFrom) {
            " $displayFrom"
        } else {
            ""
        }

        val receiver = if (to.isNullOrBlank()) {
            ""
        } else {
            "\nTo: $to"
        }
        return "$body\nFrom: $from$displayFromPlaceHolder$receiver\n$date"
    }

    fun updateSmsMessage(msg: SmsMessage?) {
        msg?.apply {
            if (from.isNullOrBlank() || from == originatingAddress) {
                from = originatingAddress
                displayFrom = displayOriginatingAddress
                body += displayMessageBody
                ts = timestampMillis
            }
        }
    }

    fun updateSmsMessage(msg: android.telephony.gsm.SmsMessage?) {
        msg?.apply {
            if (from.isNullOrBlank() || from == originatingAddress) {
                from = originatingAddress
                displayFrom = displayOriginatingAddress
                body += displayMessageBody
                ts = timestampMillis
            }
        }
    }
}

/**
 * 两条短信是否是同一条
 * */
fun SmsDetail?.isSameAs(other: SmsDetail?): Boolean {
    if (this == null || other == null) {
        return false
    }

    return this.from == other.from && this.body == other.body && this.ts == other.ts
}