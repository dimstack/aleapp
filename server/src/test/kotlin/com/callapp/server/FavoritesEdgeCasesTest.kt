package com.callapp.server

import io.ktor.client.request.bearerAuth
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals

class FavoritesEdgeCasesTest {

    @Test
    fun `addSameUserTwice_idempotent`() = testWithDatabase("test-favorites-edge") { dbPath ->
        application { module() }
        client.get("/health")
        seedInviteToken(dbPath, "FAV00001")
        seedUser(dbPath, "@user", "verysecure")
        val friendId = seedUser(dbPath, "@friend", "verysecure")
        val token = login("FAV00001", "user", "verysecure")

        repeat(2) {
            val response = client.post("/api/favorites/$friendId") {
                bearerAuth(token)
            }
            assertEquals(HttpStatusCode.OK, response.status)
        }

        val favoritesResponse = client.get("/api/favorites") {
            bearerAuth(token)
        }
        val favorites = testJson.parseToJsonElement(favoritesResponse.bodyAsText()).jsonArray
        assertEquals(1, favorites.size)
    }

    @Test
    fun `addSelf_returnsError`() = testWithDatabase("test-favorites-edge") { dbPath ->
        application { module() }
        client.get("/health")
        seedInviteToken(dbPath, "FAV00001")
        val userId = seedUser(dbPath, "@user", "verysecure")
        val token = login("FAV00001", "user", "verysecure")

        val response = client.post("/api/favorites/$userId") {
            bearerAuth(token)
        }

        assertEquals(HttpStatusCode.BadRequest, response.status)
        val body = testJson.parseToJsonElement(response.bodyAsText()).jsonObject
        assertEquals("validation_error", body["code"]!!.jsonPrimitive.content)
    }

    @Test
    fun `favoriteFromDifferentServer_notVisible`() = testWithDatabase("test-favorites-edge") { dbPath ->
        application { module() }
        client.get("/health")
        seedInviteToken(dbPath, "FAV00001")
        seedUser(dbPath, "@user", "verysecure", serverId = "test-server")
        val otherServerUserId = seedUser(dbPath, "@remote", "verysecure", serverId = "other-server")
        val token = login("FAV00001", "user", "verysecure")

        client.post("/api/favorites/$otherServerUserId") {
            bearerAuth(token)
        }

        val favoritesResponse = client.get("/api/favorites") {
            bearerAuth(token)
        }
        val favorites = testJson.parseToJsonElement(favoritesResponse.bodyAsText()).jsonArray
        assertEquals(0, favorites.size)
    }
}
