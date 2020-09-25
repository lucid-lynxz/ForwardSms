package org.lynxz.forwardsms.validation

import java.util.*

/**
 * 时间段
 * */
class TimeDurationBean(
    var startTime: TimeDef = TimeDef(), // 开始时间,默认为当前时间
    var endTime: TimeDef = TimeDef(23, 59) // 结束时间
) : Cloneable {
    init {
        val cal = Calendar.getInstance(Locale.CHINA)
        cal.timeInMillis = System.currentTimeMillis()
        startTime.hour = cal.get(Calendar.HOUR_OF_DAY)
        startTime.minute = cal.get(Calendar.MINUTE)
    }

    // 是否正在编辑起始时间,默认为true
    var isEditingStartTime = true
        set(value) {
            field = value
            curEditingTime = if (value) startTime else endTime
        }

    // 当前正在操作的时间信息
    var curEditingTime = startTime
        private set

    // 时间配置信息是否合法: 结束时间大于起始时间
    fun isValid() = endTime.isLargerThan(startTime)

    // 是否在当前时间段内
    fun isInDuration(timeDef: TimeDef) =
        timeDef.isLargerOrEqualWith(startTime) and endTime.isLargerOrEqualWith(timeDef)


    public override fun clone(): TimeDurationBean {
        return super.clone() as TimeDurationBean
    }

    override fun toString(): String {
        return "$startTime~$endTime"
    }
}

/**
 * 起止时间定义,格式: x点y分, 24h制
 * */
data class TimeDef(
    var hour: Int = 0,
    var minute: Int = 0
) : Cloneable {

    companion object {
        /**
         * 给定一个时间戳,生成 TimeDef 对象
         * */
        fun generateByTimeMs(targetTimeMs: Long = System.currentTimeMillis()) = TimeDef().apply {
            val cal = Calendar.getInstance()
            cal.timeInMillis = targetTimeMs
            hour = cal.get(Calendar.HOUR_OF_DAY)
            minute = cal.get(Calendar.MINUTE)
        }
    }

    override fun toString(): String {
        return String.format("%02d:%02d", hour, minute)
    }

    public override fun clone(): TimeDef {
        return super.clone() as TimeDef
    }

    /**
     * 两个时间点之间进行比较
     * @return true-当前时间比较大
     * */
    fun isLargerThan(other: TimeDef) = convert2Minutes() > other.convert2Minutes()

    /**
     * 两个时间点之间进行比较
     * @return true-当前时间大于等于指定的时间
     * */
    fun isLargerOrEqualWith(other: TimeDef) = convert2Minutes() >= other.convert2Minutes()

    fun convert2Date(): Calendar = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, hour)
        set(Calendar.MINUTE, minute)
    }

    /**
     * 将 x点y分 信息转换为分钟数, 便于比较两个时间值大小
     * */
    private fun convert2Minutes() = hour * 60 + minute
}