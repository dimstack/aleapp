package com.callapp.server

import io.ktor.client.request.get
import io.ktor.server.config.MapApplicationConfig
import io.ktor.server.testing.testApplication
import java.io.File
import java.sql.DriverManager
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class BootstrapAdminInviteTokenTest {

    @Test
    fun startupSeedsAdminInviteTokenFromEnvironment() = testApplication {
        val dbFile = File("build/test-bootstrap-${UUID.randomUUID()}.db")

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
                "callapp.bootstrap.adminInviteToken" to "ADMIN123",
                "callapp.bootstrap.adminInviteLabel" to "Bootstrap admin invite",
                "callapp.bootstrap.adminInviteMaxUses" to "1",
            )
        }

        application {
            module()
        }

        client.get("/health")

        DriverManager.getConnection("jdbc:sqlite:${dbFile.absolutePath}").use { connection ->
            connection.prepareStatement(
                """
                SELECT token, granted_role, require_approval, max_uses
                FROM invite_tokens
                WHERE token = ?
                """.trimIndent(),
            ).use { statement ->
                statement.setString(1, "ADMIN123")
                statement.executeQuery().use { rs ->
                    assertTrue(rs.next())
                    assertEquals("ADMIN123", rs.getString("token"))
                    assertEquals("ADMIN", rs.getString("granted_role"))
                    assertEquals(0, rs.getInt("require_approval"))
                    assertEquals(1, rs.getInt("max_uses"))
                }
            }
        }
    }
}
