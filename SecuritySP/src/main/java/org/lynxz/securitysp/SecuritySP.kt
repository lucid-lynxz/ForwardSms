package org.lynxz.securitysp

import android.annotation.TargetApi
import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import android.preference.PreferenceManager
import android.util.Base64
import org.lynxz.securitysp.util.AESUtil
import org.lynxz.securitysp.util.DeviceUtil
import org.lynxz.securitysp.util.DigestUtil
import kotlin.random.Random

inline fun <reified T> Set<*>.isSetOfType(): Boolean = all { it is T }

/**
 * Created by lynxz on 2020/3/7
 * E-mail: lynxz8866@gmail.com
 *
 * 可加密的文件
 * @param spName 要读写的sp文件名
 * @param spMode sp文件打开模式, 如 [Context.MODE_PRIVATE] [Context.MODE_WORLD_READABLE] [Context.MODE_WORLD_WRITEABLE]
 * @param autoGenerateKey 是否自动生成密钥, 若为true,则key会自动存储到当前sp文件中, 若为false,则由用户自行保存
 * @param secretKey aes加解密密钥key [autoGenerateKey]
 *   使用 Base64.encodeToString(byte[], Base64.NO_WRAP) 生成的字符串
 *   若为空则表示不加解密,明文存储, 若 [autoGenerateKey] 为false,则本参数不能为空
 * @param spEncryptUtil sp文件key-value加解密规则,由用户自定义,若为null表示不加解密, 明文存储
 *
 * 使用方法:
 * 1. 生成/打开 sp 文件: val securitySP = SecuritySP(this, "security_sp", Context.MODE_PRIVATE)
 * 2. 存储数据到sp: [putPreference]
 * 3. 从sp中提取数据: [getPreference]
 * 4. 获取当前加密密钥内容: [getSecurityKey]
 * 5. 生成一个aeskey: [generateRandomKey]
 * 5. 从sp中删除自动生成的解密密钥: [removeSecurityKey], 注意: 移除后由用户保存, 下次使用sp需要传入securityKey, 否则解密失败
 *
 * 示例:
 * val securitySP = SecuritySP(this, "security_sp", Context.MODE_PRIVATE) // 获取sp实例
 *
 * securitySP.putPreference("StrKey", "hhhh")  // 存储字符串数据
 *   .putPreference("BooleanKey", true) // 存储 boolean 数据
 *   .putPreference("LongKey", 0L) // 存储 long 数据
 *   .putPreference("IntKey", 0) // 存储 int 数据
 *   .putPreference("FloatKey", 0F) // 存储 float 数据
 *
 * val strValue = securitySP.getPreference("StrKey", "defaultStr") // 获取字符串值
 * val booleanValue = securitySP.getPreference("BooleanKey", false) // 获取 boolean 值
 * val longValue = securitySP.getPreference("LongKey", 0L) // 获取 long 值
 * val intValue = securitySP.getPreference("IntKey", 0) // 获取 int 值
 * val floatValue = securitySP.getPreference("FloatKey", 1.0f) // 获取 float 值
 * */
