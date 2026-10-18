package com.creativeblisslabs.omnimind.core.ui.util


import kotlinx.coroutines.flow.StateFlow

data class HeaderStatusData(
    val timeText: String = "",
    val batteryPercentage: Int = 100,
    val isCharging: Boolean = false
)

expect class PlatformStatusManager() {
    val statusFlow: StateFlow<HeaderStatusData>
    fun startMonitoring()
    fun stopMonitoring()
}