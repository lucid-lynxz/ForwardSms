package org.lynxz.forwardsms.ui.widget

import android.app.Activity
import android.app.Application

import org.lynxz.forwardsms.ui.activity.OnePixelActivity

import java.lang.ref.WeakReference

/**
 * 单像素页面管理
 * OnePixelActManager.init(application) 后再调用其他方法
 * */
object OnePixelActManager {

    private var mActivityWref: WeakReference<Activity>? = null

    fun setActivity(pActivity: Activity) {
        mActivityWref = WeakReference(pActivity)
    }

    // 启动单像素页面
    fun startActivity() {
        OnePixelActivity.startOnePixelActivity(app)
    }

    // 关闭单像素
    fun finishActivity() {
        mActivityWref?.let {
            it.get()?.finish()
        }
    }

    private var app: Application? = null

    fun init(app: Application): OnePixelActManager {
        this.app = app
        return this
    }
}