class SecuritySP constructor(
    private val context: Context,
    private val spName: String?,
    spMode: Int,
    private val autoGenerateKey: Boolean,
    private var secretKey: String?,
    private val spEncryptUtil: ISpEncryptUtil?
) : SharedPreferences {
    constructor(context: Context, spName: String?, spMode: Int)
            : this(context, spName, spMode, true, null, AESUtil())

    companion object {
        const val TAG = "SecuritySP"
        private val SecurityKeyName =
            DigestUtil.encryptToString("SecuritySP_key", DigestUtil.SHA256) ?: ""

        // 生成随机密钥,建议由
        fun generateRandomKey(context: Context): String {
            val deviceSerialNumber = DeviceUtil.getDeviceSerialNumber(context) ?: ""
            val seed =
                deviceSerialNumber + Random(System.currentTimeMillis()).nextLong() + "$#sjl93&!7jsfj~0|"
//            val key = DigestUtil.encryptToString(seed, DigestUtil.SHA256) ?: seed
            return Base64.encodeToString(AESUtil.getRawKey(seed.toByteArray()), Base64.NO_WRAP)
        }
    }

    private var mSharedPreferences: SharedPreferences

    init {
        val innerSp: SharedPreferences =
            if (spName.isNullOrEmpty()) PreferenceManager.getDefaultSharedPreferences(context)
            else context.getSharedPreferences(spName, spMode)
        mSharedPreferences = innerSp

        if (autoGenerateKey) {
            // 自动生成密钥, 优先从sp文件中获取,若为首次创建sp,则获取失败,自动生成并存入sp中
            secretKey = innerSp.getString(SecurityKeyName, "")
            if (secretKey.isNullOrBlank()) {
                secretKey = generateRandomKey(context)
                innerSp.edit().putString(SecurityKeyName, secretKey).apply()
            }
        } else if (secretKey.isNullOrBlank()) {
            // 非自动生成密钥,则密钥必传,否则直接抛出异常
            throw IllegalArgumentException("autoGenerateKey=true but secretKey is empty")
        }
    }

    /**
     * 从sp中提取数据
     * @param  name sp中存储的key明文,内部会自动调用加密算法,注意: 暂不支持非对称加密
     * @param default 若sp未存储相关key数据,则返回该默认值
     * */
    @Suppress("UNCHECKED_CAST", "IMPLICIT_CAST_TO_ANY")
    fun <U> getPreference(name: String, default: U?): U? {
        if (default is MutableSet<*> && default.isSetOfType<String>()) {
            val result = getStringSet(name, (default as MutableSet<String>?))
            return result as? U
        }

        val oriValue = getString(name, default.toString()) ?: ""
        if (oriValue.isBlank()) return default

        val res = when (default) {
            is Long -> oriValue.toLong()
            is String -> oriValue
            is Int -> oriValue.toInt()
            is Boolean -> oriValue.toBoolean()
            is Float -> oriValue.toFloat()
            else -> throw IllegalArgumentException("This type can not be saved")
        }
        return res as? U
    }

    /**
     * 将数据写入到sp文件中
     * */
    @Suppress("UNCHECKED_CAST")
    fun <U> putPreference(name: String, value: U): SecuritySP {
        with(edit()) {
            if (value is MutableSet<*> && value.isSetOfType<String>()) {
                putStringSet(name, value as Set<String>).apply()
            } else {
                when (value) {
                    is Long -> putLong(name, value)
                    is String -> putString(name, value)
                    is Int -> putInt(name, value)
                    is Boolean -> putBoolean(name, value)
                    is Float -> putFloat(name, value)
                    else -> throw IllegalArgumentException("This type can not be saved")
                }.apply()
            }
        }
        return this
    }

    // 获取当前加密密钥
    fun getSecurityKey() = secretKey

    // 删除存储在sp中的密钥信息(用户自行保存, sp无需再保存)
    fun removeSecurityKey() = mSharedPreferences.edit().remove(SecurityKeyName).apply()


    /**
     * 由于存储时会将各类型数据加密转换为字符串,因此获取后也都是字符串类型
     * */
    override fun getAll(): MutableMap<String, String> {
        val encryptMap = mSharedPreferences.all
        val decryptMap: MutableMap<String, String> = mutableMapOf()
        for ((key, value) in encryptMap) {
            decryptMap[doDecrypt(key)] = doDecrypt(value)
        }
        return decryptMap
    }

    override fun contains(key: String?) = mSharedPreferences.contains(doEncrypt(key))
    override fun edit() = SecurityEditor()
    override fun getBoolean(key: String, defValue: Boolean) = getPreference(key, defValue)!!
    override fun getInt(key: String, defValue: Int) = getPreference(key, defValue)!!
    override fun getLong(key: String, defValue: Long) = getPreference(key, defValue)!!
    override fun getFloat(key: String, defValue: Float) = getPreference(key, defValue)!!

    /**
     * 提取sp中的字符串属性类型,并自动解密还原成原始值
     * @param key sp中存储的key原始明文值
     * @param defValue 获取不到相关属性时,返回默认值
     * @return 解密后的原始属性值
     * */
    override fun getString(key: String, defValue: String?): String? {
        val encryptText = mSharedPreferences.getString(doEncrypt(key), null) ?: return defValue
        return doDecrypt(encryptText)
    }

    /**
     * 提取sp中的set属性类型,并自动解所有密元素,还原成原始值
     * @param key sp中存储的key原始明文值
     * @param defValue 获取不到相关属性时,返回默认值
     * @return 解密后的原始属性值
     * */
    override fun getStringSet(key: String, defValue: MutableSet<String>?): MutableSet<String> {
        val encryptSet = mSharedPreferences.getStringSet(doEncrypt(key), defValue)
        val decryptSet = mutableSetOf<String>()
        encryptSet?.forEach { decryptSet.add(doDecrypt(it)) }
        return decryptSet
    }

    override fun registerOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener?) {
        mSharedPreferences.registerOnSharedPreferenceChangeListener(listener)
    }

    override fun unregisterOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener?) {
        mSharedPreferences.unregisterOnSharedPreferenceChangeListener(listener)
    }

    /**
     * 隔离一层加密逻辑
     * 若加密算法结果返回null,则存储为 ""
     * */
    private fun doEncrypt(srcInput: Any?): String {
        val plainText = srcInput?.toString() ?: ""
        return spEncryptUtil?.encrypt(secretKey, plainText) ?: plainText
    }

    /**
     * 隔离一层解密逻辑
     * 若解密失败,则返回为 ""
     * */
    private fun doDecrypt(cipherInfo: Any?): String {
        val cipherText = cipherInfo?.toString() ?: ""
        return spEncryptUtil?.decrypt(secretKey, cipherText) ?: cipherText
    }

    /**
     * 加密editor
     * 写入数据到sp时,对key-value进行加密
     * */
    inner class SecurityEditor : SharedPreferences.Editor {
        private val mEditor = mSharedPreferences.edit()

        /**
         * 非 [putStringSet] 方法最终都走方法进行存储,基本类型加密后都变成string值
         * */
        private fun putStringImpl(key: String, value: Any?): SharedPreferences.Editor {
            mEditor.putString(doEncrypt(key), doEncrypt(value))
            return this
        }

        override fun putStringSet(key: String, values: Set<String>): SharedPreferences.Editor {
            val encryptSet: MutableSet<String> = HashSet()
            for (value in values) {
                encryptSet.add(doEncrypt(value))
            }
            mEditor.putStringSet(doEncrypt(key), encryptSet)
            return this
        }

        override fun putString(key: String, value: String) = putStringImpl(key, value)

        override fun putInt(key: String, value: Int) = putStringImpl(key, value)

        override fun putLong(key: String, value: Long) = putStringImpl(key, value)

        override fun putFloat(key: String, value: Float) = putStringImpl(key, value)

        override fun putBoolean(key: String, value: Boolean) = putStringImpl(key, value)

        override fun remove(key: String): SharedPreferences.Editor {
            mEditor.remove(doEncrypt(key))
            return this
        }

        override fun clear(): SharedPreferences.Editor {
            mEditor.clear()
            return this
        }

        override fun commit() = mEditor.commit()

        /**
         * Unlike commit(), which writes its preferences out to persistent storage synchronously,
         * apply() commits its changes to the in-memory SharedPreferences immediately but starts
         * an asynchronous commit to disk and you won't be notified of any failures.
         */
        @TargetApi(Build.VERSION_CODES.GINGERBREAD)
        override fun apply() = mEditor.apply()
    }
}