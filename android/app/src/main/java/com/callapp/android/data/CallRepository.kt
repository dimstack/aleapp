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
import org.webrtc.VideoTrack

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

    private val _remoteVideoTrack = MutableStateFlow<VideoTrack?>(null)
    val remoteVideoTrack: StateFlow<VideoTrack?> = _remoteVideoTrack

    private var targetUserId: String = ""
    private var pendingOutgoingVideoEnabled: Boolean = false
    private var outgoingOfferStarted = false

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

    /** True once the first offer/answer exchange is complete and renegotiation is safe. */
    private var initialNegotiationDone = false

    val webRtcManager: WebRtcManager = WebRtcManager(context, object : WebRtcManager.Listener {
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
            // Also extract video track from the stream for initial calls with video
            track.videoTracks?.firstOrNull()?.let { videoTrack ->
                _remoteVideoTrack.value = videoTrack
            }
        }

        override fun onRemoteVideoTrackReceived(track: VideoTrack) {
            _remoteVideoTrack.value = track
        }

        override fun onRemoteVideoTrackRemoved() {
            _remoteVideoTrack.value = null
            _remoteStream.value = null
        }

        override fun onRenegotiationNeeded() {
            onWebRtcRenegotiationNeeded()
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

    private fun onWebRtcRenegotiationNeeded() {
        if (!initialNegotiationDone || targetUserId.isEmpty()) return
        scope.launch {
            webRtcManager.createOffer { sdp ->
                signalingClient.send(
                    SignalMessage.Offer(
                        sdp = sdp.description,
                        targetUserId = targetUserId,
                    )
                )
            }
        }
    }

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
        pendingOutgoingVideoEnabled = enableVideo
        outgoingOfferStarted = false
        _connectionState.value = CallConnectionState.CONNECTING

        signalingClient.send(
            SignalMessage.CallRequest(
                fromUserName = ServiceLocator.currentUserId.ifBlank { "Unknown caller" },
                fromServerName = runCatching {
                    ServiceLocator.sessionStore.getSession(serverAddress)?.serverName.orEmpty()
                }.getOrDefault(""),
                targetUserId = targetUserId,
            ),
        )
    }

    // ── Incoming call ────────────────────────────────────────────────────

    suspend fun acceptIncomingCall(callerUserId: String, enableVideo: Boolean) {
        this.targetUserId = callerUserId
        _connectionState.value = CallConnectionState.CONNECTING

        fetchTurnCredentials()
        webRtcManager.startCapture(enableVideo = enableVideo, enableAudio = true)
        webRtcManager.createPeerConnection(serverAddress, turnUsername, turnPassword)
        peerConnectionReady = true

        val offer = pendingRemoteOffer
        if (offer != null) {
            applyRemoteOfferAndAnswer(offer, callerUserId)
        }

        signalingClient.send(
            SignalMessage.CallResponse(
                accepted = true,
                fromUserId = "",
                targetUserId = callerUserId,
            ),
        )
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
        _remoteVideoTrack.value = null
    }

    private fun clearPendingState() {
        pendingRemoteOffer = null
        pendingIceCandidates.clear()
        peerConnectionReady = false
        initialNegotiationDone = false
        pendingOutgoingVideoEnabled = false
        outgoingOfferStarted = false
    }

    // ── Signaling message handler ────────────────────────────────────────

    private fun handleSignalMessage(message: SignalMessage) {
        when (message) {
            is SignalMessage.Offer -> {
                targetUserId = message.fromUserId
                val sdp = SessionDescription(SessionDescription.Type.OFFER, message.sdp)

                if (peerConnectionReady) {
                    applyRemoteOfferAndAnswer(sdp, message.fromUserId)
                } else {
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
            is SignalMessage.StatusUpdate -> {
                // Handled at a higher level (navigation / presence)
            }

            is SignalMessage.CallResponse -> {
                if (message.accepted) {
                    if (!outgoingOfferStarted) {
                        scope.launch {
                            beginOutgoingOffer()
                        }
                    }
                } else {
                    _connectionState.value = CallConnectionState.DISCONNECTED
                    clearPendingState()
                }
            }
        }
    }

    private suspend fun beginOutgoingOffer() {
        if (outgoingOfferStarted) return
        outgoingOfferStarted = true

        fetchTurnCredentials()
        webRtcManager.startCapture(enableVideo = pendingOutgoingVideoEnabled, enableAudio = true)
        webRtcManager.createPeerConnection(serverAddress, turnUsername, turnPassword)
        peerConnectionReady = true

        webRtcManager.createOffer { sdp ->
            signalingClient.send(
                SignalMessage.Offer(
                    sdp = sdp.description,
                    targetUserId = targetUserId,
                ),
            )
            initialNegotiationDone = true
        }
    }

    private fun applyRemoteOfferAndAnswer(offer: SessionDescription, callerUserId: String) {
        pendingRemoteOffer = null
        webRtcManager.setRemoteDescription(offer)

        pendingIceCandidates.forEach { candidate ->
            webRtcManager.addIceCandidate(candidate)
        }
        pendingIceCandidates.clear()

        webRtcManager.createAnswer { sdp ->
            signalingClient.send(
                SignalMessage.Answer(
                    sdp = sdp.description,
                    targetUserId = callerUserId,
                ),
            )
            initialNegotiationDone = true
        }
    }

    companion object {
        private const val TAG = "CallRepository"
    }
}
