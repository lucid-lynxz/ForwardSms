package org.lynxz.forwardsms.para

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import org.lynxz.baseimlib.bean.ImType
import org.lynxz.forwardsms.SmsApplication
import org.lynxz.forwardsms.bean.ImSetting
import org.lynxz.forwardsms.para.GlobalImSettingPara.initPara
import org.lynxz.forwardsms.para.GlobalImSettingPara.updateImSetting
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
     * 获取配置livedata
     * */
    fun imSettingMapLiveData(): LiveData<MutableMap<String, ImSetting?>> = imSettingMapLiveData

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
        initImSettingBySp(ImType.DingDing)
        initImSettingBySp(ImType.TG)
        initImSettingBySp(ImType.FeiShu)

        imSettingMapLiveData.value = imSettingMap
    }

    /**
     * 从sp中提取配置, 并返回配置内容
     * 若之前未配置过,则返回null
     * */
    private fun initImSettingBySp(imType: String): ImSetting? {
        val keyName = getImTypeSpKeyName(imType)
        val setting = when (imType) {
            ImType.DingDing -> imSettingSp.getPreference<ImSetting.DDImSetting>(keyName, null)
            ImType.TG -> imSettingSp.getPreference<ImSetting.TGImSetting>(keyName, null)
            ImType.FeiShu -> imSettingSp.getPreference<ImSetting.FeishuImSetting>(keyName, null)
            else -> null
        }

        setting?.let {
            imSettingMap[imType] = it
        }

        return setting
    }

    /**
     * 更新配置信息
     * */
    fun updateImSetting(imType: String, recookImSettingPara: RecookImSettingPara) {
        val imSetting = imSettingMap[imType] ?: ImSetting.generateDefaultImSetting(imType)
        recookImSettingPara(imSetting)
        imSettingMap[imType] = imSetting
        imSettingSp.putPreference(getImTypeSpKeyName(imType), imSetting)
        imSettingMapLiveData.value = imSettingMap
    }
}

/**
 * 更新现有配置
 * 若指定的imtype未配置,则会生成一个默认配置对象
 * */
typealias RecookImSettingPara = (ImSetting) -> Unit

