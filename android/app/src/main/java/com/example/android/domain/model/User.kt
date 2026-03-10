package com.example.android.domain.model

enum class UserStatus { ONLINE, DO_NOT_DISTURB, INVISIBLE }

enum class UserRole { ADMIN, MEMBER }

data class User(
    val id: String,
    val name: String,
    val username: String,
    val avatarUrl: String? = null,
    val role: UserRole = UserRole.MEMBER,
    val status: UserStatus = UserStatus.ONLINE,
    val serverId: String = "",
)
