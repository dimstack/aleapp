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

data class MyProfileUiState(
    val isLoading: Boolean = true,
    val profile: MyProfileData? = null,
    val error: String? = null,
    val isSaving: Boolean = false,
    val saveError: String? = null,
    val saveSuccess: Boolean = false,
    val isUploadingImage: Boolean = false,
    val uploadError: String? = null,
)

interface MyProfileDependencies {
    fun getServerById(serverId: String): Server?
    fun currentUserId(): String
    suspend fun getUser(serverAddress: String, userId: String): ApiResult<User>
    suspend fun updateUser(
        serverAddress: String,
        userId: String,
        name: String,
        username: String,
        avatarUrl: String,
    ): ApiResult<User>
    suspend fun uploadProfileImage(
        serverAddress: String,
        bytes: ByteArray,
        fileName: String,
        mimeType: String,
    ): ApiResult<String>
}

object DefaultMyProfileDependencies : MyProfileDependencies {
    private val repository get() = ServiceLocator.serverRepository

    override fun getServerById(serverId: String): Server? = repository.getServerById(serverId)

    override fun currentUserId(): String = ServiceLocator.currentUserId

    override suspend fun getUser(serverAddress: String, userId: String): ApiResult<User> =
        repository.getUser(serverAddress, userId)

    override suspend fun updateUser(
        serverAddress: String,
        userId: String,
        name: String,
        username: String,
        avatarUrl: String,
    ): ApiResult<User> = repository.updateUser(
        serverAddress = serverAddress,
        userId = userId,
        name = name,
        username = username,
        avatarUrl = avatarUrl,
    )

    override suspend fun uploadProfileImage(
        serverAddress: String,
        bytes: ByteArray,
        fileName: String,
        mimeType: String,
    ): ApiResult<String> = repository.uploadProfileImage(
        serverAddress = serverAddress,
        bytes = bytes,
        fileName = fileName,
        mimeType = mimeType,
    )
}

class MyProfileViewModel(
    savedStateHandle: SavedStateHandle,
    private val dependencies: MyProfileDependencies = DefaultMyProfileDependencies,
) : ViewModel() {

    constructor(savedStateHandle: SavedStateHandle) : this(
        savedStateHandle = savedStateHandle,
        dependencies = DefaultMyProfileDependencies,
    )

    private val serverId: String = savedStateHandle["serverId"] ?: ""

    private val _state = MutableStateFlow(MyProfileUiState())
    val state: StateFlow<MyProfileUiState> = _state.asStateFlow()

    private var serverAddress: String = ""

    init {
        loadProfile()
    }

    fun loadProfile() {
        val server = dependencies.getServerById(serverId)
        val userId = dependencies.currentUserId()

        if (server == null || userId.isEmpty()) {
            _state.value = MyProfileUiState(isLoading = false, error = "Данные не найдены")
            return
        }

        serverAddress = server.address

        viewModelScope.launch {
            when (val result = dependencies.getUser(serverAddress, userId)) {
                is ApiResult.Success -> {
                    val user = result.data
                    _state.value = MyProfileUiState(
                        isLoading = false,
                        profile = MyProfileData(
                            name = user.name,
                            username = user.username,
                            avatarUrl = user.avatarUrl.orEmpty(),
                            serverName = server.name,
                            isAdmin = user.role == UserRole.ADMIN,
                        ),
                    )
                }

                is ApiResult.Failure -> {
                    _state.value = MyProfileUiState(
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

    fun saveProfile(name: String, username: String, avatarUrl: String) {
        val userId = dependencies.currentUserId()
        if (serverAddress.isEmpty() || userId.isEmpty()) return

        _state.value = _state.value.copy(isSaving = true, saveError = null)
        viewModelScope.launch {
            when (val result = dependencies.updateUser(serverAddress, userId, name, username, avatarUrl)) {
                is ApiResult.Success -> {
                    _state.value = _state.value.copy(
                        isSaving = false,
                        saveSuccess = true,
                        profile = _state.value.profile?.copy(
                            name = name,
                            username = username,
                            avatarUrl = result.data.avatarUrl.orEmpty(),
                        ),
                    )
                }

                is ApiResult.Failure -> {
                    _state.value = _state.value.copy(
                        isSaving = false,
                        saveError = apiErrorMessage(
                            error = result.error,
                            fallback = "Не удалось сохранить профиль",
                            notFound = "Профиль не найден",
                        ),
                    )
                }
            }
        }
    }

    fun uploadProfileImage(
        bytes: ByteArray,
        fileName: String,
        mimeType: String,
        onUploaded: (String) -> Unit,
    ) {
        if (serverAddress.isEmpty()) return

        _state.value = _state.value.copy(isUploadingImage = true, uploadError = null)
        viewModelScope.launch {
            when (val result = dependencies.uploadProfileImage(serverAddress, bytes, fileName, mimeType)) {
                is ApiResult.Success -> {
                    _state.value = _state.value.copy(isUploadingImage = false, uploadError = null)
                    onUploaded(result.data)
                }

                is ApiResult.Failure -> {
                    _state.value = _state.value.copy(
                        isUploadingImage = false,
                        uploadError = apiErrorMessage(
                            error = result.error,
                            fallback = "Не удалось загрузить изображение",
                        ),
                    )
                }
            }
        }
    }

    fun clearUploadError() {
        _state.value = _state.value.copy(uploadError = null)
    }
}
