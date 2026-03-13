package com.callapp.server.database

import com.callapp.server.config.DatabaseConfig
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import java.io.File
import javax.sql.DataSource

class DatabaseFactory(private val config: DatabaseConfig) {

    fun createDataSource(): DataSource {
        val dbFile = File(config.path).absoluteFile
        dbFile.parentFile?.mkdirs()

        val hikariConfig = HikariConfig().apply {
            jdbcUrl = "jdbc:sqlite:${dbFile.path}"
            driverClassName = "org.sqlite.JDBC"
            maximumPoolSize = config.maximumPoolSize
            isAutoCommit = true
            validate()
        }

        return HikariDataSource(hikariConfig)
    }
}
