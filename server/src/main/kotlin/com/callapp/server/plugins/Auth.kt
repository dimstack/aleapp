package com.callapp.server.plugins

import com.auth0.jwt.interfaces.Payload
import com.callapp.server.auth.JwtService
import com.callapp.server.auth.SessionPrincipal
import com.callapp.server.dependencies
import com.callapp.server.models.Role
import com.callapp.server.models.SessionType
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.auth.Authentication
import io.ktor.server.auth.jwt.jwt
import io.ktor.server.response.respond
import io.ktor.http.HttpStatusCode
import com.callapp.server.routes.ErrorResponse

fun Application.configureAuth() {
    val jwtService = dependencies.jwtService

    install(Authentication) {
        jwt("auth-jwt") {
            realm = this@configureAuth.dependencies.config.security.issuer
            verifier(jwtService.verifier())
            challenge { _, _ ->
                call.respond(
                    HttpStatusCode.Unauthorized,
                    ErrorResponse(code = "unauthorized", message = "Authentication is required"),
                )
            }
            validate { credential ->
                credential.payload.toPrincipal()
            }
        }
    }
}

private fun Payload.toPrincipal(): SessionPrincipal? {
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
