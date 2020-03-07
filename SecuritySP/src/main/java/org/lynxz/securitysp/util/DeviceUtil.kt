package org.lynxz.securitysp.util

import android.annotation.SuppressLint
import android.content.Context
import android.os.Build
import android.provider.Settings
import android.text.TextUtils

/**
 * Created by lynxz on 2020/03/07
 * */
object DeviceUtil {
    /**
     * Gets the hardware serial number of this device.
     *
     * @return serial number or Settings.Secure.ANDROID_ID if not available.
     */
    @SuppressLint("HardwareIds")
    fun getDeviceSerialNumber(context: Context): String? {
        return try {
            val deviceSerial =
                Build::class.java.getField("SERIAL")[null] as String
            if (TextUtils.isEmpty(deviceSerial)) {
                Settings.Secure.getString(
                    context.contentResolver,
                    Settings.Secure.ANDROID_ID
                )
            } else {
                deviceSerial
            }
        } catch (ignored: Exception) { // Fall back  to Android_ID
            Settings.Secure.getString(
                context.contentResolver,
                Settings.Secure.ANDROID_ID
            )
        }
    }
}