package org.lynxz.forwardsms

import PermissionResultInfo
import android.Manifest
import android.text.method.ScrollingMovementMethod
import androidx.lifecycle.Observer
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.coroutines.*
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

    override fun getLayoutRes() = R.layout.activity_main
    var lastSms = ""
    override fun afterViewCreated() {

        // 显示tg用户名
        SmsConstantParas.tgUserNme = tgUserName
        SmsConstantParas.ddName = ddUserName

        edt_user_name_tg.setText(tgUserName)
        edt_user_name_dd.setText(ddUserName)

        tv_info.movementMethod = ScrollingMovementMethod.getInstance()

        requestPermission(Manifest.permission.READ_SMS)

        // 注册im
        SmsViewModel.activeIm()

        // 通知栏消息
        NotificationUtils.getInstance(this).sendNotification("短信转发", "正在运行中...", 100)

        // 添加测试代码
        btn_confirm_tg.setOnClickListener {
            tgUserName = edt_user_name_tg.text.toString().trim()
            SmsConstantParas.tgUserNme = tgUserName
        }
        btn_confirm_dd.setOnClickListener {
            ddUserName = edt_user_name_dd.text.toString().trim()
            SmsConstantParas.ddName = ddUserName
        }

//        smsModel = ViewModelProviders.of(this).get(SmsViewModel::class.java).apply {
//            init(application)
//        }
        SmsViewModel.getReceivedSms().observe(this, Observer<SmsDetail> {
            tv_info.text = it.toString()
        })

        SmsViewModel.getSmsHistory().observe(this, Observer {
            val his = StringBuilder(100)
            his.append("共获取短信 ").append(it.size).append("条:")
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
                content = "$lastSms-${msec2date()}"
            }

            // tg发送失败则尝试使用钉钉发送
            IMManager.sendTextMessage(ImType.TG, body.apply {
                name = SmsConstantParas.tgUserNme
            }) {
                launch(Dispatchers.Main) {
                    tv_info.text = "send msg from tg result ${it.ok}"
                }

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
    }
}
