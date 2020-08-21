package org.lynxz.forwardsms.ui.trans

// 导入 android X 使用如下导包
//import PermissionFragment.Companion.isPermissionGranted
//import PermissionFragment.Companion.startSettingActivity
import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import android.util.SparseArray
import androidx.annotation.StringRes
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import kotlin.random.Random


// 导入普通support库时启用以下库
//import android.support.annotation.StringRes
//import android.support.v4.app.ActivityCompat
//import android.support.v4.content.ContextCompat
//import android.support.v7.app.AlertDialog


/**
 * Created by lynxz on 2019/1/29
 * E-mail: lynxz8866@gmail.com
 *
 * Description: 权限申请结果信息
 */
data class PermissionResultInfo(
    var name: String, // 权限名称
    var granted: Boolean = false, // 是否已被授权
    var shouldShowRequestPermissionRationale: Boolean = false // 用户拒绝授权时,是否同时选中了 "Don’t ask again"
)

/**
 * 权限申请回调
 * */
interface IPermissionCallback {
    /**
     * 某权限的授权申请结果
     * */
    fun onRequestResult(permission: PermissionResultInfo)

    /**
     * 所有权限是否都已被授权
     * @param allGranted false-至少有一个权限被拒绝
     * */
    fun onAllRequestResult(allGranted: Boolean) {}
}

/**
 * 允许对特定的permission判断/申请操作进行定制
 * */
interface IPermissionChecker {

    /**
     * 本工具实现类能处理的权限名称, 如: Manifest.permission.REQUEST_IGNORE_BATTERY_OPTIMIZATIONS
     * */
    val targetPermission: String

    /**
     * 权限是否已被授予
     * */
    fun isPermissionGranted(context: Context, permission: String): Boolean


    /**
     * 申请指定权限
     * @return  true: 通过 startActivityForResult() 发起的请求
     *          else: 通过 Fragment#requestPermissions() 发起的权限申请
     * */
    fun requestPermission(fragment: Fragment, permission: String, requestCode: Int): Boolean
}


/**
 * v1.0
 * 封装权限申请长流程 Fragment
 * 1. 通过 [isPermissionGranted] 来判断权限是否已被授权
 * 2. 通过 [requestPermissions] 来批量申请权限
 * 3. 通过 [requestPermission] 来申请某个权限的授权
 * 4. 通过 [requestPermissionWithDialogIfNeeded] 来申请某个权限的授权,并按需弹出申请理由dialog
 * 5. 通过 [startSettingActivity] 来跳转到设置页面
 * 使用方法:
 *  <pre>
 *      // 1. 注入权限申请fragment到指定的 activity 中
 *      val permissionFrag = BaseTransFragment.getTransFragment(hostActivity, "permission_tag", PermissionFragment())
 *
 *      // 2. 设置回调接口
 *      val permissionCallback = object : IPermissionCallback {
 *          override fun onRequestResult(permission: PermissionResultInfo) {
 *              // 具体某个权限的授权结果
 *              Logger.d("授权结果\n权限名=${permission.name},是否授权=${permission.granted},是否可再弹出系统权限框=${permission.shouldShowRequestPermissionRationale}")
 *          }
 *
 *          override fun onAllRequestResult(allGranted: Boolean) {
 *              // 所申请的权限是否全部都通过了
 *          }
 *      }
 *
 *      // 3. [可选] 设置特定权限的申请方法,主要用于非dangerous权限申请,如电池优化白名单
 *      permissionFrag?.registerPermissionChecker(IPermissionChecker)
 *
 *      // 4. 申请单个权限(步骤3有效)
 *      permissionFrag?.requestPermission(Manifest.permission.RECORD_AUDIO, permissionCallback)
 *
 *      // 5. 申请单个权限,并按需弹出dialog跳转到设置页面
 *      permissionFrag?.requestPermissionWithDialogIfNeeded(Manifest.permission.RECORD_AUDIO, "缺少录音权限", "请点击确定按钮到设置页面开启权限", permissionCallback)
 *
 *      // 6. 批量申请权限
 *      permissionFrag?.requestPermissions(arrayOf(Manifest.permission.RECORD_AUDIO, Manifest.permission.CAMERA), permissionCallback)
 *  </pre>
 */
class PermissionFragment : BaseTransFragment() {
    // 权限申请结果回调
    private val mCallbacks = SparseArray<IPermissionCallback?>()

