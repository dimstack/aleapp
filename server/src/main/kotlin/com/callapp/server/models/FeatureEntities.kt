package com.callapp.server.models

import java.time.Instant

data class JoinRequestRecord(
    val id: String,
    val userName: String,
    val username: String,
    val serverId: String,
    val status: JoinRequestStatus,
    val createdAt: Instant,
)

data class NotificationRecord(
    val id: String,
    val userId: String,
    val type: NotificationType,
    val serverName: String,
    val message: String,
    val actorUserId: String?,
    val actorUsername: String?,
    val actorDisplayName: String?,
    val actorAvatarUrl: String?,
    val isRead: Boolean,
    val createdAt: Instant,
)
