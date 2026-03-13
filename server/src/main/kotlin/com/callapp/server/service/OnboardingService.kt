package com.callapp.server.service

import com.callapp.server.auth.JwtService
import com.callapp.server.models.JoinRequestStatus
import com.callapp.server.models.Role
import com.callapp.server.models.UserRecord
import com.callapp.server.repository.InviteTokenRepository
import com.callapp.server.repository.LoginAttemptRepository
import com.callapp.server.repository.ServerRepository
import com.callapp.server.repository.UserRepository
import com.callapp.server.routes.ApiException
import com.callapp.server.routes.AuthResponseDto
import com.callapp.server.routes.ConnectResponseDto
import com.callapp.server.routes.CreateUserRequestDto
import com.callapp.server.routes.LoginRequestDto
import com.callapp.server.routes.toDto
import io.ktor.http.HttpStatusCode
import java.time.Clock
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.UUID

class OnboardingService(
    private val serverRepository: ServerRepository,
    private val userRepository: UserRepository,
    private val inviteTokenRepository: InviteTokenRepository,
    private val loginAttemptRepository: LoginAttemptRepository,
    private val inviteTokenService: InviteTokenService,
    private val passwordService: PasswordService,
    private val jwtService: JwtService,
    private val clock: Clock = Clock.systemUTC(),
) {
    fun connect(rawToken: String): ConnectResponseDto {
        val inviteToken = inviteTokenService.validateForConnect(rawToken)
        val server = requireServer()
        val guestToken = jwtService.createGuestToken(inviteToken.serverId, inviteToken.token)
        return ConnectResponseDto(
            sessionToken = guestToken,
            server = server.toDto(),
            status = "needs_profile",
        )
    }

    fun login(request: LoginRequestDto): AuthResponseDto {
        val inviteToken = inviteTokenService.validateForLogin(request.inviteToken)
        val server = requireServer()
        val normalizedUsername = normalizeUsername(request.username)
        val attempt = loginAttemptRepository.find(server.id, normalizedUsername)
        if (attempt?.lockedUntil != null && attempt.lockedUntil.isAfter(Instant.now(clock))) {
            throw ApiException(HttpStatusCode.Unauthorized, "login_locked", "Too many failed login attempts")
        }

        val pendingRequest = userRepository.findPendingJoinRequest(server.id, normalizedUsername)
        if (pendingRequest != null && passwordService.verify(request.password, pendingRequest.passwordHash)) {
            val guestToken = jwtService.createGuestToken(inviteToken.serverId, inviteToken.token)
            return AuthResponseDto(
                sessionToken = guestToken,
                server = server.toDto(),
                status = "pending",
            )
        }

        val user = userRepository.findByUsername(server.id, normalizedUsername)
            ?: failLogin(server.id, normalizedUsername)

        if (!user.isApproved) {
            val guestToken = jwtService.createGuestToken(inviteToken.serverId, inviteToken.token)
            return AuthResponseDto(
                sessionToken = guestToken,
                user = user.toDto(),
                server = server.toDto(),
                status = "pending",
            )
        }

        if (!passwordService.verify(request.password, user.passwordHash)) {
            failLogin(server.id, normalizedUsername)
        }

        loginAttemptRepository.reset(server.id, normalizedUsername)
        val userToken = jwtService.createUserToken(user.id, user.serverId, user.role)
        return AuthResponseDto(
            sessionToken = userToken,
            user = user.toDto(),
            server = server.toDto(),
            status = "joined",
        )
    }

    fun createUser(
        inviteTokenValue: String,
        request: CreateUserRequestDto,
    ): Any {
        validatePassword(request.password)
        val inviteToken = inviteTokenService.validateForCreate(inviteTokenValue)
        val server = requireServer()
        val normalizedUsername = normalizeUsername(request.username)

        if (userRepository.findByUsername(server.id, normalizedUsername) != null ||
            userRepository.findPendingJoinRequest(server.id, normalizedUsername) != null
        ) {
            throw ApiException(HttpStatusCode.Conflict, "username_taken", "Username is already taken")
        }

        val passwordHash = passwordService.hash(request.password)

        return if (inviteToken.requireApproval) {
            userRepository.createJoinRequest(
                id = UUID.randomUUID().toString(),
                username = normalizedUsername,
                displayName = request.name.trim(),
                passwordHash = passwordHash,
                avatarUrl = request.avatarUrl,
                inviteTokenId = inviteToken.id,
                serverId = server.id,
                requestedRole = inviteToken.grantedRole,
            )
            AuthResponseDto(
                sessionToken = jwtService.createGuestToken(server.id, inviteToken.token),
                server = server.toDto(),
                status = "pending",
            )
        } else {
            val user = userRepository.createUser(
                id = UUID.randomUUID().toString(),
                username = normalizedUsername,
                displayName = request.name.trim(),
                passwordHash = passwordHash,
                avatarUrl = request.avatarUrl,
                role = inviteToken.grantedRole,
                serverId = server.id,
                isApproved = true,
            )
            inviteTokenRepository.incrementUsage(inviteToken.id)
            user
        }
    }

    private fun validatePassword(password: String) {
        if (password.length < 8) {
            throw ApiException(HttpStatusCode.BadRequest, "validation_error", "Password must be at least 8 characters long")
        }
    }

    private fun normalizeUsername(username: String): String {
        val trimmed = username.trim()
        if (trimmed.isBlank()) {
            throw ApiException(HttpStatusCode.BadRequest, "validation_error", "Username is required")
        }
        return if (trimmed.startsWith("@")) trimmed else "@$trimmed"
    }

    private fun requireServer() = serverRepository.getCurrentServer()
        ?: throw ApiException(HttpStatusCode.InternalServerError, "server_error", "Server metadata is missing")

    private fun failLogin(serverId: String, username: String): Nothing {
        val current = loginAttemptRepository.find(serverId, username)
        val failedAttempts = (current?.failedAttempts ?: 0) + 1
        val lockedUntil = if (failedAttempts >= 5) Instant.now(clock).plus(15, ChronoUnit.MINUTES) else null
        loginAttemptRepository.upsert(serverId, username, failedAttempts, lockedUntil)
        throw ApiException(
            status = HttpStatusCode.Unauthorized,
            code = if (lockedUntil != null) "login_locked" else "unauthorized",
            message = if (lockedUntil != null) "Too many failed login attempts" else "Invalid username or password",
        )
    }
}
