package com.callapp.android.calling

import android.content.SharedPreferences
import com.callapp.android.domain.model.Notification
import com.callapp.android.domain.model.NotificationType

internal class MissedCallNotificationTracker(
    private val prefs: SharedPreferences,
) {
    fun consumeNew(serverAddress: String, notifications: List<Notification>): List<Notification> {
        val unreadMissedCalls = notifications.filter {
            it.type == NotificationType.MISSED_CALL && !it.isRead
        }
        if (unreadMissedCalls.isEmpty()) return emptyList()

        val delivered = prefs.getStringSet(KEY_DELIVERED, emptySet()).orEmpty().toMutableSet()
        val initializedServers = prefs.getStringSet(KEY_INITIALIZED_SERVERS, emptySet()).orEmpty().toMutableSet()

        if (serverAddress !in initializedServers) {
            unreadMissedCalls.forEach { delivered += deliveredKey(serverAddress, it.id) }
            initializedServers += serverAddress
            save(delivered, initializedServers)
            return emptyList()
        }

        val fresh = unreadMissedCalls.filter { notification ->
            val key = deliveredKey(serverAddress, notification.id)
            if (key in delivered) {
                false
            } else {
                delivered += key
                true
            }
        }

        if (fresh.isNotEmpty()) {
            save(delivered, initializedServers)
        }

        return fresh
    }

    private fun save(
        delivered: Set<String>,
        initializedServers: Set<String>,
    ) {
        val trimmedDelivered = delivered.toList().takeLast(MAX_TRACKED_IDS).toSet()
        prefs.edit()
            .putStringSet(KEY_DELIVERED, trimmedDelivered)
            .putStringSet(KEY_INITIALIZED_SERVERS, initializedServers)
            .apply()
    }

    private fun deliveredKey(serverAddress: String, notificationId: String): String =
        "$serverAddress::$notificationId"

    private companion object {
        private const val KEY_DELIVERED = "delivered_ids"
        private const val KEY_INITIALIZED_SERVERS = "initialized_servers"
        private const val MAX_TRACKED_IDS = 300
    }
}
