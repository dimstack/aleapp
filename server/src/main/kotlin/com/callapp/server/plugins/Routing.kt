package com.callapp.server.plugins

import com.callapp.server.auth.requireSessionPrincipal
import com.callapp.server.database.HealthRepository
import com.callapp.server.dependencies
import com.callapp.server.routes.AuthResponseDto
import com.callapp.server.routes.ConnectRequestDto
import com.callapp.server.routes.CreateUserRequestDto
import com.callapp.server.routes.HealthResponse
import com.callapp.server.routes.LoginRequestDto
import com.callapp.server.routes.toDto
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.auth.authenticate
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.routing

fun Application.configureRouting() {
    routing {
        get("/") {
            call.respond(
                HttpStatusCode.OK,
                mapOf(
                    "service" to "callapp-server",
                    "status" to "ok",
                ),
            )
        }

        get("/health") {
            val databaseState = HealthRepository(this@configureRouting.dependencies.database).probe()
            call.respond(
                HttpStatusCode.OK,
                HealthResponse(
                    service = "callapp-server",
                    environment = this@configureRouting.dependencies.config.environment,
                    status = if (databaseState.connected) "ok" else "degraded",
                    database = databaseState,
                ),
            )
        }

        post("/api/connect") {
            val request = call.receive<ConnectRequestDto>()
            val response = this@configureRouting.dependencies.onboardingService.connect(request.token)
            call.respond(HttpStatusCode.OK, response)
        }

        post("/api/auth/login") {
            val request = call.receive<LoginRequestDto>()
            val response = this@configureRouting.dependencies.onboardingService.login(request)
            call.respond(HttpStatusCode.OK, response)
        }

        authenticate("auth-jwt") {
            post("/api/users") {
                val principal = call.requireSessionPrincipal()
                if (!principal.isGuest || principal.inviteToken.isNullOrBlank()) {
                    call.respond(
                        HttpStatusCode.Forbidden,
                        com.callapp.server.routes.ErrorResponse(
                            code = "forbidden",
                            message = "Guest session is required",
                        ),
                    )
                    return@post
                }

                val request = call.receive<CreateUserRequestDto>()
                val result = this@configureRouting.dependencies.onboardingService.createUser(
                    inviteTokenValue = principal.inviteToken,
                    request = request,
                )

                when (result) {
                    is AuthResponseDto -> call.respond(HttpStatusCode.Accepted, result)
                    is com.callapp.server.models.UserRecord -> call.respond(HttpStatusCode.OK, result.toDto())
                    else -> call.respond(HttpStatusCode.InternalServerError)
                }
            }
        }
    }
}
