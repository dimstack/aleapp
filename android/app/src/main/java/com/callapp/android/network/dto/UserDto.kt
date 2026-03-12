package com.callapp.android.network.dto

import com.callapp.android.domain.model.User
import com.callapp.android.domain.model.UserRole
import com.callapp.android.domain.model.UserStatus
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class UserDto(
    val id: String,
    val username: String,
    @SerialName("display_name") val displayName: String,
    @SerialName("avatar_url") val avatarUrl: String? = null,
    @SerialName("is_online") val isOnline: Boolean = false,
    val role: String = "MEMBER",
    val status: String = "online",
    @SerialName("server_id") val serverId: String = "",
)

fun UserDto.toDomain(): User = User(
    id = id,
    name = displayName,
    username = username,
    avatarUrl = avatarUrl,
    role = when (role.uppercase()) {
        "ADMIN" -> UserRole.ADMIN
        else -> UserRole.MEMBER
    },
    status = when {
        status.equals("do_not_disturb", ignoreCase = true) -> UserStatus.DO_NOT_DISTURB
        status.equals("invisible", ignoreCase = true) -> UserStatus.INVISIBLE
        isOnline || status.equals("online", ignoreCase = true) -> UserStatus.ONLINE
        else -> UserStatus.INVISIBLE
    },
    serverId = serverId,
)
