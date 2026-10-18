package com.creativeblisslabs.omnimind.core.service

actual fun dispatchPlatformUiAction(json: String) {
    // UI actions via accessibility not supported on iOS in this prototype
}
