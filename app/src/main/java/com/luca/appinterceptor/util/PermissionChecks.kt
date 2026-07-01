package com.luca.appinterceptor.util

import android.app.AppOpsManager
import android.content.ComponentName
import android.content.Context
import android.os.PowerManager
import android.os.Process
import android.provider.Settings
import androidx.core.app.NotificationManagerCompat
import com.luca.appinterceptor.service.InterceptorService

object PermissionChecks {

    fun accessibilityEnabled(context: Context): Boolean {
        if (InterceptorService.isRunning) return true
        val cn = ComponentName(context, InterceptorService::class.java)
        val enabled = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: return false
        return enabled.split(':').any {
            it.equals(cn.flattenToString(), ignoreCase = true) ||
                it.equals(cn.flattenToShortString(), ignoreCase = true)
        }
    }

    fun canDrawOverlays(context: Context): Boolean = Settings.canDrawOverlays(context)

    fun ignoresBatteryOptimizations(context: Context): Boolean {
        val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        return pm.isIgnoringBatteryOptimizations(context.packageName)
    }

    fun notificationsAllowed(context: Context): Boolean =
        NotificationManagerCompat.from(context).areNotificationsEnabled()

    /**
     * MIUI-eigene AppOp 10021 = "Popup-Fenster im Hintergrund anzeigen".
     * Best-effort über Reflection; null = auf diesem Gerät nicht ermittelbar
     * (dann muss der Nutzer manuell prüfen).
     */
    fun miuiBackgroundPopupAllowed(context: Context): Boolean? {
        if (!MiuiIntents.isMiui()) return null
        return try {
            val ops = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
            val method = AppOpsManager::class.java.getMethod(
                "checkOpNoThrow",
                Int::class.javaPrimitiveType,
                Int::class.javaPrimitiveType,
                String::class.java
            )
            val result = method.invoke(ops, 10021, Process.myUid(), context.packageName) as Int
            result == AppOpsManager.MODE_ALLOWED
        } catch (e: Exception) {
            null
        }
    }
}
