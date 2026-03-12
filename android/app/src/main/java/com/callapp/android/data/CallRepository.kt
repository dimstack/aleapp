package com.callapp.android.data

import android.content.Context
import android.util.Log
import com.callapp.android.network.ServerConnectionManager
import com.callapp.android.network.result.ApiResult
import com.callapp.android.network.signaling.SignalingClient
import com.callapp.android.network.signaling.SignalMessage
import com.callapp.android.webrtc.WebRtcManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
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
    private val connectionManager: ServerConnectionManager = ServiceLocator.connectionManager,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private val _connectionState = MutableStateFlow(CallConnectionState.IDLE)
    val connectionState: StateFlow<CallConnectionState> = _connectionState

    private val _remoteStream = MutableStateFlow<MediaStream?>(null)
    val remoteStream: StateFlow<MediaStream?> = _remoteStream

    private var targetUserId: String = ""

    /**
     * Buffered remote SDP offer received before PeerConnection was created.
     * Applied in [acceptIncomingCall] after createPeerConnection().
     */
    private var pendingRemoteOffer: SessionDescription? = null

    /**
     * Buffered ICE candidates received before PeerConnection was created.
     * Applied in [acceptIncomingCall] after setting the remote description.
     */
    private val pendingIceCandidates = mutableListOf<IceCandidate>()

    /** True once [WebRtcManager.createPeerConnection] has been called. */
    private var peerConnectionReady = false

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

        override fun onRemoteTrackAdded(track: MediaStream) {
            _remoteStream.value = track
        }
    })

    init {
        scope.launch {
            signalingClient.messages.collect { message ->
                handleSignalMessage(message)
            }
        }
    }

    // ── TURN credentials ─────────────────────────────────────────────────

    private var turnUsername: String = ""
    private var turnPassword: String = ""

    private suspend fun fetchTurnCredentials() {
        val client = connectionManager.getClient(serverAddress)
        when (val result = client.getTurnCredentials()) {
            is ApiResult.Success -> {
                turnUsername = result.data.username
                turnPassword = result.data.credential
            }
            is ApiResult.Failure -> {
                Log.w(TAG, "Failed to fetch TURN credentials, calls may fail behind strict NAT")
                turnUsername = ""
                turnPassword = ""
            }
        }
    }

    // ── Outgoing call ────────────────────────────────────────────────────

    suspend fun startOutgoingCall(targetUserId: String, enableVideo: Boolean) {
        this.targetUserId = targetUserId
        _connectionState.value = CallConnectionState.CONNECTING

        fetchTurnCredentials()
        webRtcManager.startCapture(enableVideo = enableVideo, enableAudio = true)
        webRtcManager.createPeerConnection(serverAddress, turnUsername, turnPassword)
        peerConnectionReady = true

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

    suspend fun acceptIncomingCall(callerUserId: String, enableVideo: Boolean) {
        this.targetUserId = callerUserId
        _connectionState.value = CallConnectionState.CONNECTING

        fetchTurnCredentials()
        webRtcManager.startCapture(enableVideo = enableVideo, enableAudio = true)
        webRtcManager.createPeerConnection(serverAddress, turnUsername, turnPassword)
        peerConnectionReady = true

        // Apply buffered remote offer
        val offer = pendingRemoteOffer
        if (offer != null) {
            pendingRemoteOffer = null
            webRtcManager.setRemoteDescription(offer)

            // Apply buffered ICE candidates
            pendingIceCandidates.forEach { candidate ->
                webRtcManager.addIceCandidate(candidate)
            }
            pendingIceCandidates.clear()

            // Create and send answer
            webRtcManager.createAnswer { sdp ->
                signalingClient.send(
                    SignalMessage.Answer(
                        sdp = sdp.description,
                        targetUserId = callerUserId,
                    )
                )
            }
        } else {
            Log.w(TAG, "acceptIncomingCall: no pending offer from $callerUserId")
        }
    }

    fun declineIncomingCall(callerUserId: String) {
        signalingClient.send(
            SignalMessage.CallDecline(targetUserId = callerUserId)
        )
        clearPendingState()
    }

    // ── End call ─────────────────────────────────────────────────────────

    fun endCall() {
        if (targetUserId.isNotEmpty()) {
            signalingClient.send(
                SignalMessage.CallEnd(targetUserId = targetUserId)
            )
        }
        _connectionState.value = CallConnectionState.DISCONNECTED
        clearPendingState()
    }

    // ── Media controls (delegate to WebRtcManager) ───────────────────────

    fun setMicEnabled(enabled: Boolean) = webRtcManager.setAudioEnabled(enabled)
    fun setVideoEnabled(enabled: Boolean) = webRtcManager.setVideoEnabled(enabled)
    fun switchCamera() = webRtcManager.switchCamera()

    // ── Cleanup ──────────────────────────────────────────────────────────

    fun dispose() {
        scope.cancel()
        webRtcManager.dispose()
        clearPendingState()
        _connectionState.value = CallConnectionState.IDLE
        _remoteStream.value = null
    }

    private fun clearPendingState() {
        pendingRemoteOffer = null
        pendingIceCandidates.clear()
        peerConnectionReady = false
    }

    // ── Signaling message handler ────────────────────────────────────────

    private fun handleSignalMessage(message: SignalMessage) {
        when (message) {
            is SignalMessage.Offer -> {
                // fromUserId = the caller who sent us this offer
                targetUserId = message.fromUserId
                val sdp = SessionDescription(SessionDescription.Type.OFFER, message.sdp)

                if (peerConnectionReady) {
                    // PeerConnection already exists (shouldn't normally happen for incoming)
                    webRtcManager.setRemoteDescription(sdp)
                } else {
                    // Buffer until acceptIncomingCall creates the PeerConnection
                    pendingRemoteOffer = sdp
                    Log.d(TAG, "Buffered incoming offer from ${message.fromUserId}")
                }
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
                if (peerConnectionReady) {
                    webRtcManager.addIceCandidate(candidate)
                } else {
                    // Buffer until PeerConnection is created
                    pendingIceCandidates.add(candidate)
                    Log.d(TAG, "Buffered ICE candidate (${pendingIceCandidates.size} pending)")
                }
            }

            is SignalMessage.CallEnd -> {
                _connectionState.value = CallConnectionState.DISCONNECTED
                clearPendingState()
            }

            is SignalMessage.CallDecline -> {
                _connectionState.value = CallConnectionState.DISCONNECTED
                clearPendingState()
            }

            is SignalMessage.CallBusy -> {
                _connectionState.value = CallConnectionState.DISCONNECTED
                clearPendingState()
            }

            is SignalMessage.CallRequest,
            is SignalMessage.CallResponse,
            is SignalMessage.StatusUpdate -> {
                // Handled at a higher level (navigation / presence)
            }
        }
    }

    companion object {
        private const val TAG = "CallRepository"
    }
}
