package org.lynxz.forwardsms.ui.fragment.othersetting

import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import org.lynxz.forwardsms.para.BatteryListenerManager
import org.lynxz.forwardsms.para.MosaicParaManager
import org.lynxz.forwardsms.para.TimeValidationParaManager
import org.lynxz.forwardsms.validation.TimeDef
import org.lynxz.forwardsms.validation.TimeDurationBean
import org.lynxz.utils.no
import java.util.*

class OtherSettingViewModel : ViewModel() {

    // 所有可编辑的时间段信息
    val allTimeDurationLiveData = TimeValidationParaManager.allTimeDurationLiveData
    val allDateLiveData = TimeValidationParaManager.allDateLiveData

    // 是否启用时间段设置
    val enableTimeDurationLiveData = TimeValidationParaManager.enableTimeDurationLiveData

    // 是否启用可转发星期几设置
    val enableDateLiveData = TimeValidationParaManager.enableDateLiveData

    // 当前正在编辑的时间段对象序号, -1表示新增的
    private var selectTimeDurationIndex = -1

    // 当前正在编辑的时间信息
    val timeDurationBeanLiveData = MutableLiveData(TimeDurationBean(TimeDef(), TimeDef()))

    // 是否允许添加转发时间段
    val enableAddTimeDurationLiveData = MediatorLiveData<Boolean>().apply {
        addSource(enableTimeDurationLiveData) { enable ->
            value = enable && allTimeDurationLiveData.value!!.size <= 5
            TimeValidationParaManager.updateAndSavePara()
        }
        addSource(allTimeDurationLiveData) { list ->
            value = enableTimeDurationLiveData.value!! && list.size <= 5
            TimeValidationParaManager.updateAndSavePara()
        }
    }

    /**
     * 更新当前选中的时间
     * */
    fun updateEditingTime(date: Date) {
        val calendar = Calendar.getInstance()
        calendar.time = date
        timeDurationBeanLiveData.value?.let { bean ->
            bean.curEditingTime.apply {
                hour = calendar.get(Calendar.HOUR_OF_DAY)
                minute = calendar.get(Calendar.MINUTE)
            }
            timeDurationBeanLiveData.value = bean
        }
    }

    /**
     * 切换当前正在编辑的时间信息
     * @param start true-正在编辑起始时间 false-编辑结束时间
     * */
    fun setEditingTime(start: Boolean) {
        timeDurationBeanLiveData.value?.let {
            it.isEditingStartTime = start
            timeDurationBeanLiveData.value = it
        }
    }

    /**
     * 将当前正在编辑的保存并使之生效
     * */
    fun applyTimeDuration(): Boolean {
        val bean = timeDurationBeanLiveData.value ?: return false
        bean.isValid().no { return false }

        val list = allTimeDurationLiveData.value!!
        if (selectTimeDurationIndex in 0 until list.size) {
            list[selectTimeDurationIndex] = bean
        } else {
            list.add(bean)
        }
        allTimeDurationLiveData.value = list
        return true
    }

    /**
     * 选择当前正在编辑的时间段序号
     * @param index 负数表示新增
     * @param delete true-删除指定时间段
     *
     * */
    fun chooseTimeDurationForEditing(index: Int, delete: Boolean = false) {
        val list = allTimeDurationLiveData.value!!
        if (delete) {
            if (index in 0 until list.size) {
                list.removeAt(index)
                allTimeDurationLiveData.value = list
            }
            return
        }
        selectTimeDurationIndex = index
        timeDurationBeanLiveData.value =
            if (index in 0 until list.size) {
                list[index].clone()
            } else {
                TimeDurationBean().apply {
                    isEditingStartTime = true
                }
            }
    }

    /**
     * 删除正在编辑的时间段信息
     * */
    fun deleteCurrentEditingTimeDuration() {
        val list = allTimeDurationLiveData.value!!
        if (selectTimeDurationIndex in 0 until list.size) {
            list.removeAt(selectTimeDurationIndex)
            allTimeDurationLiveData.value = list
        }
    }

    // 低电量监控
    val batteryLiveData = BatteryListenerManager.batterySettingLiveData

    // 马赛克模糊处理配置参数
    val mosaicParaLiveData = MosaicParaManager.mosaicParaLiveData

    /**
     * 更新马赛克配对
     */
    fun updateMosaicPara(key: String, value: String = "", delete: Boolean = true) =
        MosaicParaManager.updateMosaicPara(key, value, delete)
}