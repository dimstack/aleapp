package com.callapp.server.routes

import com.callapp.server.models.InviteTokenRecord
import com.callapp.server.models.JoinRequestRecord
import com.callapp.server.models.NotificationRecord
import com.callapp.server.models.ServerRecord
import com.callapp.server.models.UserRecord
import com.callapp.server.models.UserStatus

fun ServerRecord.toDto(): ServerDto = ServerDto(
    id = id,
    name = name,
    username = username,
    description = description,
    imageUrl = imageUrl,
)

fun UserRecord.toDto(): UserDto = UserDto(
    id = id,
    username = username,
    displayName = displayName,
    avatarUrl = avatarUrl,
    isOnline = status == UserStatus.ONLINE,
    role = role.name,
    status = status.name.lowercase(),
    serverId = serverId,
)

fun InviteTokenRecord.toDto(): InviteTokenDto = InviteTokenDto(
    id = id,
    code = token,
    label = label,
    maxUses = maxUses,
    useCount = currentUses,
    grantedRole = grantedRole.name,
    requireApproval = requireApproval,
    revoked = isRevoked,
    createdAt = createdAt.toString(),
)

fun JoinRequestRecord.toDto(): JoinRequestDto = JoinRequestDto(
    id = id,
    userName = userName,
    username = username,
    serverId = serverId,
    status = status.name.lowercase(),
    createdAt = createdAt.toString(),
)

fun NotificationRecord.toDto(): NotificationDto = NotificationDto(
    id = id,
    type = type.name,
    serverName = serverName,
    message = message,
    actorUserId = actorUserId,
    actorUsername = actorUsername,
    actorDisplayName = actorDisplayName,
    actorAvatarUrl = actorAvatarUrl,
    isRead = isRead,
    createdAt = createdAt.toString(),
)
