package com.callapp.server.database

import java.sql.Connection
import javax.sql.DataSource

class MigrationRunner(
    private val dataSource: DataSource,
) {
    private val migrations = listOf(
        "V1__bootstrap.sql",
        "V2__auth_foundation.sql",
    )

    fun run() {
        dataSource.connection.use { connection ->
            connection.autoCommit = false
            ensureSchemaMigrationsTable(connection)

            migrations.forEach { fileName ->
                val version = fileName.substringBefore("__")
                if (isApplied(connection, version)) {
                    return@forEach
                }

                val sql = loadMigration(fileName)
                sqlStatements(sql).forEach { statementSql ->
                    connection.createStatement().use { statement ->
                        statement.executeUpdate(statementSql)
                    }
                }
                connection.prepareStatement(
                    "INSERT INTO schema_migrations(version) VALUES (?)",
                ).use { statement ->
                    statement.setString(1, version)
                    statement.executeUpdate()
                }
            }

            connection.commit()
        }
    }

    private fun ensureSchemaMigrationsTable(connection: Connection) {
        connection.createStatement().use { statement ->
            statement.executeUpdate(
                """
                CREATE TABLE IF NOT EXISTS schema_migrations (
                    version TEXT PRIMARY KEY,
                    applied_at TEXT NOT NULL DEFAULT (strftime('%Y-%m-%dT%H:%M:%fZ', 'now'))
                )
                """.trimIndent(),
            )
        }
    }

    private fun isApplied(connection: Connection, version: String): Boolean {
        connection.prepareStatement(
            "SELECT 1 FROM schema_migrations WHERE version = ? LIMIT 1",
        ).use { statement ->
            statement.setString(1, version)
            statement.executeQuery().use { resultSet ->
                return resultSet.next()
            }
        }
    }

    private fun loadMigration(fileName: String): String {
        val path = "db/migration/$fileName"
        return checkNotNull(javaClass.classLoader.getResource(path)) {
            "Missing migration resource: $path"
        }.readText()
    }

    private fun sqlStatements(sql: String): List<String> =
        sql.split(';')
            .map { it.trim() }
            .filter { it.isNotEmpty() }
}
