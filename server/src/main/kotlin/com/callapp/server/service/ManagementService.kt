package com.callapp.server.service

import com.callapp.server.models.JoinRequestStatus
import com.callapp.server.models.NotificationType
import com.callapp.server.models.Role
import com.callapp.server.models.UserStatus
import com.callapp.server.repository.FavoriteRepository
import com.callapp.server.repository.InviteTokenRepository
import com.callapp.server.repository.JoinRequestRepository
import com.callapp.server.repository.NotificationRepository
import com.callapp.server.repository.ServerRepository
import com.callapp.server.repository.UserRepository
import com.callapp.server.routes.ApiException
import io.ktor.http.HttpStatusCode
import java.util.UUID

class ManagementService(
    private val serverRepository: ServerRepository,
    private val userRepository: UserRepository,
    private val inviteTokenRepository: InviteTokenRepository,
    private val joinRequestRepository: JoinRequestRepository,
    private val favoriteRepository: FavoriteRepository,
    private val notificationRepository: NotificationRepository,
    private val passwordService: PasswordService,
) {
    fun getServer() = requireServer()

    fun updateServer(name: String?, username: String?, description: String?, imageUrl: String?) =
        serverRepository.update(name, username, description, imageUrl)
            ?: throw ApiException(HttpStatusCode.NotFound, "not_found", "Server not found")

    fun deleteServer() = serverRepository.deleteCurrentServer()

    fun listUsers(serverId: String) = userRepository.listByServer(serverId)

    fun getUser(userId: String) =
        userRepository.findById(userId)
            ?: throw ApiException(HttpStatusCode.NotFound, "not_found", "User not found")

    fun updateUser(actorUserId: String, isAdmin: Boolean, userId: String, name: String?, username: String?, avatarUrl: String?, status: String?) =
        run {
            if (!isAdmin && actorUserId != userId) {
                throw ApiException(HttpStatusCode.Forbidden, "forbidden", "Cannot update another user")
            }
            val parsedStatus = status?.let {
                runCatching { UserStatus.valueOf(it.uppercase()) }.getOrElse {
                    throw ApiException(HttpStatusCode.BadRequest, "validation_error", "Invalid status value")
                }
            }
            val normalizedUsername = username?.let { if (it.startsWith("@")) it else "@$it" }
            userRepository.updateUser(userId, name, normalizedUsername, avatarUrl, parsedStatus)
                ?: throw ApiException(HttpStatusCode.NotFound, "not_found", "User not found")
        }

    fun deleteUser(actorUserId: String, isAdmin: Boolean, userId: String) {
        if (!isAdmin && actorUserId != userId) {
            throw ApiException(HttpStatusCode.Forbidden, "forbidden", "Cannot delete another user")
        }
        userRepository.deleteUser(userId)
    }

    fun createInviteToken(createdBy: String, serverId: String, label: String, maxUses: Int, grantedRole: String, requireApproval: Boolean) =
        inviteTokenRepository.create(
            id = UUID.randomUUID().toString(),
            token = randomTokenCode(),
            label = label,
            serverId = serverId,
            createdBy = createdBy,
            maxUses = maxUses,
            grantedRole = parseRole(grantedRole),
            requireApproval = requireApproval,
        )

    fun listInviteTokens(serverId: String) = inviteTokenRepository.listByServer(serverId)

    fun revokeInviteToken(inviteTokenId: String) = inviteTokenRepository.revoke(inviteTokenId)

    fun listJoinRequests(serverId: String) = joinRequestRepository.listPending(serverId)

    fun submitJoinRequest(serverId: String, username: String, name: String): com.callapp.server.models.JoinRequestRecord {
        val normalizedUsername = if (username.startsWith("@")) username else "@$username"
        if (userRepository.findByUsername(serverId, normalizedUsername) != null ||
            userRepository.findPendingJoinRequest(serverId, normalizedUsername) != null
        ) {
            throw ApiException(HttpStatusCode.Conflict, "username_taken", "Username is already taken")
        }
        return joinRequestRepository.create(
            username = normalizedUsername,
            displayName = name.trim(),
            passwordHash = passwordService.hash(UUID.randomUUID().toString()),
            avatarUrl = null,
            inviteTokenId = inviteTokenRepository.listByServer(serverId).firstOrNull()?.id
                ?: throw ApiException(HttpStatusCode.BadRequest, "validation_error", "No invite token available"),
            serverId = serverId,
            requestedRole = Role.MEMBER,
        )
    }

    fun updateJoinRequest(requestId: String, action: String, reviewerId: String): com.callapp.server.models.JoinRequestRecord {
        val pending = joinRequestRepository.findPendingById(requestId)
            ?: throw ApiException(HttpStatusCode.NotFound, "not_found", "Join request not found")
        return when (action.uppercase()) {
            "APPROVED" -> {
                val user = userRepository.createUser(
                    id = UUID.randomUUID().toString(),
                    username = pending.username,
                    displayName = pending.displayName,
                    passwordHash = pending.passwordHash,
                    avatarUrl = pending.avatarUrl,
                    role = pending.requestedRole,
                    serverId = pending.serverId,
                    isApproved = true,
                )
                joinRequestRepository.approve(requestId, reviewerId, user.id)
                checkNotNull(joinRequestRepository.findSummaryById(requestId))
            }
            "DECLINED" -> {
                joinRequestRepository.decline(requestId, reviewerId)
                checkNotNull(joinRequestRepository.findSummaryById(requestId))
            }
            else -> throw ApiException(HttpStatusCode.BadRequest, "validation_error", "Invalid join request action")
        }
    }

    fun listFavorites(userId: String) = favoriteRepository.listFavorites(userId)

    fun addFavorite(userId: String, favoriteUserId: String) = favoriteRepository.addFavorite(userId, favoriteUserId)

    fun removeFavorite(userId: String, favoriteUserId: String) = favoriteRepository.removeFavorite(userId, favoriteUserId)

    fun listNotifications(userId: String) = notificationRepository.listByUser(userId)

    fun markNotificationsRead(userId: String) = notificationRepository.markAllRead(userId)

    fun clearNotifications(userId: String) = notificationRepository.clearAll(userId)

    private fun requireServer() =
        serverRepository.getCurrentServer()
            ?: throw ApiException(HttpStatusCode.NotFound, "not_found", "Server not found")

    private fun parseRole(value: String): Role =
        runCatching { Role.valueOf(value.uppercase()) }.getOrElse {
            throw ApiException(HttpStatusCode.BadRequest, "validation_error", "Invalid role value")
        }

    private fun randomTokenCode(length: Int = 8): String {
        val alphabet = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz"
        return buildString(length) {
            repeat(length) {
                append(alphabet.random())
            }
        }
    }
}
