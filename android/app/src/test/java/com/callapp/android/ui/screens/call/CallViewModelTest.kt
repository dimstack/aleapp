package com.callapp.android.ui.screens.call

import android.app.Application
import androidx.lifecycle.SavedStateHandle
import com.callapp.android.data.CallConnectionState
import com.callapp.android.data.CallRepositoryEvent
import com.callapp.android.domain.model.User
import com.callapp.android.network.result.ApiResult
import com.callapp.android.network.signaling.ConnectionState
import com.callapp.android.network.signaling.SignalMessage
import com.callapp.android.ui.screens.connect.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.webrtc.EglBase
import org.webrtc.VideoTrack

@OptIn(ExperimentalCoroutinesApi::class)
class CallViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun startOutgoingCall_setsCallingState() = runTest {
        val fakeSession = FakeCallSession()
        val viewModel = createViewModel(
            dependencies = FakeCallViewModelDependencies(fakeSession = fakeSession),
        )

        assertEquals(CallPhase.CALLING, viewModel.callPhase.value)
        runCurrent()
        assertEquals(CallPhase.RINGING, viewModel.callPhase.value)
        assertEquals(listOf("user-42" to false), fakeSession.outgoingCalls)
        viewModel.endCall()
        runCurrent()
    }

    @Test
    fun receiveAnswer_setsConnectedState() = runTest {
        val fakeSession = FakeCallSession()
        val viewModel = createViewModel(
            dependencies = FakeCallViewModelDependencies(fakeSession = fakeSession),
        )
        runCurrent()

        fakeSession.receiveAnswer()
        runCurrent()

        assertEquals(CallPhase.CONNECTED, viewModel.callPhase.value)
        viewModel.endCall()
        advanceUntilIdle()
    }

    @Test
    fun endCall_cleansUp() = runTest {
        val fakeSession = FakeCallSession()
        val viewModel = createViewModel(
            dependencies = FakeCallViewModelDependencies(fakeSession = fakeSession),
        )
        runCurrent()

        viewModel.endCall()
        runCurrent()

        assertEquals(CallPhase.ENDED, viewModel.callPhase.value)
        assertTrue(fakeSession.endCallCalled)
        assertTrue(fakeSession.disposeCalled)
    }

    @Test
    fun toggleMute_togglingState() = runTest {
        val fakeSession = FakeCallSession()
        val viewModel = createViewModel(
            dependencies = FakeCallViewModelDependencies(fakeSession = fakeSession),
        )
        runCurrent()

        viewModel.toggleMic()
        viewModel.toggleMic()

        assertTrue(viewModel.isMicOn.value)
        assertEquals(listOf(false, true), fakeSession.micStates)
        viewModel.endCall()
        runCurrent()
    }

    @Test
    fun toggleVideo_togglingState() = runTest {
        val fakeSession = FakeCallSession()
        val viewModel = createViewModel(
            dependencies = FakeCallViewModelDependencies(fakeSession = fakeSession),
        )
        runCurrent()

        viewModel.toggleCamera()
        assertTrue(viewModel.isCameraOn.value)
        viewModel.toggleCamera()

        assertFalse(viewModel.isCameraOn.value)
        assertEquals(listOf(true, false), fakeSession.videoStates)
        viewModel.endCall()
        runCurrent()
    }

    @Test
    fun callTimeout_noAnswer() = runTest {
        val fakeSession = FakeCallSession()
        val viewModel = createViewModel(
            dependencies = FakeCallViewModelDependencies(
                fakeSession = fakeSession,
                callTimeoutMillis = 5_000L,
            ),
        )
        runCurrent()

        assertEquals(CallPhase.RINGING, viewModel.callPhase.value)
        advanceTimeBy(4_999L)
        assertEquals(CallPhase.RINGING, viewModel.callPhase.value)

        advanceTimeBy(1L)
        advanceUntilIdle()

        assertEquals(CallPhase.ENDED, viewModel.callPhase.value)
        assertTrue(fakeSession.endCallCalled)
        assertTrue(fakeSession.disposeCalled)
    }

    @Test
    fun receiveDecline() = runTest {
        val fakeSession = FakeCallSession()
        val viewModel = createViewModel(
            dependencies = FakeCallViewModelDependencies(fakeSession = fakeSession),
        )
        runCurrent()

        fakeSession.receiveDecline()
        runCurrent()

        assertEquals(CallPhase.ENDED, viewModel.callPhase.value)
        assertTrue(fakeSession.disposeCalled)
    }

    @Test
    fun loadsContactAvatarUrl() = runTest {
        val fakeSession = FakeCallSession()
        val viewModel = createViewModel(
            dependencies = FakeCallViewModelDependencies(
                fakeSession = fakeSession,
                user = User(
                    id = "user-42",
                    name = "Maria",
                    username = "maria",
                    avatarUrl = "https://example.com/avatar.jpg",
                ),
            ),
        )

        advanceUntilIdle()

        assertEquals("https://example.com/avatar.jpg", viewModel.contactAvatarUrl.value)
        viewModel.endCall()
        runCurrent()
    }

    @Test
    fun iceCandidateExchange() = runTest {
        val fakeSignaling = FakeCallSignalingGateway()
        val fakeSession = FakeCallSession(signaling = fakeSignaling)
        val viewModel = createViewModel(
            dependencies = FakeCallViewModelDependencies(
                fakeSession = fakeSession,
                fakeSignaling = fakeSignaling,
            ),
        )
        runCurrent()

        fakeSession.emitLocalIceCandidate("candidate-1", "audio", 0, "user-42")

        assertEquals(
            listOf(
                SignalMessage.IceCandidate(
                    candidate = "candidate-1",
                    sdpMid = "audio",
                    sdpMLineIndex = 0,
                    targetUserId = "user-42",
                ),
            ),
            fakeSignaling.sentMessages,
        )
        viewModel.endCall()
        runCurrent()
    }

    private fun createViewModel(
        dependencies: CallViewModelDependencies,
    ) = CallViewModel(
        application = Application(),
        savedStateHandle = SavedStateHandle(
            mapOf(
                "serverAddress" to "https%3A%2F%2Fserver.example.com",
                "contactName" to "Maria",
                "userId" to "user-42",
            ),
        ),
        dependencies = dependencies,
    )

    private class FakeCallViewModelDependencies(
        private val fakeSession: FakeCallSession,
        private val fakeSignaling: FakeCallSignalingGateway = FakeCallSignalingGateway(),
        private val user: User = User(
            id = "user-42",
            name = "Maria",
            username = "maria",
        ),
        override val callTimeoutMillis: Long = 60_000_000L,
    ) : CallViewModelDependencies {
        override fun createCallSession(application: Application, serverAddress: String): CallSession = fakeSession

        override fun getSignaling(serverAddress: String): CallSignalingGateway = fakeSignaling

        override suspend fun getUser(serverAddress: String, userId: String): ApiResult<User> =
            ApiResult.Success(user)
    }

    private class FakeCallSignalingGateway : CallSignalingGateway {
        private val _connectionState = MutableStateFlow(ConnectionState.Disconnected)
        override val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

        var connectCalls = 0
        val sentMessages = mutableListOf<SignalMessage>()

        override fun connect() {
            connectCalls += 1
            _connectionState.value = ConnectionState.Connected
        }

        override fun send(message: SignalMessage) {
            sentMessages += message
        }
    }

    private class FakeCallSession(
        private val signaling: FakeCallSignalingGateway = FakeCallSignalingGateway(),
    ) : CallSession {
        private val _connectionState = MutableStateFlow(CallConnectionState.IDLE)
        override val connectionState: StateFlow<CallConnectionState> = _connectionState.asStateFlow()

        private val _remoteVideoTrack = MutableStateFlow<VideoTrack?>(null)
        override val remoteVideoTrack: StateFlow<VideoTrack?> = _remoteVideoTrack.asStateFlow()

        private val _events = MutableSharedFlow<CallRepositoryEvent>(extraBufferCapacity = 16)
        override val events: Flow<CallRepositoryEvent> = _events

        override val localVideoTrack: VideoTrack? = null
        override val eglBase: EglBase? = null

        val outgoingCalls = mutableListOf<Pair<String, Boolean>>()
        val micStates = mutableListOf<Boolean>()
        val videoStates = mutableListOf<Boolean>()
        var endCallCalled = false
        var disposeCalled = false

        override suspend fun startOutgoingCall(targetUserId: String, enableVideo: Boolean) {
            outgoingCalls += targetUserId to enableVideo
        }

        override suspend fun acceptIncomingCall(callerUserId: String, enableVideo: Boolean) = Unit

        override fun declineIncomingCall(callerUserId: String) = Unit

        override fun endCall() {
            endCallCalled = true
        }

        override fun setMicEnabled(enabled: Boolean) {
            micStates += enabled
        }

        override fun setVideoEnabled(enabled: Boolean) {
            videoStates += enabled
        }

        override fun switchCamera() = Unit

        override fun dispose() {
            disposeCalled = true
        }

        fun receiveAnswer() {
            _events.tryEmit(CallRepositoryEvent.AnswerReceived)
        }

        fun receiveDecline() {
            _events.tryEmit(CallRepositoryEvent.CallDeclined)
        }

        fun emitLocalIceCandidate(candidate: String, sdpMid: String, sdpMLineIndex: Int, targetUserId: String) {
            signaling.send(
                SignalMessage.IceCandidate(
                    candidate = candidate,
                    sdpMid = sdpMid,
                    sdpMLineIndex = sdpMLineIndex,
                    targetUserId = targetUserId,
                ),
            )
        }
    }
}
