package com.callapp.android.calling

import android.content.Intent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class IncomingCallPayloadTest {

    @Test
    fun putExtrasAndRestorePayload() {
        val payload = IncomingCallPayload(
            serverAddress = "https://callapp.example",
            userId = "user-1",
            contactName = "Анна Смирнова",
            serverName = "Tech Community",
            notificationId = 42,
        )

        val restored = IncomingCallIntentContract.fromIntent(
            IncomingCallIntentContract.putExtras(Intent(), payload),
        )

        assertEquals(payload, restored)
    }

    @Test
    fun fromIntentReturnsNullWhenRequiredFieldsAreMissing() {
        val intent = Intent().putExtra("incoming_call_user_id", "user-1")

        assertNull(IncomingCallIntentContract.fromIntent(intent))
        assertNull(IncomingCallIntentContract.fromIntent(null))
    }
}
