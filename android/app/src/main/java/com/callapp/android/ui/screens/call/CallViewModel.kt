package com.callapp.android.ui.screens.call

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.callapp.android.data.CallConnectionState
import com.callapp.android.data.CallRepository
import com.callapp.android.data.CallRepositoryEvent
import com.callapp.android.data.ServiceLocator
import com.callapp.android.domain.model.User
import com.callapp.android.network.signaling.ConnectionState
import com.callapp.android.network.signaling.SignalMessage
import com.callapp.android.network.signaling.SignalingClient
import com.callapp.android.network.result.ApiResult
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.webrtc.EglBase
import org.webrtc.VideoTrack

enum class CallPhase {
    CALLING,
    RINGING,
    INCOMING,
    CONNECTED,
    ENDED,
}

interface CallSession {
    val connectionState: StateFlow<CallConnectionState>
    val remoteVideoTrack: StateFlow<VideoTrack?>
    val events: Flow<CallRepositoryEvent>
    val localVideoTrack: VideoTrack?
    val eglBase: EglBase?

    suspend fun startOutgoingCall(targetUserId: String, enableVideo: Boolean)
    suspend fun acceptIncomingCall(callerUserId: String, enableVideo: Boolean)
    fun declineIncomingCall(callerUserId: String)
    fun endCall()
    fun setMicEnabled(enabled: Boolean)
    fun setVideoEnabled(enabled: Boolean)
    fun switchCamera()
    fun dispose()
}

interface CallSignalingGateway {
    val connectionState: StateFlow<ConnectionState>
    fun connect()
    fun send(message: SignalMessage)
}

interface CallViewModelDependencies {
    fun createCallSession(application: Application, serverAddress: String): CallSession
    fun getSignaling(serverAddress: String): CallSignalingGateway
    suspend fun getUser(serverAddress: String, userId: String): ApiResult<User>
    val callTimeoutMillis: Long
        get() = 30_000L
}

private class RepositoryCallSession(
    application: Application,
    serverAddress: String,
    signalingClient: SignalingClient,
) : CallSession {
    private val repository = CallRepository(
        context = application,
        signalingClient = signalingClient,
        serverAddress = serverAddress,
    )

    override val connectionState: StateFlow<CallConnectionState> = repository.connectionState
    override val remoteVideoTrack: StateFlow<VideoTrack?> = repository.remoteVideoTrack
    override val events: Flow<CallRepositoryEvent> = repository.events
    override val localVideoTrack: VideoTrack?
        get() = repository.webRtcManager.localVideoTrack
    override val eglBase: EglBase?
        get() = repository.webRtcManager.eglBase

    override suspend fun startOutgoingCall(targetUserId: String, enableVideo: Boolean) {
        repository.startOutgoingCall(targetUserId, enableVideo)
    }

    override suspend fun acceptIncomingCall(callerUserId: String, enableVideo: Boolean) {
        repository.acceptIncomingCall(callerUserId, enableVideo)
    }

    override fun declineIncomingCall(callerUserId: String) {
        repository.declineIncomingCall(callerUserId)
    }

    override fun endCall() {
        repository.endCall()
    }

    override fun setMicEnabled(enabled: Boolean) {
        repository.setMicEnabled(enabled)
    }

    override fun setVideoEnabled(enabled: Boolean) {
        repository.setVideoEnabled(enabled)
    }

    override fun switchCamera() {
        repository.switchCamera()
    }

    override fun dispose() {
        repository.dispose()
    }
}

private class SignalingGatewayAdapter(
    private val signalingClient: SignalingClient,
) : CallSignalingGateway {
    override val connectionState: StateFlow<ConnectionState> = signalingClient.connectionState

    override fun connect() {
        signalingClient.connect()
    }

    override fun send(message: SignalMessage) {
        signalingClient.send(message)
    }
}

object DefaultCallViewModelDependencies : CallViewModelDependencies {
    override fun createCallSession(application: Application, serverAddress: String): CallSession {
        val signaling = ServiceLocator.connectionManager.getSignaling(serverAddress)
        signaling.connect()
        return RepositoryCallSession(application, serverAddress, signaling)
    }

    override fun getSignaling(serverAddress: String): CallSignalingGateway =
        SignalingGatewayAdapter(ServiceLocator.connectionManager.getSignaling(serverAddress))

    override suspend fun getUser(serverAddress: String, userId: String): ApiResult<User> =
        ServiceLocator.serverRepository.getUser(serverAddress, userId)
}

