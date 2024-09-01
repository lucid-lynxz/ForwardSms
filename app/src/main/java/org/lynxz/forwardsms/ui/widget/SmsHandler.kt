package org.lynxz.forwardsms.ui.widget

import android.os.Handler
import android.os.Looper
import android.os.Message

object SmsHandler : Handler(Looper.getMainLooper()) {
    override fun handleMessage(msg: Message) {
        super.handleMessage(msg)
    }
}