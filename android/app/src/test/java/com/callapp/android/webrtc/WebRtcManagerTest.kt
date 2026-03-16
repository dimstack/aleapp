package com.callapp.android.webrtc

import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.webrtc.IceCandidate
import org.webrtc.MediaStream
import org.webrtc.PeerConnection
import org.webrtc.VideoTrack

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class WebRtcManagerTest {

    @Test
    fun buildIceServersUsesHostWithoutSchemeOrPort() {
        val manager = createManager()

        val iceServers = manager.buildIceServers(
            serverAddress = "https://calls.example.com:3000",
            turnUsername = "turn-user",
            turnPassword = "turn-pass",
        )

        assertEquals(2, iceServers.size)
        assertEquals(listOf("stun:stun.l.google.com:19302"), iceServers[0].urls)
        assertEquals(listOf("turn:calls.example.com:3478"), iceServers[1].urls)
        assertEquals("turn-user", iceServers[1].username)
        assertEquals("turn-pass", iceServers[1].password)
    }

    @Test
    fun createSdpObserverInvokesSuccessCallback() {
        val manager = createManager()
        var createdDescriptionType: String? = null

        val observer = manager.createSdpObserver { sdp ->
            createdDescriptionType = sdp.type.canonicalForm()
        }

        observer.onCreateSuccess(
            org.webrtc.SessionDescription(
                org.webrtc.SessionDescription.Type.OFFER,
                "v=0",
            ),
        )
        observer.onCreateFailure("failed")
        observer.onSetFailure("failed")

        assertEquals("offer", createdDescriptionType)
    }

    @Test
    fun safeOperationsWithoutPeerConnectionDoNotCrash() {
        val manager = createManager()

        manager.switchCamera()
        manager.setAudioEnabled(enabled = false)
        manager.setVideoEnabled(enabled = false)
        manager.addIceCandidate(IceCandidate("audio", 0, "candidate:1 1 UDP 1 0.0.0.0 9 typ host"))
        manager.setRemoteDescription(
            org.webrtc.SessionDescription(
                org.webrtc.SessionDescription.Type.ANSWER,
                "v=0",
            ),
        )
        manager.createOffer { error("callback should not be called without peer connection") }
        manager.createAnswer { error("callback should not be called without peer connection") }
        manager.dispose()
    }

    @Test
    fun peerConnectionObserverForwardsListenerCallbacks() {
        val listener = RecordingListener()
        val manager = WebRtcManager(
            context = ApplicationProvider.getApplicationContext(),
            listener = listener,
        )

        val observerClass = manager.javaClass.declaredClasses.single { it.simpleName == "PeerConnectionObserver" }
        val constructor = observerClass.getDeclaredConstructor(manager.javaClass)
        constructor.isAccessible = true
        val observer = constructor.newInstance(manager) as PeerConnection.Observer

        val candidate = IceCandidate("audio", 0, "candidate:1 1 UDP 1 0.0.0.0 9 typ host")
        observer.onIceCandidate(candidate)
        observer.onConnectionChange(PeerConnection.PeerConnectionState.CONNECTED)
        observer.onRenegotiationNeeded()
        observer.onRemoveTrack(null)
        observer.onRemoveStream(null)

        assertEquals(candidate, listener.lastIceCandidate)
        assertEquals(PeerConnection.PeerConnectionState.CONNECTED, listener.lastConnectionState)
        assertTrue(listener.renegotiationRequested)
    }

    private fun createManager(): WebRtcManager = WebRtcManager(
        context = ApplicationProvider.getApplicationContext(),
        listener = RecordingListener(),
    )

    private class RecordingListener : WebRtcManager.Listener {
        var lastIceCandidate: IceCandidate? = null
        var lastConnectionState: PeerConnection.PeerConnectionState? = null
        var renegotiationRequested = false

        override fun onIceCandidate(candidate: IceCandidate) {
            lastIceCandidate = candidate
        }

        override fun onConnectionChange(state: PeerConnection.PeerConnectionState) {
            lastConnectionState = state
        }

        override fun onRemoteTrackAdded(track: MediaStream) = Unit

        override fun onRemoteVideoTrackReceived(track: VideoTrack) = Unit

        override fun onRemoteVideoTrackRemoved() = Unit

        override fun onRenegotiationNeeded() {
            renegotiationRequested = true
        }
    }
}
