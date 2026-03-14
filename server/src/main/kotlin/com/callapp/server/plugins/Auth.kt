package com.callapp.server.plugins

import com.callapp.server.auth.JwtService
import com.callapp.server.auth.toSessionPrincipal
import com.callapp.server.dependencies
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
                credential.payload.toSessionPrincipal()
            }
        }
    }
}
