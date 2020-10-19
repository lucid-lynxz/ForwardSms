package org.lynxz.forwardsms.bean.emptyimpl

import android.content.ComponentName
import android.content.ServiceConnection
import android.os.IBinder

open class EmptyServiceConnection : ServiceConnection {
    override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
    }

    override fun onServiceDisconnected(name: ComponentName?) {
    }
}