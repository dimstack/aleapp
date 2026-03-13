package com.callapp.server.database

import com.callapp.server.config.ServerConfig
import javax.sql.DataSource

class ServerBootstrap(
    private val dataSource: DataSource,
    private val config: ServerConfig,
) {
    fun ensureServerRow() {
        dataSource.connection.use { connection ->
            connection.prepareStatement(
                """
                INSERT INTO servers(id, name, username, description, image_url)
                SELECT ?, ?, ?, ?, ?
                WHERE NOT EXISTS (SELECT 1 FROM servers LIMIT 1)
                """.trimIndent(),
            ).use { statement ->
                statement.setString(1, config.id)
                statement.setString(2, config.name)
                statement.setString(3, config.username)
                statement.setString(4, config.description)
                statement.setString(5, config.imageUrl)
                statement.executeUpdate()
            }
        }
    }
}
