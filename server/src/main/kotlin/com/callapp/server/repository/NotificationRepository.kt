package com.callapp.server.repository

import com.callapp.server.models.NotificationRecord
import com.callapp.server.models.NotificationType
import com.callapp.server.repository.getInstant
import java.sql.ResultSet
import javax.sql.DataSource

class NotificationRepository(
    private val dataSource: DataSource,
) {
    fun listByUser(userId: String): List<NotificationRecord> {
        dataSource.connection.use { connection ->
            connection.prepareStatement(
                """
                SELECT id, user_id, type, server_name, message, actor_user_id, actor_username, actor_display_name, actor_avatar_url, is_read, created_at
                FROM notifications
                WHERE user_id = ?
                ORDER BY created_at DESC
                """.trimIndent(),
            ).use { statement ->
                statement.setString(1, userId)
                statement.executeQuery().use { rs ->
                    return buildList {
                        while (rs.next()) add(rs.toNotificationRecord())
                    }
                }
            }
        }
    }

    fun create(
        userId: String,
        type: NotificationType,
        serverName: String,
        message: String,
        actorUserId: String? = null,
        actorUsername: String? = null,
        actorDisplayName: String? = null,
        actorAvatarUrl: String? = null,
    ) {
        dataSource.connection.use { connection ->
            connection.prepareStatement(
                """
                INSERT INTO notifications(
                    id, user_id, type, server_name, message, actor_user_id, actor_username, actor_display_name, actor_avatar_url, is_read, created_at
                )
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, 0, strftime('%Y-%m-%dT%H:%M:%fZ', 'now'))
                """.trimIndent(),
            ).use { statement ->
                statement.setString(1, java.util.UUID.randomUUID().toString())
                statement.setString(2, userId)
                statement.setString(3, type.name)
                statement.setString(4, serverName)
                statement.setString(5, message)
                statement.setString(6, actorUserId)
                statement.setString(7, actorUsername)
                statement.setString(8, actorDisplayName)
                statement.setString(9, actorAvatarUrl)
                statement.executeUpdate()
            }
        }
    }

    fun markAllRead(userId: String) {
        dataSource.connection.use { connection ->
            connection.prepareStatement(
                "UPDATE notifications SET is_read = 1 WHERE user_id = ?",
            ).use { statement ->
                statement.setString(1, userId)
                statement.executeUpdate()
            }
        }
    }

    fun clearAll(userId: String) {
        dataSource.connection.use { connection ->
            connection.prepareStatement(
                "DELETE FROM notifications WHERE user_id = ?",
            ).use { statement ->
                statement.setString(1, userId)
                statement.executeUpdate()
            }
        }
    }
}

private fun ResultSet.toNotificationRecord(): NotificationRecord = NotificationRecord(
    id = getString("id"),
    userId = getString("user_id"),
    type = NotificationType.valueOf(getString("type")),
    serverName = getString("server_name"),
    message = getString("message"),
    actorUserId = getString("actor_user_id"),
    actorUsername = getString("actor_username"),
    actorDisplayName = getString("actor_display_name"),
    actorAvatarUrl = getString("actor_avatar_url"),
    isRead = getInt("is_read") == 1,
    createdAt = getInstant("created_at"),
)
