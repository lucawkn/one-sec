package com.onesec.interceptor.util

import android.content.ComponentName
import android.content.Context
import android.provider.Settings
import android.text.TextUtils

object PermissionUtils {

    fun hasOverlayPermission(context: Context): Boolean = Settings.canDrawOverlays(context)

    fun isAccessibilityServiceEnabled(context: Context, serviceClass: Class<*>): Boolean {
        val expected = ComponentName(context, serviceClass)
        val enabledServices = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: return false

        val splitter = TextUtils.SimpleStringSplitter(':')
        splitter.setString(enabledServices)
        while (splitter.hasNext()) {
            val enabled = ComponentName.unflattenFromString(splitter.next())
            if (enabled != null && enabled == expected) return true
        }
        return false
    }
}
