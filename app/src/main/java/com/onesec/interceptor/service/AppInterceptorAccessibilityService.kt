package com.onesec.interceptor.service

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.content.pm.PackageManager
import android.os.Build
import android.os.SystemClock
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import com.onesec.interceptor.data.local.entity.InterceptOutcome
import com.onesec.interceptor.di.ServiceLocator
import com.onesec.interceptor.overlay.OverlayManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Watches TYPE_WINDOW_STATE_CHANGED events to notice when a different app
 * comes to the foreground, and shows the pause overlay for apps the user
 * has marked as "monitored" - unless that app is still within its grace
 * period from a previous "Ja, öffnen".
 *
 * Foreground-change detection: Android delivers a WINDOW_STATE_CHANGED
 * event whenever the top-level window's package changes (i.e. a new app,
 * or a new activity of the same app, comes to the front). We only care
 * about *package* transitions, so:
 *  - events are de-duplicated against the last seen package to ignore the
 *    many events fired for sub-windows/activities within the same app,
 *  - a short debounce absorbs bursts of events during a single transition
 *    (e.g. a splash screen followed immediately by the main activity),
 *  - [ForegroundAppFilter] drops packages that can never be meaningfully
 *    "opened" by the user: our own app/overlay, the current launcher, and
 *    known system packages (systemui, keyboards, ...).
 */
class AppInterceptorAccessibilityService : AccessibilityService() {

    private lateinit var filter: ForegroundAppFilter
    private lateinit var overlayManager: OverlayManager
    private var serviceScope: CoroutineScope? = null

    private var lastPackageName: String? = null
    private var lastEventUptimeMillis: Long = 0L

    override fun onServiceConnected() {
        super.onServiceConnected()
        filter = ForegroundAppFilter(applicationContext)
        overlayManager = OverlayManager(applicationContext)
        serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

        val info = AccessibilityServiceInfo().apply {
            eventTypes = AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
            feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
            notificationTimeout = NOTIFICATION_TIMEOUT_MS
        }
        serviceInfo = info
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        if (event.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return

        val packageName = event.packageName?.toString() ?: return
        if (filter.shouldIgnore(packageName)) return

        val now = SystemClock.elapsedRealtime()
        if (packageName == lastPackageName && now - lastEventUptimeMillis < DEBOUNCE_MS) return
        val isNewForegroundApp = packageName != lastPackageName
        lastPackageName = packageName
        lastEventUptimeMillis = now

        if (!isNewForegroundApp) return
        if (overlayManager.isShowing) return

        handleForegroundApp(packageName)
    }

    private fun handleForegroundApp(packageName: String) {
        val scope = serviceScope ?: return
        scope.launch {
            val settings = ServiceLocator.settingsDataStore.settingsFlow.first()
            if (!settings.interceptorEnabled) return@launch

            val monitoredRepo = ServiceLocator.monitoredAppRepository
            if (!monitoredRepo.isMonitored(packageName)) return@launch
            if (monitoredRepo.isSnoozed(packageName, System.currentTimeMillis())) return@launch

            val appLabel = resolveAppLabel(packageName)

            withContext(Dispatchers.Main) {
                // Re-check: the foreground app may have already changed again
                // by the time this coroutine hop completes.
                if (packageName != lastPackageName) return@withContext

                overlayManager.show(
                    appLabel = appLabel,
                    breathingSeconds = settings.breathingSeconds,
                    onAllow = {
                        serviceScope?.launch {
                            ServiceLocator.statsRepository.recordEvent(
                                packageName, appLabel, InterceptOutcome.ALLOWED
                            )
                            val untilMillis = System.currentTimeMillis() +
                                settings.graceMinutes * 60_000L
                            monitoredRepo.snooze(packageName, untilMillis)
                        }
                    },
                    onDeny = {
                        serviceScope?.launch {
                            ServiceLocator.statsRepository.recordEvent(
                                packageName, appLabel, InterceptOutcome.CANCELLED
                            )
                        }
                        performGlobalAction(GLOBAL_ACTION_HOME)
                    }
                )
            }
        }
    }

    @Suppress("DEPRECATION")
    private fun resolveAppLabel(packageName: String): String {
        return runCatching {
            val pm = applicationContext.packageManager
            val appInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                pm.getApplicationInfo(packageName, PackageManager.ApplicationInfoFlags.of(0L))
            } else {
                pm.getApplicationInfo(packageName, 0)
            }
            pm.getApplicationLabel(appInfo).toString()
        }.getOrDefault(packageName)
    }

    override fun onInterrupt() {
        Log.w(TAG, "Accessibility service interrupted")
    }

    override fun onDestroy() {
        super.onDestroy()
        overlayManager.hide()
        serviceScope?.cancel()
        serviceScope = null
    }

    private companion object {
        const val TAG = "AppInterceptorService"
        const val DEBOUNCE_MS = 300L
        const val NOTIFICATION_TIMEOUT_MS = 100L
    }
}
