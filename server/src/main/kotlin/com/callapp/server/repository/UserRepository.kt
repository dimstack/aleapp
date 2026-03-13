package com.callapp.server.repository

import com.callapp.server.models.Role
import com.callapp.server.models.JoinRequestStatus
import com.callapp.server.models.PendingApprovalRecord
import com.callapp.server.models.UserRecord
import com.callapp.server.models.UserStatus
import java.sql.ResultSet
import java.time.Instant
import javax.sql.DataSource

class UserRepository(
    private val dataSource: DataSource,
) {
    fun findById(userId: String): UserRecord? {
        dataSource.connection.use { connection ->
            connection.prepareStatement(
                """
                SELECT id, username, display_name, password_hash, avatar_url, role, status,
                       server_id, is_approved, created_at, updated_at, last_seen_at, lockout_until
                FROM users
                WHERE id = ?
                LIMIT 1
                """.trimIndent(),
            ).use { statement ->
                statement.setString(1, userId)
                statement.executeQuery().use { rs ->
                    return if (rs.next()) rs.toUserRecord() else null
                }
            }
        }
    }

    fun findByUsername(serverId: String, username: String): UserRecord? {
        dataSource.connection.use { connection ->
            connection.prepareStatement(
                """
                SELECT id, username, display_name, password_hash, avatar_url, role, status,
                       server_id, is_approved, created_at, updated_at, last_seen_at, lockout_until
                FROM users
                WHERE server_id = ? AND username = ?
                LIMIT 1
                """.trimIndent(),
            ).use { statement ->
                statement.setString(1, serverId)
                statement.setString(2, username)
                statement.executeQuery().use { rs ->
                    return if (rs.next()) rs.toUserRecord() else null
                }
            }
        }
    }

    fun findPendingJoinRequest(serverId: String, username: String): PendingJoinRequestRecord? {
        dataSource.connection.use { connection ->
            connection.prepareStatement(
                """
                SELECT id, username, display_name, password_hash, avatar_url, invite_token_id, server_id,
                       requested_role, status, created_at
                FROM join_requests
                WHERE server_id = ? AND username = ? AND status = 'PENDING'
                LIMIT 1
                """.trimIndent(),
            ).use { statement ->
                statement.setString(1, serverId)
                statement.setString(2, username)
                statement.executeQuery().use { rs ->
                    return if (rs.next()) rs.toPendingJoinRequestRecord() else null
                }
            }
        }
    }

    fun createUser(
        id: String,
        username: String,
        displayName: String,
        passwordHash: String,
        avatarUrl: String?,
        role: Role,
        serverId: String,
        isApproved: Boolean,
    ): UserRecord {
        dataSource.connection.use { connection ->
            connection.prepareStatement(
                """
                INSERT INTO users(
                    id, username, display_name, password_hash, avatar_url, role, status,
                    server_id, is_approved, created_at, updated_at
                )
                VALUES (?, ?, ?, ?, ?, ?, 'ONLINE', ?, ?, strftime('%Y-%m-%dT%H:%M:%fZ', 'now'),
                        strftime('%Y-%m-%dT%H:%M:%fZ', 'now'))
                """.trimIndent(),
            ).use { statement ->
                statement.setString(1, id)
                statement.setString(2, username)
                statement.setString(3, displayName)
                statement.setString(4, passwordHash)
                statement.setString(5, avatarUrl)
                statement.setString(6, role.name)
                statement.setString(7, serverId)
                statement.setInt(8, if (isApproved) 1 else 0)
                statement.executeUpdate()
            }
        }
        return checkNotNull(findById(id))
    }

    fun createJoinRequest(
        id: String,
        username: String,
        displayName: String,
        passwordHash: String,
        avatarUrl: String?,
        inviteTokenId: String,
        serverId: String,
        requestedRole: Role,
    ) {
        dataSource.connection.use { connection ->
            connection.prepareStatement(
                """
                INSERT INTO join_requests(
                    id, username, display_name, password_hash, avatar_url, invite_token_id,
                    server_id, requested_role, status, created_at
                )
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, 'PENDING', strftime('%Y-%m-%dT%H:%M:%fZ', 'now'))
                """.trimIndent(),
            ).use { statement ->
                statement.setString(1, id)
                statement.setString(2, username)
                statement.setString(3, displayName)
                statement.setString(4, passwordHash)
                statement.setString(5, avatarUrl)
                statement.setString(6, inviteTokenId)
                statement.setString(7, serverId)
                statement.setString(8, requestedRole.name)
                statement.executeUpdate()
            }
        }
    }

    fun listByServer(serverId: String): List<UserRecord> {
        dataSource.connection.use { connection ->
            connection.prepareStatement(
                """
                SELECT id, username, display_name, password_hash, avatar_url, role, status,
                       server_id, is_approved, created_at, updated_at, last_seen_at, lockout_until
                FROM users
                WHERE server_id = ?
                ORDER BY display_name, username
                """.trimIndent(),
            ).use { statement ->
                statement.setString(1, serverId)
                statement.executeQuery().use { rs ->
                    return buildList {
                        while (rs.next()) add(rs.toUserRecord())
                    }
                }
            }
        }
    }

    fun updateUser(
        userId: String,
        name: String?,
        username: String?,
        avatarUrl: String?,
        status: UserStatus?,
    ): UserRecord? {
        dataSource.connection.use { connection ->
            connection.prepareStatement(
                """
                UPDATE users
                SET display_name = COALESCE(?, display_name),
                    username = COALESCE(?, username),
                    avatar_url = CASE WHEN ? IS NULL THEN avatar_url ELSE ? END,
                    status = COALESCE(?, status),
                    updated_at = strftime('%Y-%m-%dT%H:%M:%fZ', 'now')
                WHERE id = ?
                """.trimIndent(),
            ).use { statement ->
                statement.setString(1, name)
                statement.setString(2, username)
                statement.setString(3, avatarUrl)
                statement.setString(4, avatarUrl)
                statement.setString(5, status?.name)
                statement.setString(6, userId)
                statement.executeUpdate()
            }
        }
        return findById(userId)
    }

    fun deleteUser(userId: String) {
        dataSource.connection.use { connection ->
            connection.prepareStatement("DELETE FROM favorites WHERE user_id = ? OR favorite_user_id = ?").use { statement ->
                statement.setString(1, userId)
                statement.setString(2, userId)
                statement.executeUpdate()
            }
            connection.prepareStatement("DELETE FROM notifications WHERE user_id = ?").use { statement ->
                statement.setString(1, userId)
                statement.executeUpdate()
            }
            connection.prepareStatement("DELETE FROM users WHERE id = ?").use { statement ->
                statement.setString(1, userId)
                statement.executeUpdate()
            }
        }
    }
}

