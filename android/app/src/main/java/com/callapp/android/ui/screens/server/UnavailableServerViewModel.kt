package com.callapp.android.ui.screens.server

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.callapp.android.data.ServerAvailabilityInfo
import com.callapp.android.data.ServiceLocator
import com.callapp.android.domain.model.Server
import com.callapp.android.domain.model.ServerAvailabilityStatus
import kotlinx.coroutines.flow.Flow
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

interface UnavailableServerDependencies {
    fun observeServerById(serverId: String): Flow<Server?>
    suspend fun refreshServerAvailability(serverAddress: String): ServerAvailabilityInfo
    fun clearServerSession(serverAddress: String)
}

object DefaultUnavailableServerDependencies : UnavailableServerDependencies {
    private val repository get() = ServiceLocator.serverRepository

    override fun observeServerById(serverId: String): Flow<Server?> = repository.observeServerById(serverId)

    override suspend fun refreshServerAvailability(serverAddress: String): ServerAvailabilityInfo =
        repository.refreshServerAvailability(serverAddress)

    override fun clearServerSession(serverAddress: String) {
        ServiceLocator.clearServerSession(serverAddress)
    }
}

class UnavailableServerViewModel(
    savedStateHandle: SavedStateHandle,
    private val dependencies: UnavailableServerDependencies = DefaultUnavailableServerDependencies,
) : ViewModel() {

    constructor(savedStateHandle: SavedStateHandle) : this(
        savedStateHandle = savedStateHandle,
        dependencies = DefaultUnavailableServerDependencies,
    )

    private val serverId: String = savedStateHandle["serverId"] ?: ""

    private val _state = MutableStateFlow(UnavailableServerUiState())
    val state: StateFlow<UnavailableServerUiState> = _state.asStateFlow()

    private var serverAddress: String = ""

    init {
        observeServer()
    }

    private fun observeServer() {
        viewModelScope.launch {
            dependencies.observeServerById(serverId).collect { server ->
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
            val availability = dependencies.refreshServerAvailability(serverAddress)
            _state.value = _state.value.copy(
                isRefreshing = false,
                openServer = availability.status == ServerAvailabilityStatus.AVAILABLE,
            )
        }
    }

    fun removeServer() {
        if (serverAddress.isBlank()) return
        _state.value = _state.value.copy(isRemoving = true)
        dependencies.clearServerSession(serverAddress)
        _state.value = _state.value.copy(
            isRemoving = false,
            isRemoved = true,
        )
    }

    fun consumeOpenServer() {
        _state.value = _state.value.copy(openServer = false)
    }
}
