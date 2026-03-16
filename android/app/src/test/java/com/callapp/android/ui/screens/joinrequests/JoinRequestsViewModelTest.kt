package com.callapp.android.ui.screens.joinrequests

import androidx.lifecycle.SavedStateHandle
import com.callapp.android.domain.model.JoinRequest
import com.callapp.android.domain.model.JoinRequestStatus
import com.callapp.android.domain.model.Server
import com.callapp.android.network.result.ApiError
import com.callapp.android.network.result.ApiResult
import com.callapp.android.ui.screens.connect.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class JoinRequestsViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun loadRequests_success() = runTest {
        val pendingRequests = listOf(
            testRequest(id = "req-1", status = JoinRequestStatus.PENDING),
            testRequest(id = "req-2", status = JoinRequestStatus.APPROVED),
            testRequest(id = "req-3", status = JoinRequestStatus.PENDING),
        )
        val dependencies = FakeJoinRequestsDependencies().apply {
            joinRequestsResult = ApiResult.Success(pendingRequests)
        }

        val viewModel = createViewModel(dependencies)
        advanceUntilIdle()

        assertEquals(listOf("req-1", "req-3"), viewModel.state.value.requests.map { it.id })
        assertEquals("Test Server", viewModel.state.value.serverName)
        assertNull(viewModel.state.value.error)
    }

    @Test
    fun approveRequest_removesFromList() = runTest {
        val dependencies = FakeJoinRequestsDependencies().apply {
            joinRequestsResult = ApiResult.Success(
                listOf(
                    testRequest(id = "req-1"),
                    testRequest(id = "req-2"),
                ),
            )
        }
        val viewModel = createViewModel(dependencies)
        advanceUntilIdle()

        viewModel.approve("req-1")
        advanceUntilIdle()

        assertEquals(listOf("req-2"), viewModel.state.value.requests.map { it.id })
        assertEquals(listOf(UpdateJoinRequestCall("req-1", "APPROVED")), dependencies.updateCalls)
    }

    @Test
    fun declineRequest_removesFromList() = runTest {
        val dependencies = FakeJoinRequestsDependencies().apply {
            joinRequestsResult = ApiResult.Success(
                listOf(
                    testRequest(id = "req-1"),
                    testRequest(id = "req-2"),
                ),
            )
        }
        val viewModel = createViewModel(dependencies)
        advanceUntilIdle()

        viewModel.decline("req-2")
        advanceUntilIdle()

        assertEquals(listOf("req-1"), viewModel.state.value.requests.map { it.id })
        assertEquals(listOf(UpdateJoinRequestCall("req-2", "DECLINED")), dependencies.updateCalls)
    }

    @Test
    fun approveRequest_networkError() = runTest {
        val dependencies = FakeJoinRequestsDependencies().apply {
            joinRequestsResult = ApiResult.Success(
                listOf(
                    testRequest(id = "req-1"),
                    testRequest(id = "req-2"),
                ),
            )
            updateJoinRequestResult = ApiResult.Failure(ApiError.NetworkError)
        }
        val viewModel = createViewModel(dependencies)
        advanceUntilIdle()

        viewModel.approve("req-1")
        advanceUntilIdle()

        assertEquals(listOf("req-1", "req-2"), viewModel.state.value.requests.map { it.id })
        assertEquals("Нет соединения с сервером", viewModel.state.value.error)
    }

    @Test
    fun emptyList_showsEmptyState() = runTest {
        val dependencies = FakeJoinRequestsDependencies().apply {
            joinRequestsResult = ApiResult.Success(emptyList())
        }

        val viewModel = createViewModel(dependencies)
        advanceUntilIdle()

        assertTrue(viewModel.state.value.requests.isEmpty())
        assertNull(viewModel.state.value.error)
        assertEquals(false, viewModel.state.value.isLoading)
    }

    private fun createViewModel(
        dependencies: JoinRequestsDependencies,
    ) = JoinRequestsViewModel(
        savedStateHandle = SavedStateHandle(mapOf("serverId" to "srv-1")),
        dependencies = dependencies,
    )

    private class FakeJoinRequestsDependencies : JoinRequestsDependencies {
        var server: Server = Server(
            id = "srv-1",
            name = "Stored Server",
            username = "@stored",
            address = "https://server.example.com",
        )
        var serverResult: ApiResult<Server> = ApiResult.Success(
            Server(
                id = "srv-1",
                name = "Test Server",
                username = "@test",
                address = "https://server.example.com",
            ),
        )
        var joinRequestsResult: ApiResult<List<JoinRequest>> = ApiResult.Success(emptyList())
        var updateJoinRequestResult: ApiResult<JoinRequest> = ApiResult.Success(testRequest(id = "req-updated"))
        val updateCalls = mutableListOf<UpdateJoinRequestCall>()

        override fun getServerById(serverId: String): Server = server

        override suspend fun getServer(serverAddress: String): ApiResult<Server> = serverResult

        override suspend fun getJoinRequests(serverAddress: String): ApiResult<List<JoinRequest>> = joinRequestsResult

        override suspend fun updateJoinRequest(
            serverAddress: String,
            requestId: String,
            status: String,
        ): ApiResult<JoinRequest> {
            updateCalls += UpdateJoinRequestCall(requestId, status)
            return updateJoinRequestResult
        }
    }

    private data class UpdateJoinRequestCall(
        val requestId: String,
        val status: String,
    )

    private companion object {
        fun testRequest(
            id: String,
            status: JoinRequestStatus = JoinRequestStatus.PENDING,
        ) = JoinRequest(
            id = id,
            userName = "Alex",
            username = "@alex",
            serverId = "srv-1",
            status = status,
            createdAt = "2026-03-16T10:00:00Z",
        )
    }
}
