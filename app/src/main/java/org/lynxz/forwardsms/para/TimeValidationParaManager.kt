package org.lynxz.forwardsms.para

import androidx.lifecycle.MutableLiveData
import org.lynxz.forwardsms.para.TimeValidationParaManager.allTimeDurationLiveData
import org.lynxz.forwardsms.para.TimeValidationParaManager.enableTimeDurationLiveData
import org.lynxz.forwardsms.para.TimeValidationParaManager.updateAndSavePara
import org.lynxz.forwardsms.validation.TimeDef
import org.lynxz.forwardsms.validation.TimeDurationBean
import java.util.*

/**
 * 时间配置参数,用于持久化
 * 通过 [allTimeDurationLiveData] 获取所有时间段信息
 * 通过 [enableTimeDurationLiveData] 获取是否启用时间段设置
 * 通过 [updateAndSavePara] 来更新并将数据存储到sp中
 *
 * todo:
 * 1. 多个时间段去重判断
 * */
object TimeValidationParaManager :
    AbsSpSettingInfoManager<TimeValidationParaManager.TimeValidationPara>() {
    // 时间段参数设置类,用于存储sp
    data class TimeValidationPara(
        val timeDurationList: MutableList<TimeDurationBean> = mutableListOf(), // 时间段信息列表
        val weekDayList: MutableList<Int> = mutableListOf(
            1, // 周一
            2, // 周二
            3, // 周三
            4, // 周四
            5, // 周五
            6, // 周六
            7 // 周天
        ), // 可转发的星期几信息列表,默认都可转发
        var enableTimeValidate: Boolean = false, // 是否启用可转发时间段设置
        var enableDateValidate: Boolean = false // 是否启用可转发星期设置
    )

    // 所有可编辑的时间段信息
    val allTimeDurationLiveData = MutableLiveData(mutableListOf<TimeDurationBean>())
    val allDateLiveData = MutableLiveData(mutableListOf<Int>())

    // 是否启用时间段设置
    val enableTimeDurationLiveData = MutableLiveData(false)

    // 是否启用日期设置
    val enableDateLiveData = MutableLiveData(true)

    init {
        paraBean.apply {
            allTimeDurationLiveData.value = timeDurationList
            allDateLiveData.value = weekDayList
            enableTimeDurationLiveData.value = enableTimeValidate
            enableDateLiveData.value = enableDateValidate
        }
    }

    override fun getParaFromSp(paraKey: String) =
        getSecuritySp().getPreference(
            paraKey,
            TimeValidationPara::class.java,
            TimeValidationPara()
        )!!

    /**
     * 更新数据到sp文件中
     * @param enableTimeDurationSetting true-启用可转发时间段设置
     * @param enableDateSetting true-启用可转发星期几设置
     * */
    fun updateAndSavePara(
        enableTimeDurationSetting: Boolean = enableTimeDurationLiveData.value ?: false,
        enableDateSetting: Boolean = enableDateLiveData.value ?: false
    ) {
        paraBean.enableTimeValidate = enableTimeDurationSetting
        paraBean.enableDateValidate = enableDateSetting
        savePara()
    }

    /**
     * 判断指定时间是否在用户设置的可转发时间段内
     * 若未启用转发时间段设置,则返回true
     * 若有多个重叠的时间段, 只要命中其中一个时间段,则返回true
     * */
    fun isInTimeDuration(targetTimeMs: Long = System.currentTimeMillis()): Boolean {
        val timeDef = TimeDef.generateByTimeMs(
            targetTimeMs
        )
        return !paraBean.enableTimeValidate
                || paraBean.timeDurationList.isNullOrEmpty()
                || paraBean.timeDurationList.any { it.isInDuration(timeDef) }
    }

    /**
     * 指定星期几是否可转发
     * @param targetWeekDay 指定的星期几(1-周一, 2-周二, .... 7-周天, -1 表示今天)
     * */
    fun isInWeekDays(targetWeekDay: Int = -1): Boolean {
        // 若未启用可转发星期几设置,则默认符合要求
        if (!paraBean.enableDateValidate) {
            return true
        }

        return if (targetWeekDay <= 0) {
            val calendar = Calendar.getInstance()
            calendar.time = Date(System.currentTimeMillis())
            var todayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)  // 周天是1, 周一是2...
            // 转换为: 周1:1 周2:2...
            todayOfWeek = if (todayOfWeek == Calendar.SUNDAY) Calendar.SATURDAY + 1 else todayOfWeek
            todayOfWeek -= 1
            todayOfWeek in paraBean.weekDayList
        } else {
            targetWeekDay in paraBean.weekDayList
        }
    }
}
