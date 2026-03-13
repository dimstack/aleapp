package com.callapp.server.service

import com.callapp.server.models.InviteTokenRecord
import com.callapp.server.repository.InviteTokenRepository
import com.callapp.server.routes.ApiException
import io.ktor.http.HttpStatusCode
import java.time.Clock
import java.time.Instant

class InviteTokenService(
    private val repository: InviteTokenRepository,
    private val parser: InviteTokenParser,
    private val clock: Clock = Clock.systemUTC(),
) {
    fun validateForConnect(rawToken: String): InviteTokenRecord =
        validate(rawToken, enforceCapacity = false)

    fun validateForLogin(rawToken: String): InviteTokenRecord =
        validate(rawToken, enforceCapacity = false)

    fun validateForCreate(rawToken: String): InviteTokenRecord =
        validate(rawToken, enforceCapacity = true)

    private fun validate(rawToken: String, enforceCapacity: Boolean): InviteTokenRecord {
        val tokenCode = runCatching { parser.extractCode(rawToken) }.getOrElse {
            throw ApiException(HttpStatusCode.Unauthorized, "invite_token_invalid", "Invite token is invalid")
        }
        val token = repository.findByToken(tokenCode)
            ?: throw ApiException(HttpStatusCode.Unauthorized, "invite_token_invalid", "Invite token is invalid")

        if (token.isRevoked) {
            throw ApiException(HttpStatusCode.Unauthorized, "invite_token_revoked", "Invite token has been revoked")
        }
        if (token.expiresAt != null && token.expiresAt.isBefore(Instant.now(clock))) {
            throw ApiException(HttpStatusCode.Unauthorized, "invite_token_invalid", "Invite token has expired")
        }
        if (enforceCapacity && token.maxUses > 0 && token.currentUses >= token.maxUses) {
            throw ApiException(HttpStatusCode.Unauthorized, "invite_token_exhausted", "Invite token has reached its limit")
        }
        return token
    }
}
