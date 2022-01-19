package org.lynxz.forwardsms.ui.fragment.smslist

import android.Manifest
import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.view.View
import android.widget.TextView
import androidx.lifecycle.Observer
import com.angcyo.dsladapter.DslAdapterStatusItem
import com.angcyo.dsladapter.dslItem
import org.lynxz.forwardsms.R
import org.lynxz.forwardsms.bean.SmsDetail
import org.lynxz.forwardsms.convertDateFormat
import org.lynxz.forwardsms.ui.fragment.BaseRecyclerViewFragment
import org.lynxz.forwardsms.ui.trans.IPermissionCallback
import org.lynxz.forwardsms.ui.trans.PermissionResultInfo
import org.lynxz.forwardsms.viewmodel.GlobalParaUtil

/**
 * 短信列表
 * 列表item支持一键发送
 * */
class SmsListFragment : BaseRecyclerViewFragment() {
    override fun onRefresh() {
        dslAdapter.resetItem(listOf())
        loadSmsHistory()
    }

    override fun onResume() {
        super.onResume()
        // 设置页面权限授予后,返回本页面时检查
        // 仅当权限授予后,再执行后续操作,避免反复弹框
        if (shouldCheckPermissionAgainWhenOnResume && isPermissionGranted(
                *arrayOf(
                    Manifest.permission.RECEIVE_SMS,
                    Manifest.permission.READ_SMS
                )
            )
        ) {
            requestSmsPermission()
        }
        shouldCheckPermissionAgainWhenOnResume = false
    }

    @SuppressLint("SetTextI18n")
    override fun onAfterInitBaseLayout() {
        // 抛到主线程队列最后,发起权限申请
        dataBinding.srfCommon.post { requestSmsPermission() }
    }

    /**
     * 申请读取短信权限
     * */
    private fun requestSmsPermission() {
        checkSmsPermission(
            arrayOf(Manifest.permission.RECEIVE_SMS, Manifest.permission.READ_SMS),
            "获取短信列表需要以下权限:\n读取短信, 通知类短信\n请到设置页面进行启用"
        ) {
            showTipInfo(false)
            loadSmsHistory()
            GlobalParaUtil.getSmsHistory().observe(
                this,
                Observer<List<SmsDetail>> { data ->
                    val size = data?.size ?: 0
                    val isEmpty = size == 0
                    dataBinding.srfCommon.isRefreshing = false
                    dslAdapter.setAdapterStatus(if (isEmpty) DslAdapterStatusItem.ADAPTER_STATUS_EMPTY else DslAdapterStatusItem.ADAPTER_STATUS_NONE)
                    if (!isEmpty) {
                        data.forEach {
                            dslAdapter.dslItem(R.layout.item_sms_list) {
                                itemBindOverride = { itemHolder, itemPosition, _, _ ->
                                    itemHolder.v<TextView>(R.id.tv_phone)?.text =
                                        "${it.from} ${it.displayFrom}"
                                    itemHolder.v<TextView>(R.id.tv_time)?.text =
                                        it.ts.convertDateFormat("M月d日 HH:mm")
                                    itemHolder.v<TextView>(R.id.tv_content)?.text = it.body
                                }
                            }
                        }
                    }
                })
        }
    }

    /**
     * 加载短信
     * */
    private fun loadSmsHistory(maxCount: Int = 10) {
        dslAdapter.setAdapterStatus(DslAdapterStatusItem.ADAPTER_STATUS_LOADING)
        GlobalParaUtil.loadSmsHistory(maxCount)
    }

    private fun checkSmsPermission(
        permissions: Array<String>,
        requestPermissionReason: String,
        doOnGranted: () -> Unit = { loadSmsHistory() }
    ) {
        if (isPermissionGranted(*permissions)) {
            doOnGranted.invoke()
        } else {
            requestPermissions(
                permissions,
                object : IPermissionCallback {
                    override fun onRequestResult(permission: PermissionResultInfo) {
                        val showTipDialog =
                            !permission.granted && !permission.shouldShowRequestPermissionRationale
                        if (showTipDialog) {
                            showPermissionSettingTipDialog("权限申请", requestPermissionReason)
                        }
                    }

                    override fun onAllRequestResult(allGranted: Boolean) {
                        super.onAllRequestResult(allGranted)
                        showTipInfo(!allGranted)
                        if (!allGranted) {
                            updateTipInfo(
                                "权限申请失败,请点击重试",
                                View.OnClickListener {
                                    checkSmsPermission(
                                        permissions,
                                        requestPermissionReason,
                                        doOnGranted
                                    )
                                })
                        } else {
                            doOnGranted.invoke()
                        }
                    }
                }
            )
        }
    }

    // 权限申请提示dialog是否显示中
    private var isDialogShowing = false

    // 权限申请时若跳转到其他设置页面,返回后需要再次检查权限
    private var shouldCheckPermissionAgainWhenOnResume = false

    /**
     * 跳转到设置页面
     * */
    private fun showPermissionSettingTipDialog(title: String, content: String) {
        if (isDialogShowing) return
        isDialogShowing = true
        AlertDialog.Builder(requireActivity()).apply {
            setMessage(content)
            setTitle(title)
        }
            .setPositiveButton(android.R.string.yes) { _, which ->
                try {
                    shouldCheckPermissionAgainWhenOnResume = true
                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                    intent.data = Uri.parse("package:${requireActivity().packageName}")
                    startActivity(intent)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            .setNegativeButton(android.R.string.no, null)
            .show()
            .setOnDismissListener {
                isDialogShowing = false
            }
    }
}