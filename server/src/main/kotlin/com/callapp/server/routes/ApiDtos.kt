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

@Serializable
data class UpdateServerRequestDto(
    val name: String? = null,
    val username: String? = null,
    val description: String? = null,
    @SerialName("image_url") val imageUrl: String? = null,
)

@Serializable
data class UpdateUserRequestDto(
    val name: String? = null,
    val username: String? = null,
    @SerialName("avatar_url") val avatarUrl: String? = null,
    val status: String? = null,
)

@Serializable
data class UploadImageResponseDto(
    val url: String,
)

@Serializable
data class CreateInviteTokenRequestDto(
    val label: String,
    @SerialName("max_uses") val maxUses: Int = 0,
    @SerialName("granted_role") val grantedRole: String = "MEMBER",
    @SerialName("require_approval") val requireApproval: Boolean = false,
)

@Serializable
data class InviteTokenDto(
    val id: String,
    val code: String,
    val label: String,
    @SerialName("max_uses") val maxUses: Int,
    @SerialName("use_count") val useCount: Int,
    @SerialName("granted_role") val grantedRole: String = "MEMBER",
    @SerialName("require_approval") val requireApproval: Boolean = false,
    val revoked: Boolean = false,
    @SerialName("created_at") val createdAt: String = "",
)

@Serializable
data class JoinRequestDto(
    val id: String,
    @SerialName("user_name") val userName: String,
    val username: String,
    @SerialName("server_id") val serverId: String = "",
    val status: String = "pending",
    @SerialName("created_at") val createdAt: String = "",
)

@Serializable
data class SubmitJoinRequestDto(
    val username: String,
    val name: String,
)

@Serializable
data class JoinRequestActionDto(
    val status: String,
)

@Serializable
data class NotificationDto(
    val id: String,
    val type: String,
    @SerialName("server_name") val serverName: String = "",
    val message: String = "",
    @SerialName("actor_user_id") val actorUserId: String? = null,
    @SerialName("actor_username") val actorUsername: String? = null,
    @SerialName("actor_display_name") val actorDisplayName: String? = null,
    @SerialName("actor_avatar_url") val actorAvatarUrl: String? = null,
    @SerialName("is_read") val isRead: Boolean = false,
    @SerialName("created_at") val createdAt: String = "",
)
