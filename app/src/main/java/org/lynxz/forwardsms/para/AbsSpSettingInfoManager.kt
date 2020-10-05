package org.lynxz.forwardsms.para

import android.content.Context
import org.lynxz.forwardsms.SmsApplication
import org.lynxz.forwardsms.util.SpJsonUtilImpl
import org.lynxz.securitysp.SecuritySP

/**
 * 使用sp文件进行存储的抽象父类, 实现sp持久化, 子类所需配置参数均继承自 IValidationDataBean
 * 子类需实现 [getParaFromSp] 来从sp中获取配置信息, 若不存在返回一个默认值, key默认为类名
 * 子类可直接使用 [paraBean] 获取自定义的配置信息
 * 通过 [savePara] 来保存更新后的 paraBean 信息
 * */
abstract class AbsSpSettingInfoManager<T> {
    companion object {
        private const val TAG = "AbsSpSettingInfoManager"
        private const val KEY_SP_NAME = "sp_settings_para"

        val settingSp = SecuritySP(SmsApplication.app, KEY_SP_NAME, Context.MODE_PRIVATE).apply {
            spJsonUtil = SpJsonUtilImpl
        }
    }

    protected val paraBean by lazy { getParaFromSp(getParaKey()) }

    /**
     * 持久化的参数数据key,默认使用类名信息, 可重写 [getParaKey] 来设置
     * */
    private var paraBeanKey: String = ""

    /**
     * 生成sp中的存储key
     * */
    protected fun getParaKey(): String {
        if (paraBeanKey.isBlank()) {
            paraBeanKey = "${this.javaClass.canonicalName}"
        }
        return paraBeanKey
    }


    /**
     * 获取sharePreference对象,默认使用固定的 settingSp,子类可重写切换其他sp对象
     * */
    protected fun getSecuritySp() = settingSp

    /**
     * 子类重写实现从sp中提取数据
     * 由于T是泛型, 子类只需要提供具体类型即可, 格式为: getSecuritySp().getPreference(getParaKey(), YourValidationPara::class.java, YourValidationPara() )
     * */
    abstract fun getParaFromSp(paraKey: String): T

    /**
     * 保存配置数据到sp中
     * */
    fun savePara() {
        getSecuritySp().putPreference(getParaKey(), paraBean)
    }
}


/**
 * 更新现有配置
 * */
typealias RecookPara<T> = (para: T) -> Unit