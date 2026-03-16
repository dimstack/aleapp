package com.callapp.server

import io.ktor.client.request.bearerAuth
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import java.time.Instant
import java.time.temporal.ChronoUnit
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals

class InviteTokenEdgeCasesTest {

    @Test
    fun `expiredToken_returnsError`() = testWithDatabase("test-token-edge") { dbPath ->
        application { module() }
        client.get("/health")
        seedInviteToken(
            dbPath = dbPath,
            token = "EXPIRED1",
            expiresAt = Instant.now().minus(1, ChronoUnit.DAYS),
        )

        val response = client.post("/api/connect") {
            header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            setBody("""{"token":"EXPIRED1"}""")
        }

        assertEquals(HttpStatusCode.Unauthorized, response.status)
    }

    @Test
    fun `revokedToken_returnsError`() = testWithDatabase("test-token-edge") { dbPath ->
        application { module() }
        client.get("/health")
        seedInviteToken(dbPath = dbPath, token = "REVOKED1", isRevoked = true)

        val response = client.post("/api/connect") {
            header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            setBody("""{"token":"REVOKED1"}""")
        }

        assertEquals(HttpStatusCode.Unauthorized, response.status)
    }

    @Test
    fun `tokenAtMaxUses_returnsError`() = testWithDatabase("test-token-edge") { dbPath ->
        application { module() }
        client.get("/health")
        seedInviteToken(dbPath = dbPath, token = "LIMIT001", maxUses = 1, currentUses = 1)
        val guestToken = connectGuest("LIMIT001")

        val response = client.post("/api/users") {
            bearerAuth(guestToken)
            header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            setBody("""{"name":"Tester","username":"tester","password":"verysecure"}""")
        }

        assertEquals(HttpStatusCode.Unauthorized, response.status)
        val body = testJson.parseToJsonElement(response.bodyAsText()).jsonObject
        assertEquals("invite_token_exhausted", body["code"]!!.jsonPrimitive.content)
    }

    @Test
    fun `nullMaxUses_unlimitedUses`() = testWithDatabase("test-token-edge") { dbPath ->
        application { module() }
        client.get("/health")
        seedInviteToken(dbPath = dbPath, token = "UNLIMIT1", maxUses = null)

        repeat(3) { index ->
            val guestToken = connectGuest("UNLIMIT1")
            val response = client.post("/api/users") {
                bearerAuth(guestToken)
                header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                setBody("""{"name":"User $index","username":"user$index","password":"verysecure"}""")
            }
            assertEquals(HttpStatusCode.OK, response.status)
        }

        assertEquals(3, countInviteTokenUses(dbPath, "UNLIMIT1"))
    }

    @Test
    fun `loginDoesNotConsumeUse`() = testWithDatabase("test-token-edge") { dbPath ->
        application { module() }
        client.get("/health")
        seedInviteToken(dbPath = dbPath, token = "LOGINUSE", maxUses = 5)
        seedUser(dbPath = dbPath, username = "@tester", password = "verysecure")

        val response = client.post("/api/auth/login") {
            header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            setBody("""{"invite_token":"LOGINUSE","username":"tester","password":"verysecure"}""")
        }

        assertEquals(HttpStatusCode.OK, response.status)
        assertEquals(0, countInviteTokenUses(dbPath, "LOGINUSE"))
    }

    @Test
    fun `createUserConsumesOneUse`() = testWithDatabase("test-token-edge") { dbPath ->
        application { module() }
        client.get("/health")
        seedInviteToken(dbPath = dbPath, token = "CREATE01", maxUses = 5)
        val guestToken = connectGuest("CREATE01")

        val response = client.post("/api/users") {
            bearerAuth(guestToken)
            header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            setBody("""{"name":"Tester","username":"tester","password":"verysecure"}""")
        }

        assertEquals(HttpStatusCode.OK, response.status)
        assertEquals(1, countInviteTokenUses(dbPath, "CREATE01"))
    }

    private suspend fun io.ktor.server.testing.ApplicationTestBuilder.connectGuest(token: String): String {
        val response = client.post("/api/connect") {
            header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            setBody("""{"token":"$token"}""")
        }
        return testJson.parseToJsonElement(response.bodyAsText()).jsonObject["session_token"]!!.jsonPrimitive.content
    }
}
