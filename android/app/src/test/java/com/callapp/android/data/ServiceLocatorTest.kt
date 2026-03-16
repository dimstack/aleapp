package com.callapp.android.data

import com.callapp.android.testutil.InMemorySharedPreferences
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

class ServiceLocatorTest {

    @Before
    fun setUp() {
        ServiceLocator.sessionStore = SessionStore.createForTests(InMemorySharedPreferences())
        ServiceLocator.activeServerAddress = ""
        ServiceLocator.currentUserId = ""
    }

    @Test
    fun clearServerSessionRemovesSessionPendingApprovalAndActivePointers() {
        ServiceLocator.sessionStore.saveSession(
            serverAddress = "https://alpha.example.com",
            sessionToken = "token-1",
            userId = "user-1",
        )
        ServiceLocator.sessionStore.savePendingApproval(
            serverAddress = "https://alpha.example.com",
            inviteToken = "INVITE123",
            username = "@anna",
            password = "secret123",
        )
        ServiceLocator.activeServerAddress = "https://alpha.example.com"
        ServiceLocator.currentUserId = "user-1"
        ServiceLocator.sessionStore.activeServerAddress = "https://alpha.example.com"
        ServiceLocator.sessionStore.activeUserId = "user-1"

        ServiceLocator.clearServerSession("https://alpha.example.com")

        assertNull(ServiceLocator.sessionStore.getSession("https://alpha.example.com"))
        assertEquals(emptyMap<String, PendingApprovalSession>(), ServiceLocator.sessionStore.getPendingApprovals())
        assertEquals("", ServiceLocator.activeServerAddress)
        assertEquals("", ServiceLocator.currentUserId)
        assertEquals("", ServiceLocator.sessionStore.activeServerAddress)
        assertEquals("", ServiceLocator.sessionStore.activeUserId)
    }

    @Test
    fun clearServerSessionKeepsOtherActiveSessionUntouched() {
        ServiceLocator.sessionStore.saveSession(
            serverAddress = "https://alpha.example.com",
            sessionToken = "token-1",
            userId = "user-1",
        )
        ServiceLocator.sessionStore.saveSession(
            serverAddress = "https://beta.example.com",
            sessionToken = "token-2",
            userId = "user-2",
            setActive = false,
        )
        ServiceLocator.activeServerAddress = "https://alpha.example.com"
        ServiceLocator.currentUserId = "user-1"
        ServiceLocator.sessionStore.activeServerAddress = "https://alpha.example.com"
        ServiceLocator.sessionStore.activeUserId = "user-1"

        ServiceLocator.clearServerSession("https://beta.example.com")

        assertNull(ServiceLocator.sessionStore.getSession("https://beta.example.com"))
        requireNotNull(ServiceLocator.sessionStore.getSession("https://alpha.example.com"))
        assertEquals("https://alpha.example.com", ServiceLocator.activeServerAddress)
        assertEquals("user-1", ServiceLocator.currentUserId)
        assertEquals("https://alpha.example.com", ServiceLocator.sessionStore.activeServerAddress)
        assertEquals("user-1", ServiceLocator.sessionStore.activeUserId)
    }
}
