package org.lynxz.forwardsms.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import org.lynxz.forwardsms.MainActivity
import org.lynxz.forwardsms.util.LoggerUtil

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        intent?.let {
            if (it.action == Intent.ACTION_BOOT_COMPLETED) {
                LoggerUtil.d("boot complete...")
                context?.startActivity(Intent(context, MainActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                })
            }
        }
    }
}