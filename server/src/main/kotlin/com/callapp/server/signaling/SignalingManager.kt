package com.callapp.server.signaling

import com.callapp.server.auth.SessionPrincipal
import com.callapp.server.models.NotificationType
import com.callapp.server.repository.NotificationRepository
import com.callapp.server.repository.ServerRepository
import io.ktor.websocket.CloseReason
import io.ktor.websocket.Frame
import io.ktor.websocket.WebSocketSession
import io.ktor.websocket.close
import io.ktor.websocket.readText
import java.util.concurrent.ConcurrentHashMap

class SignalingManager(
    private val serverRepository: ServerRepository,
    private val notificationRepository: NotificationRepository,
) {
    private val sessions = ConcurrentHashMap<String, ConnectedClient>()

    suspend fun connect(principal: SessionPrincipal, session: WebSocketSession) {
        val userId = requireNotNull(principal.userId)
        sessions[userId] = ConnectedClient(principal, session)
    }

    fun disconnect(userId: String?) {
        if (userId != null) {
            sessions.remove(userId)
        }
    }

    suspend fun handleIncoming(principal: SessionPrincipal, rawMessage: String) {
        val senderId = requireNotNull(principal.userId)
        val message = SignalMessage.fromJson(rawMessage).withSender(senderId)
        val target = sessions[message.targetUserId]

        if (message is SignalMessage.StatusUpdate) {
            broadcastStatus(principal.serverId, message)
            return
        }

        if (target == null || target.principal.serverId != principal.serverId) {
            sessions[senderId]?.session?.send(Frame.Text(SignalMessage.CallBusy(targetUserId = message.targetUserId).toJson()))
            if (message is SignalMessage.CallRequest || message is SignalMessage.Offer) {
                val serverName = serverRepository.getCurrentServer()?.name ?: "CallApp Server"
                notificationRepository.create(
                    userId = senderId,
                    type = NotificationType.MISSED_CALL,
                    serverName = serverName,
                    message = "Target user is offline",
                )
            }
            return
        }

        target.session.send(Frame.Text(message.toJson()))
    }

    private suspend fun broadcastStatus(serverId: String, message: SignalMessage.StatusUpdate) {
        sessions.values
            .filter { it.principal.serverId == serverId && it.principal.userId != message.userId }
            .forEach { it.session.send(Frame.Text(message.toJson())) }
    }

    suspend fun reject(session: WebSocketSession, reason: String) {
        session.close(CloseReason(CloseReason.Codes.VIOLATED_POLICY, reason))
    }
}

private data class ConnectedClient(
    val principal: SessionPrincipal,
    val session: WebSocketSession,
)

private fun SignalMessage.withSender(senderId: String): SignalMessage = when (this) {
    is SignalMessage.Offer -> copy(fromUserId = senderId)
    is SignalMessage.Answer -> copy(fromUserId = senderId)
    is SignalMessage.IceCandidate -> copy(fromUserId = senderId)
    is SignalMessage.CallRequest -> copy(fromUserId = senderId)
    is SignalMessage.CallResponse -> copy(fromUserId = senderId)
    is SignalMessage.CallEnd -> this
    is SignalMessage.CallDecline -> this
    is SignalMessage.CallBusy -> this
    is SignalMessage.StatusUpdate -> copy(userId = senderId)
}
