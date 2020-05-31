package org.lynxz.forwardsms.util

import android.app.Application
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import java.lang.IllegalArgumentException


/**
 * 手机厂商判定及自启动页面跳转
 * 参考: https://juejin.im/post/5dfaeccbf265da33910a441d#heading-3
 * */
object BrandUtil {

    // 手机厂商名称
    private val brandName = Build.BRAND.toLowerCase()

    fun isHuawei() = brandName == "huawei" || brandName == "honor"
    fun isXiaomi() = brandName == "xiaomi"
    fun isOPPO() = brandName == "oppo"
    fun isVIVO() = brandName == "vivo"
    fun isMeiZu() = brandName == "meizu"
    fun isSamSung() = brandName == "samsung"
    fun isLeTV() = brandName == "letv"
    fun isSmartisan() = brandName == "smartisan"

    /**
     * 跳转到手机管家自启动管理页面
     * */
    fun goAutoStartSetting(context: Context) {
        when {
            isHuawei() -> goHuaweiSetting(context)
            isXiaomi() -> goXiaomiSetting(context)
            isOPPO() -> goOPPOSetting(context)
            isVIVO() -> goVIVOSetting(context)
            isMeiZu() -> goMeizuSetting(context)
            isSamSung() -> goSamsungSetting(context)
            isLeTV() -> goLetvSetting(context)
            isSmartisan() -> goSmartisanSetting(context)
        }
    }


    /**
     * 跳转到指定应用的首页
     */
    private fun showActivity(context: Context, packageName: String) {
        val intent: Intent =
            context.packageManager.getLaunchIntentForPackage(context.packageName) ?: return

        if (context is Application) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        context.startActivity(intent)
    }

    /**
     * 跳转到指定应用的指定页面
     */
    private fun showActivity(
        context: Context,
        packageName: String,
        activityDir: String
    ) {

        try {
            context.startActivity(Intent().apply {
                component = ComponentName(packageName, activityDir)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            })
        } catch (e: IllegalArgumentException) {
            e.printStackTrace()
        }
    }


    /**
     * 跳转华为手机管家的启动管理页
     * 应用启动管理 -> 关闭应用开关 -> 打开允许自启动
     * */
    private fun goHuaweiSetting(context: Context) {
        try {
            showActivity(
                context,
                "com.huawei.systemmanager",
                "com.huawei.systemmanager.startupmgr.ui.StartupNormalAppListActivity"
            )
        } catch (e: Exception) {
            showActivity(
                context,
                "com.huawei.systemmanager",
                "com.huawei.systemmanager.optimize.bootstart.BootStartActivity"
            )
        }
    }


    /**
     * 跳转小米安全中心的自启动管理页面
     * 授权管理 -> 自启动管理 -> 允许应用自启动
     * */
    private fun goXiaomiSetting(context: Context) {
        showActivity(
            context,
            "com.miui.securitycenter",
            "com.miui.permcenter.autostart.AutoStartManagementActivity"
        )
    }

    /**
     * 跳转 OPPO 手机管家
     * 权限隐私 -> 自启动管理 -> 允许应用自启动
     * */
    private fun goOPPOSetting(context: Context) {
        try {
            showActivity(context, "com.coloros.phonemanager")
        } catch (e1: java.lang.Exception) {
            try {
                showActivity(context, "com.oppo.safe")
            } catch (e2: java.lang.Exception) {
                try {
                    showActivity(context, "com.coloros.oppoguardelf")
                } catch (e3: java.lang.Exception) {
                    showActivity(context, "com.coloros.safecenter")
                }
            }
        }
    }

    /**
     * 跳转 VIVO 手机管家
     * 权限管理 -> 自启动 -> 允许应用自启动
     * */
    private fun goVIVOSetting(context: Context) {
        showActivity(context, "com.iqoo.secure")
    }

    /**
     * 跳转魅族手机管家
     * 权限管理 -> 后台管理 -> 点击应用 -> 允许后台运行
     * */
    private fun goMeizuSetting(context: Context) {
        showActivity(context, "com.meizu.safe")
    }

    /**
     * 跳转三星智能管理器
     * 自动运行应用程序 -> 打开应用开关 -> 电池管理 -> 未监视的应用程序 -> 添加应用
     * */
    private fun goSamsungSetting(context: Context) {
        try {
            showActivity(context, "com.samsung.android.sm_cn")
        } catch (e: java.lang.Exception) {
            showActivity(context, "com.samsung.android.sm")
        }
    }

    /**
     * 跳转到乐视手机管家
     * 自启动管理 -> 允许应用自启动
     * */
    private fun goLetvSetting(context: Context) {
        showActivity(
            context,
            "com.letv.android.letvsafe",
            "com.letv.android.letvsafe.AutobootManageActivity"
        )
    }

    /**
     * 跳转到锤子手机管家
     * 权限管理 -> 自启动权限管理 -> 点击应用 -> 允许被系统启动
     * */
    private fun goSmartisanSetting(context: Context) {
        showActivity(context, "com.smartisanos.security")
    }
}
