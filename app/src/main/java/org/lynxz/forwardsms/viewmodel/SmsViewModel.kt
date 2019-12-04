package org.lynxz.forwardsms.viewmodel

import PermissionFragment
import android.Manifest
import android.app.Application
import android.content.IntentFilter
import android.database.ContentObserver
import android.database.Cursor
import android.net.Uri
import android.provider.Telephony
import androidx.core.database.getDoubleOrNull
import androidx.core.database.getIntOrNull
import androidx.core.database.getLongOrNull
import androidx.core.database.getStringOrNull
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.lynxz.baseimlib.IMManager
import org.lynxz.baseimlib.actions.IIMAction
import org.lynxz.baseimlib.bean.ImInitPara
import org.lynxz.baseimlib.bean.ImType
import org.lynxz.forwardsms.BuildConfig
import org.lynxz.forwardsms.bean.SmsDetail
import org.lynxz.forwardsms.bean.isSameAs
import org.lynxz.forwardsms.observer.ISmsReceiveObserver
import org.lynxz.forwardsms.receiver.SmsReceiver
import org.lynxz.forwardsms.util.ConfigUtil
import org.lynxz.forwardsms.util.Logger
import org.lynxz.forwardsms.viewmodel.SmsViewModel.getReceivedSms
import org.lynxz.forwardsms.viewmodel.SmsViewModel.init
import org.lynxz.forwardsms.widget.SmsHandler
import org.lynxz.imdingding.DingDingActionImpl
import org.lynxz.imtg.TGActionImpl

/**
 * sms接收监听及短信列表读取
 * 使用:
 * 1.在application中调用初始化方法 [init]
 * 2. 获取上一次接收到的短信信息 [getReceivedSms]
 * */
object SmsViewModel : ViewModel() {
    private const val TAG = "SmsViewModel"
    private var app: Application? = null
    private val smsContentUri: Uri = Uri.parse("content://sms/inbox")

    // 最后收到的短信
    private val smsReceivedLiveData by lazy { MutableLiveData<SmsDetail>() }

    fun getReceivedSms(): LiveData<SmsDetail> = smsReceivedLiveData

    // 短信列表
    private val smsHistoryLiveData by lazy { MutableLiveData<List<SmsDetail>>() }

    fun getSmsHistory(): LiveData<List<SmsDetail>> = smsHistoryLiveData

    // contentResolver 中读取的最新的短信
    private var lastSmsInDb: SmsDetail? = null

    private val iSmsReceiveObserver = object : ISmsReceiveObserver {
        override fun onReceiveSms(smsDetail: SmsDetail?) {
            if (!smsDetail?.body.isNullOrBlank()) {
                smsReceivedLiveData.value = smsDetail
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
            Logger.d(TAG, "onChange $selfChange")
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
            Logger.d(TAG, "onChange2 $selfChange $uri")
        }
    }

    fun init(application: Application): SmsViewModel {
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


                // 通过短信数据库监听内容变化
                // contentResolver.registerContentObserver(
                //     smsContentUri, true,
                //     smsContentResolverObserver
                // )
            }
        }
        return this
    }

    override fun onCleared() {
        super.onCleared()
        app?.unregisterReceiver(smsReceiver)
        // SmsNotificationListenerService.registerSmsObserver(null)
        // app?.contentResolver?.unregisterContentObserver(smsContentResolverObserver)
    }

    private val smsQueryProjection =
        arrayOf("_id", "address", "person", "body", "date", "status", "type", "read")

    fun loadSmsHistory(maxCount: Int = 10) {
        val list = reloadSmsHistory(maxCount)
        if (list.isNotEmpty()) {
            smsHistoryLiveData.postValue(list)
        }
    }

    /**
     * 读取历史短信列表
     * @param maxCount 最多读取条数限制
     * */
    private fun reloadSmsHistory(maxCount: Int = 10): MutableList<SmsDetail> {
        val list = mutableListOf<SmsDetail>()
        val granted = PermissionFragment.isPermissionGranted(app!!, Manifest.permission.READ_SMS)
        if (!granted) {
            Logger.e(TAG, "read_sms权限未授予,读取短信列表失败")
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
     * 启用支持的im中断
     * */
    fun activeIm() {

        var initResult = DingDingActionImpl.init(
            ImInitPara.DDInitPara(
                BuildConfig.dd_corpid,
                BuildConfig.dd_corpsecret,
                BuildConfig.dd_agent
            ).apply {
                propertyUtil = ConfigUtil(app!!, IIMAction.spIm)
            })
        Logger.d(TAG, "activeImDD result $initResult")


        initResult = TGActionImpl.init(
            ImInitPara.TGInitPara(
                BuildConfig.tg_bottoken,
                BuildConfig.tg_default_userName
            ).apply {
                propertyUtil = ConfigUtil(app!!, IIMAction.spIm)
            })
        Logger.d(TAG, "activeImTg result $initResult")

        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                IMManager.registerIm(ImType.DingDing, DingDingActionImpl)
                IMManager.registerIm(ImType.TG, TGActionImpl)

                IMManager.refresh(ImType.DingDing)
                IMManager.refresh(ImType.TG)
            }
        }
    }
}