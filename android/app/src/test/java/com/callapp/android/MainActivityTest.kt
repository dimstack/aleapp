package com.callapp.android

import android.content.Intent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import com.callapp.android.calling.IncomingCallIntentContract
import com.callapp.android.calling.IncomingCallPayload
import com.callapp.android.data.ServiceLocator
import com.callapp.android.data.SessionStore
import com.callapp.android.testutil.InMemorySharedPreferences
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

    @Test
    fun incomingCallIntentEnablesShowWhenLockedAndTurnScreenOn() {
        val intent = IncomingCallIntentContract.putExtras(
            Intent(),
            IncomingCallPayload(
                serverAddress = "https://alpha.example.com",
                userId = "user-1",
                contactName = "Alex",
                serverName = "Tech Community",
                notificationId = 42,
            ),
        )

        var showWhenLocked: Boolean? = null
        var turnScreenOn: Boolean? = null
        var addedFlags: Int? = null
        var clearedFlags: Int? = null

        IncomingCallWindowBehavior.apply(
            enabled = IncomingCallWindowBehavior.shouldEnable(intent),
            sdkInt = 34,
            setShowWhenLocked = { showWhenLocked = it },
            setTurnScreenOn = { turnScreenOn = it },
            addLegacyFlags = { addedFlags = it },
            clearLegacyFlags = { clearedFlags = it },
        )

        assertTrue(showWhenLocked == true)
        assertTrue(turnScreenOn == true)
        assertEquals(null, addedFlags)
        assertEquals(null, clearedFlags)
    }

    @Test
    fun regularLaunchKeepsLockScreenOverridesDisabled() {
        var showWhenLocked: Boolean? = null
        var turnScreenOn: Boolean? = null
        var addedFlags: Int? = null
        var clearedFlags: Int? = null

        IncomingCallWindowBehavior.apply(
            enabled = IncomingCallWindowBehavior.shouldEnable(null),
            sdkInt = 34,
            setShowWhenLocked = { showWhenLocked = it },
            setTurnScreenOn = { turnScreenOn = it },
            addLegacyFlags = { addedFlags = it },
            clearLegacyFlags = { clearedFlags = it },
        )

        assertFalse(showWhenLocked == true)
        assertFalse(turnScreenOn == true)
        assertEquals(null, addedFlags)
        assertEquals(null, clearedFlags)
    }

    @Test
    fun legacyDevicesToggleWindowFlags() {
        var addedFlags: Int? = null
        var clearedFlags: Int? = null

        IncomingCallWindowBehavior.apply(
            enabled = true,
            sdkInt = 26,
            setShowWhenLocked = {},
            setTurnScreenOn = {},
            addLegacyFlags = { addedFlags = it },
            clearLegacyFlags = { clearedFlags = it },
        )

        assertTrue((addedFlags ?: 0) != 0)
        assertEquals(null, clearedFlags)

        IncomingCallWindowBehavior.apply(
            enabled = false,
            sdkInt = 26,
            setShowWhenLocked = {},
            setTurnScreenOn = {},
            addLegacyFlags = { addedFlags = it },
            clearLegacyFlags = { clearedFlags = it },
        )

        assertTrue((clearedFlags ?: 0) != 0)
    }

    @Test
    fun closesAfterIncomingFlowOnlyWhenStillLocked() {
        assertTrue(
            IncomingCallWindowBehavior.shouldCloseActivityAfterIncomingCallEnds(
                openedFromIncomingCallWhileLocked = true,
                isKeyguardLocked = true,
            ),
        )
        assertFalse(
            IncomingCallWindowBehavior.shouldCloseActivityAfterIncomingCallEnds(
                openedFromIncomingCallWhileLocked = true,
                isKeyguardLocked = false,
            ),
        )
        assertFalse(
            IncomingCallWindowBehavior.shouldCloseActivityAfterIncomingCallEnds(
                openedFromIncomingCallWhileLocked = false,
                isKeyguardLocked = true,
            ),
        )
    }

    private fun invokeRestoreSessions(activity: MainActivity, store: SessionStore) {
        val method = MainActivity::class.java.getDeclaredMethod("restoreSessions", SessionStore::class.java)
        method.isAccessible = true
        method.invoke(activity, store)
    }
}
