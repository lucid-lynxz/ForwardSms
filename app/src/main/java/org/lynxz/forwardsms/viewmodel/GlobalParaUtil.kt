package org.lynxz.forwardsms.viewmodel

import android.Manifest
import android.app.Application
import android.app.Notification
import android.content.IntentFilter
import android.database.ContentObserver
import android.database.Cursor
import android.net.Uri
import android.provider.Telephony
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import androidx.core.database.getDoubleOrNull
import androidx.core.database.getIntOrNull
import androidx.core.database.getLongOrNull
import androidx.core.database.getStringOrNull
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.lynxz.baseimlib.IMManager
import org.lynxz.baseimlib.actions.IIMAction
import org.lynxz.baseimlib.bean.ImInitPara
import org.lynxz.baseimlib.bean.ImType
import org.lynxz.baseimlib.convert2Str
import org.lynxz.forwardsms.BuildConfig
import org.lynxz.forwardsms.bean.MessageSrcType
import org.lynxz.forwardsms.bean.SmsDetail
import org.lynxz.forwardsms.bean.isSameAs
import org.lynxz.forwardsms.network.SmsConstantParas
import org.lynxz.forwardsms.observer.IAppNotificationObserver
import org.lynxz.forwardsms.observer.ISmsReceiveObserver
import org.lynxz.forwardsms.para.BatteryListenerManager
import org.lynxz.forwardsms.receiver.SmsReceiver
import org.lynxz.forwardsms.ui.trans.PermissionFragment
import org.lynxz.forwardsms.ui.widget.SmsHandler
import org.lynxz.forwardsms.ui.widget.SmsNotificationListenerService
import org.lynxz.forwardsms.util.ConfigUtil
import org.lynxz.forwardsms.util.StringUtil
import org.lynxz.forwardsms.validation.IForwardVerify
import org.lynxz.forwardsms.validation.TimeDef
import org.lynxz.forwardsms.validation.VerifyActionManager
import org.lynxz.forwardsms.viewmodel.GlobalParaUtil.addVerifyActionIfAbsent
import org.lynxz.forwardsms.viewmodel.GlobalParaUtil.getReceivedSms
import org.lynxz.forwardsms.viewmodel.GlobalParaUtil.init
import org.lynxz.forwardsms.viewmodel.GlobalParaUtil.removeVerifyAction
import org.lynxz.imdingding.DingDingActionImpl
import org.lynxz.imfeishu.FeishuActionImpl
import org.lynxz.imtg.TGActionImpl
import org.lynxz.utils.log.LoggerUtil

/**
 * sms接收监听及短信列表读取,并监听通知栏变化(需要自行在手机设置中启用通知栏权限)
 * 使用:
 * 1.在application中调用初始化方法 [init]
 * 2. 获取上一次接收到的短信信息 [getReceivedSms]
 * 3. 通过 [addVerifyActionIfAbsent] 和 [removeVerifyAction] 来 添加/移除 转发条件判定
 * */
object GlobalParaUtil {
    private const val TAG = "SmsViewModel"
    private var app: Application? = null
    private val smsContentUri: Uri = Uri.parse("content://sms/inbox")

    // 用于自测,生成模拟短信信息
    fun mockSmsReceived() {
        val info =
            "{\"body\":\"【测试信息】当前时间:" +
                    TimeDef.generateByTimeMs().toString() +
                    "。\",\"displayFrom\":\"1069129206112106575\",\"forward\":true,\"from\":\"1069129206112106575\",\"read\":0,\"srcType\":\"com.android.mms\",\"status\":0,\"to\":\"HM 1SC\",\"ts\":1601124202000,\"type\":1}"
        val msg = StringUtil.parseJson(info, SmsDetail::class.java)
        iSmsReceiveObserver.onReceiveSms(msg)
    }

    // 最后收到的短信
    private val smsReceivedLiveData by lazy { MutableLiveData<SmsDetail>() }

