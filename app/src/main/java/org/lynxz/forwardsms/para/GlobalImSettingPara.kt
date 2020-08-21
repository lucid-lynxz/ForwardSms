package org.lynxz.forwardsms.para

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import org.lynxz.baseimlib.bean.ImType
import org.lynxz.baseimlib.convert2Str
import org.lynxz.forwardsms.SmsApplication
import org.lynxz.forwardsms.para.GlobalImSettingPara.initPara
import org.lynxz.forwardsms.para.GlobalImSettingPara.updateImSetting
import org.lynxz.forwardsms.util.LoggerUtil
import org.lynxz.forwardsms.util.StringUtil
import org.lynxz.securitysp.ISpJsonUtil
import org.lynxz.securitysp.SecuritySP

/**
 * 全局参数设置
 * 1. 必须先调用 [initPara] 进行初始化
 * 2. 通过 [updateImSetting] 更新配置信息
 * */
object GlobalImSettingPara {
    private const val KEY_PREFIX = "imsetting_"

    private lateinit var application: SmsApplication
    private val imSettingSp by lazy {
        SecuritySP(
            application,
            "sp_imSettings",
            Context.MODE_PRIVATE
        ).apply {
            spJsonUtil = object : ISpJsonUtil {
                override fun <T> parseJson(json: String, cls: Class<out T?>?) =
                    StringUtil.parseJson<T>(json, cls)

                override fun toJson(obj: Any?) = StringUtil.toJson(obj)
            }
        }
    }

    /**
     * 缓存支持的所有IM及对应的设置信息
     * 在 [initPara] 进行初始化
     * */
    private val imSettingMapLiveData = MutableLiveData<MutableMap<String, ImSetting?>>().apply {
        value = imSettingMap
    }

    /**
     * 缓存支持的所有IM及对应的设置信息
     * 在 [initPara] 进行初始化
     * */
    private val imSettingMap = mutableMapOf<String, ImSetting?>()

    /**
     * 获取指定im在sp唤醒信息的key名
     * */
    private fun getImTypeSpKeyName(imType: String) = "${KEY_PREFIX}$imType"


    /**
     * 初始化配置信息
     * */
    fun initPara(application: SmsApplication) {
        this.application = application

        // 遍历支持的平台,并从sp中提取配置信息
        listOf(ImType.DingDing, ImType.FeiShu, ImType.TG).forEach { type ->
            val spKeyName = getImTypeSpKeyName(type)
            val imPara = imSettingSp.getPreference(spKeyName, ImSetting(type, false, ""))
            imSettingMap[type] = imPara
            imSettingSp.putPreference(spKeyName, convert2Str(imPara))
            LoggerUtil.w(
                "xxxx",
                "$type ... im setting: type=$type ${imPara?.targetUserName} ${imPara?.enable}"
            )
        }
        imSettingMapLiveData.value = imSettingMap
    }

    /**
     * 获取配置livedata
     * */
    fun imSettingMapLiveData(): LiveData<MutableMap<String, ImSetting?>> = imSettingMapLiveData

    /**
     * 更新配置信息
     * */
    fun updateImSetting(imType: String, recookImSettingPara: RecookImSettingPara) {
        val imSetting = recookImSettingPara(imSettingMap[imType])
        imSettingMap[imType] = imSetting
//        imSettingSp.putPreference(getImTypeSpKeyName(imType), convert2Str(imSetting))
        imSettingSp.putPreference(getImTypeSpKeyName(imType), imSetting)
    }
}

typealias RecookImSettingPara = (ImSetting?) -> ImSetting

data class ImSetting(
    val imType: String, // im标志,参考 [ImType]
    var enable: Boolean, // 是否允许转发到该IM
    var targetUserName: String // 接收该消息的用户名
)