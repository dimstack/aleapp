package com.callapp.android.ui.screens.server

import androidx.lifecycle.SavedStateHandle
import com.callapp.android.data.ServerAvailabilityInfo
import com.callapp.android.domain.model.Server
import com.callapp.android.domain.model.ServerAvailabilityStatus
import com.callapp.android.ui.screens.connect.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class UnavailableServerViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun init_loadsServerFromObserver() = runTest {
        val server = testServer()
        val dependencies = FakeUnavailableServerDependencies(server)

        val viewModel = createViewModel(dependencies)
        advanceUntilIdle()

        assertFalse(viewModel.state.value.isLoading)
        assertEquals(server, viewModel.state.value.server)
        assertFalse(viewModel.state.value.isRemoved)
    }

    @Test
    fun refreshConnection_availableServer_setsOpenServer() = runTest {
        val dependencies = FakeUnavailableServerDependencies(testServer()).apply {
            refreshResult = ServerAvailabilityInfo(ServerAvailabilityStatus.AVAILABLE)
        }
        val viewModel = createViewModel(dependencies)
        advanceUntilIdle()

        viewModel.refreshConnection()
        advanceUntilIdle()

        assertTrue(viewModel.state.value.openServer)
        assertFalse(viewModel.state.value.isRefreshing)
    }

    @Test
    fun consumeOpenServer_resetsFlag() = runTest {
        val dependencies = FakeUnavailableServerDependencies(testServer()).apply {
            refreshResult = ServerAvailabilityInfo(ServerAvailabilityStatus.AVAILABLE)
        }
        val viewModel = createViewModel(dependencies)
        advanceUntilIdle()

        viewModel.refreshConnection()
        advanceUntilIdle()
        viewModel.consumeOpenServer()

        assertFalse(viewModel.state.value.openServer)
    }

    @Test
    fun removeServer_marksRemovedAndClearsSession() = runTest {
        val dependencies = FakeUnavailableServerDependencies(testServer())
        val viewModel = createViewModel(dependencies)
        advanceUntilIdle()

        viewModel.removeServer()

        assertTrue(viewModel.state.value.isRemoved)
        assertEquals(listOf("https://server.example.com"), dependencies.clearedAddresses)
    }

    private fun createViewModel(
        dependencies: UnavailableServerDependencies,
    ) = UnavailableServerViewModel(
        savedStateHandle = SavedStateHandle(mapOf("serverId" to "srv-1")),
        dependencies = dependencies,
    )

    private class FakeUnavailableServerDependencies(
        server: Server?,
    ) : UnavailableServerDependencies {
        private val serverFlow = MutableStateFlow(server)
        var refreshResult: ServerAvailabilityInfo =
            ServerAvailabilityInfo(ServerAvailabilityStatus.UNAVAILABLE, "Сервер недоступен")
        val clearedAddresses = mutableListOf<String>()

        override fun observeServerById(serverId: String): Flow<Server?> = serverFlow

        override suspend fun refreshServerAvailability(serverAddress: String): ServerAvailabilityInfo = refreshResult

        override fun clearServerSession(serverAddress: String) {
            clearedAddresses += serverAddress
        }
    }

    private companion object {
        fun testServer() = Server(
            id = "srv-1",
            name = "Test Server",
            username = "@test",
            address = "https://server.example.com",
            availabilityStatus = ServerAvailabilityStatus.UNAVAILABLE,
        )
    }
}
