package com.callapp.server

import com.callapp.server.signaling.SignalMessage
import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.client.plugins.websocket.webSocket
import io.ktor.client.request.get
import io.ktor.websocket.Frame
import io.ktor.websocket.close
import io.ktor.websocket.readText
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class StatusUpdateTest {

    @Test
    fun `statusUpdate_broadcastsToOtherConnectedUsers`() = testWithDatabase("test-status-update") { dbPath ->
        application { module() }
        client.get("/health")
        seedInviteToken(dbPath, "STATUS01")
        val senderId = seedUser(dbPath, "@sender", "verysecure")
        seedUser(dbPath, "@receiver", "verysecure")
        val senderToken = login("STATUS01", "sender", "verysecure")
        val receiverToken = login("STATUS01", "receiver", "verysecure")

        val wsClient = createClient { install(WebSockets) }
        wsClient.webSocket("/ws?token=$receiverToken") {
            val receiveJob = async {
                withTimeout(5_000) {
                    val frame = incoming.receive() as Frame.Text
                    SignalMessage.fromJson(frame.readText())
                }
            }

            wsClient.webSocket("/ws?token=$senderToken") {
                send(Frame.Text(SignalMessage.StatusUpdate(userId = senderId, status = "do_not_disturb").toJson()))
            }

            val message = receiveJob.await() as SignalMessage.StatusUpdate
            assertEquals(senderId, message.userId)
            assertEquals("do_not_disturb", message.status)
        }
    }

    @Test
    fun `invisibleUser_notShownAsOnline`() = testWithDatabase("test-status-update") { dbPath ->
        application { module() }
        client.get("/health")
        seedInviteToken(dbPath, "STATUS01")
        val senderId = seedUser(dbPath, "@sender", "verysecure")
        seedUser(dbPath, "@receiver", "verysecure")
        val senderToken = login("STATUS01", "sender", "verysecure")
        val receiverToken = login("STATUS01", "receiver", "verysecure")

        val wsClient = createClient { install(WebSockets) }
        wsClient.webSocket("/ws?token=$receiverToken") {
            val senderJob = async {
                wsClient.webSocket("/ws?token=$senderToken") {
                    send(Frame.Text(SignalMessage.StatusUpdate(userId = senderId, status = "invisible").toJson()))
                    delay(1_500)
                }
            }

            val frame = withTimeoutOrNull(1_000) { incoming.receiveCatching().getOrNull() }
            assertNull(frame)
            senderJob.await()
        }
    }

    @Test
    fun `disconnectSignaling_setsOfflineStatus`() = testWithDatabase("test-status-update") { dbPath ->
        application { module() }
        client.get("/health")
        seedInviteToken(dbPath, "STATUS01")
        seedUser(dbPath, "@sender", "verysecure")
        seedUser(dbPath, "@receiver", "verysecure")
        val senderToken = login("STATUS01", "sender", "verysecure")
        val receiverToken = login("STATUS01", "receiver", "verysecure")

        val wsClient = createClient { install(WebSockets) }
        wsClient.webSocket("/ws?token=$receiverToken") {
            val receiveJob = async {
                withTimeout(5_000) {
                    val frame = incoming.receive() as Frame.Text
                    SignalMessage.fromJson(frame.readText())
                }
            }

            wsClient.webSocket("/ws?token=$senderToken") {
                close()
            }

            val message = receiveJob.await() as SignalMessage.StatusUpdate
            assertEquals("offline", message.status)
        }
    }
}
