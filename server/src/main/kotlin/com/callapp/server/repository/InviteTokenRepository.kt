package com.callapp.server.repository

import com.callapp.server.models.InviteTokenRecord
import com.callapp.server.models.Role
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
            connection.prepareStatement(
                """
                UPDATE invite_tokens
                SET current_uses = current_uses + 1
                WHERE id = ?
                """.trimIndent(),
            ).use { statement ->
                statement.setString(1, inviteTokenId)
                statement.executeUpdate()
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
