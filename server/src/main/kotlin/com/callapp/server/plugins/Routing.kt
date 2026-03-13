package com.callapp.server.plugins

import com.callapp.server.auth.requireSessionPrincipal
import com.callapp.server.database.HealthRepository
import com.callapp.server.dependencies
import com.callapp.server.routes.ApiException
import com.callapp.server.routes.AuthResponseDto
import com.callapp.server.routes.ConnectRequestDto
import com.callapp.server.routes.CreateInviteTokenRequestDto
import com.callapp.server.routes.CreateUserRequestDto
import com.callapp.server.routes.HealthResponse
import com.callapp.server.routes.JoinRequestActionDto
import com.callapp.server.routes.LoginRequestDto
import com.callapp.server.routes.SubmitJoinRequestDto
import com.callapp.server.routes.UpdateServerRequestDto
import com.callapp.server.routes.UpdateUserRequestDto
import com.callapp.server.routes.toDto
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.auth.authenticate
import io.ktor.server.request.receive
import io.ktor.server.request.receiveNullable
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.put
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

            get("/api/server") {
                val principal = call.requireSessionPrincipal()
                requireUser(principal)
                call.respond(HttpStatusCode.OK, this@configureRouting.dependencies.managementService.getServer().toDto())
            }

            put("/api/server") {
                val principal = call.requireSessionPrincipal()
                requireAdmin(principal)
                val request = call.receive<UpdateServerRequestDto>()
                val server = this@configureRouting.dependencies.managementService.updateServer(
                    name = request.name,
                    username = request.username,
                    description = request.description,
                    imageUrl = request.imageUrl,
                )
                call.respond(HttpStatusCode.OK, server.toDto())
            }

            delete("/api/server") {
                val principal = call.requireSessionPrincipal()
                requireAdmin(principal)
                this@configureRouting.dependencies.managementService.deleteServer()
                call.respond(HttpStatusCode.NoContent)
            }

            get("/api/users") {
                val principal = call.requireSessionPrincipal()
                requireUser(principal)
                val users = this@configureRouting.dependencies.managementService
                    .listUsers(principal.serverId)
                    .map { it.toDto() }
                call.respond(HttpStatusCode.OK, users)
            }

            get("/api/users/{id}") {
                val principal = call.requireSessionPrincipal()
                requireUser(principal)
                val userId = call.parameters["id"] ?: throw ApiException(HttpStatusCode.BadRequest, "validation_error", "User id is required")
                call.respond(HttpStatusCode.OK, this@configureRouting.dependencies.managementService.getUser(userId).toDto())
            }

            put("/api/users/{id}") {
                val principal = call.requireSessionPrincipal()
                requireUser(principal)
                val userId = call.parameters["id"] ?: throw ApiException(HttpStatusCode.BadRequest, "validation_error", "User id is required")
                val request = call.receive<UpdateUserRequestDto>()
                val user = this@configureRouting.dependencies.managementService.updateUser(
                    actorUserId = principal.userId.orEmpty(),
                    isAdmin = principal.isAdmin,
                    userId = userId,
                    name = request.name,
                    username = request.username,
                    avatarUrl = request.avatarUrl,
                    status = request.status,
                )
                call.respond(HttpStatusCode.OK, user.toDto())
            }

            delete("/api/users/{id}") {
                val principal = call.requireSessionPrincipal()
                requireUser(principal)
                val userId = call.parameters["id"] ?: throw ApiException(HttpStatusCode.BadRequest, "validation_error", "User id is required")
                this@configureRouting.dependencies.managementService.deleteUser(
                    actorUserId = principal.userId.orEmpty(),
                    isAdmin = principal.isAdmin,
                    userId = userId,
                )
                call.respond(HttpStatusCode.NoContent)
            }

            get("/api/invite-tokens") {
                val principal = call.requireSessionPrincipal()
                requireAdmin(principal)
                val tokens = this@configureRouting.dependencies.managementService
                    .listInviteTokens(principal.serverId)
                    .map { it.toDto() }
                call.respond(HttpStatusCode.OK, tokens)
            }

            post("/api/invite-tokens") {
                val principal = call.requireSessionPrincipal()
                requireAdmin(principal)
                val request = call.receive<CreateInviteTokenRequestDto>()
                val token = this@configureRouting.dependencies.managementService.createInviteToken(
                    createdBy = principal.userId.orEmpty(),
                    serverId = principal.serverId,
                    label = request.label,
                    maxUses = request.maxUses,
                    grantedRole = request.grantedRole,
                    requireApproval = request.requireApproval,
                )
                call.respond(HttpStatusCode.OK, token.toDto())
            }

            delete("/api/invite-tokens/{id}") {
                val principal = call.requireSessionPrincipal()
                requireAdmin(principal)
                val inviteTokenId = call.parameters["id"] ?: throw ApiException(HttpStatusCode.BadRequest, "validation_error", "Invite token id is required")
                this@configureRouting.dependencies.managementService.revokeInviteToken(inviteTokenId)
                call.respond(HttpStatusCode.NoContent)
            }

            get("/api/join-requests") {
                val principal = call.requireSessionPrincipal()
                requireAdmin(principal)
                val requests = this@configureRouting.dependencies.managementService
                    .listJoinRequests(principal.serverId)
                    .map { it.toDto() }
                call.respond(HttpStatusCode.OK, requests)
            }

            post("/api/join-requests") {
                val principal = call.requireSessionPrincipal()
                requireUser(principal)
                val request = call.receive<SubmitJoinRequestDto>()
                val joinRequest = this@configureRouting.dependencies.managementService.submitJoinRequest(
                    serverId = principal.serverId,
                    username = request.username,
                    name = request.name,
                )
                call.respond(HttpStatusCode.OK, joinRequest.toDto())
            }

            put("/api/join-requests/{id}") {
                val principal = call.requireSessionPrincipal()
                requireAdmin(principal)
                val requestId = call.parameters["id"] ?: throw ApiException(HttpStatusCode.BadRequest, "validation_error", "Join request id is required")
                val request = call.receive<JoinRequestActionDto>()
                val joinRequest = this@configureRouting.dependencies.managementService.updateJoinRequest(
                    requestId = requestId,
                    action = request.status,
                    reviewerId = principal.userId.orEmpty(),
                )
                call.respond(HttpStatusCode.OK, joinRequest.toDto())
            }

            get("/api/favorites") {
                val principal = call.requireSessionPrincipal()
                requireUser(principal)
                val favorites = this@configureRouting.dependencies.managementService
                    .listFavorites(principal.userId.orEmpty())
                    .map { it.toDto() }
                call.respond(HttpStatusCode.OK, favorites)
            }

            post("/api/favorites/{userId}") {
                val principal = call.requireSessionPrincipal()
                requireUser(principal)
                val favoriteUserId = call.parameters["userId"] ?: throw ApiException(HttpStatusCode.BadRequest, "validation_error", "Favorite user id is required")
                this@configureRouting.dependencies.managementService.addFavorite(principal.userId.orEmpty(), favoriteUserId)
                call.respond(HttpStatusCode.OK)
            }

            delete("/api/favorites/{userId}") {
                val principal = call.requireSessionPrincipal()
                requireUser(principal)
                val favoriteUserId = call.parameters["userId"] ?: throw ApiException(HttpStatusCode.BadRequest, "validation_error", "Favorite user id is required")
                this@configureRouting.dependencies.managementService.removeFavorite(principal.userId.orEmpty(), favoriteUserId)
                call.respond(HttpStatusCode.NoContent)
            }

            get("/api/notifications") {
                val principal = call.requireSessionPrincipal()
                requireUser(principal)
                val notifications = this@configureRouting.dependencies.managementService
                    .listNotifications(principal.userId.orEmpty())
                    .map { it.toDto() }
                call.respond(HttpStatusCode.OK, notifications)
            }

            put("/api/notifications/read") {
                val principal = call.requireSessionPrincipal()
                requireUser(principal)
                this@configureRouting.dependencies.managementService.markNotificationsRead(principal.userId.orEmpty())
                call.respond(HttpStatusCode.OK)
            }

            delete("/api/notifications") {
                val principal = call.requireSessionPrincipal()
                requireUser(principal)
                this@configureRouting.dependencies.managementService.clearNotifications(principal.userId.orEmpty())
                call.respond(HttpStatusCode.NoContent)
            }
        }
    }
}

private fun requireUser(principal: com.callapp.server.auth.SessionPrincipal) {
    if (!principal.isUser || principal.userId.isNullOrBlank()) {
        throw ApiException(HttpStatusCode.Forbidden, "forbidden", "User session is required")
    }
}

private fun requireAdmin(principal: com.callapp.server.auth.SessionPrincipal) {
    requireUser(principal)
    if (!principal.isAdmin) {
        throw ApiException(HttpStatusCode.Forbidden, "forbidden", "Admin session is required")
    }
}