    // 通过 startActivityForResult() 进行权限申请的权限名信息
    private val mStartForResultPermissionRequest = SparseArray<String>()

    // 用于随机生成 requestCode
    private val mRandom = Random(System.currentTimeMillis())

    companion object {
        // 套装到设置页面进行权限申请
        private const val CODE_SETTING = 100

        /**
         * 跳转到权限设置页面
         * */
        fun startSettingActivity(from: Activity, requestCode: Int = CODE_SETTING) {
            try {
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                intent.data = Uri.parse("package:${from.packageName}")
                from.startActivityForResult(intent, requestCode)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        /**
         * 判断指定权限是否已被授权
         * 支持普通dangerous权限以及 REQUEST_IGNORE_BATTERY_OPTIMIZATIONS 判断
         * */
        fun isPermissionGranted(context: Context, permission: String): Boolean {
            return if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
                true
            } else {
                when (permission) {
                    // 电池优化白名单判断(normal级别,需要单独判断)
                    Manifest.permission.REQUEST_IGNORE_BATTERY_OPTIMIZATIONS -> {
                        val powerManager =
                            context.getSystemService(Context.POWER_SERVICE) as PowerManager?
                        powerManager?.isIgnoringBatteryOptimizations(context.packageName) ?: false

                    }
                    else -> ContextCompat.checkSelfPermission(
                        context,
                        permission
                    ) == PackageManager.PERMISSION_GRANTED
                }
            }
        }
    }

    /**
     * 判断指定权限是否已被授权
     * 支持普通dangerous权限以及 REQUEST_IGNORE_BATTERY_OPTIMIZATIONS 判断
     * */
    private fun isPermissionGrantedInternal(permission: String): Boolean {
        return if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) true
        else if (!isAttached) false
        else {
            permissionCheckerMap[permission]?.isPermissionGranted(hostActivity, permission)
                ?: (ContextCompat.checkSelfPermission(
                    hostActivity,
                    permission
                ) == PackageManager.PERMISSION_GRANTED)
        }
    }

    private val permissionCheckerMap = mutableMapOf<String, IPermissionChecker>()

    /**
     * 注册自定义的特定权限申请实现类
     * 若未注册,则使用默认的权限申请
     * */
    fun registerPermissionChecker(permissionChecker: IPermissionChecker) {
        permissionCheckerMap[permissionChecker.targetPermission] = permissionChecker
    }

    /**
     * 申请指定权限
     * */
    fun requestPermission(permission: String, callback: IPermissionCallback?) {
        if (isPermissionGrantedInternal(permission)) {
            callback?.onRequestResult(
                PermissionResultInfo(
                    permission,
                    granted = true,
                    shouldShowRequestPermissionRationale = false
                )
            )
            callback?.onAllRequestResult(true)
        } else {
            requestPermissions(arrayOf(permission), callback)
        }
    }

    /**
     * 申请权限, 若权限已被拒绝并 "Don’t ask again",则弹出提示框,点击确定按钮则跳转到设置页面
     * */
    fun requestPermissionWithDialogIfNeeded(
        permission: String,
        title: CharSequence? = "",
        msg: CharSequence? = "",
        @StringRes titleResId: Int = 0,
        @StringRes msgResId: Int = 0,
        callback: IPermissionCallback?
    ) {
        val granted = isPermissionGrantedInternal(permission)
        val canRequestAgain =
            !ActivityCompat.shouldShowRequestPermissionRationale(hostActivity, permission)
        if (granted) {
            callback?.onRequestResult(PermissionResultInfo(permission, granted, canRequestAgain))
            callback?.onAllRequestResult(true)
        } else {
            if (canRequestAgain) {
                requestPermissions(arrayOf(permission), callback)
            } else {// 显示提示dialog并跳转设置页面
                val requestCode = generateRequestCode()
                mCallbacks.put(requestCode, callback)
                mStartForResultPermissionRequest.put(requestCode, permission)
                showRequestDialog(title, msg, titleResId, msgResId, requestCode)
            }
        }
    }

    // attach 后才进行权限申请, 若attach前, host页面就发起了权限请求,则存入 pendingPermissions 列表中,延迟申请
    private var isAttached = false
    private var isThreadPendingPermission = false // 是否存在待申请的权限,大部分时候可以不用创建 list
    private val pendingPermissions by lazy { mutableListOf<Pair<Array<String>, IPermissionCallback?>>() }


