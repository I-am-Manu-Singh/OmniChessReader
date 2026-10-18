package com.creativeblisslabs.omnimind.core.ui.util

import android.app.Activity
import android.content.pm.ActivityInfo
import android.view.WindowManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView

@Composable
actual fun SetScreenBrightnessEffect(brightness: Float, isAuto: Boolean) {
    val view = LocalView.current
    DisposableEffect(brightness, isAuto) {
        val window = (view.context as? Activity)?.window
        if (window != null) {
            val lp = window.attributes
            if (isAuto) {
                lp.screenBrightness = WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE
            } else {
                lp.screenBrightness = brightness.coerceIn(0.01f, 1.0f)
            }
            window.attributes = lp
        }
        onDispose {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                val lp = window.attributes
                lp.screenBrightness = WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE
                window.attributes = lp
            }
        }
    }
}

actual class ScreenOrientationController(private val activity: Activity?) {
    actual fun setOrientation(orientation: String) {
        activity?.requestedOrientation = when (orientation) {
            "Landscape" -> ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
            "Sensor" -> ActivityInfo.SCREEN_ORIENTATION_SENSOR
            else -> ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        }
    }

    actual fun setKeepScreenAwake(keepAwake: Boolean) {
        if (keepAwake) {
            activity?.window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        } else {
            activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }
}

@Composable
actual fun rememberScreenOrientationController(): ScreenOrientationController {
    val context = LocalContext.current
    return remember(context) {
        ScreenOrientationController(context as? Activity)
    }
}