package com.callapp.android.ui.screens.profile

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.callapp.android.data.ServiceLocator
import com.callapp.android.domain.model.Server
import com.callapp.android.domain.model.User
import com.callapp.android.domain.model.UserRole
import com.callapp.android.network.result.ApiResult
import com.callapp.android.ui.common.apiErrorMessage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class UserProfileUiState(
    val isLoading: Boolean = true,
    val user: UserProfileData? = null,
    val error: String? = null,
)

interface UserProfileDependencies {
    fun getServerById(serverId: String): Server?
    suspend fun getUser(serverAddress: String, userId: String): ApiResult<User>
    suspend fun getFavorites(serverAddress: String): ApiResult<List<User>>
    suspend fun addFavorite(serverAddress: String, userId: String): ApiResult<Unit>
    suspend fun removeFavorite(serverAddress: String, userId: String): ApiResult<Unit>
}

object DefaultUserProfileDependencies : UserProfileDependencies {
    private val repository get() = ServiceLocator.serverRepository

    override fun getServerById(serverId: String): Server? = repository.getServerById(serverId)

    override suspend fun getUser(serverAddress: String, userId: String): ApiResult<User> =
        repository.getUser(serverAddress, userId)

    override suspend fun getFavorites(serverAddress: String): ApiResult<List<User>> =
        repository.getFavoritesRemote(serverAddress)

    override suspend fun addFavorite(serverAddress: String, userId: String): ApiResult<Unit> =
        repository.addFavorite(serverAddress, userId)

    override suspend fun removeFavorite(serverAddress: String, userId: String): ApiResult<Unit> =
        repository.removeFavorite(serverAddress, userId)
}

class UserProfileViewModel(
    savedStateHandle: SavedStateHandle,
    private val dependencies: UserProfileDependencies = DefaultUserProfileDependencies,
) : ViewModel() {

    constructor(savedStateHandle: SavedStateHandle) : this(
        savedStateHandle = savedStateHandle,
        dependencies = DefaultUserProfileDependencies,
    )

    private val serverId: String = savedStateHandle["serverId"] ?: ""
    private val userId: String = savedStateHandle["userId"] ?: ""

    private val _state = MutableStateFlow(UserProfileUiState())
    val state: StateFlow<UserProfileUiState> = _state.asStateFlow()

    private var serverAddress: String = ""

    init {
        loadUser()
    }

    fun loadUser() {
        val server = dependencies.getServerById(serverId)
        if (server == null) {
            _state.value = UserProfileUiState(isLoading = false, error = "Сервер не найден")
            return
        }
        serverAddress = server.address

        viewModelScope.launch {
            when (val result = dependencies.getUser(serverAddress, userId)) {
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
                            isFavorite = false,
                        ),
                    )
                    checkFavorite()
                }

                is ApiResult.Failure -> {
                    _state.value = UserProfileUiState(
                        isLoading = false,
                        error = apiErrorMessage(
                            error = result.error,
                            fallback = "Не удалось загрузить профиль",
                            notFound = "Профиль не найден",
                        ),
                    )
                }
            }
        }
    }

    private suspend fun checkFavorite() {
        if (serverAddress.isEmpty()) return
        when (val result = dependencies.getFavorites(serverAddress)) {
            is ApiResult.Success -> {
                val isFav = result.data.any { it.id == userId }
                _state.value = _state.value.copy(
                    user = _state.value.user?.copy(isFavorite = isFav),
                )
            }

            is ApiResult.Failure -> Unit
        }
    }

    fun addToFavorites() {
        updateFavorite(true)
    }

    fun removeFromFavorites() {
        updateFavorite(false)
    }

    fun toggleFavorite() {
        val current = _state.value.user ?: return
        updateFavorite(!current.isFavorite)
    }

    private fun updateFavorite(newFav: Boolean) {
        val current = _state.value.user ?: return
        _state.value = _state.value.copy(
            error = null,
            user = current.copy(isFavorite = newFav),
        )

        viewModelScope.launch {
            val result = if (newFav) {
                dependencies.addFavorite(serverAddress, userId)
            } else {
                dependencies.removeFavorite(serverAddress, userId)
            }
            if (result is ApiResult.Failure) {
                _state.value = _state.value.copy(
                    error = apiErrorMessage(
                        error = result.error,
                        fallback = "Не удалось обновить избранное",
                    ),
                    user = _state.value.user?.copy(isFavorite = !newFav),
                )
            }
        }
    }
}
