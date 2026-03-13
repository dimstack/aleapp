package com.callapp.server.repository

import com.callapp.server.models.JoinRequestRecord
import com.callapp.server.models.JoinRequestStatus
import com.callapp.server.models.NotificationType
import com.callapp.server.models.PendingApprovalRecord
import com.callapp.server.models.Role
import java.sql.Connection
import java.sql.ResultSet
import java.util.UUID
import javax.sql.DataSource

class JoinRequestRepository(
    private val dataSource: DataSource,
) {
    fun listPending(serverId: String): List<JoinRequestRecord> {
        dataSource.connection.use { connection ->
            connection.prepareStatement(
                """
                SELECT id, display_name, username, server_id, status, created_at
                FROM join_requests
                WHERE server_id = ? AND status = 'PENDING'
                ORDER BY created_at ASC
                """.trimIndent(),
            ).use { statement ->
                statement.setString(1, serverId)
                statement.executeQuery().use { rs ->
                    return buildList {
                        while (rs.next()) add(rs.toJoinRequestRecord())
                    }
                }
            }
        }
    }

    fun findPendingById(requestId: String): PendingApprovalRecord? {
        dataSource.connection.use { connection ->
            connection.prepareStatement(
                """
                SELECT id, username, display_name, password_hash, avatar_url, invite_token_id,
                       server_id, requested_role, status, created_at
                FROM join_requests
                WHERE id = ? AND status = 'PENDING'
                LIMIT 1
                """.trimIndent(),
            ).use { statement ->
                statement.setString(1, requestId)
                statement.executeQuery().use { rs ->
                    return if (rs.next()) rs.toPendingApprovalRecord() else null
                }
            }
        }
    }

    fun create(
        username: String,
        displayName: String,
        passwordHash: String,
        avatarUrl: String?,
        inviteTokenId: String,
        serverId: String,
        requestedRole: Role,
    ): JoinRequestRecord {
        val id = UUID.randomUUID().toString()
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
        return checkNotNull(findSummaryById(id))
    }

    fun approve(requestId: String, reviewerId: String, userId: String) {
        dataSource.connection.use { connection ->
            approve(connection, requestId, reviewerId, userId)
        }
    }

    fun approve(connection: Connection, requestId: String, reviewerId: String, userId: String) {
        connection.prepareStatement(
            """
            UPDATE join_requests
            SET status = 'APPROVED', reviewed_at = strftime('%Y-%m-%dT%H:%M:%fZ', 'now'), reviewed_by = ?
            WHERE id = ?
            """.trimIndent(),
        ).use { statement ->
            statement.setString(1, reviewerId)
            statement.setString(2, requestId)
            statement.executeUpdate()
        }
        connection.prepareStatement(
            """
            INSERT INTO notifications(id, user_id, type, server_name, message, is_read, created_at)
            SELECT ?, ?, 'REQUEST_APPROVED', s.name, 'Join request approved', 0, strftime('%Y-%m-%dT%H:%M:%fZ', 'now')
            FROM servers s
            LIMIT 1
            """.trimIndent(),
        ).use { statement ->
            statement.setString(1, UUID.randomUUID().toString())
            statement.setString(2, userId)
            statement.executeUpdate()
        }
    }

    fun decline(requestId: String, reviewerId: String) {
        dataSource.connection.use { connection ->
            connection.prepareStatement(
                """
                UPDATE join_requests
                SET status = 'DECLINED', reviewed_at = strftime('%Y-%m-%dT%H:%M:%fZ', 'now'), reviewed_by = ?
                WHERE id = ?
                """.trimIndent(),
            ).use { statement ->
                statement.setString(1, reviewerId)
                statement.setString(2, requestId)
                statement.executeUpdate()
            }
        }
    }

    fun findSummaryById(requestId: String): JoinRequestRecord? {
        dataSource.connection.use { connection ->
            return findSummaryById(connection, requestId)
        }
    }

    fun findSummaryById(connection: Connection, requestId: String): JoinRequestRecord? {
        connection.prepareStatement(
            """
            SELECT id, display_name, username, server_id, status, created_at
            FROM join_requests
            WHERE id = ?
            LIMIT 1
            """.trimIndent(),
        ).use { statement ->
            statement.setString(1, requestId)
            statement.executeQuery().use { rs ->
                return if (rs.next()) rs.toJoinRequestRecord() else null
            }
        }
    }
}

private fun ResultSet.toJoinRequestRecord(): JoinRequestRecord = JoinRequestRecord(
    id = getString("id"),
    userName = getString("display_name"),
    username = getString("username"),
    serverId = getString("server_id"),
    status = JoinRequestStatus.valueOf(getString("status")),
    createdAt = getInstant("created_at"),
)

private fun ResultSet.toPendingApprovalRecord(): PendingApprovalRecord = PendingApprovalRecord(
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
