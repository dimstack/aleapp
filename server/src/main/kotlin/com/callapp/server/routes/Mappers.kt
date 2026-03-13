package com.callapp.server.routes

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
