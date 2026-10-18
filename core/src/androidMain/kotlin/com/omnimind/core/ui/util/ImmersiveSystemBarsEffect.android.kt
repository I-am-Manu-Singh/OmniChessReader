package com.creativeblisslabs.omnimind.core.ui.util

import android.app.Activity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat

@Composable
actual fun ImmersiveSystemBarsEffect(enable: Boolean) {
    if (!enable) return
    val view = LocalView.current
    DisposableEffect(view) {
        var parentCtx: android.content.Context? = view.context
        var dialogWindow: android.view.Window? = null
        while (parentCtx != null) {
            if (parentCtx is android.app.Dialog) {
                dialogWindow = parentCtx.window
                break
            }
            if (parentCtx is android.content.ContextWrapper) {
                parentCtx = parentCtx.baseContext
            } else {
                break
            }
        }
        val activityWindow = (view.context as? Activity)?.window
        val windowsToHide = listOfNotNull(dialogWindow, activityWindow).distinct()
        windowsToHide.forEach { window ->
            val controller = WindowInsetsControllerCompat(window, window.decorView)
            controller.hide(WindowInsetsCompat.Type.statusBars())
            controller.hide(WindowInsetsCompat.Type.navigationBars())
            controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            androidx.core.view.WindowCompat.setDecorFitsSystemWindows(window, false)
            window.statusBarColor = android.graphics.Color.TRANSPARENT
            window.navigationBarColor = android.graphics.Color.TRANSPARENT
        }
        onDispose {
            windowsToHide.forEach { window ->
                val controller = WindowInsetsControllerCompat(window, window.decorView)
                controller.show(WindowInsetsCompat.Type.systemBars())
            }
        }
    }
}
