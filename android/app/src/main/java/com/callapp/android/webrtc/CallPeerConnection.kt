package com.callapp.android.webrtc

import org.webrtc.EglBase
import org.webrtc.IceCandidate
import org.webrtc.PeerConnection
import org.webrtc.SessionDescription
import org.webrtc.VideoTrack

interface CallPeerConnection {
    val eglBase: EglBase
    val localVideoTrack: VideoTrack?

    fun startCapture(enableVideo: Boolean = true, enableAudio: Boolean = true)
    fun createPeerConnection(
        serverAddress: String,
        turnUsername: String,
        turnPassword: String,
    )
    fun switchCamera()
    fun setVideoEnabled(enabled: Boolean)
    fun setAudioEnabled(enabled: Boolean)
    fun createOffer(
        callback: (SessionDescription) -> Unit,
        onFailure: ((String?) -> Unit)? = null,
    )
    fun createAnswer(
        callback: (SessionDescription) -> Unit,
        onFailure: ((String?) -> Unit)? = null,
    )
    fun setRemoteDescription(
        sdp: SessionDescription,
        onSuccess: (() -> Unit)? = null,
        onFailure: ((String?) -> Unit)? = null,
    )
    fun rollbackLocalDescription(
        onSuccess: (() -> Unit)? = null,
        onFailure: ((String?) -> Unit)? = null,
    )
    fun addIceCandidate(candidate: IceCandidate)
    fun currentSignalingState(): PeerConnection.SignalingState?
    fun dispose()
}
