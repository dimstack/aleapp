package com.callapp.server.signaling

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs

class SignalMessageTest {

    @Test
    fun serializesAndDeserializesAllSupportedMessages() {
        val messages = listOf(
            SignalMessage.Offer(
                sdp = "offer-sdp",
                fromUserId = "user-a",
                targetUserId = "user-b",
            ),
            SignalMessage.Answer(
                sdp = "answer-sdp",
                fromUserId = "user-b",
                targetUserId = "user-a",
            ),
            SignalMessage.IceCandidate(
                candidate = "candidate:1",
                sdpMid = "audio",
                sdpMLineIndex = 0,
                fromUserId = "user-a",
                targetUserId = "user-b",
            ),
            SignalMessage.CallRequest(
                fromUserId = "user-a",
                fromUserName = "Anna",
                fromServerName = "Tech Community",
                targetUserId = "user-b",
            ),
            SignalMessage.CallResponse(
                accepted = true,
                fromUserId = "user-b",
                targetUserId = "user-a",
            ),
            SignalMessage.CallEnd(targetUserId = "user-b"),
            SignalMessage.CallDecline(targetUserId = "user-b"),
            SignalMessage.CallBusy(targetUserId = "user-b"),
            SignalMessage.StatusUpdate(
                userId = "user-a",
                status = "ONLINE",
            ),
        )

        messages.forEach { message ->
            assertEquals(message, SignalMessage.fromJson(message.toJson()))
        }
    }

    @Test
    fun ignoresUnknownFieldsForKnownType() {
        val parsed = SignalMessage.fromJson(
            """
            {
              "type": "call_request",
              "from_user_id": "user-a",
              "from_user_name": "Anna",
              "from_server_name": "Tech Community",
              "target_user_id": "user-b",
              "extra_field": "ignored"
            }
            """.trimIndent(),
        )

        assertIs<SignalMessage.CallRequest>(parsed)
        assertEquals("user-a", parsed.fromUserId)
        assertEquals("user-b", parsed.targetUserId)
    }

    @Test
    fun throwsForUnknownType() {
        assertFailsWith<IllegalArgumentException> {
            SignalMessage.fromJson("""{"type":"unknown","target_user_id":"user-b"}""")
        }
    }

    @Test
    fun throwsForMissingRequiredField() {
        assertFailsWith<IllegalArgumentException> {
            SignalMessage.fromJson("""{"type":"offer","sdp":"offer-sdp"}""")
        }
    }
}
