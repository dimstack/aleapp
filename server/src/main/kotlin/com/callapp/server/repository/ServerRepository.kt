package com.callapp.server.repository

import com.callapp.server.models.ServerRecord
import java.sql.ResultSet
import javax.sql.DataSource

class ServerRepository(
    private val dataSource: DataSource,
) {
    fun getCurrentServer(): ServerRecord? {
        dataSource.connection.use { connection ->
            connection.prepareStatement(
                """
                SELECT id, name, username, description, image_url, created_at
                FROM servers
                LIMIT 1
                """.trimIndent(),
            ).use { statement ->
                statement.executeQuery().use { rs ->
                    return if (rs.next()) rs.toServerRecord() else null
                }
            }
        }
    }

    fun update(name: String?, username: String?, description: String?, imageUrl: String?): ServerRecord? {
        dataSource.connection.use { connection ->
            connection.prepareStatement(
                """
                UPDATE servers
                SET name = COALESCE(?, name),
                    username = COALESCE(?, username),
                    description = COALESCE(?, description),
                    image_url = CASE WHEN ? IS NULL THEN image_url ELSE ? END
                WHERE id = (SELECT id FROM servers LIMIT 1)
                """.trimIndent(),
            ).use { statement ->
                statement.setString(1, name)
                statement.setString(2, username)
                statement.setString(3, description)
                statement.setString(4, imageUrl)
                statement.setString(5, imageUrl)
                statement.executeUpdate()
            }
        }
        return getCurrentServer()
    }

    fun deleteCurrentServer() {
        dataSource.connection.use { connection ->
            connection.createStatement().use { statement ->
                statement.executeUpdate("DELETE FROM favorites")
                statement.executeUpdate("DELETE FROM notifications")
                statement.executeUpdate("DELETE FROM login_attempts")
                statement.executeUpdate("DELETE FROM join_requests")
                statement.executeUpdate("DELETE FROM invite_tokens")
                statement.executeUpdate("DELETE FROM users")
                statement.executeUpdate("DELETE FROM servers")
            }
        }
    }
}

private fun ResultSet.toServerRecord(): ServerRecord = ServerRecord(
    id = getString("id"),
    name = getString("name"),
    username = getString("username"),
    description = getString("description"),
    imageUrl = getNullableString("image_url"),
    createdAt = getInstant("created_at"),
)
