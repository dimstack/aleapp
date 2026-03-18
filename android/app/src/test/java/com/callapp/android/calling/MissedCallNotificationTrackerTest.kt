package com.callapp.android.calling

import com.callapp.android.domain.model.Notification
import com.callapp.android.domain.model.NotificationType
import com.callapp.android.testutil.InMemorySharedPreferences
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MissedCallNotificationTrackerTest {

    @Test
    fun firstSyncSeedsExistingNotificationsWithoutPush() {
        val tracker = MissedCallNotificationTracker(InMemorySharedPreferences())

        val fresh = tracker.consumeNew(
            serverAddress = "https://server.example.com",
            notifications = listOf(testMissedCall("n1"), testMissedCall("n2")),
        )

        assertTrue(fresh.isEmpty())
    }

    @Test
    fun nextSyncReturnsOnlyNewUnreadMissedCalls() {
        val tracker = MissedCallNotificationTracker(InMemorySharedPreferences())
        val serverAddress = "https://server.example.com"

        tracker.consumeNew(
            serverAddress = serverAddress,
            notifications = listOf(testMissedCall("n1")),
        )

        val fresh = tracker.consumeNew(
            serverAddress = serverAddress,
            notifications = listOf(
                testMissedCall("n1"),
                testMissedCall("n2"),
                testRequest("n3"),
            ),
        )

        assertEquals(listOf("n2"), fresh.map { it.id })
    }

    @Test
    fun readMissedCallsDoNotTriggerPush() {
        val tracker = MissedCallNotificationTracker(InMemorySharedPreferences())
        val serverAddress = "https://server.example.com"

        tracker.consumeNew(serverAddress, emptyList())

        val fresh = tracker.consumeNew(
            serverAddress = serverAddress,
            notifications = listOf(testMissedCall("n1", isRead = true)),
        )

        assertTrue(fresh.isEmpty())
    }

    private fun testMissedCall(
        id: String,
        isRead: Boolean = false,
    ) = Notification(
        id = id,
        type = NotificationType.MISSED_CALL,
        serverName = "Test Server",
        message = "Missed call",
        isRead = isRead,
        createdAt = "2026-03-18T20:13:00Z",
    )

    private fun testRequest(id: String) = Notification(
        id = id,
        type = NotificationType.REQUEST_SENT,
        serverName = "Test Server",
        message = "Request",
        isRead = false,
        createdAt = "2026-03-18T20:13:00Z",
    )
}
