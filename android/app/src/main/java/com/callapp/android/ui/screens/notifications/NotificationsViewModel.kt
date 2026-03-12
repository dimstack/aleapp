package com.callapp.android.ui.screens.notifications

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

class NotificationsViewModel : ViewModel() {

    private val _state = MutableStateFlow(NotificationsUiState())
    val state: StateFlow<NotificationsUiState> = _state

    init {
        loadNotifications()
    }

    fun loadNotifications() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)
            val client = ServiceLocator.connectionManager
                .getClient(ServiceLocator.activeServerAddress)
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
                        error = result.error.name,
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
            val client = ServiceLocator.connectionManager
                .getClient(ServiceLocator.activeServerAddress)
            client.markNotificationsRead()
        }
    }

    fun clearAll() {
        viewModelScope.launch {
            val client = ServiceLocator.connectionManager
                .getClient(ServiceLocator.activeServerAddress)
            when (client.clearNotifications()) {
                is ApiResult.Success -> {
                    _state.value = _state.value.copy(notifications = emptyList())
                }
                is ApiResult.Failure -> { /* silently fail */ }
            }
        }
    }
}
