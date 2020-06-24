package org.lynxz.forwardsms

import PermissionResultInfo
import android.Manifest
import android.content.ComponentName
import android.content.Intent
import android.os.Build
import android.provider.Settings
import android.provider.Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS
import android.text.TextUtils
import android.text.method.ScrollingMovementMethod
import androidx.lifecycle.Observer
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.MainScope
import org.lynxz.baseimlib.IMManager
import org.lynxz.baseimlib.actions.IIMAction
import org.lynxz.baseimlib.bean.ImType
import org.lynxz.baseimlib.bean.SendMessageReqBean
import org.lynxz.baseimlib.msec2date
import org.lynxz.forwardsms.bean.SmsDetail
import org.lynxz.forwardsms.network.SmsConstantParas
import org.lynxz.forwardsms.ui.BaseActivity
import org.lynxz.forwardsms.util.BrandUtil
import org.lynxz.forwardsms.util.LoggerUtil
import org.lynxz.forwardsms.util.NotificationUtils
import org.lynxz.forwardsms.util.SpDelegateUtil
import org.lynxz.forwardsms.viewmodel.SmsViewModel


/**
 * 测试及设置页面
 * */
@UseExperimental(ExperimentalCoroutinesApi::class)
class MainActivity : BaseActivity(), CoroutineScope by MainScope() {
    companion object {
        const val TAG = "MainActivity"
        private const val delayActionTagRefreshDingDing = 0x001L // 刷新钉钉token
    }

    private var lastSmsTs by SpDelegateUtil(this, IIMAction.lastSendMsgInfo, "", IIMAction.spIm)

    private var tgUserName by SpDelegateUtil(this, SmsConstantParas.SpKeyTgUserName, "")
    private var ddUserName by SpDelegateUtil(this, SmsConstantParas.SpKeyDDUserName, "")
    private var feiShuUserName by SpDelegateUtil(this, SmsConstantParas.SpKeyFeishuUserName, "")
    private var phoneTag by SpDelegateUtil(this, SmsConstantParas.SpKeyPhoneTag, "")
    private val smsReceiveLiveData by lazy { SmsViewModel.getReceivedSms() }

    override fun getLayoutRes() = R.layout.activity_main
    var lastSms = ""
    override fun afterViewCreated() {
        // 显示tg用户名
        if (phoneTag.isBlank()) {
            phoneTag = Build.MODEL
        }
        SmsConstantParas.phoneTag = phoneTag

        edt_user_name_tg.setText(tgUserName)
        edt_user_name_dd.setText(ddUserName)
        edt_user_name_feishu.setText(feiShuUserName)
        edt_phone_tag.setText(phoneTag)
        tv_info.movementMethod = ScrollingMovementMethod.getInstance()
//        requestPermission(Manifest.permission.READ_SMS)

        // 注册im
        activeTg(tgUserName)
        activeDingding(ddUserName)
        activeFeishu(feiShuUserName)

        // 通知栏消息
        NotificationUtils.getInstance(this).sendNotification("短信转发", "正在运行中...", 100)

        // 转发微信消息,默认转发
        cbx_wechat.setOnCheckedChangeListener { buttonView, isChecked ->
            if (isChecked) {
                enableMonitorNotification()
            }
            SmsViewModel.enableWechatForward(isChecked)
        }
        enableMonitorNotification()
        SmsViewModel.enableWechatForward(true)


        // 设置telegram接收用户
        btn_confirm_tg.setOnClickListener {
            tgUserName = edt_user_name_tg.text.toString().trim()
            activeTg(tgUserName)
        }

        // 设置钉钉接收用户
        btn_confirm_dd.setOnClickListener {
            ddUserName = edt_user_name_dd.text.toString().trim()
            activeDingding(ddUserName)
        }

        // 设置飞书接收用户名
        btn_confirm_feishu.setOnClickListener {
            feiShuUserName = edt_user_name_feishu.text.toString().trim()
            activeFeishu(feiShuUserName)
        }

        // 设置本机识别名
        btn_confirm_phone_tag.setOnClickListener {
            phoneTag = edt_phone_tag.text.toString().trim()
            SmsConstantParas.phoneTag = phoneTag
        }

//        smsModel = ViewModelProviders.of(this).get(SmsViewModel::class.java).apply {
//            init(application)
//        }
        SmsViewModel.getReceivedSms().observe(this, Observer<SmsDetail> {
            tv_info.text = it.toString()
        })

        SmsViewModel.getSmsHistory().observe(this, Observer {
            val his = StringBuilder(100)
            his.append("版本:").append(BuildConfig.VERSION_NAME)
                .append("\n共获取短信 ").append(it.size).append("条:")
            if (it.isNotEmpty()) {
                lastSms = it[0].format()
            }

            it.forEach { sms ->
                his.append("\n").append(sms.toString())
            }
            tv_info.text = his.toString()
        })

        requestPermissions(
            arrayOf(
                Manifest.permission.RECEIVE_SMS,
                Manifest.permission.READ_SMS
            )
        )

        btn_sms_list.setOnClickListener {
            requestPermission(Manifest.permission.READ_SMS)
        }

        btn_send_msg.setOnClickListener {

            val body = SendMessageReqBean().apply {
                content = "$lastSms\n测试:${msec2date()}"
            }

            // tg发送失败则尝试使用钉钉发送
            tv_info.text = "发送消息到各im..."
            IMManager.sendTextMessage(ImType.TG, body.duplicate().apply {
                name = SmsConstantParas.tgUserNme
            }) {
                tv_info.append("\nsend msg from tg result ${it.ok}\n${it.detail}")
                println("send msg from tg result ${it.ok}")
            }

            IMManager.sendTextMessage(ImType.DingDing, body.duplicate().apply {
                name = SmsConstantParas.ddName
            }) {
                tv_info.append("\nsend msg from dingding result ${it.ok}\n${it.detail}")
                println("send msg from dingding result ${it.ok}")
            }

            IMManager.sendTextMessage(ImType.FeiShu, body.duplicate().apply {
                name = SmsConstantParas.feishuName
            }) {
                tv_info.append("\nsend msg from feishu result ${it.ok}\n${it.detail}")
                println("send msg from feishu result ${it.ok}")
            }
        }

        btn_auto_start.setOnClickListener { BrandUtil.goAutoStartSetting(this) }
        btn_temp_test.setOnClickListener { activeImTest() }
    }


