package com.example.android.ui.screens.call

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

enum class CallPhase {
    CALLING,
    RINGING,
    INCOMING,
    CONNECTED,
    ENDED,
}

class CallViewModel(savedStateHandle: SavedStateHandle) : ViewModel() {

    val contactName: String = (savedStateHandle.get<String>("contactName") ?: "")
        .let { java.net.URLDecoder.decode(it, "UTF-8") }

    val serverName: String? = savedStateHandle.get<String>("serverName")
        ?.let { java.net.URLDecoder.decode(it, "UTF-8") }

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

    private var timerJob: Job? = null

    init {
        if (!isIncoming) {
            viewModelScope.launch {
                delay(CALLING_DURATION_MS)
                _callPhase.value = CallPhase.RINGING
                delay(RINGING_DURATION_MS)
                _callPhase.value = CallPhase.CONNECTED
                startTimer()
            }
        }
    }

    fun acceptCall() {
        if (_callPhase.value == CallPhase.INCOMING) {
            _callPhase.value = CallPhase.CONNECTED
            startTimer()
        }
    }

    fun declineCall() {
        _callPhase.value = CallPhase.ENDED
        timerJob?.cancel()
    }

    fun endCall() {
        _callPhase.value = CallPhase.ENDED
        timerJob?.cancel()
    }

    fun toggleMic() {
        _isMicOn.value = !_isMicOn.value
    }

    fun toggleCamera() {
        _isCameraOn.value = !_isCameraOn.value
    }

    private fun startTimer() {
        timerJob = viewModelScope.launch {
            while (isActive) {
                delay(1000)
                _elapsedSeconds.value++
            }
        }
    }

    companion object {
        private const val CALLING_DURATION_MS = 2000L
        private const val RINGING_DURATION_MS = 3000L
    }
}
