package com.callapp.android.ui.screens.notifications

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.callapp.android.data.ServiceLocator
import com.callapp.android.domain.model.Notification
import com.callapp.android.domain.model.Server
import com.callapp.android.domain.model.User
import com.callapp.android.network.result.ApiResult
import com.callapp.android.ui.common.apiErrorMessage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class NotificationsUiState(
    val notifications: List<Notification> = emptyList(),
    val server: Server? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
)

interface NotificationsDependencies {
    fun getServerById(serverId: String): Server?
    suspend fun getUsers(serverAddress: String): ApiResult<List<User>>
    suspend fun getNotifications(serverAddress: String): ApiResult<List<Notification>>
    suspend fun markNotificationsRead(serverAddress: String): ApiResult<Unit>
    suspend fun clearNotifications(serverAddress: String): ApiResult<Unit>
}

object DefaultNotificationsDependencies : NotificationsDependencies {
    override fun getServerById(serverId: String): Server? =
        ServiceLocator.serverRepository.getServerById(serverId)

    override suspend fun getUsers(serverAddress: String): ApiResult<List<User>> =
        ServiceLocator.serverRepository.getUsers(serverAddress)

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
    private val server: Server? = dependencies.getServerById(serverId)
    private val serverAddress: String = server?.address.orEmpty()

    private val _state = MutableStateFlow(NotificationsUiState())
    val state: StateFlow<NotificationsUiState> = _state.asStateFlow()

    init {
        loadNotifications()
    }

    fun loadNotifications() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)
            if (serverAddress.isBlank()) {
                _state.value = _state.value.copy(
                    server = server,
                    isLoading = false,
                    error = "Сервер не найден",
                )
                return@launch
            }

            when (val result = dependencies.getNotifications(serverAddress)) {
                is ApiResult.Success -> {
                    val notifications = enrichNotifications(result.data).map { it.copy(isRead = true) }
                    _state.value = _state.value.copy(
                        notifications = notifications,
                        server = server,
                        isLoading = false,
                    )

                    if (result.data.any { !it.isRead }) {
                        dependencies.markNotificationsRead(serverAddress)
                    }
                }

                is ApiResult.Failure -> {
                    _state.value = _state.value.copy(
                        server = server,
                        isLoading = false,
                        error = apiErrorMessage(
                            error = result.error,
                            fallback = "Не удалось загрузить уведомления",
                            notFound = "Сервер не найден",
                        ),
                    )
                }
            }
        }
    }

    fun markAllRead() {
        _state.value = _state.value.copy(
            notifications = _state.value.notifications.map { it.copy(isRead = true) },
        )

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

    private suspend fun enrichNotifications(notifications: List<Notification>): List<Notification> {
        val missingUsernames = notifications.filter {
            it.type == com.callapp.android.domain.model.NotificationType.MISSED_CALL &&
                it.actorUserId != null &&
                it.actorUsername.isNullOrBlank()
        }
        if (missingUsernames.isEmpty()) return notifications

        val usersById = when (val usersResult = dependencies.getUsers(serverAddress)) {
            is ApiResult.Success -> usersResult.data.associateBy { it.id }
            is ApiResult.Failure -> return notifications
        }

        return notifications.map { notification ->
            val actorUserId = notification.actorUserId ?: return@map notification
            val username = notification.actorUsername
                ?: usersById[actorUserId]?.username
                ?: return@map notification
            notification.copy(actorUsername = username)
        }
    }
}
