package com.callapp.server

import com.callapp.server.models.UserStatus
import com.callapp.server.service.PasswordService
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.server.config.MapApplicationConfig
import io.ktor.server.testing.ApplicationTestBuilder
import io.ktor.server.testing.testApplication
import java.io.File
import java.sql.DriverManager
import java.time.Instant
import java.util.UUID
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

internal val testJson = Json { ignoreUnknownKeys = true }

internal fun testWithDatabase(
    prefix: String,
    block: suspend ApplicationTestBuilder.(String) -> Unit,
) = testApplication {
    val dbFile = File("build/$prefix-${UUID.randomUUID()}.db")
    environment {
        config = MapApplicationConfig(
            "callapp.environment" to "test",
            "callapp.database.path" to dbFile.absolutePath,
            "callapp.database.maximumPoolSize" to "1",
            "callapp.server.id" to "test-server",
            "callapp.server.name" to "Test Server",
            "callapp.server.username" to "@test",
            "callapp.server.description" to "Test description",
            "callapp.security.jwtSecret" to "test-secret",
            "callapp.security.issuer" to "test-issuer",
            "callapp.security.audience" to "test-audience",
            "callapp.security.guestTokenTtlMinutes" to "30",
            "callapp.security.userTokenTtlDays" to "30",
            "callapp.turn.host" to "localhost",
            "callapp.turn.port" to "3478",
            "callapp.turn.secret" to "turn-secret",
            "callapp.turn.realm" to "callapp-test",
            "callapp.turn.ttlSeconds" to "3600",
        )
    }
    block(dbFile.absolutePath)
}

internal suspend fun ApplicationTestBuilder.login(inviteToken: String, username: String, password: String): String {
    val response = client.post("/api/auth/login") {
        header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
        setBody("""{"invite_token":"$inviteToken","username":"$username","password":"$password"}""")
    }
    return testJson.parseToJsonElement(response.bodyAsText()).jsonObject["session_token"]!!.jsonPrimitive.content
}

internal fun seedInviteToken(
    dbPath: String,
    token: String,
    serverId: String = "test-server",
    label: String = "Seed Token",
    maxUses: Int? = 0,
    currentUses: Int = 0,
    grantedRole: String = "MEMBER",
    requireApproval: Boolean = false,
    isRevoked: Boolean = false,
    expiresAt: Instant? = null,
): String {
    val inviteTokenId = UUID.randomUUID().toString()
    DriverManager.getConnection("jdbc:sqlite:$dbPath").use { connection ->
        connection.prepareStatement(
            """
            INSERT INTO invite_tokens(
                id, token, label, server_id, created_by, max_uses, current_uses, granted_role,
                require_approval, expires_at, is_revoked, created_at
            )
            VALUES (?, ?, ?, ?, NULL, ?, ?, ?, ?, ?, ?, strftime('%Y-%m-%dT%H:%M:%fZ', 'now'))
            """.trimIndent(),
        ).use { statement ->
            statement.setString(1, inviteTokenId)
            statement.setString(2, token)
            statement.setString(3, label)
            statement.setString(4, serverId)
            if (maxUses == null) {
                statement.setObject(5, null)
            } else {
                statement.setInt(5, maxUses)
            }
            statement.setInt(6, currentUses)
            statement.setString(7, grantedRole)
            statement.setInt(8, if (requireApproval) 1 else 0)
            statement.setString(9, expiresAt?.toString())
            statement.setInt(10, if (isRevoked) 1 else 0)
            statement.executeUpdate()
        }
    }
    return inviteTokenId
}

internal fun seedUser(
    dbPath: String,
    username: String,
    password: String,
    role: String = "MEMBER",
    displayName: String = username.removePrefix("@").replaceFirstChar { it.uppercase() },
    status: UserStatus = UserStatus.ONLINE,
    serverId: String = "test-server",
    isApproved: Boolean = true,
): String {
    val userId = UUID.randomUUID().toString()
    DriverManager.getConnection("jdbc:sqlite:$dbPath").use { connection ->
        connection.prepareStatement(
            """
            INSERT INTO users(
                id, username, display_name, password_hash, avatar_url, role, status,
                server_id, is_approved, created_at, updated_at
            )
            VALUES (?, ?, ?, ?, NULL, ?, ?, ?, ?,
                    strftime('%Y-%m-%dT%H:%M:%fZ', 'now'), strftime('%Y-%m-%dT%H:%M:%fZ', 'now'))
            """.trimIndent(),
        ).use { statement ->
            statement.setString(1, userId)
            statement.setString(2, username)
            statement.setString(3, displayName)
            statement.setString(4, PasswordService().hash(password))
            statement.setString(5, role)
            statement.setString(6, status.name)
            statement.setString(7, serverId)
            statement.setInt(8, if (isApproved) 1 else 0)
            statement.executeUpdate()
        }
    }
    return userId
}

internal fun seedNotification(
    dbPath: String,
    userId: String,
    type: String,
    serverName: String,
    message: String,
    isRead: Boolean = false,
) {
    DriverManager.getConnection("jdbc:sqlite:$dbPath").use { connection ->
        connection.prepareStatement(
            """
            INSERT INTO notifications(id, user_id, type, server_name, message, is_read, created_at)
            VALUES (?, ?, ?, ?, ?, ?, strftime('%Y-%m-%dT%H:%M:%fZ', 'now'))
            """.trimIndent(),
        ).use { statement ->
            statement.setString(1, UUID.randomUUID().toString())
            statement.setString(2, userId)
            statement.setString(3, type)
            statement.setString(4, serverName)
            statement.setString(5, message)
            statement.setInt(6, if (isRead) 1 else 0)
            statement.executeUpdate()
        }
    }
}

internal fun countInviteTokenUses(dbPath: String, token: String): Int =
    DriverManager.getConnection("jdbc:sqlite:$dbPath").use { connection ->
        connection.prepareStatement("SELECT current_uses FROM invite_tokens WHERE token = ?").use { statement ->
            statement.setString(1, token)
            statement.executeQuery().use { rs ->
                check(rs.next())
                rs.getInt("current_uses")
            }
        }
    }

internal fun countNotifications(dbPath: String, userId: String): Int =
    DriverManager.getConnection("jdbc:sqlite:$dbPath").use { connection ->
        connection.prepareStatement("SELECT COUNT(*) FROM notifications WHERE user_id = ?").use { statement ->
            statement.setString(1, userId)
            statement.executeQuery().use { rs ->
                check(rs.next())
                rs.getInt(1)
            }
        }
    }

internal fun countUnreadNotifications(dbPath: String, userId: String): Int =
    DriverManager.getConnection("jdbc:sqlite:$dbPath").use { connection ->
        connection.prepareStatement("SELECT COUNT(*) FROM notifications WHERE user_id = ? AND is_read = 0").use { statement ->
            statement.setString(1, userId)
            statement.executeQuery().use { rs ->
                check(rs.next())
                rs.getInt(1)
            }
        }
    }

internal fun seedLoginAttempt(
    dbPath: String,
    serverId: String = "test-server",
    username: String,
    failedAttempts: Int,
    lockedUntil: Instant?,
) {
    DriverManager.getConnection("jdbc:sqlite:$dbPath").use { connection ->
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
