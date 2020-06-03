package org.lynxz.forwardsms.ui

import IPermissionCallback
import PermissionFragment
import PermissionResultInfo
import android.os.Bundle
import android.os.Handler
import android.view.View
import androidx.annotation.LayoutRes
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import kotlinx.android.synthetic.main.activity_base.*
import org.lynxz.forwardsms.R
import org.lynxz.forwardsms.ui.trans.permissionCheckerImpl.IgnoreBatteryOptimCheckerImpl
import org.lynxz.forwardsms.util.LoggerUtil
import org.lynxz.forwardsms.util.ScreenUtil

abstract class BaseActivity : AppCompatActivity(), IPermissionCallback {
    private val permissionFrag by lazy {
        BaseTransFragment.getTransFragment(this, "permission_tag", PermissionFragment().apply {
            registerPermissionChecker(IgnoreBatteryOptimCheckerImpl)
        })
    }

    protected fun requestPermission(permission: String) {
        permissionFrag?.requestPermission(permission, this)
    }

    protected fun requestPermissions(permissions: Array<String>) {
        permissionFrag?.requestPermissions(permissions, this)
    }

    override fun onRequestResult(permission: PermissionResultInfo) {
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        beforeSetContentView()
        val layoutRes = getLayoutRes()

        // 可自定义布局或者通过fragment替换
        if (layoutRes == 0) {
            setContentView(R.layout.activity_base)

            getContentFragment()?.let {
                rl_base_container.visibility = View.VISIBLE
                supportFragmentManager
                    .beginTransaction()
                    .replace(R.id.rl_base_container, it)
                    .commit()

                intent?.extras?.let { bundle ->
                    if (it is BaseFragment
                        && it.shouldInterceptParentArgumentTrans()
                    ) {
                        return@let
                    }

                    if (bundle.size() > 0) {
                        it.arguments = bundle
                    }
                }
            }
        } else {
            setContentView(layoutRes)
        }

        afterViewCreated()
    }

    private val mHandler by lazy { Handler() }
    private val delayActionMap by lazy { mutableMapOf<Long, Runnable>() }

    fun doDelay(delayMs: Long, tag: Long = System.currentTimeMillis(), block: () -> Unit) {
        delayActionMap[tag]?.let {
            mHandler.removeCallbacks(it)
        }

        val delayRunnable = Runnable {
            LoggerUtil.d("doDelay action $tag , running.... $isFinishing")
            if (!isFinishing) {
                block()
            }
        }

        delayActionMap[tag] = delayRunnable
        mHandler.postDelayed(delayRunnable, delayMs)
    }

    protected fun cancelDelayAction(tag: Long = Long.MIN_VALUE) {
        LoggerUtil.d("cancelDelayAction $tag")
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


    override fun onDestroy() {
        super.onDestroy()
        cancelDelayAction()
    }


    @LayoutRes
    abstract fun getLayoutRes(): Int

    /**
     * 若 [getLayoutRes] 返回0, 则通过重写本方法来替换fragment
     * */
    open fun getContentFragment(): Fragment? = null

    abstract fun afterViewCreated()

    open fun beforeSetContentView() {

    }

    /**
     * 由于fragment嵌套多层,内层fragment沉浸式时,需要为状态栏预留空间
     * @param containerView fragment/activity 的容器布局,通过动态设置其paddingTop值来实现
     * */
    open fun updatePaddingTopWithStatusHeight(containerView: View?) {
        if (containerView == null) {
            LoggerUtil.d("updatePaddingTopWithStatusHeight fail  $containerView $this")
            return
        }

        val height = ScreenUtil.getStatusBarHeight(this)
        // Logger.d("height $height")
        containerView.setPadding(
            containerView.paddingLeft, containerView.paddingTop + height,
            containerView.paddingRight, containerView.paddingBottom
        )
    }
}