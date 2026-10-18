package com.creativeblisslabs.omnimind.core.ui.util

import kotlinx.coroutines.flow.StateFlow

actual class PlatformStatusManager actual constructor() {
    actual val statusFlow: StateFlow<HeaderStatusData>
        get() = TODO("Not yet implemented")

    actual fun startMonitoring() {
    }

    actual fun stopMonitoring() {
    }
}