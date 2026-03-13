package com.callapp.server

import com.callapp.server.signaling.SignalMessage
import com.callapp.server.service.PasswordService
import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.client.plugins.websocket.webSocket
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpHeaders
import io.ktor.server.config.MapApplicationConfig
import io.ktor.server.testing.testApplication
import io.ktor.websocket.Frame
import io.ktor.websocket.readText
import java.io.File
import java.sql.DriverManager
import java.util.UUID
import kotlinx.coroutines.async
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals

class SignalingRoutesTest {
    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun websocketRelaysOfferBetweenConnectedUsers() = testApplication {
        val dbFile = File("build/test-signal-${UUID.randomUUID()}.db")
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

        application { module() }
        client.get("/health")

        seedInviteToken(dbFile.absolutePath, "TOKEN123")
        val callerId = seedUser(dbFile.absolutePath, "@caller", "verysecure")
        val calleeId = seedUser(dbFile.absolutePath, "@callee", "verysecure")

        val callerToken = login("TOKEN123", "caller", "verysecure")
        val calleeToken = login("TOKEN123", "callee", "verysecure")

        val wsClient = createClient {
            install(WebSockets)
        }

        wsClient.webSocket("/ws?token=$calleeToken") {
            val receiveJob = async {
                withTimeout(5_000) {
                    val frame = incoming.receive() as Frame.Text
                    SignalMessage.fromJson(frame.readText())
                }
            }

            wsClient.webSocket("/ws?token=$callerToken") {
                send(Frame.Text(SignalMessage.Offer(sdp = "fake-sdp", targetUserId = calleeId).toJson()))
            }

            val message = receiveJob.await()
            val offer = message as SignalMessage.Offer
            assertEquals(callerId, offer.fromUserId)
            assertEquals(calleeId, offer.targetUserId)
            assertEquals("fake-sdp", offer.sdp)
        }
    }

    private suspend fun io.ktor.server.testing.ApplicationTestBuilder.login(inviteToken: String, username: String, password: String): String {
        val response = client.post("/api/auth/login") {
            header(HttpHeaders.ContentType, "application/json")
            setBody("""{"invite_token":"$inviteToken","username":"$username","password":"$password"}""")
        }
        val body = response.bodyAsText()
        return json.parseToJsonElement(body).jsonObject["session_token"]!!.jsonPrimitive.content
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
                statement.setString(3, "Signal Token")
                statement.executeUpdate()
            }
        }
    }

    private fun seedUser(dbPath: String, username: String, password: String): String {
        val userId = UUID.randomUUID().toString()
        DriverManager.getConnection("jdbc:sqlite:$dbPath").use { connection ->
            connection.prepareStatement(
                """
                INSERT INTO users(
                    id, username, display_name, password_hash, avatar_url, role, status,
                    server_id, is_approved, created_at, updated_at
                )
                VALUES (?, ?, ?, ?, NULL, 'MEMBER', 'ONLINE', 'test-server', 1,
                        strftime('%Y-%m-%dT%H:%M:%fZ', 'now'), strftime('%Y-%m-%dT%H:%M:%fZ', 'now'))
                """.trimIndent(),
            ).use { statement ->
                statement.setString(1, userId)
                statement.setString(2, username)
                statement.setString(3, username.removePrefix("@").replaceFirstChar { it.uppercase() })
                statement.setString(4, PasswordService().hash(password))
                statement.executeUpdate()
            }
        }
        return userId
    }
}
