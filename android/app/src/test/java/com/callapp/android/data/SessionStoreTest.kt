package com.callapp.android.data

import com.callapp.android.testutil.InMemorySharedPreferences
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SessionStoreTest {

    private fun createStore(prefs: InMemorySharedPreferences = InMemorySharedPreferences()): SessionStore =
        SessionStore.createForTests(prefs)

    @Test
    fun saveAndLoadSingleSession() {
        val store = createStore()

        store.saveSession(
            serverAddress = "http://server-1:3000",
            sessionToken = "token-1",
            userId = "user-1",
            serverName = "Server 1",
            serverUsername = "@server1",
            serverId = "srv-1",
        )

        val session = store.getSession("http://server-1:3000")
        requireNotNull(session)
        assertEquals("token-1", session.sessionToken)
        assertEquals("user-1", session.userId)
        assertEquals("Server 1", session.serverName)
        assertEquals("@server1", session.serverUsername)
        assertEquals("srv-1", session.serverId)
        assertEquals("", session.serverDescription)
        assertNull(session.serverImageUrl)
    }

    @Test
    fun saveMultipleSessions() {
        val store = createStore()

        store.saveSession("http://server-1:3000", "token-1", "user-1")
        store.saveSession("http://server-2:3000", "token-2", "user-2")

        val sessions = store.getSessions()
        assertEquals(2, sessions.size)
        assertEquals("token-1", sessions["http://server-1:3000"]?.sessionToken)
        assertEquals("token-2", sessions["http://server-2:3000"]?.sessionToken)
    }

    @Test
    fun removeSession() {
        val store = createStore()

        store.saveSession("http://server-1:3000", "token-1", "user-1")
        store.saveSession("http://server-2:3000", "token-2", "user-2")

        store.removeSession("http://server-1:3000")

        assertNull(store.getSession("http://server-1:3000"))
        assertEquals("token-2", store.getSession("http://server-2:3000")?.sessionToken)
    }

    @Test
    fun saveSession_setsActiveAddressAndUserId() {
        val store = createStore()

        store.saveSession("http://server-1:3000", "token-1", "user-1")

        assertEquals("http://server-1:3000", store.activeServerAddress)
        assertEquals("user-1", store.activeUserId)
    }

    @Test
    fun saveSession_setActiveFalse_doesNotChangeActiveSession() {
        val store = createStore()

        store.saveSession("http://server-1:3000", "token-1", "user-1")
        store.saveSession("http://server-2:3000", "token-2", "user-2", setActive = false)

        assertEquals("http://server-1:3000", store.activeServerAddress)
        assertEquals("user-1", store.activeUserId)
    }

    @Test
    fun removeActiveSession_clearsActivePointers() {
        val store = createStore()

        store.saveSession("http://server-1:3000", "token-1", "user-1")

        store.removeSession("http://server-1:3000")

        assertEquals("", store.activeServerAddress)
        assertEquals("", store.activeUserId)
    }

    @Test
    fun removeNonActiveSession_keepsActivePointers() {
        val store = createStore()

        store.saveSession("http://server-1:3000", "token-1", "user-1")
        store.saveSession("http://server-2:3000", "token-2", "user-2", setActive = false)

        store.removeSession("http://server-2:3000")

        assertEquals("http://server-1:3000", store.activeServerAddress)
        assertEquals("user-1", store.activeUserId)
    }

    @Test
    fun savePendingApproval() {
        val store = createStore()

        store.savePendingApproval(
            serverAddress = "http://server-1:3000",
            inviteToken = "INVITE1",
            username = "@alex",
            password = "password123",
            serverName = "Server 1",
        )

        val pending = store.getPendingApprovals()["http://server-1:3000"]
        requireNotNull(pending)
        assertEquals("INVITE1", pending.inviteToken)
        assertEquals("@alex", pending.username)
        assertEquals("Server 1", pending.serverName)
    }

    @Test
    fun savePendingApproval_overwritesExistingByServerAddress() {
        val store = createStore()

        store.savePendingApproval("http://server-1:3000", "INVITE1", "@alex", "password123", "Server 1")
        store.savePendingApproval("http://server-1:3000", "INVITE2", "@mira", "password456", "Server 2")

        val pending = store.getPendingApprovals()
        assertEquals(1, pending.size)
        assertEquals("INVITE2", pending["http://server-1:3000"]?.inviteToken)
        assertEquals("@mira", pending["http://server-1:3000"]?.username)
    }

    @Test
    fun removePendingApproval() {
        val store = createStore()

        store.savePendingApproval("http://server-1:3000", "INVITE1", "@alex", "password123")
        store.removePendingApproval("http://server-1:3000")

        assertTrue(store.getPendingApprovals().isEmpty())
    }

    @Test
    fun getPendingApprovals() {
        val store = createStore()

        store.savePendingApproval("http://server-1:3000", "INVITE1", "@alex", "password123", "Server 1")
        store.savePendingApproval("http://server-2:3000", "INVITE2", "@mira", "password456", "Server 2")

        val pending = store.getPendingApprovals()
        assertEquals(2, pending.size)
        assertEquals("http://server-1:3000", pending["http://server-1:3000"]?.serverAddress)
        assertEquals("http://server-2:3000", pending["http://server-2:3000"]?.serverAddress)
    }

    @Test
    fun setGetDarkTheme() {
        val store = createStore()

        store.isDarkTheme = true

        assertTrue(store.isDarkTheme)
    }

    @Test
    fun setGetUserStatus() {
        val store = createStore()

        store.userStatus = "DO_NOT_DISTURB"

        assertEquals("DO_NOT_DISTURB", store.userStatus)
    }

    @Test
    fun updateServerMetadata_preservesTokenAndUserId() {
        val store = createStore()

        store.saveSession("http://server-1:3000", "token-1", "user-1")

        store.updateServerMetadata(
            serverAddress = "http://server-1:3000",
            serverId = "srv-1",
            serverName = "Server 1",
            serverUsername = "@server1",
            serverDescription = "Updated description",
            serverImageUrl = "https://image.example.com/server.png",
        )

        val session = store.getSession("http://server-1:3000")
        requireNotNull(session)
        assertEquals("token-1", session.sessionToken)
        assertEquals("user-1", session.userId)
        assertEquals("srv-1", session.serverId)
        assertEquals("Server 1", session.serverName)
        assertEquals("@server1", session.serverUsername)
        assertEquals("Updated description", session.serverDescription)
        assertEquals("https://image.example.com/server.png", session.serverImageUrl)
    }

    @Test
    fun getConnectedServers_returnsDescriptionAndImageUrl() {
        val store = createStore()

        store.saveSession(
            serverAddress = "http://server-1:3000",
            sessionToken = "token-1",
            userId = "user-1",
            serverName = "Server 1",
            serverUsername = "@server1",
            serverId = "srv-1",
            serverDescription = "Server description",
            serverImageUrl = "https://image.example.com/server.png",
        )

        val server = store.getConnectedServers().single()

        assertEquals("Server description", server.description)
        assertEquals("https://image.example.com/server.png", server.imageUrl)
    }

    @Test
    fun overwriteExistingSession() {
        val store = createStore()

        store.saveSession("http://server-1:3000", "token-1", "user-1")
        store.saveSession("http://server-1:3000", "token-2", "user-2")

        assertEquals(1, store.getSessions().size)
        assertEquals("token-2", store.getSession("http://server-1:3000")?.sessionToken)
        assertEquals("user-2", store.getSession("http://server-1:3000")?.userId)
    }

    @Test
    fun readCorruptedSessionsJson_returnsEmptyWithoutCrash() {
        val prefs = InMemorySharedPreferences()
        prefs.edit().putString("sessions", "{broken").apply()

        val store = createStore(prefs)

        assertTrue(store.getSessions().isEmpty())
        assertNull(store.getSession("http://server-1:3000"))
    }

    @Test
    fun readCorruptedPendingJson_returnsEmptyWithoutCrash() {
        val prefs = InMemorySharedPreferences()
        prefs.edit().putString("pending_approvals", "{broken").apply()

        val store = createStore(prefs)

        assertTrue(store.getPendingApprovals().isEmpty())
    }

    @Test
    fun removeMissingSession_noCrash() {
        val store = createStore()

        store.removeSession("http://missing:3000")

        assertTrue(store.getSessions().isEmpty())
    }

    @Test
    fun removeMissingPendingApproval_noCrash() {
        val store = createStore()

        store.removePendingApproval("http://missing:3000")

        assertTrue(store.getPendingApprovals().isEmpty())
    }

    @Test
    fun getConnectedServers_fallsBackToAddressWhenMetadataEmpty() {
        val store = createStore()

        store.saveSession("http://server-1:3000", "token-1", "user-1")

        val server = store.getConnectedServers().single()
        assertEquals("http://server-1:3000", server.id)
        assertEquals("http://server-1:3000", server.name)
        assertEquals("", server.username)
        assertEquals("http://server-1:3000", server.address)
    }

    @Test
    fun emptyStore() {
        val store = createStore()

        assertTrue(store.getSessions().isEmpty())
        assertTrue(store.getPendingApprovals().isEmpty())
        assertFalse(store.isDarkTheme)
        assertEquals("ONLINE", store.userStatus)
    }
}
