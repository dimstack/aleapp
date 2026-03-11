package com.example.android.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.android.data.ServiceLocator
import com.example.android.domain.model.Server
import com.example.android.domain.model.User
import com.example.android.ui.common.UiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class HomeViewModel : ViewModel() {

    private val repo = ServiceLocator.serverRepository

    private val _serversState = MutableStateFlow<UiState<List<Server>>>(UiState.Loading)
    val serversState: StateFlow<UiState<List<Server>>> = _serversState.asStateFlow()

    private val _favoritesState = MutableStateFlow<UiState<List<User>>>(UiState.Loading)
    val favoritesState: StateFlow<UiState<List<User>>> = _favoritesState.asStateFlow()

    private val _notificationCount = MutableStateFlow(1)
    val notificationCount: StateFlow<Int> = _notificationCount.asStateFlow()

    init {
        loadData()
    }

    fun loadData() {
        viewModelScope.launch {
            _serversState.value = UiState.Loading
            _favoritesState.value = UiState.Loading
            try {
                _serversState.value = UiState.Success(repo.getConnectedServers())
                _favoritesState.value = UiState.Success(repo.getFavorites())
            } catch (e: Exception) {
                _serversState.value = UiState.Error("Не удалось загрузить данные")
                _favoritesState.value = UiState.Error("Не удалось загрузить данные")
            }
        }
    }
}
