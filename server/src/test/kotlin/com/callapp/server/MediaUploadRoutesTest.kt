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
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class MediaUploadRoutesTest {

    @Test
    fun guestCanUploadProfileImageDuringOnboarding() = testWithDatabase("test-upload-guest") { dbPath ->
        application { module() }
        client.get("/health")
        seedInviteToken(dbPath, "GUESTU01")
        val guestToken = connectGuest("GUESTU01")

        val response = client.post("/api/uploads/profile-image") {
            bearerAuth(guestToken)
            header(HttpHeaders.ContentType, ContentType.Image.JPEG.toString())
            header("X-File-Name", "guest.jpg")
            setBody(byteArrayOf(1, 2, 3))
        }

        assertEquals(HttpStatusCode.OK, response.status)
        val url = testJson.parseToJsonElement(response.bodyAsText()).jsonObject["url"]!!.jsonPrimitive.content
        assertTrue(url.startsWith("/uploads/profile/"))
    }

    @Test
    fun userCanUploadProfileImageAndFetchIt() = testWithDatabase("test-upload") { dbPath ->
        application { module() }
        client.get("/health")
        seedInviteToken(dbPath, "USERUP01")
        seedUser(dbPath, "@user", "verysecure")
        val userToken = login("USERUP01", "user", "verysecure")

        val uploadResponse = client.post("/api/uploads/profile-image") {
            bearerAuth(userToken)
            header(HttpHeaders.ContentType, ContentType.Image.PNG.toString())
            header("X-File-Name", "avatar.png")
            setBody(byteArrayOf(1, 2, 3, 4))
        }

        assertEquals(HttpStatusCode.OK, uploadResponse.status)
        val body = testJson.parseToJsonElement(uploadResponse.bodyAsText()).jsonObject
        val url = body["url"]!!.jsonPrimitive.content
        assertTrue(url.startsWith("/uploads/profile/"))

        val fetchResponse = client.get(url)
        assertEquals(HttpStatusCode.OK, fetchResponse.status)
    }

    @Test
    fun memberCannotUploadServerImage() = testWithDatabase("test-upload-forbidden") { dbPath ->
        application { module() }
        client.get("/health")
        seedInviteToken(dbPath, "USERUP02")
        seedUser(dbPath, "@user", "verysecure")
        val userToken = login("USERUP02", "user", "verysecure")

        val response = client.post("/api/uploads/server-image") {
            bearerAuth(userToken)
            header(HttpHeaders.ContentType, ContentType.Image.JPEG.toString())
            header("X-File-Name", "server.jpg")
            setBody(byteArrayOf(1, 2, 3))
        }

        assertEquals(HttpStatusCode.Forbidden, response.status)
    }

    private suspend fun io.ktor.server.testing.ApplicationTestBuilder.connectGuest(token: String): String {
        val response = client.post("/api/connect") {
            header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            setBody("""{"token":"$token"}""")
        }
        return testJson.parseToJsonElement(response.bodyAsText())
            .jsonObject["session_token"]!!
            .jsonPrimitive
            .content
    }
}
