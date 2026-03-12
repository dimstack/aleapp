package com.callapp.android.ui.screens.profile

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.callapp.android.data.ServiceLocator
import com.callapp.android.domain.model.UserRole
import com.callapp.android.network.result.ApiResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class UserProfileUiState(
    val isLoading: Boolean = true,
    val user: UserProfileData? = null,
    val error: String? = null,
)

class UserProfileViewModel(
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val serverId: String = savedStateHandle["serverId"] ?: ""
    private val userId: String = savedStateHandle["userId"] ?: ""
    private val repository = ServiceLocator.serverRepository

    private val _state = MutableStateFlow(UserProfileUiState())
    val state: StateFlow<UserProfileUiState> = _state.asStateFlow()

    private var serverAddress: String = ""

    init {
        loadUser()
    }

    private fun loadUser() {
        val server = repository.getServerById(serverId)
        if (server == null) {
            _state.value = UserProfileUiState(isLoading = false, error = "Сервер не найден")
            return
        }
        serverAddress = server.address

        viewModelScope.launch {
            when (val result = repository.getUser(serverAddress, userId)) {
                is ApiResult.Success -> {
                    val user = result.data
                    _state.value = UserProfileUiState(
                        isLoading = false,
                        user = UserProfileData(
                            userId = user.id,
                            name = user.name,
                            username = user.username,
                            serverName = server.name,
                            isAdmin = user.role == UserRole.ADMIN,
                            isFavorite = false, // will be updated below
                        ),
                    )
                    checkFavorite()
                }
                is ApiResult.Failure -> {
                    _state.value = UserProfileUiState(isLoading = false, error = "Не удалось загрузить профиль")
                }
            }
        }
    }

    private suspend fun checkFavorite() {
        if (serverAddress.isEmpty()) return
        when (val result = repository.getFavoritesRemote(serverAddress)) {
            is ApiResult.Success -> {
                val isFav = result.data.any { it.id == userId }
                _state.value = _state.value.copy(
                    user = _state.value.user?.copy(isFavorite = isFav),
                )
            }
            is ApiResult.Failure -> { /* ignore, default false */ }
        }
    }

    fun toggleFavorite() {
        val current = _state.value.user ?: return
        val newFav = !current.isFavorite
        _state.value = _state.value.copy(user = current.copy(isFavorite = newFav))

        viewModelScope.launch {
            val result = if (newFav) {
                repository.addFavorite(serverAddress, userId)
            } else {
                repository.removeFavorite(serverAddress, userId)
            }
            if (result is ApiResult.Failure) {
                // Revert on failure
                _state.value = _state.value.copy(
                    user = _state.value.user?.copy(isFavorite = !newFav),
                )
            }
        }
    }
}
