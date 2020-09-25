package org.lynxz.forwardsms.para

import android.content.Context
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
import org.lynxz.forwardsms.BuildConfig
import org.lynxz.forwardsms.SmsApplication
import org.lynxz.forwardsms.bean.ImSetting
import org.lynxz.forwardsms.para.ImSettingManager.initPara
import org.lynxz.forwardsms.para.ImSettingManager.updateImSetting
import org.lynxz.forwardsms.util.ConfigUtil
import org.lynxz.forwardsms.util.LoggerUtil
import org.lynxz.forwardsms.util.StringUtil
import org.lynxz.imdingding.DingDingActionImpl
import org.lynxz.imfeishu.FeishuActionImpl
import org.lynxz.imtg.TGActionImpl
import org.lynxz.securitysp.ISpJsonUtil
import org.lynxz.securitysp.SecuritySP

/**
 * 全局参数设置
 * 1. 必须先调用 [initPara] 进行初始化
 * 2. 通过 [updateImSetting] 更新配置信息
 * */
object ImSettingManager {
    private const val TAG = "ImSettingManager"
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

        // 遍历支持的平台,并从sp中提取配置信息,并按需刷新token等信息
        initImSettingBySp(ImType.DingDing)
        initImSettingBySp(ImType.TG)
        initImSettingBySp(ImType.FeiShu)

        // 兼容旧版,迁移buildConfig中配置的数据
        updateImSetting(ImType.DingDing) {
            val setting = it as ImSetting.DDImSetting
            setting.corpId = BuildConfig.dd_corpid
            setting.corpSecret = BuildConfig.dd_corpsecret
            setting.agentId = BuildConfig.dd_agent
        }
        updateImSetting(ImType.TG) {
            val setting = it as ImSetting.TGImSetting
            setting.botToken = BuildConfig.tg_bottoken
            setting.targetUserName = BuildConfig.tg_default_userName
        }
        updateImSetting(ImType.FeiShu) {
            val setting = it as ImSetting.FeishuImSetting
            setting.appId = BuildConfig.feishu_appid
            setting.appSecret = BuildConfig.feishu_appsecret
        }
        imSettingMapLiveData.value = imSettingMap
    }

    /**
     * 从sp中提取配置, 并返回配置内容
     * 若之前未配置过,则返回null
     * */
    private fun initImSettingBySp(imType: String): ImSetting? {
        val keyName = getImTypeSpKeyName(imType)
        val setting =
            when (imType) {
                ImType.DingDing ->
                    imSettingSp.getPreference(keyName, ImSetting.DDImSetting::class.java, null)
                ImType.TG ->
                    imSettingSp.getPreference(keyName, ImSetting.TGImSetting::class.java, null)
                ImType.FeiShu ->
                    imSettingSp.getPreference(keyName, ImSetting.FeishuImSetting::class.java, null)
                else -> null
            }

        setting?.let {
            imSettingMap[imType] = it
            activeIm(imType, it.enable)
        }

        return setting
    }

    /**
     * 更新配置信息
     * 按需进行im刷新或者禁用
     * */
    fun updateImSetting(imType: String, recookImSettingPara: RecookImSettingPara) {
        val imSetting = imSettingMap[imType] ?: ImSetting.generateDefaultImSetting(imType)

        val oriEnable = imSetting.enable
        recookImSettingPara(imSetting)

        imSettingMap[imType] = imSetting
        imSettingSp.putPreference(getImTypeSpKeyName(imType), imSetting)
        imSettingMapLiveData.value = imSettingMap

        // 状态有发生变化时,更新
        if (imSetting.enable != oriEnable) {
            activeIm(imType, imSetting.enable)
        }
    }

    /**
     * 获取指定imType的配置文件
     * @param imType 平台类型,参考 [ImType]
     * */
    fun getImSetting(imType: String) = imSettingMap[imType]

    /**
     * 启用/禁用im
     * @return true-成功 false-失败
     * */
    private fun activeIm(imType: String, active: Boolean = true): Boolean {
        // 禁用im
        if (!active) {
            IMManager.setEnable(imType, active)
            return true
        }

        // 启用im: 重新初始化及刷新token等数据
        val imSetting = imSettingMap[imType] ?: return false
        imSetting.enable = active

        var impl: IIMAction? = null
        val para = when (imType) {
            ImType.DingDing -> {
                impl = DingDingActionImpl
                (imSetting as ImSetting.DDImSetting).let {
                    ImInitPara.DDInitPara(it.corpId, it.corpSecret, it.agentId)
                }
            }

            ImType.TG -> {
                impl = TGActionImpl
                (imSetting as ImSetting.TGImSetting).let {
                    ImInitPara.TGInitPara(it.botToken, it.targetUserName)
                }
            }

            ImType.FeiShu -> {
                impl = FeishuActionImpl
                (imSetting as ImSetting.FeishuImSetting).let {
                    ImInitPara.FeiShuPara(it.appId, it.appSecret)
                }
            }
            else -> null
        }

        if (para == null || impl == null) {
            LoggerUtil.w(TAG, "im $imType not support")
            return false
        }

        val result = impl.init(para.apply {
            propertyUtil = ConfigUtil(application, IIMAction.spIm)
        })
        LoggerUtil.w(TAG, "init im $imType result: ${StringUtil.toJson(result)}")
        val initSuccess = result.ok

        GlobalScope.launch {
            withContext(Dispatchers.IO) {
                IMManager.registerIm(imType, impl)
                IMManager.refresh(imType) {
                    LoggerUtil.d(TAG, "im $imType 初始化结果:${it.ok} ${it.detail}")
                }
            }
        }

        return initSuccess
    }
}

/**
 * 更新现有配置
 * 若指定的imtype未配置,则会生成一个默认配置对象
 * */
typealias RecookImSettingPara = (ImSetting) -> Unit

