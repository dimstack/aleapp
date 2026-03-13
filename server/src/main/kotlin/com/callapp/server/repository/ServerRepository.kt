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
}

private fun ResultSet.toServerRecord(): ServerRecord = ServerRecord(
    id = getString("id"),
    name = getString("name"),
    username = getString("username"),
    description = getString("description"),
    imageUrl = getNullableString("image_url"),
    createdAt = getInstant("created_at"),
)
