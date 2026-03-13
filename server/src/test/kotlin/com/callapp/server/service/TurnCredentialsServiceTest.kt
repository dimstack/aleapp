package com.callapp.server.service

import com.callapp.server.config.TurnConfig
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TurnCredentialsServiceTest {

    @Test
    fun generatesCoturnCompatibleCredentials() {
        val service = TurnCredentialsService(
            TurnConfig(
                host = "turn.example.com",
                port = 3478,
                secret = "secret",
                realm = "callapp",
                ttlSeconds = 3600,
            ),
        )

        val credentials = service.create("user-1")

        assertEquals(2, credentials.urls.size)
        assertTrue(credentials.urls.first().contains("turn:turn.example.com:3478"))
        assertTrue(credentials.username.endsWith(":user-1"))
        assertTrue(credentials.credential.isNotBlank())
    }
}
