package com.callapp.android.ui.screens.notifications

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.callapp.android.data.ServiceLocator
import com.callapp.android.domain.model.Notification
import com.callapp.android.network.result.ApiResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class NotificationsUiState(
    val notifications: List<Notification> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
)

class NotificationsViewModel(
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val serverId: String = savedStateHandle["serverId"] ?: ""
    private val serverAddress: String = ServiceLocator.serverRepository
        .getServerById(serverId)
        ?.address
        .orEmpty()

    private val _state = MutableStateFlow(NotificationsUiState())
    val state: StateFlow<NotificationsUiState> = _state

    init {
        loadNotifications()
    }

    fun loadNotifications() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)
            if (serverAddress.isBlank()) {
                _state.value = _state.value.copy(
                    isLoading = false,
                    error = "Сервер не найден",
                )
                return@launch
            }
            val client = ServiceLocator.connectionManager
                .getClient(serverAddress)
            when (val result = client.getNotifications()) {
                is ApiResult.Success -> {
                    _state.value = _state.value.copy(
                        notifications = result.data,
                        isLoading = false,
                    )
                }
                is ApiResult.Failure -> {
                    _state.value = _state.value.copy(
                        isLoading = false,
                        error = "Не удалось загрузить уведомления",
                    )
                }
            }
        }
    }

    fun markAsRead(notificationId: String) {
        _state.value = _state.value.copy(
            notifications = _state.value.notifications.map {
                if (it.id == notificationId) it.copy(isRead = true) else it
            }
        )
        viewModelScope.launch {
            if (serverAddress.isBlank()) return@launch
            val client = ServiceLocator.connectionManager
                .getClient(serverAddress)
            client.markNotificationsRead()
        }
    }

    fun clearAll() {
        viewModelScope.launch {
            if (serverAddress.isBlank()) return@launch
            val client = ServiceLocator.connectionManager
                .getClient(serverAddress)
            when (client.clearNotifications()) {
                is ApiResult.Success -> {
                    _state.value = _state.value.copy(notifications = emptyList())
                }
                is ApiResult.Failure -> { /* silently fail */ }
            }
        }
    }
}
