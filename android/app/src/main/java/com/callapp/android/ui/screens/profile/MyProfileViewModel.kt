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

data class MyProfileUiState(
    val isLoading: Boolean = true,
    val profile: MyProfileData? = null,
    val error: String? = null,
    val isSaving: Boolean = false,
    val saveError: String? = null,
    val saveSuccess: Boolean = false,
)

class MyProfileViewModel(
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val serverId: String = savedStateHandle["serverId"] ?: ""
    private val repository = ServiceLocator.serverRepository

    private val _state = MutableStateFlow(MyProfileUiState())
    val state: StateFlow<MyProfileUiState> = _state.asStateFlow()

    private var serverAddress: String = ""

    init {
        loadProfile()
    }

    private fun loadProfile() {
        val server = repository.getServerById(serverId)
        val userId = ServiceLocator.currentUserId

        if (server == null || userId.isEmpty()) {
            _state.value = MyProfileUiState(isLoading = false, error = "Данные не найдены")
            return
        }

        serverAddress = server.address

        viewModelScope.launch {
            when (val result = repository.getUser(serverAddress, userId)) {
                is ApiResult.Success -> {
                    val user = result.data
                    _state.value = MyProfileUiState(
                        isLoading = false,
                        profile = MyProfileData(
                            name = user.name,
                            username = user.username,
                            serverName = server.name,
                            isAdmin = user.role == UserRole.ADMIN,
                        ),
                    )
                }
                is ApiResult.Failure -> {
                    _state.value = MyProfileUiState(isLoading = false, error = "Не удалось загрузить профиль")
                }
            }
        }
    }

    fun saveProfile(name: String, username: String) {
        val userId = ServiceLocator.currentUserId
        if (serverAddress.isEmpty() || userId.isEmpty()) return

        _state.value = _state.value.copy(isSaving = true, saveError = null)
        viewModelScope.launch {
            when (repository.updateUser(serverAddress, userId, name = name, username = username)) {
                is ApiResult.Success -> {
                    _state.value = _state.value.copy(
                        isSaving = false,
                        saveSuccess = true,
                        profile = _state.value.profile?.copy(name = name, username = username),
                    )
                }
                is ApiResult.Failure -> {
                    _state.value = _state.value.copy(
                        isSaving = false,
                        saveError = "Не удалось сохранить профиль",
                    )
                }
            }
        }
    }
}
