package com.callapp.android.network.dto

import com.callapp.android.domain.model.Notification
import com.callapp.android.domain.model.NotificationType
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class NotificationDto(
    val id: String,
    val type: String,
    @SerialName("server_name") val serverName: String = "",
    val message: String = "",
    @SerialName("is_read") val isRead: Boolean = false,
    @SerialName("created_at") val createdAt: String = "",
)

fun NotificationDto.toDomain(): Notification = Notification(
    id = id,
    type = when (type.uppercase()) {
        "REQUEST_SENT" -> NotificationType.REQUEST_SENT
        "REQUEST_APPROVED" -> NotificationType.REQUEST_APPROVED
        "REQUEST_DECLINED" -> NotificationType.REQUEST_DECLINED
        "INCOMING_CALL" -> NotificationType.INCOMING_CALL
        "MISSED_CALL" -> NotificationType.MISSED_CALL
        else -> NotificationType.REQUEST_SENT
    },
    serverName = serverName,
    message = message,
    isRead = isRead,
    createdAt = createdAt,
)