class CallViewModel(
    application: Application,
    savedStateHandle: SavedStateHandle,
    private val dependencies: CallViewModelDependencies = DefaultCallViewModelDependencies,
) : AndroidViewModel(application) {

    constructor(
        application: Application,
        savedStateHandle: SavedStateHandle,
    ) : this(application, savedStateHandle, DefaultCallViewModelDependencies)

    private val serverAddress: String = (savedStateHandle.get<String>("serverAddress") ?: "")
        .let { java.net.URLDecoder.decode(it, "UTF-8") }

    val contactName: String = (savedStateHandle.get<String>("contactName") ?: "")
        .let { java.net.URLDecoder.decode(it, "UTF-8") }

    val serverName: String? = savedStateHandle.get<String>("serverName")
        ?.let { java.net.URLDecoder.decode(it, "UTF-8") }

    private val userId: String = savedStateHandle.get<String>("userId") ?: ""
    private val _contactAvatarUrl = MutableStateFlow<String?>(null)
    val contactAvatarUrl: StateFlow<String?> = _contactAvatarUrl.asStateFlow()

    val isIncoming: Boolean = serverName != null

    private val _callPhase = MutableStateFlow(
        if (isIncoming) CallPhase.INCOMING else CallPhase.CALLING,
    )
    val callPhase: StateFlow<CallPhase> = _callPhase.asStateFlow()

    private val _elapsedSeconds = MutableStateFlow(0)
    val elapsedSeconds: StateFlow<Int> = _elapsedSeconds.asStateFlow()

    private val _isMicOn = MutableStateFlow(true)
    val isMicOn: StateFlow<Boolean> = _isMicOn.asStateFlow()

    private val _isCameraOn = MutableStateFlow(false)
    val isCameraOn: StateFlow<Boolean> = _isCameraOn.asStateFlow()

    private val _localVideoTrack = MutableStateFlow<VideoTrack?>(null)
    val localVideoTrack: StateFlow<VideoTrack?> = _localVideoTrack.asStateFlow()

    private val _remoteVideoTrack = MutableStateFlow<VideoTrack?>(null)
    val remoteVideoTrack: StateFlow<VideoTrack?> = _remoteVideoTrack.asStateFlow()

    val eglBase: EglBase?
        get() = callSession?.eglBase

    private var timerJob: Job? = null
    private var timeoutJob: Job? = null
    private var callSession: CallSession? = null

    init {
        loadContactAvatar()
        if (isIncoming) {
            getOrCreateCallSession()
        } else {
            startOutgoingCall()
        }
    }

    private fun loadContactAvatar() {
        if (serverAddress.isBlank() || userId.isBlank()) return

        viewModelScope.launch {
            when (val result = dependencies.getUser(serverAddress, userId)) {
                is ApiResult.Success -> _contactAvatarUrl.value = result.data.avatarUrl
                is ApiResult.Failure -> Unit
            }
        }
    }

    private fun getOrCreateCallSession(): CallSession {
        callSession?.let { return it }

        val session = dependencies.createCallSession(getApplication(), serverAddress)
        callSession = session

        viewModelScope.launch {
            session.connectionState.collect { state ->
                when (state) {
                    CallConnectionState.CONNECTED -> {
                        _callPhase.value = CallPhase.CONNECTED
                        cancelTimeout()
                        startTimer()
                    }

                    CallConnectionState.FAILED,
                    CallConnectionState.DISCONNECTED -> {
                        finishCall()
                    }

                    else -> Unit
                }
            }
        }

        viewModelScope.launch {
            session.remoteVideoTrack.collect { track ->
                _remoteVideoTrack.value = track
            }
        }

        viewModelScope.launch {
            session.events.collect { event ->
                when (event) {
                    CallRepositoryEvent.AnswerReceived -> {
                        _callPhase.value = CallPhase.CONNECTED
                        cancelTimeout()
                        startTimer()
                    }

                    CallRepositoryEvent.CallDeclined -> {
                        finishCall()
                    }

                    is CallRepositoryEvent.IceCandidateSent -> Unit
                }
            }
        }

        return session
    }

    private fun updateLocalTrack() {
        _localVideoTrack.value = callSession?.localVideoTrack
    }

    private fun startOutgoingCall() {
        _callPhase.value = CallPhase.CALLING

        viewModelScope.launch {
            val session = getOrCreateCallSession()
            session.startOutgoingCall(
                targetUserId = userId,
                enableVideo = _isCameraOn.value,
            )
            updateLocalTrack()
            _callPhase.value = CallPhase.RINGING
            startTimeoutCountdown()
        }
    }

    fun acceptCall() {
        if (_callPhase.value != CallPhase.INCOMING) return

        viewModelScope.launch {
            val session = getOrCreateCallSession()
            session.acceptIncomingCall(
                callerUserId = userId,
                enableVideo = _isCameraOn.value,
            )
            updateLocalTrack()
        }
    }

    fun declineCall() {
        val session = callSession
        if (session != null) {
            session.declineIncomingCall(userId)
        } else if (serverAddress.isNotEmpty()) {
            val signaling = dependencies.getSignaling(serverAddress)
            if (signaling.connectionState.value == ConnectionState.Disconnected) {
                signaling.connect()
            }
            signaling.send(SignalMessage.CallDecline(targetUserId = userId))
        }
        finishCall()
    }

    fun endCall() {
        callSession?.endCall()
        finishCall()
    }

    fun toggleMic() {
        val newState = !_isMicOn.value
        _isMicOn.value = newState
        callSession?.setMicEnabled(newState)
    }

    fun toggleCamera() {
        val newState = !_isCameraOn.value
        _isCameraOn.value = newState
        callSession?.setVideoEnabled(newState)
        _localVideoTrack.value = if (newState) callSession?.localVideoTrack else null
    }

    fun switchCamera() {
        callSession?.switchCamera()
    }

    private fun startTimer() {
        if (timerJob?.isActive == true) return
        timerJob = viewModelScope.launch {
            while (isActive) {
                delay(1000)
                _elapsedSeconds.value++
            }
        }
    }

    private fun startTimeoutCountdown() {
        cancelTimeout()
        timeoutJob = viewModelScope.launch {
            delay(dependencies.callTimeoutMillis)
            if (_callPhase.value == CallPhase.CALLING || _callPhase.value == CallPhase.RINGING) {
                endCall()
            }
        }
    }

    private fun cancelTimeout() {
        timeoutJob?.cancel()
        timeoutJob = null
    }

    private fun cleanupSession() {
        callSession?.dispose()
        callSession = null
        _localVideoTrack.value = null
        _remoteVideoTrack.value = null
    }

    private fun finishCall() {
        if (_callPhase.value == CallPhase.ENDED) return
        _callPhase.value = CallPhase.ENDED
        timerJob?.cancel()
        cancelTimeout()
        cleanupSession()
    }

    override fun onCleared() {
        super.onCleared()
        timerJob?.cancel()
        cancelTimeout()
        cleanupSession()
    }
}
