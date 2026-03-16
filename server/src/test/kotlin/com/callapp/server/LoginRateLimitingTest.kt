package com.callapp.server

import io.ktor.client.request.header
import io.ktor.client.request.get
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

class LoginRateLimitingTest {

    @Test
    fun `5failedAttempts_locksAccount`() = testWithDatabase("test-login-rate") { dbPath ->
        application { module() }
        client.get("/health")
        seedInviteToken(dbPath, "LOGIN123")
        seedUser(dbPath, "@tester", "verysecure")

        repeat(5) {
            val response = client.post("/api/auth/login") {
                header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                setBody("""{"invite_token":"LOGIN123","username":"tester","password":"wrong-pass"}""")
            }
            assertEquals(HttpStatusCode.Unauthorized, response.status)
        }

        val lockedResponse = client.post("/api/auth/login") {
            header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            setBody("""{"invite_token":"LOGIN123","username":"tester","password":"wrong-pass"}""")
        }

        assertEquals(HttpStatusCode.Locked, lockedResponse.status)
        val body = testJson.parseToJsonElement(lockedResponse.bodyAsText()).jsonObject
        assertEquals("login_locked", body["code"]!!.jsonPrimitive.content)
    }

    @Test
    fun `lockoutExpires_afterCooldown`() = testWithDatabase("test-login-rate") { dbPath ->
        application { module() }
        client.get("/health")
        seedInviteToken(dbPath, "LOGIN123")
        seedUser(dbPath, "@tester", "verysecure")
        seedLoginAttempt(
            dbPath = dbPath,
            username = "@tester",
            failedAttempts = 5,
            lockedUntil = Instant.now().minus(16, ChronoUnit.MINUTES),
        )

        val response = client.post("/api/auth/login") {
            header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            setBody("""{"invite_token":"LOGIN123","username":"tester","password":"verysecure"}""")
        }

        assertEquals(HttpStatusCode.OK, response.status)
    }

    @Test
    fun `successfulLogin_resetsFailureCount`() = testWithDatabase("test-login-rate") { dbPath ->
        application { module() }
        client.get("/health")
        seedInviteToken(dbPath, "LOGIN123")
        seedUser(dbPath, "@tester", "verysecure")

        repeat(4) {
            client.post("/api/auth/login") {
                header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                setBody("""{"invite_token":"LOGIN123","username":"tester","password":"wrong-pass"}""")
            }
        }

        val successResponse = client.post("/api/auth/login") {
            header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            setBody("""{"invite_token":"LOGIN123","username":"tester","password":"verysecure"}""")
        }
        val failedAgainResponse = client.post("/api/auth/login") {
            header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            setBody("""{"invite_token":"LOGIN123","username":"tester","password":"wrong-pass"}""")
        }

        assertEquals(HttpStatusCode.OK, successResponse.status)
        assertEquals(HttpStatusCode.Unauthorized, failedAgainResponse.status)
    }

    @Test
    fun `differentUsers_separateLimits`() = testWithDatabase("test-login-rate") { dbPath ->
        application { module() }
        client.get("/health")
        seedInviteToken(dbPath, "LOGIN123")
        seedUser(dbPath, "@tester1", "verysecure")
        seedUser(dbPath, "@tester2", "verysecure")

        repeat(5) {
            client.post("/api/auth/login") {
                header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                setBody("""{"invite_token":"LOGIN123","username":"tester1","password":"wrong-pass"}""")
            }
        }

        val lockedResponse = client.post("/api/auth/login") {
            header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            setBody("""{"invite_token":"LOGIN123","username":"tester1","password":"wrong-pass"}""")
        }
        val otherUserResponse = client.post("/api/auth/login") {
            header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            setBody("""{"invite_token":"LOGIN123","username":"tester2","password":"wrong-pass"}""")
        }

        assertEquals(HttpStatusCode.Locked, lockedResponse.status)
        assertEquals(HttpStatusCode.Unauthorized, otherUserResponse.status)
    }

    @Test
    fun `lockoutExpired_wrongPasswordStartsNewCounter`() = testWithDatabase("test-login-rate") { dbPath ->
        application { module() }
        client.get("/health")
        seedInviteToken(dbPath, "LOGIN123")
        seedUser(dbPath, "@tester", "verysecure")
        seedLoginAttempt(
            dbPath = dbPath,
            username = "@tester",
            failedAttempts = 5,
            lockedUntil = Instant.now().minus(16, ChronoUnit.MINUTES),
        )

        val response = client.post("/api/auth/login") {
            header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            setBody("""{"invite_token":"LOGIN123","username":"tester","password":"wrong-pass"}""")
        }

        assertEquals(HttpStatusCode.Unauthorized, response.status)
        val body = testJson.parseToJsonElement(response.bodyAsText()).jsonObject
        assertEquals("unauthorized", body["code"]!!.jsonPrimitive.content)
    }
}
