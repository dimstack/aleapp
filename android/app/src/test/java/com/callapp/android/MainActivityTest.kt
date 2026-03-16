package com.callapp.android

import com.callapp.android.data.ServiceLocator
import com.callapp.android.data.SessionStore
import com.callapp.android.testutil.InMemorySharedPreferences
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class MainActivityTest {

    @Before
    fun setUp() {
        ServiceLocator.sessionStore = SessionStore.createForTests(InMemorySharedPreferences())
        ServiceLocator.activeServerAddress = ""
        ServiceLocator.currentUserId = ""
    }

    @Test
    fun restoreSessionsRestoresClientsAndActiveContext() {
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
        ServiceLocator.sessionStore.activeServerAddress = "https://alpha.example.com"
        ServiceLocator.sessionStore.activeUserId = "user-1"

        invokeRestoreSessions(MainActivity(), ServiceLocator.sessionStore)

        assertEquals("token-1", ServiceLocator.connectionManager.getClient("https://alpha.example.com").sessionToken)
        assertEquals("token-2", ServiceLocator.connectionManager.getClient("https://beta.example.com").sessionToken)
        assertEquals("https://alpha.example.com", ServiceLocator.activeServerAddress)
        assertEquals("user-1", ServiceLocator.currentUserId)
    }

    @Test
    fun restoreSessionsLeavesActiveContextEmptyWhenNoActiveServer() {
        ServiceLocator.sessionStore.saveSession(
            serverAddress = "https://alpha.example.com",
            sessionToken = "token-1",
            userId = "user-1",
            setActive = false,
        )

        invokeRestoreSessions(MainActivity(), ServiceLocator.sessionStore)

        assertEquals("token-1", ServiceLocator.connectionManager.getClient("https://alpha.example.com").sessionToken)
        assertEquals("", ServiceLocator.activeServerAddress)
        assertEquals("", ServiceLocator.currentUserId)
    }

    private fun invokeRestoreSessions(activity: MainActivity, store: SessionStore) {
        val method = MainActivity::class.java.getDeclaredMethod("restoreSessions", SessionStore::class.java)
        method.isAccessible = true
        method.invoke(activity, store)
    }
}
