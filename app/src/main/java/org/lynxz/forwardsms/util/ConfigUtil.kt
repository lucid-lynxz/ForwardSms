package org.lynxz.forwardsms.util

import android.content.Context
import android.content.SharedPreferences
import org.lynxz.baseimlib.actions.IPropertyAction
import org.lynxz.baseimlib.convert2Str

/**
 * 持久化操作功能
 * */
class ConfigUtil(context: Context, fileName: String) : IPropertyAction {
    var sp: SharedPreferences = context.getSharedPreferences(fileName, Context.MODE_PRIVATE)

    override fun save(key: String, obj: Any): Boolean {
        with(sp.edit()) {
            when (obj) {
                is Int -> putInt(key, obj)
                is Long -> putLong(key, obj)
                is String -> putString(key, obj)
                is Float -> putFloat(key, obj)
                is Boolean -> putBoolean(key, obj)
                else -> putString(key, convert2Str(obj))
            }
            return commit()
        }
    }

    override fun remove(key: String): Boolean {
        return sp.edit().remove(key).commit()
    }

    override fun <T> get(key: String, defaultValue: T?): Any? {
        return when (defaultValue) {
            is Int -> sp.getInt(key, defaultValue)
            is Long -> sp.getLong(key, defaultValue)
            is Float -> sp.getFloat(key, defaultValue)
            is Boolean -> sp.getBoolean(key, defaultValue)
            is String -> sp.getString(key, defaultValue)
            else -> sp.getString(key, convert2Str(defaultValue))
        }
    }
}