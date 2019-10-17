package org.lynxz.forwardsms.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import org.lynxz.forwardsms.observer.IScreenStateObserver

/**
 * 屏幕开启关闭广播监听
 * */
class ScreenStateReceiver(private val observer: IScreenStateObserver? = null) :
    BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        when (intent?.action) {
            Intent.ACTION_SCREEN_ON -> observer?.onScreenOn()
            Intent.ACTION_SCREEN_OFF -> observer?.onScreenOff()
        }
    }
}