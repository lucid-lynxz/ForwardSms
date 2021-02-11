package org.lynxz.forwardsms.ui

import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.LayoutRes
import androidx.fragment.app.Fragment
import org.lynxz.forwardsms.ui.trans.BaseTransFragment
import org.lynxz.forwardsms.ui.trans.IPermissionCallback
import org.lynxz.forwardsms.ui.trans.PermissionFragment
import org.lynxz.forwardsms.ui.trans.permissionCheckerImpl.IgnoreBatteryOptimCheckerImpl

import org.lynxz.forwardsms.util.ScreenUtil
import org.lynxz.forwardsms.util.ViewUtil
import org.lynxz.utils.log.LoggerUtil

abstract class BaseFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(getLayoutRes(), container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        afterViewCreated(view)
    }


    /**
     * 将自身添加到指定的activity中
     */
    open fun add2Activity(targetActivity: BaseActivity) {
        targetActivity.showFragment(this, null)
    }

    /**
     * 跳转显示其他fragment页面
     */
    protected open fun showFragment(fragment: BaseFragment?) {
        val isFinishing = activity?.isFinishing ?: true
        if (!isFinishing && (activity is BaseActivity)) {
            (activity as BaseActivity).showFragment(fragment)
            // 清除焦点,关闭键盘
            val currentFocus = activity!!.currentFocus
            ViewUtil.hideKeyboard(currentFocus)
            currentFocus?.clearFocus()
        }

//        if ((activity is BaseActivity) and !activity.isFinishing) {
//
//            if (!(activity as BaseActivity).isFinishing()) {
//                (activity as BaseActivity?).showExtraContentFragment(fragment, null)
//            }
//
//            // 清除焦点,关闭键盘
//            val currentFocus = getActivity()!!.currentFocus
//            currentFocus?.clearFocus()
//            hideKeyBoard()
//        } else {
//            showShortToast("基类activity并非BaseActivity,显示失败,请检查")
//        }
    }


    private val mHandler by lazy { Handler() }
    private val delayActionMap by lazy { mutableMapOf<Long, Runnable>() }
    fun doDelay(delay: Long, tag: Long = System.currentTimeMillis(), block: () -> Unit) {
        delayActionMap[tag]?.let {
            mHandler.removeCallbacks(it)
        }

        val delayRunnable = Runnable {
            if (canUpdateUi()) {
                block()
            }
        }

        delayActionMap[tag] = delayRunnable
        mHandler.postDelayed(delayRunnable, delay)
    }

    protected fun cancelDelayAction(tag: Long = Long.MIN_VALUE) {
        if (delayActionMap.isEmpty()) {
            return
        }

        for ((k, v) in delayActionMap) {
            if (tag == Long.MIN_VALUE) {
                mHandler.removeCallbacks(v)
            } else if (k == tag) {
                mHandler.removeCallbacks(v)
                break
            }
        }

        if (tag == Long.MIN_VALUE) {
            delayActionMap.clear()
        } else {
            delayActionMap.remove(tag)
        }
    }

    private var isAttached = false
    override fun onAttach(context: Context) {
        super.onAttach(context)
        isAttached = true
    }

    override fun onDetach() {
        super.onDetach()
        isAttached = false
    }

    protected fun canUpdateUi(): Boolean {
        val act = activity
        if (!isAttached || act == null || act.isFinishing) {
            return false
        }
        return true
    }


    @LayoutRes
    abstract fun getLayoutRes(): Int

    abstract fun afterViewCreated(view: View)

    /**
     * 由于fragment嵌套多层,内层fragment沉浸式时,需要为状态栏预留空间
     * @param containerView fragment/activity 的容器布局,通过动态设置其paddingTop值来实现
     * */
    open fun updatePaddingTopWithStatusHeight(containerView: View?) {
        val act = activity
        if (containerView == null || act == null) {
            LoggerUtil.d("updatePaddingTopWithStatusHeight fail  $containerView $act")
            return
        }

        val height = ScreenUtil.getStatusBarHeight(act)
        // Logger.d("height $height")
        containerView.setPadding(
            containerView.paddingLeft, containerView.paddingTop + height,
            containerView.paddingRight, containerView.paddingBottom
        )
    }

    /**
     * 是否禁止父容器将参数(如将activity的Intent中的extras bundle参数)传递给本fragment
     * 若返回false, 使用父容器的参数
     * 返回true,不适用父容器的参数
     */
    open fun shouldInterceptParentArgumentTrans() = false


    override fun onDestroyView() {
        super.onDestroyView()
        cancelDelayAction()
    }

    // 权限申请fragment
    private val permissionFrag by lazy {
        BaseTransFragment.getTransFragment(
            activity!!,
            "permission_tag",
            PermissionFragment().apply { registerPermissionChecker(IgnoreBatteryOptimCheckerImpl) })
    }

    protected fun requestPermission(permission: String, callback: IPermissionCallback) {
        permissionFrag?.requestPermission(permission, callback)
    }

    protected fun requestPermissions(permissions: Array<String>, callback: IPermissionCallback) {
        permissionFrag?.requestPermissions(permissions, callback)
    }

    /**
     * 判断所有权限是否均已被授予
     * */
    protected fun isPermissionGranted(vararg permissions: String): Boolean {
        permissions.forEach {
            val permissionGranted = PermissionFragment.isPermissionGranted(activity!!, it)
            if (!permissionGranted) {
                return false
            }
        }
        return true
    }
}