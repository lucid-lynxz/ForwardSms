package org.lynxz.forwardsms.ui.trans

import android.app.Activity
import android.icu.text.UFormat
import android.view.View
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModel
import org.lynxz.forwardsms.showToast

/**
 * 权限相关view model
 * */
class PermissionViewModel : ViewModel() {

    /**
     * 获取权限申请fragment
     * @param from 要求必须是 Fragment 或者 FragmentActivity
     * */
    private fun getPermissionFragment(from: Any): PermissionFragment? {
        val act = when (from) {
            is Fragment -> from.activity
            is FragmentActivity -> from
            else -> null
        }

        act?.let {
            return BaseTransFragment.getTransFragment(it, "permission_tag", PermissionFragment())
//                .apply { registerPermissionChecker(IgnoreBatteryOptimCheckerImpl) })
        }
        return null
    }

    /**
     * 申请单个权限
     * */
    fun requestPermission(from: Any, permission: String, callback: IPermissionCallback) {
        val permissionFragment = getPermissionFragment(from)
        if (permissionFragment == null) {
            callback.onRequestResult(PermissionResultInfo(permission))
            callback.onAllRequestResult(false)
        } else {
            permissionFragment.requestPermission(permission, callback)
        }
    }

    /**
     * 批量申请权限
     * */
    fun requestPermissions(from: Any, permissions: Array<String>, callback: IPermissionCallback) {
        val permissionFragment = getPermissionFragment(from)
        if (permissionFragment == null) {
            permissions.forEach {
                callback.onRequestResult(PermissionResultInfo(it))
            }
            callback.onAllRequestResult(false)
        } else {
            permissionFragment.requestPermissions(permissions, callback)
        }
    }

    /**
     * 判断所有权限是否均已被授予
     * */
    fun isPermissionGranted(activity: Activity, vararg permissions: String): Boolean {
        permissions.forEach {
            val permissionGranted = PermissionFragment.isPermissionGranted(activity, it)
            if (!permissionGranted) {
                return false
            }
        }
        return true
    }

    /**
     * 缓存权限和对应的申请结果
     * */
    private val permissionCacheInfo = mutableMapOf<String, PermissionResultInfo?>()

    private fun checkSmsPermission(
        from: Any,
        permissions: Array<String>,
        doOnGranted: () -> Unit = { }
    ) {
        val permissionFragment = getPermissionFragment(from)
        val act = permissionFragment?.activity ?: return
        if (isPermissionGranted(act, *permissions)) {
            doOnGranted.invoke()
        } else {
            permissionFragment.requestPermissions(
                permissions,
                object : IPermissionCallback {
                    override fun onRequestResult(permission: PermissionResultInfo) {
                        permissionCacheInfo[permission.name] = permission

//                        val showTipDialog =
//                            !permission.granted && permission.shouldShowRequestPermissionRationale
//                        showToast("todo: 跳转到设置页面")
//                        if (!showTipDialog) {
//                            PermissionFragment.startSettingActivity(activity!!)
//                        }
                    }

                    override fun onAllRequestResult(allGranted: Boolean) {
                        super.onAllRequestResult(allGranted)
//                        showTipInfo(!allGranted)
                        if (!allGranted) {
//                            updateTipInfo(
//                                "权限申请失败,请点击重试",
//                                View.OnClickListener {
//                                    checkSmsPermission(permissions) { }{
//
//                                    }
//                                })
                        } else {
                            doOnGranted.invoke()
                        }
                    }
                }
            )
        }
    }
}