    override fun onAttach(context: Context) {
        super.onAttach(context)
        isAttached = true

        if (isThreadPendingPermission) {
            pendingPermissions.forEach {
                requestPermissions(it.first, it.second)
            }
            pendingPermissions.clear()
            isThreadPendingPermission = false
        }
    }

    override fun onDetach() {
        super.onDetach()
        isAttached = false
    }


    /**
     * 批量申请权限
     * */
    fun requestPermissions(permissions: Array<String>, callback: IPermissionCallback?) {

        if (!isAttached) {
            isThreadPendingPermission = true
            pendingPermissions.add(permissions to callback)
            return
        }

        val requestCode = generateRequestCode()
        mCallbacks.put(requestCode, callback)

        // 单权限才尝试进行定制化权限申请
        var hitSpecialPermissionChecker = false

        if (permissions.size == 1) {
            val permission = permissions[0]
            val checker = permissionCheckerMap[permission]
            if (checker != null) {
                hitSpecialPermissionChecker = true
                val requestByStartForResult =
                    checker.requestPermission(this, permission, requestCode)
                if (requestByStartForResult) {
                    mStartForResultPermissionRequest.put(requestCode, permission)
                }
            }
        }

        if (!hitSpecialPermissionChecker) {
            requestPermissions(permissions, requestCode)
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        val callback = mCallbacks.get(requestCode) ?: return

        var isAllGranted = true // 所申请的权限是否全部通过授权
        for (index in grantResults.indices) {
            val isGranted = grantResults[index] == PackageManager.PERMISSION_GRANTED
            val name = permissions[index]
            val canRequestAgain =
                ActivityCompat.shouldShowRequestPermissionRationale(hostActivity, name)
            callback.onRequestResult(PermissionResultInfo(name, isGranted, canRequestAgain))
            if (!isGranted) {
                isAllGranted = false
            }
        }
        callback.onAllRequestResult(isAllGranted)
        mCallbacks.remove(requestCode)
    }

    /**
     * 从设置页面返回到本app后,检查权限是否已被授权,并回调通知用户
     * */
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        val callback = mCallbacks.get(requestCode) ?: return
        val permissionName = mStartForResultPermissionRequest.get(requestCode) ?: return
        val granted = isPermissionGrantedInternal(permissionName)
        val canRequestAgain =
            ActivityCompat.shouldShowRequestPermissionRationale(hostActivity, permissionName)
        callback.onRequestResult(PermissionResultInfo(permissionName, granted, canRequestAgain))
        callback.onAllRequestResult(granted)
    }

    /**
     * 随机生成权限申请requestCode
     * */
    private fun generateRequestCode(): Int {
        var code = 0
        var tryTimes = 0
        do {
            // 选择 0xFFFF ,可以参考 FragmentActivity 类的 checkForValidRequestCode() 方法
            // 参考: https://blog.csdn.net/barryhappy/article/details/53229238
            code = mRandom.nextInt(0xFFFF)
            tryTimes++
        } while (mCallbacks.indexOfKey(code) >= 0 && tryTimes <= 20)
        return code
    }

    /**
     * 用户申请某权限时,若该权限已被拒绝过,并且用户勾选了 "Don’t ask again"
     * 则弹出提示框,并跳转设置页面
     * */
    private fun showRequestDialog(
        title: CharSequence?,
        msg: CharSequence?,
        @StringRes titleResId: Int = 0,
        @StringRes msgResId: Int = 0,
        requestCode: Int
    ) {
        AlertDialog.Builder(hostActivity).apply {
            if (titleResId != 0) {
                setTitle(titleResId)
            } else {
                setTitle(title)
            }

            if (msgResId != 0) {
                setMessage(msgResId)
            } else {
                setMessage(msg)
            }
        }
            .setPositiveButton(android.R.string.yes) { _, which ->
                try {
                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                    intent.data = Uri.parse("package:${hostActivity.packageName}")
                    startActivityForResult(intent, requestCode)
                } catch (e: Exception) {
                    e.printStackTrace()
                    mStartForResultPermissionRequest.remove(requestCode)
                }
            }
            .setNegativeButton(android.R.string.no, null)
            .show()
    }
}