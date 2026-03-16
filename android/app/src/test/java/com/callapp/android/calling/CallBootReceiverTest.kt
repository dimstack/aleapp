package com.callapp.android.calling

import android.content.Context
import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import com.callapp.android.data.SessionStore
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class CallBootReceiverTest {

    private lateinit var context: Context
    private lateinit var receiver: CallBootReceiver

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        receiver = CallBootReceiver()
        val app = context as android.app.Application
        context.getSharedPreferences("callapp_sessions", Context.MODE_PRIVATE)
            .edit()
            .clear()
            .commit()
        Shadows.shadowOf(app).clearStartedServices()
    }

    @Test
    fun bootCompletedTriggersAvailabilitySync() {
        SessionStore(context).saveSession(
            serverAddress = "https://beta.example.com",
            sessionToken = "token-2",
            userId = "user-2",
        )

        receiver.onReceive(context, Intent(Intent.ACTION_BOOT_COMPLETED))

        val startedIntent = Shadows.shadowOf(context as android.app.Application).getNextStartedService()
        requireNotNull(startedIntent)
        assertEquals(CallAvailabilityService.ACTION_START, startedIntent.action)
    }

    @Test
    fun otherBroadcastDoesNothing() {
        receiver.onReceive(context, Intent("custom.action.TEST"))

        assertNull(Shadows.shadowOf(context as android.app.Application).getNextStartedService())
        assertNull(Shadows.shadowOf(context as android.app.Application).getNextStoppedService())
    }
}
