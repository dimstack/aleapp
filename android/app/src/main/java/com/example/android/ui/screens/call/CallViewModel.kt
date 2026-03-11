package com.example.android.ui.screens.call

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.example.android.data.CallConnectionState
import com.example.android.data.CallRepository
import com.example.android.data.ServiceLocator
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
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

class CallViewModel(
    application: Application,
    savedStateHandle: SavedStateHandle,
) : AndroidViewModel(application) {

    val contactName: String = (savedStateHandle.get<String>("contactName") ?: "")
        .let { java.net.URLDecoder.decode(it, "UTF-8") }

    val serverName: String? = savedStateHandle.get<String>("serverName")
        ?.let { java.net.URLDecoder.decode(it, "UTF-8") }

    private val userId: String = savedStateHandle.get<String>("userId") ?: ""

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
        get() = callRepository?.webRtcManager?.eglBase

    private var timerJob: Job? = null
    private var callRepository: CallRepository? = null

    init {
        if (!isIncoming) {
            startOutgoingCall()
        }
    }

    private fun getOrCreateCallRepository(): CallRepository {
        callRepository?.let { return it }

        val connManager = ServiceLocator.connectionManager
        val serverAddress = ServiceLocator.activeServerAddress
        val signaling = connManager.getSignaling(serverAddress)
        signaling.connect()

        val repo = CallRepository(
            context = getApplication(),
            signalingClient = signaling,
            serverAddress = serverAddress,
        )
        callRepository = repo

        viewModelScope.launch {
            repo.connectionState.collect { state ->
                when (state) {
                    CallConnectionState.CONNECTED -> {
                        _callPhase.value = CallPhase.CONNECTED
                        startTimer()
                    }
                    CallConnectionState.FAILED,
                    CallConnectionState.DISCONNECTED -> {
                        if (_callPhase.value == CallPhase.CONNECTED) {
                            _callPhase.value = CallPhase.ENDED
                            timerJob?.cancel()
                        }
                    }
                    else -> {}
                }
            }
        }

        viewModelScope.launch {
            repo.remoteStream.collect { stream ->
                _remoteVideoTrack.value = stream?.videoTracks?.firstOrNull()
            }
        }

        return repo
    }

    private fun updateLocalTrack() {
        _localVideoTrack.value = callRepository?.webRtcManager?.localVideoTrack
    }

    // ── Outgoing call ────────────────────────────────────────────────────

    private fun startOutgoingCall() {
        _callPhase.value = CallPhase.CALLING

        viewModelScope.launch {
            val repo = getOrCreateCallRepository()
            repo.startOutgoingCall(
                targetUserId = userId,
                enableVideo = _isCameraOn.value,
            )
            updateLocalTrack()
            _callPhase.value = CallPhase.RINGING
        }
    }

    // ── Incoming call ────────────────────────────────────────────────────

    fun acceptCall() {
        if (_callPhase.value != CallPhase.INCOMING) return

        viewModelScope.launch {
            val repo = getOrCreateCallRepository()
            repo.acceptIncomingCall(
                callerUserId = userId,
                enableVideo = _isCameraOn.value,
            )
            updateLocalTrack()
        }
    }

    fun declineCall() {
        callRepository?.declineIncomingCall(userId)
        _callPhase.value = CallPhase.ENDED
        timerJob?.cancel()
    }

    // ── End call ─────────────────────────────────────────────────────────

    fun endCall() {
        callRepository?.endCall()
        _callPhase.value = CallPhase.ENDED
        timerJob?.cancel()
    }

    // ── Media controls ───────────────────────────────────────────────────

    fun toggleMic() {
        val newState = !_isMicOn.value
        _isMicOn.value = newState
        callRepository?.setMicEnabled(newState)
    }

    fun toggleCamera() {
        val newState = !_isCameraOn.value
        _isCameraOn.value = newState
        callRepository?.setVideoEnabled(newState)
        _localVideoTrack.value = if (newState) callRepository?.webRtcManager?.localVideoTrack else null
    }

    fun switchCamera() {
        callRepository?.switchCamera()
    }

    // ── Timer ────────────────────────────────────────────────────────────

    private fun startTimer() {
        if (timerJob?.isActive == true) return
        timerJob = viewModelScope.launch {
            while (isActive) {
                delay(1000)
                _elapsedSeconds.value++
            }
        }
    }

    // ── Cleanup ──────────────────────────────────────────────────────────

    override fun onCleared() {
        super.onCleared()
        callRepository?.dispose()
        callRepository = null
    }
}
