package org.lynxz.forwardsms.para

import androidx.databinding.Observable
import androidx.databinding.ObservableBoolean
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import org.lynxz.forwardsms.util.SpJsonUtilImpl
import org.lynxz.utils.log.LoggerUtil
import org.lynxz.utils.otherwise
import org.lynxz.utils.yes

/**
 * 消息内容模糊处理参数持久化管理类
 * */
object MosaicParaManager : AbsSpSettingInfoManager<MosaicParaManager.MosaicParaBean>() {

    data class MosaicParaBean(
        // 是否启用
        var enable: ObservableBoolean = ObservableBoolean(false),
        // 处理规则: key-待替换的原始内容  value-替换内容
//        val detailMosaicMap: ObservableMap<String, String> = ObservableArrayMap()
        val detailMosaicMap: MutableMap<String, String> = mutableMapOf()
    )

    private val innerLiveData = MutableLiveData(paraBean)
    val mosaicParaLiveData = innerLiveData as LiveData<MosaicParaBean>

    init {
        paraBean.enable.addOnPropertyChangedCallback(object :
            Observable.OnPropertyChangedCallback() {
            override fun onPropertyChanged(sender: Observable?, propertyId: Int) {
                savePara()
            }
        })

//        paraBean.detailMosaicMap.addOnMapChangedCallback(object :
//            OnMapChangedCallback<ObservableMap<String, String>, String, String>() {
//            override fun onMapChanged(sender: ObservableMap<String, String>?, key: String?) {
//                savePara()
//            }
//        })
    }


    /**
     * 设置替换规则
     * @param srcContent 原始待替换的文字内容
     * @param replaceInfo 替换内容, 若为空,则表示删除已存在的规则
     * */
    fun addMapInfo(srcContent: String, replaceInfo: String?) {
        replaceInfo.isNullOrBlank()
            .yes {
                paraBean.detailMosaicMap.remove(srcContent)
            }.otherwise {
                paraBean.detailMosaicMap[srcContent] = replaceInfo!!
            }
        savePara()
        innerLiveData.postValue(paraBean)
    }

    override fun getParaFromSp(paraKey: String): MosaicParaBean {
        val bean = getSecuritySp().getPreference(
            paraKey,
            MosaicParaBean::class.java,
            MosaicParaBean()
        )!!
        LoggerUtil.w("xxx", "mosaic getParaFromSp $paraKey,bean=${SpJsonUtilImpl.toJson(bean)}")
        return bean
    }
}