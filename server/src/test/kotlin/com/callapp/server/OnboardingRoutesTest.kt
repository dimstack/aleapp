package com.callapp.server

import com.callapp.server.service.PasswordService
import io.ktor.client.request.bearerAuth
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
import kotlin.test.assertTrue

class OnboardingRoutesTest {
    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun connectReturnsGuestSessionForValidToken() = testWithDatabase { dbPath ->
        application {
            module()
        }
        client.get("/health")
        seedInviteToken(dbPath, token = "ABC12345")

        val response = client.post("/api/connect") {
            header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            setBody("""{"token":"ABC12345"}""")
        }
        val body = json.parseToJsonElement(response.bodyAsText()).jsonObject

        assertEquals(HttpStatusCode.OK, response.status)
        assertEquals("needs_profile", body["status"]!!.jsonPrimitive.content)
        assertTrue(body["session_token"]!!.jsonPrimitive.content.isNotBlank())
    }

    @Test
    fun createUserConsumesGuestSessionAndReturnsUserDto() = testWithDatabase { dbPath ->
        application {
            module()
        }
        client.get("/health")
        seedInviteToken(dbPath, token = "JOIN1234")

        val connectResponse = client.post("/api/connect") {
            header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            setBody("""{"token":"JOIN1234"}""")
        }
        val guestToken = json.parseToJsonElement(connectResponse.bodyAsText())
            .jsonObject["session_token"]!!.jsonPrimitive.content

        val createResponse = client.post("/api/users") {
            header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            bearerAuth(guestToken)
            setBody("""{"name":"Dmitri","username":"dmitri","password":"verysecure"}""")
        }
        val body = json.parseToJsonElement(createResponse.bodyAsText()).jsonObject

        assertEquals(HttpStatusCode.OK, createResponse.status)
        assertEquals("@dmitri", body["username"]!!.jsonPrimitive.content)
        assertEquals("Dmitri", body["display_name"]!!.jsonPrimitive.content)
    }

    @Test
    fun loginReturnsJoinedForExistingUser() = testWithDatabase { dbPath ->
        application {
            module()
        }
        client.get("/health")
        seedInviteToken(dbPath, token = "LOGIN123")
        seedUser(dbPath, username = "@tester", password = "verysecure")

        val response = client.post("/api/auth/login") {
            header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            setBody("""{"invite_token":"LOGIN123","username":"tester","password":"verysecure"}""")
        }
        val body = json.parseToJsonElement(response.bodyAsText()).jsonObject

        assertEquals(HttpStatusCode.OK, response.status)
        assertEquals("joined", body["status"]!!.jsonPrimitive.content)
        assertEquals("@tester", body["user"]!!.jsonObject["username"]!!.jsonPrimitive.content)
    }

    @Test
    fun createUserReturnsPendingWhenApprovalIsRequired() = testWithDatabase { dbPath ->
        application {
            module()
        }
        client.get("/health")
        seedInviteToken(dbPath, token = "PENDING12", requireApproval = true)

        val connectResponse = client.post("/api/connect") {
            header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            setBody("""{"token":"PENDING12"}""")
        }
        val guestToken = json.parseToJsonElement(connectResponse.bodyAsText())
            .jsonObject["session_token"]!!.jsonPrimitive.content

        val createResponse = client.post("/api/users") {
            header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            bearerAuth(guestToken)
            setBody("""{"name":"Pending User","username":"pending","password":"verysecure"}""")
        }
        val body = json.parseToJsonElement(createResponse.bodyAsText()).jsonObject

        assertEquals(HttpStatusCode.Accepted, createResponse.status)
        assertEquals("pending", body["status"]!!.jsonPrimitive.content)
    }

    private fun testWithDatabase(block: suspend io.ktor.server.testing.ApplicationTestBuilder.(String) -> Unit) =
        testApplication {
            val dbFile = File("build/test-${UUID.randomUUID()}.db")
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
                    "callapp.turn.secret" to "turn-secret",
                    "callapp.turn.realm" to "callapp-test",
                )
            }
            block(dbFile.absolutePath)
        }

    private fun seedInviteToken(
        dbPath: String,
        token: String,
        requireApproval: Boolean = false,
    ) {
        DriverManager.getConnection("jdbc:sqlite:$dbPath").use { connection ->
            connection.prepareStatement(
                """
                INSERT INTO invite_tokens(
                    id, token, label, server_id, max_uses, current_uses, granted_role, require_approval,
                    is_revoked, created_at
                )
                VALUES (?, ?, ?, 'test-server', 0, 0, 'MEMBER', ?, 0, strftime('%Y-%m-%dT%H:%M:%fZ', 'now'))
                """.trimIndent(),
            ).use { statement ->
                statement.setString(1, UUID.randomUUID().toString())
                statement.setString(2, token)
                statement.setString(3, "Test Token")
                statement.setInt(4, if (requireApproval) 1 else 0)
                statement.executeUpdate()
            }
        }
    }

    private fun seedUser(
        dbPath: String,
        username: String,
        password: String,
    ) {
        DriverManager.getConnection("jdbc:sqlite:$dbPath").use { connection ->
            connection.prepareStatement(
                """
                INSERT INTO users(
                    id, username, display_name, password_hash, avatar_url, role, status,
                    server_id, is_approved, created_at, updated_at
                )
                VALUES (?, ?, 'Tester', ?, NULL, 'MEMBER', 'ONLINE', 'test-server', 1,
                        strftime('%Y-%m-%dT%H:%M:%fZ', 'now'), strftime('%Y-%m-%dT%H:%M:%fZ', 'now'))
                """.trimIndent(),
            ).use { statement ->
                statement.setString(1, UUID.randomUUID().toString())
                statement.setString(2, username)
                statement.setString(3, PasswordService().hash(password))
                statement.executeUpdate()
            }
        }
    }
}
