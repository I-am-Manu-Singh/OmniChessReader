package com.creativeblisslabs.omnimind.core.ui.util

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Build
import androidx.annotation.RequiresApi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalTime
import java.time.format.DateTimeFormatter

@SuppressLint("StaticFieldLeak")
object AndroidContextHolder {
    var context: Context? = null
}

actual class PlatformStatusManager(private val context: Context?) {
    actual constructor() : this(AndroidContextHolder.context)

    private val _statusFlow = MutableStateFlow(HeaderStatusData())
    actual val statusFlow: StateFlow<HeaderStatusData> = _statusFlow.asStateFlow()

    private var scopeJob: Job? = null

    private val batteryReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            intent?.let {
                val level = it.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
                val scale = it.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
                val pct = if (level != -1 && scale != -1) (level * 100 / scale.toFloat()).toInt() else 100

                val status = it.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
                val charging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                        status == BatteryManager.BATTERY_STATUS_FULL

                _statusFlow.update { state -> state.copy(batteryPercentage = pct, isCharging = charging) }
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    actual fun startMonitoring() {
        context?.registerReceiver(batteryReceiver, IntentFilter(Intent.ACTION_BATTERY_CHANGED))

        scopeJob = CoroutineScope(Dispatchers.Main).launch {
            val formatter = DateTimeFormatter.ofPattern("h:mm a")
            while (true) {
                val timeStr = LocalTime.now().format(formatter)
                _statusFlow.update { it.copy(timeText = timeStr) }
                delay(1000)
            }
        }
    }

    actual fun stopMonitoring() {
        try { context?.unregisterReceiver(batteryReceiver) } catch (_: Exception) {}
        scopeJob?.cancel()
    }
}
