package com.callapp.server.auth

import com.callapp.server.models.Role
import com.callapp.server.models.SessionType
import io.ktor.server.auth.Principal

data class SessionPrincipal(
    val userId: String?,
    val serverId: String,
    val role: Role?,
    val sessionType: SessionType,
    val inviteToken: String? = null,
) : Principal {
    val isGuest: Boolean get() = sessionType == SessionType.GUEST
    val isUser: Boolean get() = sessionType == SessionType.USER || sessionType == SessionType.ADMIN
    val isAdmin: Boolean get() = sessionType == SessionType.ADMIN || role == Role.ADMIN
}
