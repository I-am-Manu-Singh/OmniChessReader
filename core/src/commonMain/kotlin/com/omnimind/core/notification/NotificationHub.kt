package com.creativeblisslabs.omnimind.core.notification

import com.creativeblisslabs.omnimind.core.ui.theme.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class OmniNotification(
    val id: String,
    val title: String,
    val message: String,
    val icon: ImageVector,
    val color: Color,
    val targetRoute: String? = null,
    val timestamp: Long = 0L
)

object NotificationRegistry {
    private val _notifications = MutableStateFlow<List<OmniNotification>>(emptyList())

    val notifications: StateFlow<List<OmniNotification>> = _notifications.asStateFlow()

    fun addNotification(notification: OmniNotification) {
        _notifications.update { it + notification }
    }

    fun getNotificationsForScreen(route: String): List<OmniNotification> {
        val key = route.split("/").firstOrNull()?.lowercase() ?: "dashboard"
        return _notifications.value.filter { it.targetRoute == key }
    }

    fun dismissNotification(id: String) {
        _notifications.value = _notifications.value.filter { it.id != id }
    }
}
