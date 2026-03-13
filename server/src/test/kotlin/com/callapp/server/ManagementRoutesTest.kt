package com.callapp.server

import com.callapp.server.service.PasswordService
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.put
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
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ManagementRoutesTest {
    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun adminCanManageServerAndInviteTokens() = testWithDatabase { dbPath ->
        application { module() }
        client.get("/health")
        seedInviteToken(dbPath, "ADMIN111")
        val adminId = seedUser(dbPath, "@admin", "supersecure", role = "ADMIN", displayName = "Admin")
        val adminToken = login("ADMIN111", "admin", "supersecure")

        val serverResponse = client.get("/api/server") {
            bearerAuth(adminToken)
        }
        assertEquals(HttpStatusCode.OK, serverResponse.status)

        val updateResponse = client.put("/api/server") {
            bearerAuth(adminToken)
            header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            setBody("""{"name":"Updated Server","description":"Updated description"}""")
        }
        assertEquals(HttpStatusCode.OK, updateResponse.status)
        assertEquals("Updated Server", json.parseToJsonElement(updateResponse.bodyAsText()).jsonObject["name"]!!.jsonPrimitive.content)

        val deleteServerResponse = client.delete("/api/server") {
            bearerAuth(adminToken)
        }
        assertEquals(HttpStatusCode.Gone, deleteServerResponse.status)
        val deleteServerBody = json.parseToJsonElement(deleteServerResponse.bodyAsText()).jsonObject
        assertEquals("deprecated_endpoint", deleteServerBody["code"]!!.jsonPrimitive.content)

        val createTokenResponse = client.post("/api/invite-tokens") {
            bearerAuth(adminToken)
            header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            setBody("""{"label":"Design Team","max_uses":5,"granted_role":"MEMBER","require_approval":false}""")
        }
        assertEquals(HttpStatusCode.OK, createTokenResponse.status)
        val createdToken = json.parseToJsonElement(createTokenResponse.bodyAsText()).jsonObject
        val inviteTokenId = createdToken["id"]!!.jsonPrimitive.content

        val listTokensResponse = client.get("/api/invite-tokens") {
            bearerAuth(adminToken)
        }
        assertEquals(HttpStatusCode.OK, listTokensResponse.status)
        assertTrue(json.parseToJsonElement(listTokensResponse.bodyAsText()).jsonArray.size >= 2)

        val revokeResponse = client.delete("/api/invite-tokens/$inviteTokenId") {
            bearerAuth(adminToken)
        }
        assertEquals(HttpStatusCode.NoContent, revokeResponse.status)

        val usersResponse = client.get("/api/users") {
            bearerAuth(adminToken)
        }
        assertEquals(HttpStatusCode.OK, usersResponse.status)
        val users = json.parseToJsonElement(usersResponse.bodyAsText()).jsonArray
        assertTrue(users.any { it.jsonObject["id"]!!.jsonPrimitive.content == adminId })
    }

    @Test
    fun userCanManageFavoritesAndNotifications() = testWithDatabase { dbPath ->
        application { module() }
        client.get("/health")
        seedInviteToken(dbPath, "USER1111")
        val userId = seedUser(dbPath, "@user", "verysecure", role = "MEMBER", displayName = "User")
        val friendId = seedUser(dbPath, "@friend", "verysecure", role = "MEMBER", displayName = "Friend")
        seedNotification(dbPath, userId, "REQUEST_APPROVED", "Test Server", "Approved")

        val userToken = login("USER1111", "user", "verysecure")

        val addFavoriteResponse = client.post("/api/favorites/$friendId") {
            bearerAuth(userToken)
        }
        assertEquals(HttpStatusCode.OK, addFavoriteResponse.status)

        val favoritesResponse = client.get("/api/favorites") {
            bearerAuth(userToken)
        }
        assertEquals(HttpStatusCode.OK, favoritesResponse.status)
        val favorites = json.parseToJsonElement(favoritesResponse.bodyAsText()).jsonArray
        assertEquals("@friend", favorites.first().jsonObject["username"]!!.jsonPrimitive.content)

        val notificationsResponse = client.get("/api/notifications") {
            bearerAuth(userToken)
        }
        assertEquals(HttpStatusCode.OK, notificationsResponse.status)
        val notifications = json.parseToJsonElement(notificationsResponse.bodyAsText()).jsonArray
        assertEquals("REQUEST_APPROVED", notifications.first().jsonObject["type"]!!.jsonPrimitive.content)

        val turnResponse = client.get("/api/turn-credentials") {
            bearerAuth(userToken)
        }
        assertEquals(HttpStatusCode.OK, turnResponse.status)
        val turnBody = json.parseToJsonElement(turnResponse.bodyAsText()).jsonObject
        assertTrue(turnBody["urls"]!!.jsonArray.isNotEmpty())
        assertTrue(turnBody["username"]!!.jsonPrimitive.content.contains(userId))

        val markReadResponse = client.put("/api/notifications/read") {
            bearerAuth(userToken)
        }
        assertEquals(HttpStatusCode.OK, markReadResponse.status)

        val clearResponse = client.delete("/api/notifications") {
            bearerAuth(userToken)
        }
        assertEquals(HttpStatusCode.NoContent, clearResponse.status)
    }

    @Test
    fun memberCannotAccessAdminEndpointsAndAdminGetsValidationErrorsForBadPayload() = testWithDatabase { dbPath ->
        application { module() }
        client.get("/health")
        seedInviteToken(dbPath, "USER2222")
        seedInviteToken(dbPath, "ADMIN222")
        seedUser(dbPath, "@member", "verysecure", role = "MEMBER", displayName = "Member")
        seedUser(dbPath, "@admin2", "supersecure", role = "ADMIN", displayName = "Admin Two")

        val memberToken = login("USER2222", "member", "verysecure")
        val adminToken = login("ADMIN222", "admin2", "supersecure")

        val forbiddenResponse = client.get("/api/invite-tokens") {
            bearerAuth(memberToken)
        }
        assertEquals(HttpStatusCode.Forbidden, forbiddenResponse.status)

        val invalidPayloadResponse = client.post("/api/invite-tokens") {
            bearerAuth(adminToken)
            header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            setBody("""{"label":"","max_uses":-1,"granted_role":"MEMBER","require_approval":false}""")
        }
        assertEquals(HttpStatusCode.BadRequest, invalidPayloadResponse.status)
    }

    @Test
    fun joinRequestsCreationEndpointIsDeprecated() = testWithDatabase { dbPath ->
        application { module() }
        client.get("/health")
        seedInviteToken(dbPath, "USER3333")
        seedUser(dbPath, "@member3", "verysecure", role = "MEMBER", displayName = "Member Three")

        val memberToken = login("USER3333", "member3", "verysecure")

        val response = client.post("/api/join-requests") {
            bearerAuth(memberToken)
            header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            setBody("""{"username":"candidate","name":"Candidate"}""")
        }

        assertEquals(HttpStatusCode.Gone, response.status)
        val body = json.parseToJsonElement(response.bodyAsText()).jsonObject
        assertEquals("deprecated_endpoint", body["code"]!!.jsonPrimitive.content)
    }

    @Test
    fun approvingPendingRegistrationConsumesInviteTokenUse() = testWithDatabase { dbPath ->
        application { module() }
        client.get("/health")
        val inviteTokenId = seedInviteToken(dbPath, "APPROVE1", requireApproval = true, maxUses = 1)
        seedInviteToken(dbPath, "ADMIN333")
        seedUser(dbPath, "@admin3", "supersecure", role = "ADMIN", displayName = "Admin Three")

        val connectResponse = client.post("/api/connect") {
            header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            setBody("""{"token":"APPROVE1"}""")
        }
        val guestToken = json.parseToJsonElement(connectResponse.bodyAsText()).jsonObject["session_token"]!!.jsonPrimitive.content

        val createResponse = client.post("/api/users") {
            bearerAuth(guestToken)
            header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            setBody("""{"name":"Pending Member","username":"pendingmember","password":"verysecure"}""")
        }
        assertEquals(HttpStatusCode.Accepted, createResponse.status)

        val adminToken = login("ADMIN333", "admin3", "supersecure")
        val joinRequestsResponse = client.get("/api/join-requests") {
            bearerAuth(adminToken)
        }
        val requestId = json.parseToJsonElement(joinRequestsResponse.bodyAsText())
            .jsonArray.first().jsonObject["id"]!!.jsonPrimitive.content

        val approveResponse = client.put("/api/join-requests/$requestId") {
            bearerAuth(adminToken)
            header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            setBody("""{"status":"approved"}""")
        }
        assertEquals(HttpStatusCode.OK, approveResponse.status)

        DriverManager.getConnection("jdbc:sqlite:$dbPath").use { connection ->
            connection.prepareStatement("SELECT current_uses FROM invite_tokens WHERE id = ?").use { statement ->
                statement.setString(1, inviteTokenId)
                statement.executeQuery().use { rs ->
                    assertTrue(rs.next())
                    assertEquals(1, rs.getInt("current_uses"))
                }
            }
        }
    }

    private fun testWithDatabase(block: suspend io.ktor.server.testing.ApplicationTestBuilder.(String) -> Unit) =
        testApplication {
            val dbFile = File("build/test-mgmt-${UUID.randomUUID()}.db")
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

    private suspend fun io.ktor.server.testing.ApplicationTestBuilder.login(inviteToken: String, username: String, password: String): String {
        val response = client.post("/api/auth/login") {
            header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            setBody("""{"invite_token":"$inviteToken","username":"$username","password":"$password"}""")
        }
        return json.parseToJsonElement(response.bodyAsText()).jsonObject["session_token"]!!.jsonPrimitive.content
    }

    private fun seedInviteToken(dbPath: String, token: String, requireApproval: Boolean = false, maxUses: Int = 0): String {
        val inviteTokenId = UUID.randomUUID().toString()
        DriverManager.getConnection("jdbc:sqlite:$dbPath").use { connection ->
            connection.prepareStatement(
                """
                INSERT INTO invite_tokens(
                    id, token, label, server_id, max_uses, current_uses, granted_role, require_approval,
                    is_revoked, created_at
                )
                VALUES (?, ?, ?, 'test-server', ?, 0, 'MEMBER', ?, 0, strftime('%Y-%m-%dT%H:%M:%fZ', 'now'))
                """.trimIndent(),
            ).use { statement ->
                statement.setString(1, inviteTokenId)
                statement.setString(2, token)
                statement.setString(3, "Seed Token")
                statement.setInt(4, maxUses)
                statement.setInt(5, if (requireApproval) 1 else 0)
                statement.executeUpdate()
            }
        }
        return inviteTokenId
    }

    private fun seedUser(dbPath: String, username: String, password: String, role: String, displayName: String): String {
        val userId = UUID.randomUUID().toString()
        DriverManager.getConnection("jdbc:sqlite:$dbPath").use { connection ->
            connection.prepareStatement(
                """
                INSERT INTO users(
                    id, username, display_name, password_hash, avatar_url, role, status,
                    server_id, is_approved, created_at, updated_at
                )
                VALUES (?, ?, ?, ?, NULL, ?, 'ONLINE', 'test-server', 1,
                        strftime('%Y-%m-%dT%H:%M:%fZ', 'now'), strftime('%Y-%m-%dT%H:%M:%fZ', 'now'))
                """.trimIndent(),
            ).use { statement ->
                statement.setString(1, userId)
                statement.setString(2, username)
                statement.setString(3, displayName)
                statement.setString(4, PasswordService().hash(password))
                statement.setString(5, role)
                statement.executeUpdate()
            }
        }
        return userId
    }

    private fun seedNotification(dbPath: String, userId: String, type: String, serverName: String, message: String) {
        DriverManager.getConnection("jdbc:sqlite:$dbPath").use { connection ->
            connection.prepareStatement(
                """
                INSERT INTO notifications(id, user_id, type, server_name, message, is_read, created_at)
                VALUES (?, ?, ?, ?, ?, 0, strftime('%Y-%m-%dT%H:%M:%fZ', 'now'))
                """.trimIndent(),
            ).use { statement ->
                statement.setString(1, UUID.randomUUID().toString())
                statement.setString(2, userId)
                statement.setString(3, type)
                statement.setString(4, serverName)
                statement.setString(5, message)
                statement.executeUpdate()
            }
        }
    }
}
