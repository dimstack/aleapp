package com.callapp.server

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
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AdminRbacTest {

    @Test
    fun `memberCannotDeleteOtherUser`() = testWithDatabase("test-admin-rbac") { dbPath ->
        application { module() }
        client.get("/health")
        seedInviteToken(dbPath, "RBAC0001")
        seedUser(dbPath, "@member", "verysecure")
        val otherId = seedUser(dbPath, "@other", "verysecure")
        val memberToken = login("RBAC0001", "member", "verysecure")

        val response = client.delete("/api/users/$otherId") {
            bearerAuth(memberToken)
        }

        assertEquals(HttpStatusCode.Forbidden, response.status)
    }

    @Test
    fun `adminCanDeleteMember`() = testWithDatabase("test-admin-rbac") { dbPath ->
        application { module() }
        client.get("/health")
        seedInviteToken(dbPath, "RBAC0001")
        seedInviteToken(dbPath, "RBAC0002")
        seedUser(dbPath, "@admin", "supersecure", role = "ADMIN")
        val memberId = seedUser(dbPath, "@member", "verysecure")
        val adminToken = login("RBAC0002", "admin", "supersecure")

        val response = client.delete("/api/users/$memberId") {
            bearerAuth(adminToken)
        }

        assertEquals(HttpStatusCode.NoContent, response.status)
        val usersResponse = client.get("/api/users") { bearerAuth(adminToken) }
        val users = testJson.parseToJsonElement(usersResponse.bodyAsText()).jsonArray
        assertFalse(users.any { it.jsonObject["id"]!!.jsonPrimitive.content == memberId })
    }

    @Test
    fun `adminCannotDeleteSelf`() = testWithDatabase("test-admin-rbac") { dbPath ->
        application { module() }
        client.get("/health")
        seedInviteToken(dbPath, "RBAC0001")
        val adminId = seedUser(dbPath, "@admin", "supersecure", role = "ADMIN")
        val adminToken = login("RBAC0001", "admin", "supersecure")

        val response = client.delete("/api/users/$adminId") {
            bearerAuth(adminToken)
        }

        assertEquals(HttpStatusCode.BadRequest, response.status)
    }

    @Test
    fun `memberCanUpdateOnlyOwnProfile`() = testWithDatabase("test-admin-rbac") { dbPath ->
        application { module() }
        client.get("/health")
        seedInviteToken(dbPath, "RBAC0001")
        seedUser(dbPath, "@member", "verysecure")
        val otherId = seedUser(dbPath, "@other", "verysecure")
        val memberToken = login("RBAC0001", "member", "verysecure")

        val response = client.put("/api/users/$otherId") {
            bearerAuth(memberToken)
            header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            setBody("""{"name":"Hacker"}""")
        }

        assertEquals(HttpStatusCode.Forbidden, response.status)
    }

    @Test
    fun `memberCannotCreateInviteToken`() = testWithDatabase("test-admin-rbac") { dbPath ->
        application { module() }
        client.get("/health")
        seedInviteToken(dbPath, "RBAC0001")
        seedUser(dbPath, "@member", "verysecure")
        val memberToken = login("RBAC0001", "member", "verysecure")

        val response = client.post("/api/invite-tokens") {
            bearerAuth(memberToken)
            header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            setBody("""{"label":"Team","max_uses":1,"granted_role":"MEMBER","require_approval":false}""")
        }

        assertEquals(HttpStatusCode.Forbidden, response.status)
    }
}
