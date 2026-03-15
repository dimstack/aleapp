package com.callapp.android.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.callapp.android.data.ServiceLocator
import com.callapp.android.domain.model.Notification
import com.callapp.android.domain.model.Server
import com.callapp.android.domain.model.ServerAvailabilityStatus
import com.callapp.android.domain.model.User
import com.callapp.android.network.result.ApiError
import com.callapp.android.network.result.ApiResult
import com.callapp.android.ui.common.UiState
import com.callapp.android.ui.common.apiErrorMessage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

interface HomeDependencies {
    fun observeConnectedServers(): kotlinx.coroutines.flow.Flow<List<Server>>
    suspend fun processPendingApprovals()
    suspend fun refreshConnectedServersAvailability()
    suspend fun getFavorites(serverAddress: String): ApiResult<List<User>>
    suspend fun getNotifications(serverAddress: String): ApiResult<List<Notification>>
    fun activeServerAddress(): String
}

object DefaultHomeDependencies : HomeDependencies {
    private val repo get() = ServiceLocator.serverRepository

    override fun observeConnectedServers() = repo.observeConnectedServers()

    override suspend fun processPendingApprovals() = repo.processPendingApprovals()

    override suspend fun refreshConnectedServersAvailability() = repo.refreshConnectedServersAvailability()

    override suspend fun getFavorites(serverAddress: String): ApiResult<List<User>> =
        repo.getFavoritesRemote(serverAddress)

    override suspend fun getNotifications(serverAddress: String): ApiResult<List<Notification>> =
        ServiceLocator.connectionManager.getClient(serverAddress).getNotifications()

    override fun activeServerAddress(): String = ServiceLocator.activeServerAddress
}

class HomeViewModel(
    private val dependencies: HomeDependencies = DefaultHomeDependencies,
) : ViewModel() {

    private val _serversState = MutableStateFlow<UiState<List<Server>>>(UiState.Loading)
    val serversState: StateFlow<UiState<List<Server>>> = _serversState.asStateFlow()

    private val _favoritesState = MutableStateFlow<UiState<List<User>>>(UiState.Loading)
    val favoritesState: StateFlow<UiState<List<User>>> = _favoritesState.asStateFlow()

    private val _notificationCount = MutableStateFlow(0)
    val notificationCount: StateFlow<Int> = _notificationCount.asStateFlow()

    init {
        observeServers()
        loadData()
    }

    private fun observeServers() {
        viewModelScope.launch {
            dependencies.observeConnectedServers()
                .catch {
                    _serversState.value = UiState.Error("Не удалось загрузить серверы")
                }
                .collectLatest { servers ->
                    _serversState.value = UiState.Success(servers)
                    if (servers.any { it.availabilityStatus == ServerAvailabilityStatus.UNKNOWN }) {
                        viewModelScope.launch {
                            dependencies.refreshConnectedServersAvailability()
                        }
                    }
                }
        }
    }

    fun loadData() {
        viewModelScope.launch {
            _favoritesState.value = UiState.Loading
            dependencies.processPendingApprovals()
            dependencies.refreshConnectedServersAvailability()
            try {
                val activeAddress = dependencies.activeServerAddress()
                if (activeAddress.isNotEmpty()) {
                    when (val result = dependencies.getFavorites(activeAddress)) {
                        is ApiResult.Success -> {
                            _favoritesState.value = UiState.Success(result.data)
                        }

                        is ApiResult.Failure -> {
                            _favoritesState.value = UiState.Error(
                                apiErrorMessage(
                                    error = result.error,
                                    fallback = "Не удалось загрузить избранное",
                                    unauthorized = "Сессия истекла",
                                ),
                            )
                        }
                    }

                    when (val result = dependencies.getNotifications(activeAddress)) {
                        is ApiResult.Success -> {
                            _notificationCount.value = result.data.count { !it.isRead }
                        }

                        is ApiResult.Failure -> {
                            if (result.error is ApiError.Unauthorized && result.error.code == "unauthorized") {
                                _favoritesState.value = UiState.Error("Сессия истекла")
                            }
                            _notificationCount.value = 0
                        }
                    }
                } else {
                    _favoritesState.value = UiState.Success(emptyList())
                    _notificationCount.value = 0
                }
            } catch (_: Exception) {
                _favoritesState.value = UiState.Error("Не удалось загрузить данные")
                _notificationCount.value = 0
            }
        }
    }

    fun refresh() {
        loadData()
    }
}
