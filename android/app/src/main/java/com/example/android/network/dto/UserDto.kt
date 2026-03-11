package com.example.android.network.dto

import com.example.android.domain.model.User
import com.example.android.domain.model.UserRole
import com.example.android.domain.model.UserStatus
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class UserDto(
    val id: String,
    val username: String,
    @SerialName("display_name") val displayName: String,
    @SerialName("avatar_url") val avatarUrl: String? = null,
    @SerialName("is_online") val isOnline: Boolean = false,
)

fun UserDto.toDomain(): User = User(
    id = id,
    name = displayName,
    username = username,
    avatarUrl = avatarUrl,
    status = if (isOnline) UserStatus.ONLINE else UserStatus.INVISIBLE,
)
