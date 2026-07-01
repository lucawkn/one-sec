package com.luca.appinterceptor.util

import android.annotation.SuppressLint
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings

/**
 * Deep-Links in die Xiaomi-Systemeinstellungen. Die Komponenten-Namen variieren
 * je nach MIUI/HyperOS-Version, deshalb werden pro Ziel mehrere Kandidaten
 * durchprobiert; jede open*-Funktion liefert false, wenn keiner funktioniert
 * hat (→ UI zeigt dann die manuelle Anleitung).
 */
object MiuiIntents {

    fun isMiui(): Boolean = !systemProperty("ro.miui.ui.version.name").isNullOrBlank()

    @SuppressLint("PrivateApi")
    private fun systemProperty(key: String): String? = try {
        val clazz = Class.forName("android.os.SystemProperties")
        clazz.getMethod("get", String::class.java).invoke(null, key) as? String
    } catch (e: Exception) {
        null
    }

    fun openAccessibilitySettings(context: Context): Boolean =
        start(context, Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))

    fun openOverlaySettings(context: Context): Boolean =
        start(
            context,
            Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:${context.packageName}")
            )
        )

    /**
     * MIUI "Weitere Berechtigungen"-Editor. Hier sitzt u. a.
     * "Popup-Fenster anzeigen, während die App im Hintergrund ausgeführt wird".
     */
    fun openMiuiPermissionEditor(context: Context): Boolean {
        val explicit = Intent("miui.intent.action.APP_PERM_EDITOR").apply {
            setClassName(
                "com.miui.securitycenter",
                "com.miui.permcenter.permissions.PermissionsEditorActivity"
            )
            putExtra("extra_pkgname", context.packageName)
        }
        val implicit = Intent("miui.intent.action.APP_PERM_EDITOR").apply {
            putExtra("extra_pkgname", context.packageName)
        }
        return start(context, explicit) || start(context, implicit) || openAppInfo(context)
    }

    /** MIUI-Autostart-Verwaltung in der Security-App. */
    fun openAutostart(context: Context): Boolean {
        val candidates = listOf(
            Intent().setComponent(
                ComponentName(
                    "com.miui.securitycenter",
                    "com.miui.permcenter.autostart.AutoStartManagementActivity"
                )
            ),
            Intent("miui.intent.action.OP_AUTO_START").addCategory(Intent.CATEGORY_DEFAULT),
        )
        return candidates.any { start(context, it) } || openAppInfo(context)
    }

    /** MIUI-Energiespar-Einstellung der App ("Keine Einschränkungen"), sonst Standard-Android. */
    @SuppressLint("BatteryLife")
    fun openBatterySettings(context: Context): Boolean {
        val label = try {
            context.packageManager.getApplicationLabel(context.applicationInfo).toString()
        } catch (e: Exception) {
            context.packageName
        }
        val miuiPowerKeeper = Intent().apply {
            component = ComponentName(
                "com.miui.powerkeeper",
                "com.miui.powerkeeper.ui.HiddenAppsConfigActivity"
            )
            putExtra("package_name", context.packageName)
            putExtra("package_label", label)
        }
        val requestIgnore = Intent(
            Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
            Uri.parse("package:${context.packageName}")
        )
        return start(context, miuiPowerKeeper) ||
            start(context, requestIgnore) ||
            start(context, Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
    }

    fun openAppInfo(context: Context): Boolean =
        start(
            context,
            Intent(
                Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                Uri.parse("package:${context.packageName}")
            )
        )

    private fun start(context: Context, intent: Intent): Boolean = try {
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
        true
    } catch (e: Exception) {
        false
    }
}