    fun getReceivedSms(): LiveData<SmsDetail> = smsReceivedLiveData

    /**
     * 发送新的消息内容
     * */
    fun postSmsDetail(smsDetail: SmsDetail) {
        smsReceivedLiveData.postValue(smsDetail)
    }

    // 短信列表
    private val smsHistoryLiveData by lazy { MutableLiveData<List<SmsDetail>>() }

    fun getSmsHistory(): LiveData<List<SmsDetail>> = smsHistoryLiveData

    // contentResolver 中读取的最新的短信
    private var lastSmsInDb: SmsDetail? = null


    // 添加转发验证条件
    fun addVerifyActionIfAbsent(action: IForwardVerify): GlobalParaUtil {
        VerifyActionManager.addVerifyActionIfAbsent(action)
        return this
    }

    // 移除转发验证条件
    fun removeVerifyAction(action: IForwardVerify): GlobalParaUtil {
        VerifyActionManager.removeVerifyAction(action)
        return this
    }

    // 待转发的消息 liveData
    // 实际转发操作是在 ForwardService 中
    private val iSmsReceiveObserver = object : ISmsReceiveObserver {
        override fun onReceiveSms(smsDetail: SmsDetail?) {
            LoggerUtil.w(TAG, "onReceiveSms msg  info0: ${convert2Str(smsDetail)}")
            if (!smsDetail?.body.isNullOrBlank() && enableForwardSms) {
                smsReceivedLiveData.value = smsDetail?.apply {
                    forward = VerifyActionManager.isValid(this)
                    LoggerUtil.w(TAG, "onReceiveSms msg final info: ${convert2Str(smsDetail)}")
                }
            }
        }
    }

    // 通知栏中的app推送消息监听, 当前只监听微信
    private val wechatNotificationObserver = object : IAppNotificationObserver {
        override fun onReceiveAppNotification(
            pkgName: String,
            sbn: StatusBarNotification?,
            rankingMap: NotificationListenerService.RankingMap?
        ) {
            LoggerUtil.w(TAG, "获取到notification包名: $pkgName")
            if (pkgName == "com.tencent.mm") { // 微信
                val notification = sbn!!.notification

                // 通知标题信息:
                // 群消息时表示群名称
                // 普通好友消息时表示好友名称
                val title = notification?.extras?.get(Notification.EXTRA_TITLE)
                notification?.tickerText?.toString()?.let {
//                    val index = it.indexOf(":")
//                    LoggerUtil.w(TAG, "获取到微信标题: $title 内容为: $it")
//                    if (index <= 0) {
//                        return
//                    }
//
//                    val fromUser = it.substring(0, index) // 发送人
//                    val wxContent = it.substring(index + 1, it.length) // 消息内容
//
//                    smsReceivedLiveData.postValue(SmsDetail().apply {
//                        body = wxContent
//                        from = "$title $fromUser(微信)"
//                    })
                    smsReceivedLiveData.postValue(SmsDetail().apply {
                        body = it
                        from = "$title(微信)"
                        srcType = MessageSrcType.WECHAT
                        forward = VerifyActionManager.isValid(this)
                    })
                }
            }
        }
    }

    // 短信接收监听
    private val smsReceiver = SmsReceiver(iSmsReceiveObserver)

    // 短信数据库变化监听,避免国产rom无法监听短信接收时使用
    // todo 暂不使用,因国内rom对短信数据库有定制,不通用,后续有时间研究
    private val smsContentResolverObserver = object : ContentObserver(SmsHandler) {
        override fun onChange(selfChange: Boolean) {
            super.onChange(selfChange)
            LoggerUtil.d(TAG, "onChange $selfChange")
            val list = reloadSmsHistory(1)
            val tLastSms = list.getOrNull(0)

            if (tLastSms.isSameAs(lastSmsInDb)) {
                return
            }

            lastSmsInDb = tLastSms
            if (lastSmsInDb.isSameAs(smsReceivedLiveData.value)) { // 短信监听可用时,不再通过数据库获取
                app?.contentResolver?.unregisterContentObserver(this)
            } else { // 监听不可用,则从数据库读取并更新liveData
                smsReceivedLiveData.postValue(lastSmsInDb)
            }
        }

        override fun onChange(selfChange: Boolean, uri: Uri?) {
            super.onChange(selfChange, uri)
            LoggerUtil.d(TAG, "onChange2 $selfChange $uri")
        }
    }

