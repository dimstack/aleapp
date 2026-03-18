package com.callapp.server.signaling

import com.callapp.server.auth.SessionPrincipal
import com.callapp.server.models.NotificationType
import com.callapp.server.repository.NotificationRepository
import com.callapp.server.repository.ServerRepository
import com.callapp.server.repository.UserRepository
import io.ktor.websocket.CloseReason
import io.ktor.websocket.Frame
import io.ktor.websocket.WebSocketSession
import io.ktor.websocket.close
import io.ktor.websocket.readText
import java.util.concurrent.ConcurrentHashMap

class SignalingManager(
    private val serverRepository: ServerRepository,
    private val userRepository: UserRepository,
    private val notificationRepository: NotificationRepository,
) {
    private val sessions = ConcurrentHashMap<String, ConnectedClient>()
    private val pendingCalls = ConcurrentHashMap<String, PendingCall>()

    suspend fun connect(principal: SessionPrincipal, session: WebSocketSession) {
        val userId = requireNotNull(principal.userId)
        sessions[userId] = ConnectedClient(principal, session)
    }

    suspend fun disconnect(principal: SessionPrincipal?) {
        val userId = principal?.userId ?: return
        val removed = sessions.remove(userId) ?: return
        pendingCalls.remove(userId)
        pendingCalls.entries.removeIf { (_, pendingCall) -> pendingCall.callerUserId == userId }
        if (removed.principal.serverId == principal.serverId) {
            broadcastStatus(
                serverId = principal.serverId,
                message = SignalMessage.StatusUpdate(
                    userId = userId,
                    status = "offline",
                ),
            )
        }
    }

    suspend fun handleIncoming(principal: SessionPrincipal, rawMessage: String) {
        val senderId = requireNotNull(principal.userId)
        val message = SignalMessage.fromJson(rawMessage).withSender(
            senderId = senderId,
            senderDisplayName = userRepository.findById(senderId)?.displayName.orEmpty(),
            senderServerName = serverRepository.getCurrentServer()?.name.orEmpty(),
        )
        val target = sessions[message.targetUserId]

        if (message is SignalMessage.StatusUpdate) {
            if (message.status.equals("invisible", ignoreCase = true)) {
                return
            }
            broadcastStatus(principal.serverId, message)
            return
        }

        when (message) {
            is SignalMessage.CallRequest -> {
                pendingCalls[message.targetUserId] = PendingCall(
                    callerUserId = senderId,
                )
            }
            is SignalMessage.CallResponse -> {
                pendingCalls.remove(senderId)
            }
            is SignalMessage.CallDecline -> {
                pendingCalls.remove(senderId)
            }
            is SignalMessage.CallEnd -> {
                val pendingCall = pendingCalls[message.targetUserId]
                if (pendingCall?.callerUserId == senderId) {
                    pendingCalls.remove(message.targetUserId)
                    createMissedCallNotification(
                        userId = message.targetUserId,
                        callerUserId = senderId,
                    )
                }
            }
            is SignalMessage.CallBusy -> {
                pendingCalls.remove(message.targetUserId)
            }
            else -> Unit
        }

        if (target == null || target.principal.serverId != principal.serverId) {
            sessions[senderId]?.session?.send(Frame.Text(SignalMessage.CallBusy(targetUserId = message.targetUserId).toJson()))
            if (message is SignalMessage.CallRequest || message is SignalMessage.Offer) {
                pendingCalls.remove(message.targetUserId)
                createMissedCallNotification(
                    userId = message.targetUserId,
                    callerUserId = senderId,
                )
            }
            return
        }

        target.session.send(Frame.Text(message.toJson()))
    }

    private fun createMissedCallNotification(userId: String, callerUserId: String) {
        val serverName = serverRepository.getCurrentServer()?.name ?: "CallApp Server"
        val caller = userRepository.findById(callerUserId)
        notificationRepository.create(
            userId = userId,
            type = NotificationType.MISSED_CALL,
            serverName = serverName,
            message = caller?.displayName?.takeIf { it.isNotBlank() }?.let { "Missed call from $it" }
                ?: "You missed a call",
            actorUserId = callerUserId,
            actorUsername = caller?.username,
            actorDisplayName = caller?.displayName,
            actorAvatarUrl = caller?.avatarUrl,
        )
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

private data class PendingCall(
    val callerUserId: String,
)

private fun SignalMessage.withSender(
    senderId: String,
    senderDisplayName: String,
    senderServerName: String,
): SignalMessage = when (this) {
    is SignalMessage.Offer -> copy(fromUserId = senderId)
    is SignalMessage.Answer -> copy(fromUserId = senderId)
    is SignalMessage.IceCandidate -> copy(fromUserId = senderId)
    is SignalMessage.CallRequest -> copy(
        fromUserId = senderId,
        fromUserName = senderDisplayName.ifBlank { fromUserName },
        fromServerName = senderServerName.ifBlank { fromServerName },
    )
    is SignalMessage.CallResponse -> copy(fromUserId = senderId)
    is SignalMessage.CallEnd -> this
    is SignalMessage.CallDecline -> this
    is SignalMessage.CallBusy -> this
    is SignalMessage.StatusUpdate -> copy(userId = senderId)
}
