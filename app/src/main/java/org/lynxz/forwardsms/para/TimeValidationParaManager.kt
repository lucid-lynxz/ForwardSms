package org.lynxz.forwardsms.para

import android.content.Context
import androidx.lifecycle.MutableLiveData
import org.lynxz.forwardsms.SmsApplication
import org.lynxz.forwardsms.para.TimeValidationParaManager.allTimeDurationLiveData
import org.lynxz.forwardsms.para.TimeValidationParaManager.enableTimeDurationLiveData
import org.lynxz.forwardsms.para.TimeValidationParaManager.updateAndSavePara
import org.lynxz.forwardsms.util.StringUtil
import org.lynxz.forwardsms.validation.TimeDef
import org.lynxz.forwardsms.validation.TimeDurationBean
import org.lynxz.securitysp.ISpJsonUtil
import org.lynxz.securitysp.SecuritySP

/**
 * 时间配置参数,用于持久化
 * 通过 [allTimeDurationLiveData] 获取所有时间段信息
 * 通过 [enableTimeDurationLiveData] 获取是否启用时间段设置
 * 通过 [updateAndSavePara] 来更新并将数据存储到sp中
 *
 * todo:
 * 1. 多个时间段去重判断
 * */
object TimeValidationParaManager {
    private const val KEY_TIME_PARA = "sp_key_time_para"
    private const val KEY_SP_NAME = "sp_timeDurationSettings"

    // 时间段参数设置类,用于存储sp
    private data class TimeValidationPara(
        val dataList: MutableList<TimeDurationBean> = mutableListOf(), // 时间段信息列表
        var enable: Boolean = false // 是否启用
    )

    private val settingSp by lazy {
        SecuritySP(SmsApplication.app, KEY_SP_NAME, Context.MODE_PRIVATE).apply {
            spJsonUtil = object : ISpJsonUtil {
                override fun <T> parseJson(json: String, cls: Class<out T?>?) =
                    StringUtil.parseJson<T>(json, cls)

                override fun toJson(obj: Any?) = StringUtil.toJson(obj)
            }
        }
    }

    // 所有可编辑的时间段信息
    val allTimeDurationLiveData = MutableLiveData(mutableListOf<TimeDurationBean>())

    // 是否启用时间段设置
    val enableTimeDurationLiveData = MutableLiveData(false)

    // 存储在sp中的时间段配置参数
    private var timeValidationPara: TimeValidationPara

    init {
        timeValidationPara =
            settingSp.getPreference(
                KEY_TIME_PARA,
                TimeValidationPara::class.java,
                TimeValidationPara()
            )!!.apply {
                allTimeDurationLiveData.value = dataList
                enableTimeDurationLiveData.value = enable
            }
    }

    /**
     * 更新数据到sp文件中
     * */
    fun updateAndSavePara(enableSetting: Boolean = enableTimeDurationLiveData.value ?: false) =
        settingSp.putPreference(KEY_TIME_PARA, timeValidationPara.apply {
            enable = enableSetting
        })

    /**
     * 判断指定时间是否在用户设置的可转发时间段内
     * 若未启用转发时间段设置,则返回true
     * 若有多个重叠的时间段, 只要命中其中一个时间段,则返回true
     * */
    fun isInTimeDuration(targetTimeMs: Long = System.currentTimeMillis()): Boolean {
        val timeDef = TimeDef.generateByTimeMs(
            targetTimeMs
        )
        return !timeValidationPara.enable
                || timeValidationPara.dataList.isNullOrEmpty()
                || timeValidationPara.dataList.any { it.isInDuration(timeDef) }
    }
}
