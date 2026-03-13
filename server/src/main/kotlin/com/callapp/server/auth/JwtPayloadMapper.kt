package com.callapp.server.auth

import com.auth0.jwt.interfaces.Payload
import com.callapp.server.models.Role
import com.callapp.server.models.SessionType

fun Payload.toSessionPrincipal(): SessionPrincipal? {
    val serverId = getClaim(JwtService.CLAIM_SERVER_ID).asString()?.takeIf { it.isNotBlank() } ?: return null
    val sessionType = runCatching {
        SessionType.valueOf(getClaim(JwtService.CLAIM_SESSION_TYPE).asString())
    }.getOrNull() ?: return null

    val role = getClaim(JwtService.CLAIM_ROLE).asString()
        ?.takeIf { it.isNotBlank() }
        ?.let { runCatching { Role.valueOf(it) }.getOrNull() }

    return SessionPrincipal(
        userId = subject?.takeIf { it.isNotBlank() },
        serverId = serverId,
        role = role,
        sessionType = sessionType,
        inviteToken = getClaim(JwtService.CLAIM_INVITE_TOKEN).asString(),
    )
}
