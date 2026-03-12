package com.callapp.android.domain.model

enum class NotificationType {
    REQUEST_SENT,
    REQUEST_APPROVED,
    REQUEST_DECLINED,
    INCOMING_CALL,
    MISSED_CALL,
}

data class Notification(
    val id: String,
    val type: NotificationType,
    val serverName: String,
    val message: String,
    val isRead: Boolean,
    val createdAt: String,
)
