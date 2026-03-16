package com.callapp.android.ui.screens.servermanage

import androidx.lifecycle.SavedStateHandle
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
class ServerManagementViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun loadServer_success() = runTest {
        val dependencies = FakeServerManagementDependencies()

        val viewModel = createViewModel(dependencies)

        assertFalseLoading(viewModel)
        assertEquals("Test Server", viewModel.state.value.data?.name)
        assertNull(viewModel.state.value.error)
    }

    @Test
    fun save_success_marksSaveSuccessAndUpdatesMetadata() = runTest {
        val dependencies = FakeServerManagementDependencies().apply {
            updateServerResult = ApiResult.Success(
                testServer(name = "Updated Server", username = "@updated"),
            )
        }
        val viewModel = createViewModel(dependencies)

        viewModel.save(
            name = "Updated Server",
            username = "@updated",
            description = "Updated description",
            imageUrl = "https://image.example.com",
        )
        advanceUntilIdle()

        assertTrue(viewModel.state.value.saveSuccess)
        assertEquals(
            ServerMetadataUpdate(
                serverAddress = "https://server.example.com",
                serverId = "srv-1",
                serverName = "Updated Server",
                serverUsername = "@updated",
            ),
            dependencies.metadataUpdates.single(),
        )
    }

    @Test
    fun save_networkError_setsActionError() = runTest {
        val dependencies = FakeServerManagementDependencies().apply {
            updateServerResult = ApiResult.Failure(ApiError.NetworkError)
        }
        val viewModel = createViewModel(dependencies)

        viewModel.save(
            name = "Updated Server",
            username = "@updated",
            description = "Updated description",
            imageUrl = "https://image.example.com",
        )
        advanceUntilIdle()

        assertEquals("Нет соединения с сервером", viewModel.state.value.actionError)
        assertTrue(dependencies.metadataUpdates.isEmpty())
    }

    @Test
    fun clearActionError_resetsField() = runTest {
        val dependencies = FakeServerManagementDependencies().apply {
            updateServerResult = ApiResult.Failure(ApiError.NetworkError)
        }
        val viewModel = createViewModel(dependencies)

        viewModel.save(
            name = "Updated Server",
            username = "@updated",
            description = "Updated description",
            imageUrl = "https://image.example.com",
        )
        advanceUntilIdle()
        viewModel.clearActionError()

        assertNull(viewModel.state.value.actionError)
    }

    private fun createViewModel(
        dependencies: ServerManagementDependencies,
    ) = ServerManagementViewModel(
        savedStateHandle = SavedStateHandle(mapOf("serverId" to "srv-1")),
        dependencies = dependencies,
    )

    private fun assertFalseLoading(viewModel: ServerManagementViewModel) {
        assertEquals(false, viewModel.state.value.isLoading)
    }

    private class FakeServerManagementDependencies : ServerManagementDependencies {
        private val server = testServer()
        var updateServerResult: ApiResult<Server> = ApiResult.Success(server)
        val metadataUpdates = mutableListOf<ServerMetadataUpdate>()

        override fun getServerById(serverId: String): Server = server

        override suspend fun updateServer(
            serverAddress: String,
            name: String,
            username: String,
            description: String,
            imageUrl: String,
        ): ApiResult<Server> = updateServerResult

        override fun updateServerMetadata(
            serverAddress: String,
            serverId: String,
            serverName: String,
            serverUsername: String,
        ) {
            metadataUpdates += ServerMetadataUpdate(serverAddress, serverId, serverName, serverUsername)
        }
    }

    private data class ServerMetadataUpdate(
        val serverAddress: String,
        val serverId: String,
        val serverName: String,
        val serverUsername: String,
    )

    private companion object {
        fun testServer(
            name: String = "Test Server",
            username: String = "@test",
        ) = Server(
            id = "srv-1",
            name = name,
            username = username,
            description = "Description",
            imageUrl = "https://image.example.com",
            address = "https://server.example.com",
        )
    }
}
