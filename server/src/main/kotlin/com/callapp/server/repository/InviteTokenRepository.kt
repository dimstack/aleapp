package com.callapp.server.repository

import com.callapp.server.models.InviteTokenRecord
import com.callapp.server.models.Role
import com.callapp.server.routes.ApiException
import io.ktor.http.HttpStatusCode
import java.sql.Connection
import java.sql.ResultSet
import javax.sql.DataSource

class InviteTokenRepository(
    private val dataSource: DataSource,
) {
    fun findByToken(token: String): InviteTokenRecord? {
        dataSource.connection.use { connection ->
            connection.prepareStatement(
                """
                SELECT id, token, label, server_id, created_by, max_uses, current_uses,
                       granted_role, require_approval, expires_at, is_revoked, created_at
                FROM invite_tokens
                WHERE token = ?
                LIMIT 1
                """.trimIndent(),
            ).use { statement ->
                statement.setString(1, token)
                statement.executeQuery().use { rs ->
                    return if (rs.next()) rs.toInviteTokenRecord() else null
                }
            }
        }
    }

    fun incrementUsage(inviteTokenId: String) {
        dataSource.connection.use { connection ->
            incrementUsage(connection, inviteTokenId)
        }
    }

    fun incrementUsage(connection: Connection, inviteTokenId: String) {
        connection.prepareStatement(
            """
            UPDATE invite_tokens
            SET current_uses = current_uses + 1
            WHERE id = ?
              AND is_revoked = 0
              AND (max_uses IS NULL OR max_uses = 0 OR current_uses < max_uses)
            """.trimIndent(),
        ).use { statement ->
            statement.setString(1, inviteTokenId)
            val updatedRows = statement.executeUpdate()
            if (updatedRows != 1) {
                throw ApiException(
                    HttpStatusCode.Conflict,
                    "invite_token_exhausted",
                    "Invite token cannot accept more users",
                )
            }
        }
    }

    fun listByServer(serverId: String): List<InviteTokenRecord> {
        dataSource.connection.use { connection ->
            connection.prepareStatement(
                """
                SELECT id, token, label, server_id, created_by, max_uses, current_uses,
                       granted_role, require_approval, expires_at, is_revoked, created_at
                FROM invite_tokens
                WHERE server_id = ?
                ORDER BY created_at DESC
                """.trimIndent(),
            ).use { statement ->
                statement.setString(1, serverId)
                statement.executeQuery().use { rs ->
                    return buildList {
                        while (rs.next()) add(rs.toInviteTokenRecord())
                    }
                }
            }
        }
    }

    fun create(
        id: String,
        token: String,
        label: String,
        serverId: String,
        createdBy: String?,
        maxUses: Int,
        grantedRole: Role,
        requireApproval: Boolean,
    ): InviteTokenRecord {
        dataSource.connection.use { connection ->
            connection.prepareStatement(
                """
                INSERT INTO invite_tokens(
                    id, token, label, server_id, created_by, max_uses, current_uses,
                    granted_role, require_approval, is_revoked, created_at
                )
                VALUES (?, ?, ?, ?, ?, ?, 0, ?, ?, 0, strftime('%Y-%m-%dT%H:%M:%fZ', 'now'))
                """.trimIndent(),
            ).use { statement ->
                statement.setString(1, id)
                statement.setString(2, token)
                statement.setString(3, label)
                statement.setString(4, serverId)
                statement.setString(5, createdBy)
                statement.setInt(6, maxUses)
                statement.setString(7, grantedRole.name)
                statement.setInt(8, if (requireApproval) 1 else 0)
                statement.executeUpdate()
            }
        }
        return checkNotNull(findById(id))
    }

    fun revoke(inviteTokenId: String) {
        dataSource.connection.use { connection ->
            connection.prepareStatement(
                "UPDATE invite_tokens SET is_revoked = 1 WHERE id = ?",
            ).use { statement ->
                statement.setString(1, inviteTokenId)
                statement.executeUpdate()
            }
        }
    }

    fun findById(inviteTokenId: String): InviteTokenRecord? {
        dataSource.connection.use { connection ->
            connection.prepareStatement(
                """
                SELECT id, token, label, server_id, created_by, max_uses, current_uses,
                       granted_role, require_approval, expires_at, is_revoked, created_at
                FROM invite_tokens
                WHERE id = ?
                LIMIT 1
                """.trimIndent(),
            ).use { statement ->
                statement.setString(1, inviteTokenId)
                statement.executeQuery().use { rs ->
                    return if (rs.next()) rs.toInviteTokenRecord() else null
                }
            }
        }
    }
}

private fun ResultSet.toInviteTokenRecord(): InviteTokenRecord = InviteTokenRecord(
    id = getString("id"),
    token = getString("token"),
    label = getString("label"),
    serverId = getString("server_id"),
    createdBy = getNullableString("created_by"),
    maxUses = getInt("max_uses"),
    currentUses = getInt("current_uses"),
    grantedRole = Role.valueOf(getString("granted_role")),
    requireApproval = getInt("require_approval") == 1,
    expiresAt = getNullableInstant("expires_at"),
    isRevoked = getInt("is_revoked") == 1,
    createdAt = getInstant("created_at"),
)
