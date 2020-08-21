package org.lynxz.forwardsms.ui.trans.permissionCheckerImpl

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.annotation.RequiresApi
import androidx.fragment.app.Fragment
import org.lynxz.forwardsms.ui.trans.IPermissionChecker

/**
 * [Manifest.permission.REQUEST_IGNORE_BATTERY_OPTIMIZATIONS] 电池优化白名单权限申请
 * 需要在 AndroidManifest 中声明: <uses-permission android:name="android.permission.REQUEST_IGNORE_BATTERY_OPTIMIZATIONS" />
 * 参考: https://juejin.im/post/5dfaeccbf265da33910a441d
 * */
object IgnoreBatteryOptimCheckerImpl : IPermissionChecker {
    override val targetPermission: String
        @RequiresApi(Build.VERSION_CODES.M)
        get() = Manifest.permission.REQUEST_IGNORE_BATTERY_OPTIMIZATIONS

    override fun isPermissionGranted(context: Context, permission: String): Boolean {
        return if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            true
        } else {
            val powerManager =
                context.getSystemService(Context.POWER_SERVICE) as PowerManager?
            powerManager?.isIgnoringBatteryOptimizations(context.packageName) ?: false
        }
    }

    @SuppressLint("BatteryLife")
    @RequiresApi(Build.VERSION_CODES.M)
    override fun requestPermission(
        fragment: Fragment,
        permission: String,
        requestCode: Int
    ): Boolean {
        val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS)
        intent.data = Uri.parse("package:${fragment.activity!!.packageName}")
        fragment.startActivityForResult(intent, requestCode)
        return true
    }
}