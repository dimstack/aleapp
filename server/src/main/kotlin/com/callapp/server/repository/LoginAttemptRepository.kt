package com.callapp.server.repository

import com.callapp.server.models.LoginAttemptRecord
import java.sql.ResultSet
import java.time.Instant
import javax.sql.DataSource

class LoginAttemptRepository(
    private val dataSource: DataSource,
) {
    fun find(serverId: String, username: String): LoginAttemptRecord? {
        dataSource.connection.use { connection ->
            connection.prepareStatement(
                """
                SELECT server_id, username, failed_attempts, locked_until, updated_at
                FROM login_attempts
                WHERE server_id = ? AND username = ?
                LIMIT 1
                """.trimIndent(),
            ).use { statement ->
                statement.setString(1, serverId)
                statement.setString(2, username)
                statement.executeQuery().use { rs ->
                    return if (rs.next()) rs.toLoginAttemptRecord() else null
                }
            }
        }
    }

    fun upsert(serverId: String, username: String, failedAttempts: Int, lockedUntil: Instant?) {
        dataSource.connection.use { connection ->
            connection.prepareStatement(
                """
                INSERT INTO login_attempts(server_id, username, failed_attempts, locked_until, updated_at)
                VALUES (?, ?, ?, ?, strftime('%Y-%m-%dT%H:%M:%fZ', 'now'))
                ON CONFLICT(server_id, username)
                DO UPDATE SET
                    failed_attempts = excluded.failed_attempts,
                    locked_until = excluded.locked_until,
                    updated_at = strftime('%Y-%m-%dT%H:%M:%fZ', 'now')
                """.trimIndent(),
            ).use { statement ->
                statement.setString(1, serverId)
                statement.setString(2, username)
                statement.setInt(3, failedAttempts)
                statement.setString(4, lockedUntil?.toString())
                statement.executeUpdate()
            }
        }
    }

    fun reset(serverId: String, username: String) {
        dataSource.connection.use { connection ->
            connection.prepareStatement(
                "DELETE FROM login_attempts WHERE server_id = ? AND username = ?",
            ).use { statement ->
                statement.setString(1, serverId)
                statement.setString(2, username)
                statement.executeUpdate()
            }
        }
    }
}

private fun ResultSet.toLoginAttemptRecord(): LoginAttemptRecord = LoginAttemptRecord(
    serverId = getString("server_id"),
    username = getString("username"),
    failedAttempts = getInt("failed_attempts"),
    lockedUntil = getNullableInstant("locked_until"),
    updatedAt = getInstant("updated_at"),
)
