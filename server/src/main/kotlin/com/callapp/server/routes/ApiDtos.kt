package com.callapp.server.routes

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ConnectRequestDto(
    val token: String,
)

@Serializable
data class LoginRequestDto(
    @SerialName("invite_token") val inviteToken: String,
    val username: String,
    val password: String,
)

@Serializable
data class CreateUserRequestDto(
    val name: String,
    val username: String,
    val password: String,
    @SerialName("avatar_url") val avatarUrl: String? = null,
)

@Serializable
data class ConnectResponseDto(
    @SerialName("session_token") val sessionToken: String,
    val user: UserDto? = null,
    val server: ServerDto? = null,
    val status: String = "needs_profile",
)

@Serializable
data class AuthResponseDto(
    @SerialName("session_token") val sessionToken: String,
    val user: UserDto? = null,
    val server: ServerDto? = null,
    val status: String = "joined",
)

@Serializable
data class ServerDto(
    val id: String,
    val name: String,
    val username: String,
    val description: String = "",
    @SerialName("image_url") val imageUrl: String? = null,
)

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
