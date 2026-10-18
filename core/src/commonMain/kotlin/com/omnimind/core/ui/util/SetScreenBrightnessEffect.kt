package com.creativeblisslabs.omnimind.core.ui.util

import androidx.compose.runtime.Composable

@Composable
expect fun SetScreenBrightnessEffect(brightness: Float, isAuto: Boolean = false)

expect class ScreenOrientationController {
    fun setOrientation(orientation: String)
    fun setKeepScreenAwake(keepAwake: Boolean)
}

@Composable
expect fun rememberScreenOrientationController(): ScreenOrientationController
