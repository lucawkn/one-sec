package com.luca.appinterceptor.service

import android.accessibilityservice.AccessibilityService
import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.inputmethod.InputMethodManager
import com.luca.appinterceptor.data.Graph
import com.luca.appinterceptor.data.InterceptEvent
import com.luca.appinterceptor.overlay.OverlayController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * Erkennt Vordergrund-Wechsel über TYPE_WINDOW_STATE_CHANGED und legt bei
 * überwachten Apps das Atem-Overlay darüber.
 *
 * Fehlalarm-Filter:
 *  - eigenes Paket wird komplett ignoriert (sonst Endlosschleife durch das Overlay selbst)
 *  - nur Events, deren className eine echte Activity ist (filtert Dialoge,
 *    Toasts, IME-Fenster und System-Popups anderer Apps heraus)
 *  - Launcher, SystemUI und Tastaturen stehen auf einer Ignore-Liste
 *  - pro "Sitzung" nur eine Interception: solange dieselbe App im Vordergrund
 *    bleibt (Activity-Wechsel innerhalb der App), wird nicht erneut ausgelöst
 */
class InterceptorService : AccessibilityService() {

    companion object {
        private const val TAG = "InterceptorService"
        const val COUNTDOWN_SECONDS = 10

        @Volatile
        var instance: InterceptorService? = null
            private set

        /** Für den Health-Check: läuft der Service gerade wirklich? */
        val isRunning: Boolean get() = instance != null
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    @Volatile
    private var watchedApps: Set<String> = emptySet()

    @Volatile
    private var cooldownMinutes: Int = 5

    /** packageName -> Zeitstempel, bis zu dem die App nach "Ja" nicht erneut unterbrochen wird. */
    private val allowedUntil = HashMap<String, Long>()

    private var currentForeground: String? = null
    private val ignoredPackages = HashSet<String>()
    private var overlay: OverlayController? = null

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        overlay = OverlayController(this)
        buildIgnoredPackages()
        scope.launch { Graph.settings.watchedApps.collect { watchedApps = it } }
        scope.launch { Graph.settings.cooldownMinutes.collect { cooldownMinutes = it } }
        Log.i(TAG, "AccessibilityService verbunden")
    }

    private fun buildIgnoredPackages() {
        ignoredPackages.clear()
        ignoredPackages += packageName
        ignoredPackages += listOf(
            "android",
            "com.android.systemui",
            "com.android.settings",
            "com.miui.home",
            "com.mi.android.globallauncher",
            "com.miui.securitycenter",
            "com.miui.securityadd",
        )
        // Alle installierten Launcher dynamisch ermitteln
        val homeIntent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
        val launchers = if (Build.VERSION.SDK_INT >= 33) {
            packageManager.queryIntentActivities(
                homeIntent,
                PackageManager.ResolveInfoFlags.of(PackageManager.MATCH_ALL.toLong())
            )
        } else {
            @Suppress("DEPRECATION")
            packageManager.queryIntentActivities(homeIntent, PackageManager.MATCH_ALL)
        }
        launchers.forEach { ignoredPackages += it.activityInfo.packageName }
        // Tastaturen
        val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
        imm.inputMethodList.forEach { ignoredPackages += it.packageName }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        if (event.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return
        val pkg = event.packageName?.toString() ?: return
        val cls = event.className?.toString() ?: return

        if (pkg == packageName) return
        if (!isActivityWindow(pkg, cls)) return
        if (pkg == currentForeground) return
        currentForeground = pkg

        if (pkg in ignoredPackages || pkg !in watchedApps) {
            // Nutzer hat die Ziel-App verlassen -> laufendes Overlay aufräumen
            overlay?.dismiss()
            return
        }
        if (System.currentTimeMillis() < (allowedUntil[pkg] ?: 0L)) return

        intercept(pkg)
    }

    /**
     * TYPE_WINDOW_STATE_CHANGED feuert auch für Dialoge, Menüs und IME-Fenster.
     * Nur wenn className als Activity des Pakets auflösbar ist, werten wir das
     * als echten App-Wechsel.
     */
    private fun isActivityWindow(pkg: String, cls: String): Boolean {
        if (!cls.contains('.')) return false
        return try {
            packageManager.getActivityInfo(ComponentName(pkg, cls), 0)
            true
        } catch (e: PackageManager.NameNotFoundException) {
            false
        }
    }

    private fun intercept(pkg: String) {
        val label = appLabel(pkg)
        Log.i(TAG, "Unterbreche $pkg ($label)")
        overlay?.show(
            appLabel = label,
            countdownSeconds = COUNTDOWN_SECONDS,
            onOpen = {
                allowedUntil[pkg] = System.currentTimeMillis() + cooldownMinutes * 60_000L
                recordEvent(pkg, opened = true)
                overlay?.dismiss()
            },
            onGoHome = {
                recordEvent(pkg, opened = false)
                overlay?.dismiss()
                performGlobalAction(GLOBAL_ACTION_HOME)
            },
        )
    }

    private fun appLabel(pkg: String): String = try {
        val info = packageManager.getApplicationInfo(pkg, 0)
        packageManager.getApplicationLabel(info).toString()
    } catch (e: Exception) {
        pkg
    }

    private fun recordEvent(pkg: String, opened: Boolean) {
        scope.launch(Dispatchers.IO) {
            Graph.db.eventDao().insert(
                InterceptEvent(packageName = pkg, timestamp = System.currentTimeMillis(), opened = opened)
            )
        }
    }

    override fun onInterrupt() = Unit

    override fun onUnbind(intent: Intent?): Boolean {
        instance = null
        overlay?.dismiss()
        return super.onUnbind(intent)
    }

    override fun onDestroy() {
        instance = null
        overlay?.dismiss()
        scope.cancel()
        super.onDestroy()
    }
}
