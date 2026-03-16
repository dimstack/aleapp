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
import javax.sql.DataSource

class ManagementService(
    private val dataSource: DataSource,
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
        serverRepository.update(
            name = name?.also {
                if (it.isBlank()) throw ApiException(HttpStatusCode.BadRequest, "validation_error", "Server name cannot be blank")
            },
            username = username?.also {
                if (it.isBlank()) throw ApiException(HttpStatusCode.BadRequest, "validation_error", "Server username cannot be blank")
            },
            description = description,
            imageUrl = imageUrl,
        )
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
            val normalizedUsername = username?.let {
                val normalized = if (it.startsWith("@")) it else "@$it"
                if (!normalized.matches(Regex("^@[A-Za-z0-9_]{3,32}$"))) {
                    throw ApiException(HttpStatusCode.BadRequest, "validation_error", "Username format is invalid")
                }
                normalized
            }
            userRepository.updateUser(userId, name, normalizedUsername, avatarUrl, parsedStatus)
                ?: throw ApiException(HttpStatusCode.NotFound, "not_found", "User not found")
        }

    fun deleteUser(actorUserId: String, isAdmin: Boolean, userId: String) {
        if (isAdmin && actorUserId == userId) {
            throw ApiException(HttpStatusCode.BadRequest, "validation_error", "Admin cannot delete themselves")
        }
        if (!isAdmin && actorUserId != userId) {
            throw ApiException(HttpStatusCode.Forbidden, "forbidden", "Cannot delete another user")
        }
        userRepository.deleteUser(userId)
    }

    fun createInviteToken(createdBy: String, serverId: String, label: String, maxUses: Int, grantedRole: String, requireApproval: Boolean) =
        inviteTokenRepository.create(
            id = UUID.randomUUID().toString(),
            token = randomTokenCode(),
            label = label.trim().also {
                if (it.isBlank()) throw ApiException(HttpStatusCode.BadRequest, "validation_error", "Label is required")
            },
            serverId = serverId,
            createdBy = createdBy,
            maxUses = maxUses.also {
                if (it < 0) throw ApiException(HttpStatusCode.BadRequest, "validation_error", "max_uses cannot be negative")
            },
            grantedRole = parseRole(grantedRole),
            requireApproval = requireApproval,
        )

    fun listInviteTokens(serverId: String) = inviteTokenRepository.listByServer(serverId)

    fun revokeInviteToken(inviteTokenId: String) = inviteTokenRepository.revoke(inviteTokenId)

    fun listJoinRequests(serverId: String) = joinRequestRepository.listPending(serverId)

    fun updateJoinRequest(requestId: String, action: String, reviewerId: String): com.callapp.server.models.JoinRequestRecord {
        val pending = joinRequestRepository.findPendingById(requestId)
            ?: throw ApiException(HttpStatusCode.NotFound, "not_found", "Join request not found")
        return when (action.uppercase()) {
            "APPROVED" -> approveJoinRequestTransactionally(pending, reviewerId)
            "DECLINED" -> {
                joinRequestRepository.decline(requestId, reviewerId)
                checkNotNull(joinRequestRepository.findSummaryById(requestId))
            }
            else -> throw ApiException(HttpStatusCode.BadRequest, "validation_error", "Invalid join request action")
        }
    }

    fun listFavorites(userId: String, serverId: String) = favoriteRepository.listFavorites(userId, serverId)

    fun addFavorite(userId: String, favoriteUserId: String) {
        if (userId == favoriteUserId) {
            throw ApiException(HttpStatusCode.BadRequest, "validation_error", "Cannot add yourself to favorites")
        }
        favoriteRepository.addFavorite(userId, favoriteUserId)
    }

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

    private fun approveJoinRequestTransactionally(
        pending: com.callapp.server.models.PendingApprovalRecord,
        reviewerId: String,
    ): com.callapp.server.models.JoinRequestRecord {
        dataSource.connection.use { connection ->
            val previousAutoCommit = connection.autoCommit
            connection.autoCommit = false
            var committed = false
            try {
                val user = userRepository.createUser(
                    connection = connection,
                    id = UUID.randomUUID().toString(),
                    username = pending.username,
                    displayName = pending.displayName,
                    passwordHash = pending.passwordHash,
                    avatarUrl = pending.avatarUrl,
                    role = pending.requestedRole,
                    serverId = pending.serverId,
                    isApproved = true,
                )
                inviteTokenRepository.incrementUsage(connection, pending.inviteTokenId)
                joinRequestRepository.approve(
                    connection = connection,
                    requestId = pending.id,
                    reviewerId = reviewerId,
                    userId = user.id,
                )
                connection.commit()
                committed = true
            } catch (error: Throwable) {
                if (!committed) {
                    connection.rollback()
                }
                throw error
            } finally {
                connection.autoCommit = previousAutoCommit
            }
            return checkNotNull(joinRequestRepository.findSummaryById(connection, pending.id))
        }
    }
}
