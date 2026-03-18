package com.callapp.android.calling

import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.media.AudioAttributes
import android.media.RingtoneManager
import androidx.test.core.app.ApplicationProvider
import com.callapp.android.data.ServiceLocator
import com.callapp.android.data.SessionStore
import com.callapp.android.testutil.InMemorySharedPreferences
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class CallAvailabilityServiceTest {

    @Before
    fun setUp() {
        ServiceLocator.sessionStore = SessionStore.createForTests(InMemorySharedPreferences())
    }

    @Test
    fun stopActionReturnsNotSticky() {
        val service = Robolectric.buildService(CallAvailabilityService::class.java).create().get()

        val result = service.onStartCommand(
            Intent(ApplicationProvider.getApplicationContext(), CallAvailabilityService::class.java).apply {
                action = CallAvailabilityService.ACTION_STOP
            },
            0,
            1,
        )

        assertEquals(Service.START_NOT_STICKY, result)
    }

    @Test
    fun emptySessionsReturnNotSticky() {
        val service = Robolectric.buildService(CallAvailabilityService::class.java).create().get()

        val result = service.onStartCommand(Intent(), 0, 1)

        assertEquals(Service.START_NOT_STICKY, result)
    }

    @Test
    fun createsIncomingCallChannelWithSystemRingtone() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()

        Robolectric.buildService(CallAvailabilityService::class.java).create().get()

        val notificationManager = context.getSystemService(NotificationManager::class.java)
        val channel = notificationManager.getNotificationChannel("incoming_call_channel_v2")

        assertNotNull(channel)
        assertEquals(
            RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE),
            channel.sound,
        )
        assertEquals(
            AudioAttributes.USAGE_NOTIFICATION_RINGTONE,
            channel.audioAttributes?.usage,
        )
    }
}
