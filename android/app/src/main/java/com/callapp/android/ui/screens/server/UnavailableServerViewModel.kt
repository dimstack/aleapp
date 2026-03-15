package com.callapp.android.ui.screens.server

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.callapp.android.data.ServiceLocator
import com.callapp.android.domain.model.Server
import com.callapp.android.domain.model.ServerAvailabilityStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class UnavailableServerUiState(
    val isLoading: Boolean = true,
    val server: Server? = null,
    val isRefreshing: Boolean = false,
    val isRemoving: Boolean = false,
    val openServer: Boolean = false,
    val isRemoved: Boolean = false,
)

class UnavailableServerViewModel(
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val serverId: String = savedStateHandle["serverId"] ?: ""
    private val repository = ServiceLocator.serverRepository

    private val _state = MutableStateFlow(UnavailableServerUiState())
    val state: StateFlow<UnavailableServerUiState> = _state.asStateFlow()

    private var serverAddress: String = ""

    init {
        observeServer()
    }

    private fun observeServer() {
        viewModelScope.launch {
            repository.observeServerById(serverId).collect { server ->
                serverAddress = server?.address.orEmpty()
                _state.value = _state.value.copy(
                    isLoading = false,
                    server = server,
                    isRemoved = server == null || _state.value.isRemoved,
                )
            }
        }
    }

    fun refreshConnection() {
        if (serverAddress.isBlank()) return
        _state.value = _state.value.copy(isRefreshing = true)
        viewModelScope.launch {
            val availability = repository.refreshServerAvailability(serverAddress)
            _state.value = _state.value.copy(
                isRefreshing = false,
                openServer = availability.status == ServerAvailabilityStatus.AVAILABLE,
            )
        }
    }

    fun removeServer() {
        if (serverAddress.isBlank()) return
        _state.value = _state.value.copy(isRemoving = true)
        ServiceLocator.clearServerSession(serverAddress)
        _state.value = _state.value.copy(
            isRemoving = false,
            isRemoved = true,
        )
    }

    fun consumeOpenServer() {
        _state.value = _state.value.copy(openServer = false)
    }
}
