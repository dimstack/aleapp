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
class CallAvailabilityServiceControllerTest {

    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        val app = context as android.app.Application
        context.getSharedPreferences("callapp_sessions", Context.MODE_PRIVATE)
            .edit()
            .clear()
            .commit()
        Shadows.shadowOf(app).clearStartedServices()
    }

    @Test
    fun syncStartsForegroundServiceWhenSessionsExist() {
        val store = SessionStore(context)
        store.saveSession(
            serverAddress = "https://alpha.example.com",
            sessionToken = "token-1",
            userId = "user-1",
        )

        CallAvailabilityServiceController.sync(context)

        val startedIntent = Shadows.shadowOf(context as android.app.Application).getNextStartedService()
        requireNotNull(startedIntent)
        assertEquals(Intent(context, CallAvailabilityService::class.java).component, startedIntent.component)
        assertEquals(CallAvailabilityService.ACTION_START, startedIntent.action)
        assertNull(Shadows.shadowOf(context as android.app.Application).getNextStoppedService())
    }

    @Test
    fun syncStopsServiceWhenSessionsMissing() {
        CallAvailabilityServiceController.sync(context)

        val stoppedIntent = Shadows.shadowOf(context as android.app.Application).getNextStoppedService()
        requireNotNull(stoppedIntent)
        assertEquals(Intent(context, CallAvailabilityService::class.java).component, stoppedIntent.component)
        assertNull(Shadows.shadowOf(context as android.app.Application).getNextStartedService())
    }
}
