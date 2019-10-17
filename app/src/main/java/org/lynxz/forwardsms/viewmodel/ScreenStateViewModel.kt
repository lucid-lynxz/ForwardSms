package org.lynxz.forwardsms.viewmodel

import android.app.Application
import android.content.Intent
import android.content.IntentFilter
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import org.lynxz.forwardsms.observer.IScreenStateObserver
import org.lynxz.forwardsms.receiver.ScreenStateReceiver

/**
 * 监听屏幕锁屏开屏状态
 * 1. 先在 application 中调用 [init] 方法
 * 2. 通过 [getScreenStateLiveData] 获取屏幕状态livedata
 * */
object ScreenStateViewModel : ViewModel() {
    private val TAG = "SmsViewModel"
    private var app: Application? = null
    private val screenStateLiveDate = MutableLiveData<Boolean>()

    fun getScreenStateLiveData(): LiveData<Boolean> = screenStateLiveDate

    private val screenReceiver =
        ScreenStateReceiver(object : IScreenStateObserver {
            override fun onScreenOff() {
                screenStateLiveDate.value = false
            }

            override fun onScreenOn() {
                screenStateLiveDate.value = true
            }
        })

    fun init(application: Application): ScreenStateViewModel {
        if (app == null) {
            app = application.apply {
                registerReceiver(screenReceiver,
                    IntentFilter().apply {
                        addAction(Intent.ACTION_SCREEN_ON)
                        addAction(Intent.ACTION_SCREEN_OFF)
                    }
                )
            }
        }
        return this
    }

    override fun onCleared() {
        super.onCleared()
        app?.unregisterReceiver(screenReceiver)
    }
}