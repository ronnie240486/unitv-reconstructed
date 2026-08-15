package com.example.unitv

import android.content.Context
import android.provider.Settings
import java.net.NetworkInterface

object DeviceIdentity {
    fun read12(context: Context): String {
        val interfaceMac = runCatching { findHardwareAddress() }.getOrNull()
        if (!interfaceMac.isNullOrBlank()) return normalize12(interfaceMac)

        val androidId = runCatching {
            Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
        }.getOrNull().orEmpty()
        return normalize12(androidId)
    }

    fun normalize12(raw: String): String {
        val hex = raw.filter { it in "0123456789abcdefABCDEF" }.uppercase()
        return when {
            hex.length >= 12 -> hex.takeLast(12)
            else -> hex.padStart(12, '0')
        }
    }

    private fun findHardwareAddress(): String? {
        val interfaces = NetworkInterface.getNetworkInterfaces() ?: return null
        while (interfaces.hasMoreElements()) {
            val networkInterface = interfaces.nextElement()
            val hardware = networkInterface.hardwareAddress ?: continue
            if (hardware.size != 6) continue
            if (hardware.all { it.toInt() == 0 }) continue
            if (hardware[0].toInt() and 0x02 != 0 && hardware.all { it.toInt() == 0x02 }) continue
            return hardware.joinToString("") { byte -> "%02X".format(byte) }
        }
        return null
    }
}