    fun init(application: Application): GlobalParaUtil {
        if (app == null) {
            app = application.apply {
                // 注册短信接收广播监听
                registerReceiver(
                    smsReceiver, IntentFilter(Telephony.Sms.Intents.SMS_RECEIVED_ACTION)
                )

                // registerReceiver(
                //    smsReceiver, IntentFilter(Telephony.Sms.Intents.SMS_DELIVER_ACTION)
                // )

                // 通过通知栏推送获取短信内容(无法获取发件人等信息)
                // SmsNotificationListenerService.registerSmsObserver(iSmsReceiveObserver)

                // 通过通知栏获取并转发微信消息
                // SmsNotificationListenerService.registerAppObserver(wechatNotificationObserver)

                // 通过短信数据库监听内容变化
                // contentResolver.registerContentObserver(
                //     smsContentUri, true,
                //     smsContentResolverObserver
                // )
            }
        }

        // 低电量监听
        BatteryListenerManager.batteryInfoLiveData.observeForever {
            smsReceivedLiveData.value = SmsDetail().apply {
                from = "手机电量监听"// 原始发信人
                body =
                    "${SmsConstantParas.phoneTag} 当前电量: ${it.level}%, ${if (it.level >= 100) "记得拔掉电源" else "请及时充电"}" // 短信内容
                srcType = MessageSrcType.BATTERY_LISTENER
            }
        }
        return this
    }

    fun release() {
        app?.unregisterReceiver(smsReceiver)
        SmsNotificationListenerService.registerAppObserver(null)
        // app?.contentResolver?.unregisterContentObserver(smsContentResolverObserver)
    }

    private val smsQueryProjection =
        arrayOf("_id", "address", "person", "body", "date", "status", "type", "read")

    fun loadSmsHistory(maxCount: Int = 10) {
        smsHistoryLiveData.postValue(reloadSmsHistory(maxCount))
    }

    /**
     * 读取历史短信列表
     * @param maxCount 最多读取条数限制
     * */
    private fun reloadSmsHistory(maxCount: Int = 10): MutableList<SmsDetail> {
        val list = mutableListOf<SmsDetail>()
        val granted = PermissionFragment.isPermissionGranted(app!!, Manifest.permission.READ_SMS)
        if (!granted) {
            LoggerUtil.e(TAG, "read_sms权限未授予,读取短信列表失败")
            return list
        }

        val cr = app?.contentResolver ?: return list


        // todo 子线程
        // todo 添加短信内容监听
        cr.query(smsContentUri, smsQueryProjection, null, null, "date desc")?.let { cursor ->
            while (cursor.moveToNext()) {
                list.add(SmsDetail().apply {
                    from = cursor.getContent("address").toString() // 手机号
                    displayFrom = cursor.getContent("person").toString() // 姓名
                    body = cursor.getContent("body").toString() // 消息内容
                    ts = cursor.getContent("date", 0L) as Long // 日期
                    type = cursor.getContent("type", -1) as Int // 1:接收的 2:发出的
                    status = cursor.getContent("status", -1) as Int // 0:未读，1:已读
                    read = cursor.getContent("read", -1) as Int // 0:未读 1:已读
                })

                if (list.size >= maxCount) {
                    break
                }
            }
            cursor.close()
        }


        return list
    }

