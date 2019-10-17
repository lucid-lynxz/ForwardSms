package org.lynxz.forwardsms.ui

import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.LayoutRes
import androidx.fragment.app.Fragment
import org.lynxz.forwardsms.util.Logger
import org.lynxz.forwardsms.util.ScreenUtil

/**
 * Created by xqc on 2018/10/25.
 * Developer App
 */
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
            Logger.d("updatePaddingTopWithStatusHeight fail  $containerView $act")
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
}