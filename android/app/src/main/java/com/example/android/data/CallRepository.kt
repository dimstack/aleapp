package com.example.android.data

import android.content.Context
import android.util.Log
import com.example.android.network.signaling.SignalingClient
import com.example.android.network.signaling.SignalMessage
import com.example.android.webrtc.WebRtcManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.webrtc.IceCandidate
import org.webrtc.MediaStream
import org.webrtc.PeerConnection
import org.webrtc.SessionDescription

enum class CallConnectionState {
    IDLE,
    CONNECTING,
    CONNECTED,
    DISCONNECTED,
    FAILED,
}

class CallRepository(
    context: Context,
    private val signalingClient: SignalingClient,
    private val serverAddress: String,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private val _connectionState = MutableStateFlow(CallConnectionState.IDLE)
    val connectionState: StateFlow<CallConnectionState> = _connectionState

    private val _remoteStream = MutableStateFlow<MediaStream?>(null)
    val remoteStream: StateFlow<MediaStream?> = _remoteStream

    private var targetUserId: String = ""

    val webRtcManager = WebRtcManager(context, object : WebRtcManager.Listener {
        override fun onIceCandidate(candidate: IceCandidate) {
            signalingClient.send(
                SignalMessage.IceCandidate(
                    candidate = candidate.sdp,
                    sdpMid = candidate.sdpMid,
                    sdpMLineIndex = candidate.sdpMLineIndex,
                    targetUserId = targetUserId,
                )
            )
        }

        override fun onConnectionChange(state: PeerConnection.PeerConnectionState) {
            _connectionState.value = when (state) {
                PeerConnection.PeerConnectionState.CONNECTING -> CallConnectionState.CONNECTING
                PeerConnection.PeerConnectionState.CONNECTED -> CallConnectionState.CONNECTED
                PeerConnection.PeerConnectionState.DISCONNECTED -> CallConnectionState.DISCONNECTED
                PeerConnection.PeerConnectionState.FAILED -> CallConnectionState.FAILED
                PeerConnection.PeerConnectionState.CLOSED -> CallConnectionState.DISCONNECTED
                else -> _connectionState.value
            }
        }

        override fun onRemoteTrackAdded(stream: MediaStream) {
            _remoteStream.value = stream
        }
    })

    init {
        scope.launch {
            signalingClient.messages.collect { message ->
                handleSignalMessage(message)
            }
        }
    }

    // ── Outgoing call ────────────────────────────────────────────────────

    fun startOutgoingCall(targetUserId: String, enableVideo: Boolean) {
        this.targetUserId = targetUserId
        _connectionState.value = CallConnectionState.CONNECTING

        webRtcManager.startCapture(enableVideo = enableVideo, enableAudio = true)
        webRtcManager.createPeerConnection(serverAddress)

        webRtcManager.createOffer { sdp ->
            signalingClient.send(
                SignalMessage.Offer(
                    sdp = sdp.description,
                    targetUserId = targetUserId,
                )
            )
        }
    }

    // ── Incoming call ────────────────────────────────────────────────────

    fun acceptIncomingCall(callerUserId: String, enableVideo: Boolean) {
        this.targetUserId = callerUserId
        _connectionState.value = CallConnectionState.CONNECTING

        webRtcManager.startCapture(enableVideo = enableVideo, enableAudio = true)
        webRtcManager.createPeerConnection(serverAddress)

        // The remote offer should already be set via handleSignalMessage.
        // Create and send answer.
        webRtcManager.createAnswer { sdp ->
            signalingClient.send(
                SignalMessage.Answer(
                    sdp = sdp.description,
                    targetUserId = callerUserId,
                )
            )
        }
    }

    fun declineIncomingCall(callerUserId: String) {
        signalingClient.send(
            SignalMessage.CallResponse(
                accepted = false,
                fromUserId = "",
                targetUserId = callerUserId,
            )
        )
    }

    // ── End call ─────────────────────────────────────────────────────────

    fun endCall() {
        _connectionState.value = CallConnectionState.DISCONNECTED
    }

    // ── Media controls (delegate to WebRtcManager) ───────────────────────

    fun setMicEnabled(enabled: Boolean) = webRtcManager.setAudioEnabled(enabled)
    fun setVideoEnabled(enabled: Boolean) = webRtcManager.setVideoEnabled(enabled)
    fun switchCamera() = webRtcManager.switchCamera()

    // ── Cleanup ──────────────────────────────────────────────────────────

    fun dispose() {
        webRtcManager.dispose()
        _connectionState.value = CallConnectionState.IDLE
        _remoteStream.value = null
    }

    // ── Signaling message handler ────────────────────────────────────────

    private fun handleSignalMessage(message: SignalMessage) {
        when (message) {
            is SignalMessage.Offer -> {
                targetUserId = message.targetUserId
                val sdp = SessionDescription(SessionDescription.Type.OFFER, message.sdp)
                webRtcManager.setRemoteDescription(sdp)
            }

            is SignalMessage.Answer -> {
                val sdp = SessionDescription(SessionDescription.Type.ANSWER, message.sdp)
                webRtcManager.setRemoteDescription(sdp)
            }

            is SignalMessage.IceCandidate -> {
                val candidate = IceCandidate(
                    message.sdpMid,
                    message.sdpMLineIndex,
                    message.candidate,
                )
                webRtcManager.addIceCandidate(candidate)
            }

            is SignalMessage.CallRequest,
            is SignalMessage.CallResponse -> {
                // Handled at a higher level (navigation / incoming call screen)
            }
        }
    }

    companion object {
        private const val TAG = "CallRepository"
    }
}
