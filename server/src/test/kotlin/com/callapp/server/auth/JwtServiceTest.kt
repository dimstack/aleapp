package com.callapp.server.auth

import com.callapp.server.config.SecurityConfig
import com.callapp.server.models.Role
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class JwtServiceTest {

    private val config = SecurityConfig(
        jwtSecret = "test-secret",
        issuer = "test-issuer",
        audience = "test-audience",
        guestTokenTtlMinutes = 30,
        userTokenTtlDays = 30,
    )

    @Test
    fun createsGuestTokenWithInviteClaim() {
        val jwtService = JwtService(config)
        val token = jwtService.createGuestToken(
            serverId = "server-1",
            inviteToken = "ABC12345",
        )

        val decoded = jwtService.verifier().verify(token)
        assertEquals("server-1", decoded.getClaim(JwtService.CLAIM_SERVER_ID).asString())
        assertEquals("GUEST", decoded.getClaim(JwtService.CLAIM_SESSION_TYPE).asString())
        assertEquals("ABC12345", decoded.getClaim(JwtService.CLAIM_INVITE_TOKEN).asString())
    }

    @Test
    fun createsUserTokenWithSubjectAndRole() {
        val jwtService = JwtService(config)
        val token = jwtService.createUserToken(
            userId = "user-1",
            serverId = "server-1",
            role = Role.ADMIN,
        )

        val decoded = jwtService.verifier().verify(token)
        assertEquals("user-1", decoded.subject)
        assertEquals("ADMIN", decoded.getClaim(JwtService.CLAIM_ROLE).asString())
        assertEquals("ADMIN", decoded.getClaim(JwtService.CLAIM_SESSION_TYPE).asString())
        assertNotNull(decoded.expiresAt)
    }
}
