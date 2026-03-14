package com.callapp.server.auth

import com.auth0.jwt.JWT
import com.auth0.jwt.JWTVerifier
import com.auth0.jwt.algorithms.Algorithm
import com.callapp.server.config.SecurityConfig
import com.callapp.server.models.Role
import com.callapp.server.models.SessionType
import java.time.Clock
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.Date

class JwtService(
    private val config: SecurityConfig,
    private val clock: Clock = Clock.systemUTC(),
) {
    private val algorithm = Algorithm.HMAC256(config.jwtSecret)

    fun verifier(): JWTVerifier = JWT
        .require(algorithm)
        .withIssuer(config.issuer)
        .withAudience(config.audience)
        .build()

    fun createGuestToken(serverId: String, inviteToken: String): String {
        val expiresAt = Instant.now(clock).plus(config.guestTokenTtlMinutes, ChronoUnit.MINUTES)
        return baseBuilder(serverId, SessionType.GUEST, expiresAt)
            .withClaim(CLAIM_INVITE_TOKEN, inviteToken)
            .sign(algorithm)
    }

    fun createUserToken(userId: String, serverId: String, role: Role): String {
        val sessionType = if (role == Role.ADMIN) SessionType.ADMIN else SessionType.USER
        val expiresAt = Instant.now(clock).plus(config.userTokenTtlDays, ChronoUnit.DAYS)
        return baseBuilder(serverId, sessionType, expiresAt)
            .withSubject(userId)
            .withClaim(CLAIM_ROLE, role.name)
            .sign(algorithm)
    }

    private fun baseBuilder(
        serverId: String,
        sessionType: SessionType,
        expiresAt: Instant,
    ) = JWT.create()
        .withIssuer(config.issuer)
        .withAudience(config.audience)
        .withIssuedAt(Date.from(Instant.now(clock)))
        .withExpiresAt(Date.from(expiresAt))
        .withClaim(CLAIM_SERVER_ID, serverId)
        .withClaim(CLAIM_SESSION_TYPE, sessionType.name)

    companion object {
        const val CLAIM_SERVER_ID = "serverId"
        const val CLAIM_ROLE = "role"
        const val CLAIM_SESSION_TYPE = "sessionType"
        const val CLAIM_INVITE_TOKEN = "inviteToken"
    }
}
