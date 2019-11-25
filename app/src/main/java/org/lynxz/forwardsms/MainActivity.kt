package org.lynxz.forwardsms

import PermissionResultInfo
import android.Manifest
import android.content.Intent
import android.os.Build
import android.provider.Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS
import android.text.method.ScrollingMovementMethod
import androidx.lifecycle.Observer
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.MainScope
import org.lynxz.baseimlib.IMManager
import org.lynxz.baseimlib.bean.ImType
import org.lynxz.baseimlib.bean.SendMessageReqBean
import org.lynxz.baseimlib.msec2date
import org.lynxz.forwardsms.bean.SmsDetail
import org.lynxz.forwardsms.network.SmsConstantParas
import org.lynxz.forwardsms.ui.BaseActivity
import org.lynxz.forwardsms.util.Logger
import org.lynxz.forwardsms.util.NotificationUtils
import org.lynxz.forwardsms.util.SpDelegateUtil
import org.lynxz.forwardsms.viewmodel.SmsViewModel


/**
 * 测试及设置页面
 * */
@UseExperimental(ExperimentalCoroutinesApi::class)
class MainActivity : BaseActivity(), CoroutineScope by MainScope() {
    private val TAG = "MainActivity"

    private var tgUserName by SpDelegateUtil(this, SmsConstantParas.SpKeyTgUserName, "")
    private var ddUserName by SpDelegateUtil(this, SmsConstantParas.SpKeyDDUserName, "")
    private var phoneTag by SpDelegateUtil(this, SmsConstantParas.SpKeyPhoneTag, "")

    override fun getLayoutRes() = R.layout.activity_main
    var lastSms = ""
    override fun afterViewCreated() {

        // 显示tg用户名
        SmsConstantParas.tgUserNme = tgUserName
        SmsConstantParas.ddName = ddUserName
        if (phoneTag.isNullOrBlank()) {
            phoneTag = Build.MODEL
        }

        SmsConstantParas.phoneTag = phoneTag

        edt_user_name_tg.setText(tgUserName)
        edt_user_name_dd.setText(ddUserName)
        edt_phone_tag.setText(phoneTag)

        tv_info.movementMethod = ScrollingMovementMethod.getInstance()

        requestPermission(Manifest.permission.READ_SMS)

        // 注册im
        SmsViewModel.activeIm()

        // 通知栏消息
        NotificationUtils.getInstance(this).sendNotification("短信转发", "正在运行中...", 100)

        // 设置telegram接收用户
        btn_confirm_tg.setOnClickListener {
            tgUserName = edt_user_name_tg.text.toString().trim()
            SmsConstantParas.tgUserNme = tgUserName
        }

        // 设置钉钉接收用户
        btn_confirm_dd.setOnClickListener {
            ddUserName = edt_user_name_dd.text.toString().trim()
            SmsConstantParas.ddName = ddUserName
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
            IMManager.sendTextMessage(ImType.TG, body.apply {
                name = SmsConstantParas.tgUserNme
            }) {
                tv_info.text = "send msg from tg result ${it.ok}\n${it.detail}"

                println("send msg from tg result ${it.ok}")
                if (!it.ok) {
                    IMManager.sendTextMessage(ImType.DingDing, body.apply {
                        name = SmsConstantParas.ddName
                    })
                }
            }
        }

        btn_temp_test.setOnClickListener {
            activeImTest()
        }
    }


    override fun onBackPressed() {
        goHome()
    }


    override fun onRequestResult(permission: PermissionResultInfo) {
        // 具体某个权限的授权结果
        val msg =
            "授权结果\n权限名=${permission.name},是否授权=${permission.granted},是否可再弹出系统权限框=${permission.shouldShowRequestPermissionRationale}"
        Logger.d(TAG, msg)
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
        Logger.d(TAG, "main onDestroy")
    }


    private fun activeImTest() {
        // 临时测试用
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
