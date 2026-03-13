package com.callapp.server.config

import io.ktor.server.config.ApplicationConfig

data class AppConfig(
    val environment: String,
    val database: DatabaseConfig,
    val server: ServerConfig,
    val security: SecurityConfig,
    val turn: TurnConfig,
) {
    companion object {
        fun from(config: ApplicationConfig): AppConfig = AppConfig(
            environment = config.property("callapp.environment").getString(),
            database = DatabaseConfig(
                path = config.property("callapp.database.path").getString(),
                maximumPoolSize = config.property("callapp.database.maximumPoolSize").getString().toInt(),
            ),
            server = ServerConfig(
                id = config.property("callapp.server.id").getString(),
                name = config.property("callapp.server.name").getString(),
                username = config.property("callapp.server.username").getString(),
                description = config.property("callapp.server.description").getString(),
                imageUrl = config.propertyOrNull("callapp.server.imageUrl")?.getString(),
            ),
            security = SecurityConfig(
                jwtSecret = config.property("callapp.security.jwtSecret").getString(),
                issuer = config.property("callapp.security.issuer").getString(),
                audience = config.property("callapp.security.audience").getString(),
                guestTokenTtlMinutes = config.property("callapp.security.guestTokenTtlMinutes").getString().toLong(),
                userTokenTtlDays = config.property("callapp.security.userTokenTtlDays").getString().toLong(),
            ),
            turn = TurnConfig(
                secret = config.property("callapp.turn.secret").getString(),
                realm = config.property("callapp.turn.realm").getString(),
            ),
        )
    }
}

data class DatabaseConfig(
    val path: String,
    val maximumPoolSize: Int,
)

data class ServerConfig(
    val id: String,
    val name: String,
    val username: String,
    val description: String,
    val imageUrl: String?,
)

data class SecurityConfig(
    val jwtSecret: String,
    val issuer: String,
    val audience: String,
    val guestTokenTtlMinutes: Long,
    val userTokenTtlDays: Long,
)

data class TurnConfig(
    val secret: String,
    val realm: String,
)
