package com.callapp.android.network

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class ServerConnectionManagerTest {

    @Test
    fun getClient_returnsSameInstanceForSameAddress() {
        val manager = ServerConnectionManager()

        val first = manager.getClient("https://server.example.com")
        val second = manager.getClient("https://server.example.com")

        assertSame(first, second)
    }

    @Test
    fun restoreSession_updatesClientAndSignalingToken() {
        val manager = ServerConnectionManager()

        val client = manager.restoreSession("https://server.example.com", "session-123")
        val signaling = manager.getSignaling("https://server.example.com")

        assertEquals("session-123", client.sessionToken)
        assertEquals("session-123", signaling.sessionToken)
    }

    @Test
    fun removeClient_dropsCachedInstances() {
        val manager = ServerConnectionManager()
        val firstClient = manager.getClient("https://server.example.com")
        val firstSignaling = manager.getSignaling("https://server.example.com")

        manager.removeClient("https://server.example.com")

        val secondClient = manager.getClient("https://server.example.com")
        val secondSignaling = manager.getSignaling("https://server.example.com")

        assertNotSame(firstClient, secondClient)
        assertNotSame(firstSignaling, secondSignaling)
    }

    @Test
    fun parseInviteToken_returnsNullForInvalidValue() {
        assertTrue(ServerConnectionManager.parseInviteToken("not-a-token") == null)
    }
}
