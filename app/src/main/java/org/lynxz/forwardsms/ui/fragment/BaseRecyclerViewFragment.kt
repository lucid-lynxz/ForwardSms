package org.lynxz.forwardsms.ui.fragment

import android.view.View
import androidx.recyclerview.widget.LinearLayoutManager
import com.angcyo.dsladapter.DslAdapter
import com.angcyo.dsladapter.DslAdapterStatusItem
import com.angcyo.dsladapter.DslItemDecoration
import com.angcyo.dsladapter.HoverItemDecoration
import kotlinx.android.synthetic.main.fragment_common_recyclerview.*
import org.lynxz.forwardsms.R
import org.lynxz.forwardsms.ui.BaseFragment
import org.lynxz.forwardsms.ui.widget.AppAdapterStatusItem

abstract class BaseRecyclerViewFragment : BaseFragment() {

    /**提供悬停功能*/
    var hoverItemDecoration = HoverItemDecoration()

    /**提供基本的分割线功能*/
    var baseDslItemDecoration = DslItemDecoration()

    var dslAdapter: DslAdapter = DslAdapter().apply {
        dslAdapterStatusItem = AppAdapterStatusItem()
        this.setAdapterStatus(DslAdapterStatusItem.ADAPTER_STATUS_EMPTY)
        this.dslAdapterStatusItem.onRefresh = {
            onRefresh()
        }
    }

    final override fun getLayoutRes() = R.layout.fragment_common_recyclerview

    final override fun afterViewCreated(view: View) {
        rv_common.apply {
            addItemDecoration(baseDslItemDecoration)
            hoverItemDecoration.attachToRecyclerView(this)

            //防止在折叠/展开 即 itemAdd/itemRemove 的时候, 自动滚动到顶部.
            //这个属性决定了, adapter 中的item 改变, 不会影响 RecyclerView 自身的宽高属性.
            //如果设置了true, 并且又想影响RecyclerView 自身的宽高属性. 调用 notifyDataSetChanged(),
            //否则统一 使用notifyItemXXX 变种方法
            setHasFixedSize(true)
            layoutManager = LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)
            adapter = dslAdapter
        }

        srf_common.setColorSchemeResources(R.color.colorPrimary)
        srf_common.setOnRefreshListener { onRefresh() }
        onAfterInitBaseLayout()
    }

    /**
     *
     * */
    abstract fun onAfterInitBaseLayout()

    /**
     * 刷新
     * */
    abstract fun onRefresh();


    open fun renderAdapter(render: DslAdapter.() -> Unit) {
        dslAdapter.render()
    }

    /**
     * 错误提示背景
     * */
    protected fun getTipTextView() = tv_tip

    /**
     * 显隐提示语
     * */
    protected fun showTipInfo(show: Boolean = true) {
        tv_tip.visibility = if (show) View.VISIBLE else View.GONE
        srf_common.visibility = if (show) View.GONE else View.VISIBLE
    }

    protected fun updateTipInfo(msg: CharSequence?, clickListener: View.OnClickListener? = null) {
        tv_tip.text = msg
        tv_tip.setOnClickListener(clickListener)
    }
}