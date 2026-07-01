package com.onesec.interceptor.overlay

import android.content.Context
import android.graphics.PixelFormat
import android.os.Build
import android.view.KeyEvent
import android.view.WindowManager
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.onesec.interceptor.ui.theme.OneSecTheme

/**
 * The overlay window is added directly via WindowManager, so there is no
 * Activity/OnBackPressedDispatcher to hook into. Instead the hardware back
 * button is intercepted at the View level and always treated like tapping
 * "Nein" - a safe default escape hatch during either overlay phase.
 */
private class BackAwareComposeView(
    context: Context,
    private val onBackPressed: () -> Unit
) : ComposeView(context) {
    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (event.keyCode == KeyEvent.KEYCODE_BACK && event.action == KeyEvent.ACTION_UP) {
            onBackPressed()
            return true
        }
        return super.dispatchKeyEvent(event)
    }
}

/**
 * Owns the single full-screen SYSTEM_ALERT_WINDOW overlay used to pause the
 * user before a monitored app opens. Only one overlay can be shown at a
 * time; a second call to [show] while one is active is ignored.
 */
class OverlayManager(private val appContext: Context) {

    private val windowManager =
        appContext.getSystemService(Context.WINDOW_SERVICE) as WindowManager

    private var composeView: ComposeView? = null
    private var lifecycleOwner: OverlayLifecycleOwner? = null

    val isShowing: Boolean get() = composeView != null

    fun show(
        appLabel: String,
        breathingSeconds: Int,
        onAllow: () -> Unit,
        onDeny: () -> Unit
    ) {
        if (isShowing) return

        val owner = OverlayLifecycleOwner().also { it.onCreate() }
        lifecycleOwner = owner

        val view = BackAwareComposeView(appContext, onBackPressed = {
            hide()
            onDeny()
        }).apply {
            setViewTreeLifecycleOwner(owner)
            setViewTreeSavedStateRegistryOwner(owner)
            setViewTreeViewModelStoreOwner(owner)
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindowOrReleasedFromPool)
            setContent {
                OneSecTheme {
                    InterceptorOverlayContent(
                        appLabel = appLabel,
                        breathingSeconds = breathingSeconds,
                        onAllow = {
                            hide()
                            onAllow()
                        },
                        onDeny = {
                            hide()
                            onDeny()
                        }
                    )
                }
            }
        }

        val layoutFlags =
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            layoutFlags,
            PixelFormat.TRANSLUCENT
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            params.layoutInDisplayCutoutMode =
                WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
        }

        windowManager.addView(view, params)
        composeView = view
    }

    fun hide() {
        val view = composeView ?: return
        composeView = null
        runCatching { windowManager.removeView(view) }
        lifecycleOwner?.onDestroy()
        lifecycleOwner = null
    }
}
