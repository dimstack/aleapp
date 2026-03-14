package com.callapp.android.ui.screens.call

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.callapp.android.data.CallConnectionState
import com.callapp.android.data.CallRepository
import com.callapp.android.data.ServiceLocator
import com.callapp.android.network.signaling.ConnectionState
import com.callapp.android.network.signaling.SignalMessage
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

    private val serverAddress: String = (savedStateHandle.get<String>("serverAddress") ?: "")
        .let { java.net.URLDecoder.decode(it, "UTF-8") }

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
        if (isIncoming) {
            getOrCreateCallRepository()
        } else {
            startOutgoingCall()
        }
    }

    private fun getOrCreateCallRepository(): CallRepository {
        callRepository?.let { return it }

        val connManager = ServiceLocator.connectionManager
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
                        finishCall()
                    }
                    else -> {}
                }
            }
        }

        viewModelScope.launch {
            repo.remoteVideoTrack.collect { track ->
                _remoteVideoTrack.value = track
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
        val repo = callRepository
        if (repo != null) {
            repo.declineIncomingCall(userId)
        } else if (serverAddress.isNotEmpty()) {
            val signaling = ServiceLocator.connectionManager.getSignaling(serverAddress)
            if (signaling.connectionState.value == ConnectionState.Disconnected) {
                signaling.connect()
            }
            signaling.send(SignalMessage.CallDecline(targetUserId = userId))
        }
        finishCall()
    }

    // ── End call ─────────────────────────────────────────────────────────

    fun endCall() {
        callRepository?.endCall()
        finishCall()
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

    private fun finishCall() {
        if (_callPhase.value == CallPhase.ENDED) return
        _callPhase.value = CallPhase.ENDED
        timerJob?.cancel()
    }

    // ── Cleanup ──────────────────────────────────────────────────────────

    override fun onCleared() {
        super.onCleared()
        callRepository?.dispose()
        callRepository = null
    }
}
