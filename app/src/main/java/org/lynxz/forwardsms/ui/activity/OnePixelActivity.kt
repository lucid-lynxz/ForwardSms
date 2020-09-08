package org.lynxz.forwardsms.ui.activity

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.Gravity
import androidx.appcompat.app.AppCompatActivity
import org.lynxz.forwardsms.ui.widget.OnePixelActManager

/**
 * 单像素页面,用于锁屏时启用,防止进程被杀
 * */
class OnePixelActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.setGravity(Gravity.START or Gravity.TOP)
        with(window.attributes) {
            width = 1
            height = 1
            x = 0
            y = 0
            window.attributes = this
        }
        OnePixelActManager.setActivity(this)
    }

    companion object {
        fun startOnePixelActivity(context: Context?) {
            val intent = Intent(context, OnePixelActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            context?.startActivity(intent)
        }
    }
}