package com.callapp.android.calling

import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.shadow.api.Shadow
import org.robolectric.shadows.ShadowNotificationManager

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class IncomingCallNotificationManagerTest {

    @Test
    fun dismissFallsBackToComputedNotificationId() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val notificationManager = context.getSystemService(NotificationManager::class.java)
        val channelId = "test_incoming_calls"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            notificationManager.createNotificationChannel(
                NotificationChannel(
                    channelId,
                    "Test incoming calls",
                    NotificationManager.IMPORTANCE_HIGH,
                ),
            )
        }

        val notificationId = IncomingCallNotificationManager.notificationIdFor(
            serverAddress = "https://server.example.com",
            userId = "user-42",
        )

        notificationManager.notify(
            notificationId,
            NotificationCompat.Builder(context, channelId)
                .setSmallIcon(android.R.drawable.sym_call_incoming)
                .setContentTitle("Incoming call")
                .build(),
        )

        val shadowManager = Shadow.extract<ShadowNotificationManager>(notificationManager)
        assertEquals(1, shadowManager.allNotifications.size)

        IncomingCallNotificationManager.dismiss(
            context = context,
            serverAddress = "https://server.example.com",
            userId = "user-42",
        )

        assertEquals(0, shadowManager.allNotifications.size)
    }
}
