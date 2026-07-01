package com.luca.appinterceptor.overlay

import android.accessibilityservice.AccessibilityService
import android.graphics.PixelFormat
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.WindowManager
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy

/**
 * Verwaltet das Vollbild-Overlay über der Ziel-App.
 *
 * Primär TYPE_ACCESSIBILITY_OVERLAY: gehört zum AccessibilityService und
 * funktioniert auf MIUI auch dann, wenn "Popup im Hintergrund" nicht erteilt
 * wurde. Fallback: TYPE_APPLICATION_OVERLAY (braucht SYSTEM_ALERT_WINDOW +
 * MIUI-Popup-Permission).
 */
class OverlayController(private val service: AccessibilityService) {

    companion object {
        private const val TAG = "OverlayController"
    }

    private val windowManager =
        service.getSystemService(AccessibilityService.WINDOW_SERVICE) as WindowManager
    private val mainHandler = Handler(Looper.getMainLooper())

    private var composeView: ComposeView? = null
    private var lifecycleOwner: OverlayLifecycleOwner? = null

    val isShowing: Boolean get() = composeView != null

    fun show(
        appLabel: String,
        countdownSeconds: Int,
        onOpen: () -> Unit,
        onGoHome: () -> Unit,
    ) {
        dismiss()

        val owner = OverlayLifecycleOwner().also { it.onCreate() }
        val view = ComposeView(service).apply {
            owner.attachTo(this)
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                MaterialTheme(colorScheme = darkColorScheme()) {
                    OverlayScreen(
                        appLabel = appLabel,
                        countdownSeconds = countdownSeconds,
                        onOpen = onOpen,
                        onGoHome = onGoHome,
                    )
                }
            }
        }

        val added = addView(view, WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY) ||
            addView(view, WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY)
        if (added) {
            composeView = view
            lifecycleOwner = owner
            owner.onResume()
        } else {
            Log.e(TAG, "Overlay konnte nicht angezeigt werden (beide Fenstertypen fehlgeschlagen)")
            owner.onDestroy()
        }
    }

    private fun addView(view: ComposeView, windowType: Int): Boolean {
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            windowType,
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT,
        ).apply {
            if (Build.VERSION.SDK_INT >= 28) {
                layoutInDisplayCutoutMode =
                    WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
            }
        }
        return try {
            windowManager.addView(view, params)
            true
        } catch (e: Exception) {
            Log.w(TAG, "addView mit Typ $windowType fehlgeschlagen: ${e.message}")
            false
        }
    }

    fun dismiss() {
        val view = composeView ?: return
        val owner = lifecycleOwner
        composeView = null
        lifecycleOwner = null
        // Nicht mitten im Click-Event des Overlays selbst entfernen
        mainHandler.post {
            try {
                windowManager.removeView(view)
            } catch (e: Exception) {
                Log.w(TAG, "removeView fehlgeschlagen: ${e.message}")
            }
            owner?.onDestroy()
        }
    }
}
