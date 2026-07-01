package com.luca.appinterceptor.boot

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.luca.appinterceptor.util.Notifications
import com.luca.appinterceptor.util.PermissionChecks

/**
 * Nach dem Boot startet Android aktivierte AccessibilityServices selbst neu –
 * einen Service können (und müssen) wir hier nicht starten. Was auf MIUI aber
 * passiert: Der Dienst wird nach Updates/Reboots manchmal stillschweigend
 * deaktiviert. Deshalb prüfen wir hier und warnen den Nutzer per Notification.
 *
 * Hinweis: Damit dieser Receiver auf MIUI überhaupt feuert, braucht die App
 * die Autostart-Berechtigung (Teil des Onboardings).
 */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        if (action != Intent.ACTION_BOOT_COMPLETED &&
            action != "android.intent.action.QUICKBOOT_POWERON"
        ) return

        if (!PermissionChecks.accessibilityEnabled(context)) {
            Notifications.notifyServiceDown(context)
        }
    }
}
