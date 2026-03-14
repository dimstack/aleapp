package com.callapp.android.webrtc

import android.content.Context
import android.util.Log
import org.webrtc.AudioSource
import org.webrtc.AudioTrack
import org.webrtc.Camera2Capturer
import org.webrtc.Camera2Enumerator
import org.webrtc.CameraVideoCapturer
import org.webrtc.EglBase
import org.webrtc.IceCandidate
import org.webrtc.MediaConstraints
import org.webrtc.MediaStream
import org.webrtc.PeerConnection
import org.webrtc.PeerConnectionFactory
import org.webrtc.RtpSender
import org.webrtc.RtpReceiver
import org.webrtc.SdpObserver
import org.webrtc.SessionDescription
import org.webrtc.SurfaceTextureHelper
import org.webrtc.VideoSource
import org.webrtc.VideoTrack

class WebRtcManager(
    private val context: Context,
    private val listener: Listener,
) {

    interface Listener {
        fun onIceCandidate(candidate: IceCandidate)
        fun onConnectionChange(state: PeerConnection.PeerConnectionState)
        fun onRemoteTrackAdded(track: MediaStream)
        fun onRemoteVideoTrackReceived(track: VideoTrack) {}
        fun onRemoteVideoTrackRemoved() {}
        fun onRenegotiationNeeded() {}
    }

    /** EglBase from app-scoped [WebRtcFactory]. Do NOT release — it outlives any single call. */
    val eglBase: EglBase get() = WebRtcFactory.eglBase

    private val peerConnectionFactory: PeerConnectionFactory get() = WebRtcFactory.factory

    private var peerConnection: PeerConnection? = null
    private var videoCapturer: CameraVideoCapturer? = null
    private var surfaceTextureHelper: SurfaceTextureHelper? = null
    private var videoSource: VideoSource? = null
    private var audioSource: AudioSource? = null
    private var localVideoSender: RtpSender? = null
    private var isVideoCapturing = false

    var localVideoTrack: VideoTrack? = null
        private set
    var localAudioTrack: AudioTrack? = null
        private set

    private var serverAddress: String = ""

    // ── PeerConnection ───────────────────────────────────────────────────

    fun createPeerConnection(
        serverAddress: String,
        turnUsername: String,
        turnPassword: String,
    ) {
        this.serverAddress = serverAddress
        val iceServers = buildIceServers(serverAddress, turnUsername, turnPassword)

        val rtcConfig = PeerConnection.RTCConfiguration(iceServers).apply {
            sdpSemantics = PeerConnection.SdpSemantics.UNIFIED_PLAN
            continualGatheringPolicy =
                PeerConnection.ContinualGatheringPolicy.GATHER_CONTINUALLY
        }

        peerConnection = peerConnectionFactory.createPeerConnection(
            rtcConfig,
            PeerConnectionObserver()
        )

        localAudioTrack?.let { track ->
            peerConnection?.addTrack(track, listOf(LOCAL_STREAM_ID))
        }
        localVideoTrack?.let { track ->
            localVideoSender = peerConnection?.addTrack(track, listOf(LOCAL_STREAM_ID))
        }
    }

    private fun buildIceServers(
        serverAddress: String,
        turnUsername: String,
        turnPassword: String,
    ): List<PeerConnection.IceServer> {
        val host = serverAddress
            .removePrefix("http://")
            .removePrefix("https://")
            .substringBefore(":")

        return listOf(
            PeerConnection.IceServer.builder(STUN_URL).createIceServer(),
            PeerConnection.IceServer.builder("turn:$host:3478")
                .setUsername(turnUsername)
                .setPassword(turnPassword)
                .createIceServer()
        )
    }

    // ── Media capture ────────────────────────────────────────────────────

    fun startCapture(enableVideo: Boolean = true, enableAudio: Boolean = true) {
        if (enableAudio) initAudio()
        if (enableVideo) initVideo()
    }

    private fun initAudio() {
        val constraints = MediaConstraints().apply {
            mandatory.add(MediaConstraints.KeyValuePair("googEchoCancellation", "true"))
            mandatory.add(MediaConstraints.KeyValuePair("googNoiseSuppression", "true"))
            mandatory.add(MediaConstraints.KeyValuePair("googAutoGainControl", "true"))
        }
        audioSource = peerConnectionFactory.createAudioSource(constraints)
        localAudioTrack = peerConnectionFactory.createAudioTrack(AUDIO_TRACK_ID, audioSource).apply {
            setEnabled(true)
        }
    }

    private fun initVideo() {
        val enumerator = Camera2Enumerator(context)
        val cameraName = enumerator.deviceNames.firstOrNull { enumerator.isFrontFacing(it) }
            ?: enumerator.deviceNames.firstOrNull()
            ?: run {
                Log.w(TAG, "No camera found")
                return
            }

        surfaceTextureHelper = SurfaceTextureHelper.create("CaptureThread", eglBase.eglBaseContext)
        videoSource = peerConnectionFactory.createVideoSource(false)

        videoCapturer = Camera2Capturer(context, cameraName, null).also { capturer ->
            capturer.initialize(surfaceTextureHelper, context, videoSource!!.capturerObserver)
            capturer.startCapture(VIDEO_WIDTH, VIDEO_HEIGHT, VIDEO_FPS)
        }
        isVideoCapturing = true

        localVideoTrack = peerConnectionFactory.createVideoTrack(VIDEO_TRACK_ID, videoSource).apply {
            setEnabled(true)
        }
    }

    // ── Camera control ───────────────────────────────────────────────────

    fun switchCamera() {
        videoCapturer?.switchCamera(null)
    }

    fun setVideoEnabled(enabled: Boolean) {
        if (enabled) {
            if (localVideoTrack == null) {
                // Camera was never initialized — lazy init on first enable
                initVideo()
            } else if (!isVideoCapturing) {
                try {
                    videoCapturer?.startCapture(VIDEO_WIDTH, VIDEO_HEIGHT, VIDEO_FPS)
                    isVideoCapturing = true
                } catch (e: Exception) {
                    Log.w(TAG, "startCapture failed: ${e.message}")
                }
            }
            localVideoTrack?.setEnabled(true)
            ensureLocalVideoSender()
        } else {
            localVideoTrack?.setEnabled(false)
            if (isVideoCapturing) {
                try {
                    videoCapturer?.stopCapture()
                } catch (e: Exception) {
                    Log.w(TAG, "stopCapture failed: ${e.message}")
                }
                isVideoCapturing = false
            }
            removeLocalVideoSender()
        }
    }

    private fun ensureLocalVideoSender() {
        if (localVideoSender != null) return

        val track = localVideoTrack ?: return
        localVideoSender = peerConnection?.addTrack(track, listOf(LOCAL_STREAM_ID))
    }

    private fun removeLocalVideoSender() {
        val sender = localVideoSender ?: return
        val connection = peerConnection ?: return

        if (connection.removeTrack(sender)) {
            localVideoSender = null
        } else {
            Log.w(TAG, "Failed to remove local video sender")
        }
    }

    fun setAudioEnabled(enabled: Boolean) {
        localAudioTrack?.setEnabled(enabled)
    }

    // ── SDP negotiation ──────────────────────────────────────────────────

    fun createOffer(callback: (SessionDescription) -> Unit) {
        val constraints = MediaConstraints().apply {
            mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveAudio", "true"))
            mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveVideo", "true"))
        }
        peerConnection?.createOffer(sdpObserver(
            onCreateSuccess = { sdp ->
                peerConnection?.setLocalDescription(sdpObserver(), sdp)
                callback(sdp)
            }
        ), constraints)
    }

    fun createAnswer(callback: (SessionDescription) -> Unit) {
        val constraints = MediaConstraints().apply {
            mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveAudio", "true"))
            mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveVideo", "true"))
        }
        peerConnection?.createAnswer(sdpObserver(
            onCreateSuccess = { sdp ->
                peerConnection?.setLocalDescription(sdpObserver(), sdp)
                callback(sdp)
            }
        ), constraints)
    }

    fun setRemoteDescription(sdp: SessionDescription) {
        peerConnection?.setRemoteDescription(sdpObserver(), sdp)
    }

    fun addIceCandidate(candidate: IceCandidate) {
        peerConnection?.addIceCandidate(candidate)
    }

    // ── Cleanup ──────────────────────────────────────────────────────────

    fun dispose() {
        if (isVideoCapturing) {
            try {
                videoCapturer?.stopCapture()
            } catch (e: Exception) {
                Log.w(TAG, "stopCapture on dispose failed: ${e.message}")
            }
            isVideoCapturing = false
        }
        videoCapturer?.dispose()
        videoCapturer = null

        localVideoTrack?.dispose()
        localVideoTrack = null
        localVideoSender = null
        localAudioTrack?.dispose()
        localAudioTrack = null

        videoSource?.dispose()
        videoSource = null
        audioSource?.dispose()
        audioSource = null

        surfaceTextureHelper?.dispose()
        surfaceTextureHelper = null

        peerConnection?.close()
        peerConnection?.dispose()
        peerConnection = null
        // Factory и EglBase принадлежат WebRtcFactory (app-scoped) — не освобождаем здесь.
    }

    // ── PeerConnection observer ──────────────────────────────────────────

    private inner class PeerConnectionObserver : PeerConnection.Observer {
        override fun onIceCandidate(candidate: IceCandidate) {
            listener.onIceCandidate(candidate)
        }

        override fun onConnectionChange(newState: PeerConnection.PeerConnectionState) {
            Log.d(TAG, "Connection state: $newState")
            listener.onConnectionChange(newState)
        }

        override fun onAddStream(stream: MediaStream) {
            Log.d(TAG, "Remote stream added: ${stream.id}")
            listener.onRemoteTrackAdded(stream)
        }

        override fun onSignalingChange(state: PeerConnection.SignalingState?) {}
        override fun onIceConnectionChange(state: PeerConnection.IceConnectionState?) {}
        override fun onIceConnectionReceivingChange(receiving: Boolean) {}
        override fun onIceGatheringChange(state: PeerConnection.IceGatheringState?) {}
        override fun onIceCandidatesRemoved(candidates: Array<out IceCandidate>?) {}
        override fun onAddTrack(receiver: RtpReceiver?, mediaStreams: Array<out MediaStream>?) {
            val track = receiver?.track()
            if (track is VideoTrack) {
                Log.d(TAG, "Remote video track received via onAddTrack")
                listener.onRemoteVideoTrackReceived(track)
            }
        }

        override fun onRemoveTrack(receiver: RtpReceiver?) {
            if (receiver?.track() is VideoTrack) {
                Log.d(TAG, "Remote video track removed via onRemoveTrack")
                listener.onRemoteVideoTrackRemoved()
            }
        }

        override fun onRemoveStream(stream: MediaStream?) {
            if (stream?.videoTracks?.isNotEmpty() == true) {
                Log.d(TAG, "Remote stream removed: ${stream.id}")
                listener.onRemoteVideoTrackRemoved()
            }
        }
        override fun onDataChannel(channel: org.webrtc.DataChannel?) {}
        override fun onRenegotiationNeeded() {
            listener.onRenegotiationNeeded()
        }
    }

    // ── SDP observer helper ──────────────────────────────────────────────

    private fun sdpObserver(
        onCreateSuccess: ((SessionDescription) -> Unit)? = null,
    ): SdpObserver = object : SdpObserver {
        override fun onCreateSuccess(sdp: SessionDescription) {
            onCreateSuccess?.invoke(sdp)
        }

        override fun onSetSuccess() {}

        override fun onCreateFailure(error: String?) {
            Log.e(TAG, "SDP create failure: $error")
        }

        override fun onSetFailure(error: String?) {
            Log.e(TAG, "SDP set failure: $error")
        }
    }

    companion object {
        private const val TAG = "WebRtcManager"

        private const val LOCAL_STREAM_ID = "local_stream"
        private const val AUDIO_TRACK_ID = "local_audio"
        private const val VIDEO_TRACK_ID = "local_video"

        private const val STUN_URL = "stun:stun.l.google.com:19302"

        private const val VIDEO_WIDTH = 1280
        private const val VIDEO_HEIGHT = 720
        private const val VIDEO_FPS = 30
    }
}
