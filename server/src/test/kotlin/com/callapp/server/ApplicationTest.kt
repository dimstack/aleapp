package com.callapp.server

import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import io.ktor.server.config.MapApplicationConfig
import io.ktor.server.testing.testApplication
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ApplicationTest {

    @Test
    fun healthEndpointReturnsOk() = testApplication {
        environment {
            config = MapApplicationConfig(
                "callapp.environment" to "test",
                "callapp.database.path" to "./build/test-health.db",
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

        application {
            module()
        }

        val response = client.get("/health")
        assertEquals(HttpStatusCode.OK, response.status)
        assertTrue(response.bodyAsText().contains("\"status\""))
    }
}
