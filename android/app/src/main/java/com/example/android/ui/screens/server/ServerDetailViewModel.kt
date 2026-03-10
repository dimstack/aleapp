package com.example.android.ui.screens.server

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import com.example.android.data.MockJoinRequestRepository
import com.example.android.data.MockServerRepository
import com.example.android.data.MockUserRepository
import com.example.android.domain.model.JoinRequest
import com.example.android.domain.model.Server
import com.example.android.domain.model.User
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class ServerDetailViewModel(savedStateHandle: SavedStateHandle) : ViewModel() {

    private val serverId: String = savedStateHandle["serverId"] ?: ""

    private val serverRepo = MockServerRepository()
    private val userRepo = MockUserRepository()
    private val joinRequestRepo = MockJoinRequestRepository()

    private val _server = MutableStateFlow(
        serverRepo.getServerById(serverId)
            ?: Server(serverId, "Server $serverId", "@unknown"),
    )
    val server: StateFlow<Server> = _server.asStateFlow()

    private val _members = MutableStateFlow(userRepo.getMembersByServerId(serverId))
    val members: StateFlow<List<User>> = _members.asStateFlow()

    private val _isAdmin = MutableStateFlow(userRepo.isAdmin(serverId))
    val isAdmin: StateFlow<Boolean> = _isAdmin.asStateFlow()

    private val _pendingRequests = MutableStateFlow(
        if (userRepo.isAdmin(serverId)) {
            joinRequestRepo.getRequestsByServerId(serverId)
        } else {
            emptyList()
        },
    )
    val pendingRequests: StateFlow<List<JoinRequest>> = _pendingRequests.asStateFlow()
}