    override fun onBackPressed() {
        goHome()
    }


    override fun onRequestResult(permission: PermissionResultInfo) {
        // 具体某个权限的授权结果
        val msg =
            "授权结果\n权限名=${permission.name},是否授权=${permission.granted},是否可再弹出系统权限框=${permission.shouldShowRequestPermissionRationale}"
        LoggerUtil.d(TAG, msg)
        tv_info.text = msg

        if (permission.name == Manifest.permission.READ_SMS) {
            if (permission.granted) {
                SmsViewModel.loadSmsHistory()
            } else {
                showToast("请先允许读取短信列表再试")
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        LoggerUtil.d(TAG, "main onDestroy")
    }


    private fun activeImTest() {
        // 临时测试用
        requestPermission(Manifest.permission.REQUEST_IGNORE_BATTERY_OPTIMIZATIONS)
    }

    /**
     * 启用或停止钉钉im
     * @param userName 要发送的钉钉用户名信息,若为空,则停用钉钉
     * */
    private fun activeDingding(userName: String?) {
        SmsConstantParas.ddName = userName ?: ""
        if (!userName.isNullOrBlank()) {
            SmsViewModel.activeIm(ImType.DingDing)
            doDelay(10 * 60 * 1000, delayActionTagRefreshDingDing) {
                LoggerUtil.d(TAG, "定时刷新钉钉token...")
                IMManager.refresh(ImType.DingDing)
            }
        } else {
            IMManager.unregisterIm(ImType.DingDing)
            cancelDelayAction(delayActionTagRefreshDingDing)
        }
    }

    /**
     * 启用或停止tg im
     * @param userName tg用户昵称
     * */
    private fun activeTg(userName: String?) {
        SmsConstantParas.tgUserNme = userName ?: ""
        if (!userName.isNullOrBlank()) {
            SmsViewModel.activeIm(ImType.TG)
        } else {
            IMManager.unregisterIm(ImType.TG)
        }
    }

    /**
     * 启用或停止飞书 im
     * @param userName tg用户昵称
     * */
    private fun activeFeishu(userName: String?) {
        SmsConstantParas.feishuName = userName ?: ""
        if (!userName.isNullOrBlank()) {
            SmsViewModel.activeIm(ImType.FeiShu)
        } else {
            IMManager.unregisterIm(ImType.FeiShu)
        }
    }

    // 判断是否打开了通知监听权限
    private fun isNotificationListenerEnabled(): Boolean {
        val pkgName = packageName
        val flat = Settings.Secure.getString(contentResolver, "enabled_notification_listeners")
        if (!TextUtils.isEmpty(flat)) {
            val names = flat.split(":".toRegex()).toTypedArray()
            for (i in names.indices) {
                val cn = ComponentName.unflattenFromString(names[i])
                if (cn != null) {
                    if (TextUtils.equals(pkgName, cn.packageName)) {
                        return true
                    }
                }
            }
        }
        return false
    }

    /**
     * 允许读取通知栏信息
     * */
    private fun enableMonitorNotification() {
        if (isNotificationListenerEnabled()) {
            return
        }

        try {
            val intent: Intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
                Intent(ACTION_NOTIFICATION_LISTENER_SETTINGS)
            } else {
                Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS")
            }
            startActivity(intent)
        } catch (e: Exception) {
            e.printStackTrace()
            tv_info.text = e.message
        }
    }
}