    private fun Cursor.getContent(key: String, defaultValue: Any = ""): Any {
        val columnIndex = getColumnIndex(key)
        if (columnIndex < 0) return defaultValue

        return when (defaultValue) {
            is String -> getStringOrNull(columnIndex) ?: defaultValue
            is Long -> getLongOrNull(columnIndex) ?: defaultValue
            is Int -> getIntOrNull(columnIndex) ?: defaultValue
            is Double -> getDoubleOrNull(columnIndex) ?: defaultValue
            else -> defaultValue
        }
    }

    /**
     * 启用支持的im,默认启用钉钉和tg
     * */
    fun activeIm(vararg imTypes: String?) {
        if (imTypes.isNullOrEmpty()) {
            activeImDingding()
            activeImTg()
            activeImFeishu()
            return
        }
        for (type in imTypes) {
            when (type) {
                ImType.DingDing -> activeImDingding()
                ImType.TG -> activeImTg()
                ImType.FeiShu -> activeImFeishu()
            }
        }
    }

    /**
     * 启用电报im
     * */
    fun activeImTg() {
        val initResult = TGActionImpl.init(
            ImInitPara.TGInitPara(
                BuildConfig.tg_bottoken,
                BuildConfig.tg_default_userName
            ).apply {
                propertyUtil = ConfigUtil(app!!, IIMAction.spIm)
            })
        LoggerUtil.d(TAG, "activeImTg result $initResult")

        GlobalScope.launch {
            withContext(Dispatchers.IO) {
                IMManager.registerIm(ImType.TG, TGActionImpl)
                IMManager.refresh(ImType.TG) {
                    LoggerUtil.d(TAG, "tg初始化结果:${it.ok} ${it.detail}")
                }
            }
        }
    }

    /**
     * 启用钉钉im
     * */
    private fun activeImDingding() {
        val initResult = DingDingActionImpl.init(
            ImInitPara.DDInitPara(
                BuildConfig.dd_corpid,
                BuildConfig.dd_corpsecret,
                BuildConfig.dd_agent
            ).apply {
                propertyUtil = ConfigUtil(app!!, IIMAction.spIm)
            })
        LoggerUtil.d(TAG, "activeImDD result $initResult")

        GlobalScope.launch {
            withContext(Dispatchers.IO) {
                IMManager.registerIm(ImType.DingDing, DingDingActionImpl)
                IMManager.refresh(ImType.DingDing) {
                    LoggerUtil.d(TAG, "钉钉初始化结果:${it.ok} ${it.detail}")
                }
            }
        }
    }

    /**
     * 启用飞书im
     * */
    private fun activeImFeishu() {
        val initResult = FeishuActionImpl.init(
            ImInitPara.FeiShuPara(
                BuildConfig.feishu_appid,
                BuildConfig.feishu_appsecret
            ).apply {
                propertyUtil = ConfigUtil(app!!, IIMAction.spIm)
            })
        LoggerUtil.d(TAG, "activeImFeishu result $initResult")

        GlobalScope.launch {
            withContext(Dispatchers.IO) {
                IMManager.registerIm(ImType.FeiShu, FeishuActionImpl)
                IMManager.refresh(ImType.FeiShu) {
                    LoggerUtil.d(TAG, "飞书初始化结果:${it.ok} ${it.detail}")
                }
            }
        }
    }

    /**
     * 通过通知栏获取并转发微信消息
     */
    fun enableForwardWechat(enable: Boolean) {
        VerifyActionManager.setSrcTypeState(MessageSrcType.WECHAT, enable)
        if (enable) {
            SmsNotificationListenerService.registerAppObserver(wechatNotificationObserver)
        } else {
            SmsNotificationListenerService.registerAppObserver(null)
        }
    }


    private var enableForwardSms = true

    /**
     * 是否允许转发短信
     * */
    fun enableForwardSms(enable: Boolean) {
        enableForwardSms = enable
        VerifyActionManager.setSrcTypeState(MessageSrcType.SMS, enable)
    }
}