package com.callapp.server

import com.callapp.server.signaling.SignalMessage
import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.client.plugins.websocket.webSocket
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.put
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import io.ktor.websocket.Frame
import io.ktor.websocket.readText
import kotlinx.coroutines.async
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals

class NotificationsEdgeCasesTest {

    @Test
    fun `markReadOnEmptyList_noError`() = testWithDatabase("test-notifications-edge") { dbPath ->
        application { module() }
        client.get("/health")
        seedInviteToken(dbPath, "NOTIFY01")
        seedUser(dbPath, "@user", "verysecure")
        val token = login("NOTIFY01", "user", "verysecure")

        val response = client.put("/api/notifications/read") {
            bearerAuth(token)
        }

        assertEquals(HttpStatusCode.OK, response.status)
    }

    @Test
    fun `missedCallNotification_createsOnlyForOfflineUser`() = testWithDatabase("test-notifications-edge") { dbPath ->
        application { module() }
        client.get("/health")
        seedInviteToken(dbPath, "NOTIFY01")
        seedUser(dbPath, "@caller", "verysecure")
        val calleeId = seedUser(dbPath, "@callee", "verysecure")
        val callerToken = login("NOTIFY01", "caller", "verysecure")
        val calleeToken = login("NOTIFY01", "callee", "verysecure")

        val wsClient = createClient {
            install(WebSockets)
        }

        wsClient.webSocket("/ws?token=$calleeToken") {
            val receiveJob = async {
                withTimeout(5_000) {
                    val frame = incoming.receive() as Frame.Text
                    SignalMessage.fromJson(frame.readText())
                }
            }

            wsClient.webSocket("/ws?token=$callerToken") {
                send(Frame.Text(SignalMessage.CallRequest(targetUserId = calleeId).toJson()))
            }

            val delivered = receiveJob.await() as SignalMessage.CallRequest
            assertEquals(calleeId, delivered.targetUserId)
        }

        val notificationsResponse = client.get("/api/notifications") {
            bearerAuth(calleeToken)
        }
        val notifications = testJson.parseToJsonElement(notificationsResponse.bodyAsText()).jsonArray
        assertEquals(0, notifications.size)
    }

    @Test
    fun `missedCallNotification_createdWhenCallerEndsUnansweredOnlineCall`() = testWithDatabase("test-notifications-edge") { dbPath ->
        application { module() }
        client.get("/health")
        seedInviteToken(dbPath, "NOTIFY01")
        seedUser(dbPath, "@caller", "verysecure")
        val calleeId = seedUser(dbPath, "@callee", "verysecure")
        val callerToken = login("NOTIFY01", "caller", "verysecure")
        val calleeToken = login("NOTIFY01", "callee", "verysecure")

        val wsClient = createClient {
            install(WebSockets)
        }

        wsClient.webSocket("/ws?token=$calleeToken") {
            val receiveJob = async {
                withTimeout(5_000) {
                    val frame = incoming.receive() as Frame.Text
                    SignalMessage.fromJson(frame.readText())
                }
            }

            wsClient.webSocket("/ws?token=$callerToken") {
                send(Frame.Text(SignalMessage.CallRequest(targetUserId = calleeId).toJson()))
                send(Frame.Text(SignalMessage.CallEnd(targetUserId = calleeId).toJson()))
            }

            val delivered = receiveJob.await() as SignalMessage.CallRequest
            assertEquals(calleeId, delivered.targetUserId)
        }

        val notificationsResponse = client.get("/api/notifications") {
            bearerAuth(calleeToken)
        }
        val notifications = testJson.parseToJsonElement(notificationsResponse.bodyAsText()).jsonArray
        assertEquals(1, notifications.size)
        assertEquals("MISSED_CALL", notifications.first().jsonObject["type"]!!.jsonPrimitive.content)
    }

    @Test
    fun `declinedOnlineCall_doesNotCreateMissedCallNotification`() = testWithDatabase("test-notifications-edge") { dbPath ->
        application { module() }
        client.get("/health")
        seedInviteToken(dbPath, "NOTIFY01")
        val callerId = seedUser(dbPath, "@caller", "verysecure")
        val calleeId = seedUser(dbPath, "@callee", "verysecure")
        val callerToken = login("NOTIFY01", "caller", "verysecure")
        val calleeToken = login("NOTIFY01", "callee", "verysecure")

        val wsClient = createClient {
            install(WebSockets)
        }

        wsClient.webSocket("/ws?token=$callerToken") {
            val receiveJob = async {
                withTimeout(5_000) {
                    val frame = incoming.receive() as Frame.Text
                    SignalMessage.fromJson(frame.readText())
                }
            }

            send(Frame.Text(SignalMessage.CallRequest(targetUserId = calleeId).toJson()))

            wsClient.webSocket("/ws?token=$calleeToken") {
                val incomingCall = withTimeout(5_000) {
                    SignalMessage.fromJson((incoming.receive() as Frame.Text).readText())
                } as SignalMessage.CallRequest
                assertEquals(callerId, incomingCall.fromUserId)
                send(Frame.Text(SignalMessage.CallDecline(targetUserId = callerId).toJson()))
            }

            val delivered = receiveJob.await() as SignalMessage.CallDecline
            assertEquals(callerId, delivered.targetUserId)
        }

        val notificationsResponse = client.get("/api/notifications") {
            bearerAuth(calleeToken)
        }
        val notifications = testJson.parseToJsonElement(notificationsResponse.bodyAsText()).jsonArray
        assertEquals(0, notifications.size)
    }

    @Test
    fun `clearNotifications_alsoResetsUnreadCount`() = testWithDatabase("test-notifications-edge") { dbPath ->
        application { module() }
        client.get("/health")
        seedInviteToken(dbPath, "NOTIFY01")
        val userId = seedUser(dbPath, "@user", "verysecure")
        seedNotification(dbPath, userId, "REQUEST_APPROVED", "Test Server", "Approved")
        seedNotification(dbPath, userId, "REQUEST_DECLINED", "Test Server", "Declined")
        val token = login("NOTIFY01", "user", "verysecure")

        val response = client.delete("/api/notifications") {
            bearerAuth(token)
        }

        assertEquals(HttpStatusCode.NoContent, response.status)
        assertEquals(0, countNotifications(dbPath, userId))
        assertEquals(0, countUnreadNotifications(dbPath, userId))
    }
}