internal fun ResultSet.toUserRecord(): UserRecord = UserRecord(
    id = getString("id"),
    username = getString("username"),
    displayName = getString("display_name"),
    passwordHash = getString("password_hash"),
    avatarUrl = getNullableString("avatar_url"),
    role = Role.valueOf(getString("role")),
    status = UserStatus.valueOf(getString("status")),
    serverId = getString("server_id"),
    isApproved = getInt("is_approved") == 1,
    createdAt = getInstant("created_at"),
    updatedAt = getInstant("updated_at"),
    lastSeenAt = getNullableInstant("last_seen_at"),
    lockoutUntil = getNullableInstant("lockout_until"),
)

data class PendingJoinRequestRecord(
    val id: String,
    val username: String,
    val displayName: String,
    val passwordHash: String,
    val avatarUrl: String?,
    val inviteTokenId: String,
    val serverId: String,
    val requestedRole: Role,
    val status: JoinRequestStatus,
    val createdAt: Instant,
)

private fun ResultSet.toPendingJoinRequestRecord(): PendingJoinRequestRecord = PendingJoinRequestRecord(
    id = getString("id"),
    username = getString("username"),
    displayName = getString("display_name"),
    passwordHash = getString("password_hash"),
    avatarUrl = getNullableString("avatar_url"),
    inviteTokenId = getString("invite_token_id"),
    serverId = getString("server_id"),
    requestedRole = Role.valueOf(getString("requested_role")),
    status = JoinRequestStatus.valueOf(getString("status")),
    createdAt = getInstant("created_at"),
)
