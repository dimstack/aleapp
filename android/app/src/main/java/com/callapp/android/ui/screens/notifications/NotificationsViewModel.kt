package com.callapp.android.ui.screens.notifications

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.callapp.android.data.ServiceLocator
import com.callapp.android.domain.model.Notification
import com.callapp.android.domain.model.Server
import com.callapp.android.network.result.ApiResult
import com.callapp.android.ui.common.apiErrorMessage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class NotificationsUiState(
    val notifications: List<Notification> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
)

interface NotificationsDependencies {
    fun getServerById(serverId: String): Server?
    suspend fun getNotifications(serverAddress: String): ApiResult<List<Notification>>
    suspend fun markNotificationsRead(serverAddress: String): ApiResult<Unit>
    suspend fun clearNotifications(serverAddress: String): ApiResult<Unit>
}

object DefaultNotificationsDependencies : NotificationsDependencies {
    override fun getServerById(serverId: String): Server? =
        ServiceLocator.serverRepository.getServerById(serverId)

    override suspend fun getNotifications(serverAddress: String): ApiResult<List<Notification>> =
        ServiceLocator.connectionManager.getClient(serverAddress).getNotifications()

    override suspend fun markNotificationsRead(serverAddress: String): ApiResult<Unit> =
        ServiceLocator.connectionManager.getClient(serverAddress).markNotificationsRead()

    override suspend fun clearNotifications(serverAddress: String): ApiResult<Unit> =
        ServiceLocator.connectionManager.getClient(serverAddress).clearNotifications()
}

class NotificationsViewModel(
    savedStateHandle: SavedStateHandle,
    private val dependencies: NotificationsDependencies = DefaultNotificationsDependencies,
) : ViewModel() {

    constructor(savedStateHandle: SavedStateHandle) : this(
        savedStateHandle = savedStateHandle,
        dependencies = DefaultNotificationsDependencies,
    )

    private val serverId: String = savedStateHandle["serverId"] ?: ""
    private val serverAddress: String = dependencies.getServerById(serverId)?.address.orEmpty()

    private val _state = MutableStateFlow(NotificationsUiState())
    val state: StateFlow<NotificationsUiState> = _state.asStateFlow()

    private val _unreadCount = MutableStateFlow(0)
    val unreadCount: StateFlow<Int> = _unreadCount.asStateFlow()

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
                _unreadCount.value = 0
                return@launch
            }

            when (val result = dependencies.getNotifications(serverAddress)) {
                is ApiResult.Success -> {
                    _state.value = _state.value.copy(
                        notifications = result.data,
                        isLoading = false,
                    )
                    updateUnreadCount(result.data)
                }

                is ApiResult.Failure -> {
                    _state.value = _state.value.copy(
                        isLoading = false,
                        error = apiErrorMessage(
                            error = result.error,
                            fallback = "Не удалось загрузить уведомления",
                            notFound = "Сервер не найден",
                        ),
                    )
                    _unreadCount.value = 0
                }
            }
        }
    }

    fun markAsRead(notificationId: String) {
        _state.value = _state.value.copy(
            notifications = _state.value.notifications.map {
                if (it.id == notificationId) it.copy(isRead = true) else it
            },
        )
        updateUnreadCount(_state.value.notifications)

        viewModelScope.launch {
            if (serverAddress.isBlank()) return@launch
            dependencies.markNotificationsRead(serverAddress)
        }
    }

    fun markAllRead() {
        _state.value = _state.value.copy(
            notifications = _state.value.notifications.map { it.copy(isRead = true) },
        )
        updateUnreadCount(_state.value.notifications)

        viewModelScope.launch {
            if (serverAddress.isBlank()) return@launch
            dependencies.markNotificationsRead(serverAddress)
        }
    }

    fun clearAll() {
        viewModelScope.launch {
            if (serverAddress.isBlank()) return@launch
            when (val result = dependencies.clearNotifications(serverAddress)) {
                is ApiResult.Success -> {
                    _state.value = _state.value.copy(notifications = emptyList())
                    _unreadCount.value = 0
                }

                is ApiResult.Failure -> {
                    _state.value = _state.value.copy(
                        error = apiErrorMessage(
                            error = result.error,
                            fallback = "Не удалось очистить уведомления",
                            notFound = "Сервер не найден",
                        ),
                    )
                }
            }
        }
    }

    private fun updateUnreadCount(notifications: List<Notification>) {
        _unreadCount.value = notifications.count { !it.isRead }
    }
}
