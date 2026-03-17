package com.callapp.server

import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.server.config.MapApplicationConfig
import io.ktor.server.testing.testApplication
import java.io.File
import java.sql.DriverManager
import java.util.UUID
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals

class AdditionalRouteEdgeCasesTest {
    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun createUserRejectsShortPassword() = testWithDatabase { dbPath ->
        application { module() }
        client.get("/health")
        seedInviteToken(dbPath, "SHORTPWD")

        val connectResponse = client.post("/api/connect") {
            header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            setBody("""{"token":"SHORTPWD"}""")
        }
        val guestToken = json.parseToJsonElement(connectResponse.bodyAsText())
            .jsonObject["session_token"]!!.jsonPrimitive.content

        val createResponse = client.post("/api/users") {
            header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            header(HttpHeaders.Authorization, "Bearer $guestToken")
            setBody("""{"name":"Alex","username":"alex","password":"short"}""")
        }
        val body = json.parseToJsonElement(createResponse.bodyAsText()).jsonObject

        assertEquals(HttpStatusCode.BadRequest, createResponse.status)
        assertEquals("validation_error", body["code"]!!.jsonPrimitive.content)
    }

    private fun testWithDatabase(block: suspend io.ktor.server.testing.ApplicationTestBuilder.(String) -> Unit) =
        testApplication {
            val dbFile = File("build/test-edge-${UUID.randomUUID()}.db")
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

    private fun seedInviteToken(dbPath: String, token: String) {
        DriverManager.getConnection("jdbc:sqlite:$dbPath").use { connection ->
            connection.prepareStatement(
                """
                INSERT INTO invite_tokens(
                    id, token, label, server_id, max_uses, current_uses, granted_role, require_approval,
                    is_revoked, created_at
                )
                VALUES (?, ?, ?, 'test-server', 0, 0, 'MEMBER', 0, 0, strftime('%Y-%m-%dT%H:%M:%fZ', 'now'))
                """.trimIndent(),
            ).use { statement ->
                statement.setString(1, UUID.randomUUID().toString())
                statement.setString(2, token)
                statement.setString(3, "Edge Token")
                statement.executeUpdate()
            }
        }
    }
}
