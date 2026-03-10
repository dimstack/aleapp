package com.example.android.ui.screens.home

import androidx.lifecycle.ViewModel
import com.example.android.data.MockServerRepository
import com.example.android.data.MockUserRepository
import com.example.android.domain.model.Server
import com.example.android.domain.model.User
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class HomeViewModel : ViewModel() {

    private val serverRepo = MockServerRepository()
    private val userRepo = MockUserRepository()

    private val _favorites = MutableStateFlow(userRepo.getFavorites())
    val favorites: StateFlow<List<User>> = _favorites.asStateFlow()

    private val _servers = MutableStateFlow(serverRepo.getServers())
    val servers: StateFlow<List<Server>> = _servers.asStateFlow()

    private val _notificationCount = MutableStateFlow(1)
    val notificationCount: StateFlow<Int> = _notificationCount.asStateFlow()
}
