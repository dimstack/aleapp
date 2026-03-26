package com.callapp.android.data

import androidx.test.core.app.ApplicationProvider
import com.callapp.android.network.ServerConnectionManager
import com.callapp.android.network.signaling.SignalingClient
import com.callapp.android.webrtc.CallPeerConnection
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.webrtc.EglBase
import org.webrtc.IceCandidate
import org.webrtc.PeerConnection
import org.webrtc.SessionDescription
import org.webrtc.VideoTrack

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class CallRepositoryTest {

    @Test
    fun politePeerRollsBackAndAnswersOnOfferCollision() {
        val fakePeerConnection = FakePeerConnection().apply {
            signalingState = PeerConnection.SignalingState.HAVE_LOCAL_OFFER
        }
        val repository = createRepository(fakePeerConnection)

        repository.setPrivateField("peerConnectionReady", true)
        repository.setPrivateField("initialNegotiationDone", true)
        repository.setPrivateField("isPolitePeer", true)
        repository.setPrivateField("makingOffer", true)

        repository.invokeHandleIncomingOffer(
            SessionDescription(SessionDescription.Type.OFFER, "remote-offer"),
            callerUserId = "user-b",
        )

        assertEquals(1, fakePeerConnection.rollbackCalls)
        assertEquals(1, fakePeerConnection.remoteDescriptions.size)
        assertEquals(SessionDescription.Type.OFFER, fakePeerConnection.remoteDescriptions.single().type)
        assertEquals("remote-offer", fakePeerConnection.remoteDescriptions.single().description)
        assertEquals(1, fakePeerConnection.createAnswerCalls)
        assertTrue(fakePeerConnection.sentLocalAnswers.isNotEmpty())
    }

    @Test
    fun impolitePeerIgnoresOfferCollision() {
        val fakePeerConnection = FakePeerConnection().apply {
            signalingState = PeerConnection.SignalingState.HAVE_LOCAL_OFFER
        }
        val repository = createRepository(fakePeerConnection)

        repository.setPrivateField("peerConnectionReady", true)
        repository.setPrivateField("initialNegotiationDone", true)
        repository.setPrivateField("isPolitePeer", false)
        repository.setPrivateField("makingOffer", true)

        repository.invokeHandleIncomingOffer(
            SessionDescription(SessionDescription.Type.OFFER, "remote-offer"),
            callerUserId = "user-b",
        )

        assertEquals(0, fakePeerConnection.rollbackCalls)
        assertTrue(fakePeerConnection.remoteDescriptions.isEmpty())
        assertEquals(0, fakePeerConnection.createAnswerCalls)
    }

    private fun createRepository(fakePeerConnection: FakePeerConnection): CallRepository =
        CallRepository(
            context = ApplicationProvider.getApplicationContext(),
            signalingClient = SignalingClient("https://server.example.com", ""),
            serverAddress = "https://server.example.com",
            connectionManager = ServerConnectionManager(),
            peerConnectionFactory = { _, _ -> fakePeerConnection },
        )

    private fun CallRepository.invokeHandleIncomingOffer(
        offer: SessionDescription,
        callerUserId: String,
    ) {
        val method = javaClass.getDeclaredMethod(
            "handleIncomingOffer",
            SessionDescription::class.java,
            String::class.java,
        )
        method.isAccessible = true
        method.invoke(this, offer, callerUserId)
    }

    private fun CallRepository.setPrivateField(name: String, value: Any?) {
        val field = javaClass.getDeclaredField(name)
        field.isAccessible = true
        field.set(this, value)
    }

    private class FakePeerConnection : CallPeerConnection {
        override val eglBase: EglBase = EglBase.create()
        override val localVideoTrack: VideoTrack? = null

        var signalingState: PeerConnection.SignalingState? = PeerConnection.SignalingState.STABLE
        var rollbackCalls = 0
        var createAnswerCalls = 0
        val remoteDescriptions = mutableListOf<SessionDescription>()
        val sentLocalAnswers = mutableListOf<SessionDescription>()

        override fun startCapture(enableVideo: Boolean, enableAudio: Boolean) = Unit

        override fun createPeerConnection(
            serverAddress: String,
            turnUsername: String,
            turnPassword: String,
        ) = Unit

        override fun switchCamera() = Unit

        override fun setVideoEnabled(enabled: Boolean) = Unit

        override fun setAudioEnabled(enabled: Boolean) = Unit

        override fun createOffer(
            callback: (SessionDescription) -> Unit,
            onFailure: ((String?) -> Unit)?,
        ) = Unit

        override fun createAnswer(
            callback: (SessionDescription) -> Unit,
            onFailure: ((String?) -> Unit)?,
        ) {
            createAnswerCalls += 1
            val answer = SessionDescription(SessionDescription.Type.ANSWER, "local-answer")
            sentLocalAnswers += answer
            callback(answer)
        }

        override fun setRemoteDescription(
            sdp: SessionDescription,
            onSuccess: (() -> Unit)?,
            onFailure: ((String?) -> Unit)?,
        ) {
            remoteDescriptions += sdp
            signalingState = PeerConnection.SignalingState.HAVE_REMOTE_OFFER
            onSuccess?.invoke()
        }

        override fun rollbackLocalDescription(
            onSuccess: (() -> Unit)?,
            onFailure: ((String?) -> Unit)?,
        ) {
            rollbackCalls += 1
            signalingState = PeerConnection.SignalingState.STABLE
            onSuccess?.invoke()
        }

        override fun addIceCandidate(candidate: IceCandidate) = Unit

        override fun currentSignalingState(): PeerConnection.SignalingState? = signalingState

        override fun dispose() {
            eglBase.release()
        }
    }
}
