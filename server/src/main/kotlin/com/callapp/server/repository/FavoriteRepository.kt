package com.callapp.server.repository

import com.callapp.server.models.UserRecord
import javax.sql.DataSource

class FavoriteRepository(
    private val dataSource: DataSource,
) {
    fun listFavorites(userId: String, serverId: String): List<UserRecord> {
        dataSource.connection.use { connection ->
            connection.prepareStatement(
                """
                SELECT u.id, u.username, u.display_name, u.password_hash, u.avatar_url, u.role, u.status,
                       u.server_id, u.is_approved, u.created_at, u.updated_at, u.last_seen_at, u.lockout_until
                FROM favorites f
                JOIN users u ON u.id = f.favorite_user_id
                WHERE f.user_id = ? AND u.server_id = ?
                ORDER BY u.display_name, u.username
                """.trimIndent(),
            ).use { statement ->
                statement.setString(1, userId)
                statement.setString(2, serverId)
                statement.executeQuery().use { rs ->
                    return buildList {
                        while (rs.next()) add(rs.toUserRecord())
                    }
                }
            }
        }
    }

    fun addFavorite(userId: String, favoriteUserId: String) {
        dataSource.connection.use { connection ->
            connection.prepareStatement(
                """
                INSERT OR IGNORE INTO favorites(user_id, favorite_user_id, created_at)
                VALUES (?, ?, strftime('%Y-%m-%dT%H:%M:%fZ', 'now'))
                """.trimIndent(),
            ).use { statement ->
                statement.setString(1, userId)
                statement.setString(2, favoriteUserId)
                statement.executeUpdate()
            }
        }
    }

    fun removeFavorite(userId: String, favoriteUserId: String) {
        dataSource.connection.use { connection ->
            connection.prepareStatement(
                "DELETE FROM favorites WHERE user_id = ? AND favorite_user_id = ?",
            ).use { statement ->
                statement.setString(1, userId)
                statement.setString(2, favoriteUserId)
                statement.executeUpdate()
            }
        }
    }
}
