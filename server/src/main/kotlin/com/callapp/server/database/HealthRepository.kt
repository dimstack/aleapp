package com.callapp.server.database

import kotlinx.serialization.Serializable
import javax.sql.DataSource

class HealthRepository(
    private val dataSource: DataSource,
) {
    fun probe(): DatabaseHealth {
        return runCatching {
            dataSource.connection.use { connection ->
                connection.createStatement().use { statement ->
                    statement.executeQuery("SELECT 1").use { resultSet ->
                        resultSet.next()
                    }
                }
            }
            DatabaseHealth(connected = true)
        }.getOrElse { error ->
            DatabaseHealth(
                connected = false,
                error = error.message,
            )
        }
    }
}

@Serializable
data class DatabaseHealth(
    val connected: Boolean,
    val error: String? = null,
)
