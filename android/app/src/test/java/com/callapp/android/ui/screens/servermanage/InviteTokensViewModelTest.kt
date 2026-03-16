package com.callapp.android.ui.screens.servermanage

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.callapp.android.domain.model.InviteToken
import com.callapp.android.domain.model.Server
import com.callapp.android.domain.model.UserRole
import com.callapp.android.network.result.ApiError
import com.callapp.android.network.result.ApiResult
import com.callapp.android.ui.screens.connect.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class InviteTokensViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun loadTokens_success() = runTest {
        val tokens = listOf(
            testToken(id = "token-1", useCount = 2, maxUses = 5),
            testToken(id = "token-2", useCount = 0, maxUses = 0),
        )
        val dependencies = FakeInviteTokensDependencies().apply {
            tokensResult = ApiResult.Success(tokens)
        }

        val viewModel = createViewModel(dependencies)
        advanceUntilIdle()

        val state = viewModel.state.value as InviteTokensUiState.Success
        assertEquals(tokens, state.tokens)
    }

    @Test
    fun createToken_validParams() = runTest {
        val initial = listOf(testToken(id = "token-1"))
        val created = testToken(id = "token-2", label = "Design Team")
        val dependencies = FakeInviteTokensDependencies().apply {
            tokensResult = ApiResult.Success(initial)
            createResult = ApiResult.Success(created)
        }

        val viewModel = createViewModel(dependencies)
        advanceUntilIdle()

        viewModel.createToken(
            label = "Design Team",
            maxUses = 10,
            grantedRole = "MEMBER",
            requireApproval = true,
        )
        advanceUntilIdle()

        val state = viewModel.state.value as InviteTokensUiState.Success
        assertEquals(listOf("token-2", "token-1"), state.tokens.map { it.id })
    }

    @Test
    fun createToken_blankLabel() = runTest {
        val dependencies = FakeInviteTokensDependencies().apply {
            tokensResult = ApiResult.Success(emptyList())
        }

        val viewModel = createViewModel(dependencies)
        advanceUntilIdle()

        viewModel.createToken(
            label = "   ",
            maxUses = 10,
            grantedRole = "MEMBER",
            requireApproval = false,
        )

        val state = viewModel.state.value as InviteTokensUiState.Success
        assertEquals("Название токена обязательно", state.actionError)
        assertTrue(dependencies.createCalls.isEmpty())
    }

    @Test
    fun revokeToken_removesFromList() = runTest {
        val dependencies = FakeInviteTokensDependencies().apply {
            tokensResult = ApiResult.Success(
                listOf(
                    testToken(id = "token-1"),
                    testToken(id = "token-2"),
                ),
            )
        }

        val viewModel = createViewModel(dependencies)
        advanceUntilIdle()

        viewModel.revokeToken("token-1")
        advanceUntilIdle()

        val state = viewModel.state.value as InviteTokensUiState.Success
        assertEquals(listOf("token-2"), state.tokens.map { it.id })
    }

    @Test
    fun copyTokenToClipboard_event() = runTest {
        val dependencies = FakeInviteTokensDependencies().apply {
            tokensResult = ApiResult.Success(emptyList())
        }
        val viewModel = createViewModel(dependencies)
        advanceUntilIdle()

        viewModel.events.test {
            viewModel.copyTokenToClipboard("server.example.com:3000/ABCD1234")
            assertEquals(
                InviteTokensEvent.CopyTokenToClipboard("server.example.com:3000/ABCD1234"),
                awaitItem(),
            )
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun loadTokens_error() = runTest {
        val dependencies = FakeInviteTokensDependencies().apply {
            tokensResult = ApiResult.Failure(ApiError.NetworkError)
        }

        val viewModel = createViewModel(dependencies)
        advanceUntilIdle()

        assertTrue(viewModel.state.value is InviteTokensUiState.Error)
    }

    @Test
    fun createToken_failure_setsActionError() = runTest {
        val dependencies = FakeInviteTokensDependencies().apply {
            tokensResult = ApiResult.Success(emptyList())
            createResult = ApiResult.Failure(ApiError.NetworkError)
        }

        val viewModel = createViewModel(dependencies)
        advanceUntilIdle()

        viewModel.createToken("Team", 5, "MEMBER", false)
        advanceUntilIdle()

        val state = viewModel.state.value as InviteTokensUiState.Success
        assertTrue(state.actionError != null)
        assertTrue(state.tokens.isEmpty())
    }

    @Test
    fun revokeToken_failure_keepsList() = runTest {
        val initialTokens = listOf(testToken(id = "token-1"), testToken(id = "token-2"))
        val dependencies = FakeInviteTokensDependencies().apply {
            tokensResult = ApiResult.Success(initialTokens)
            revokeResult = ApiResult.Failure(ApiError.NetworkError)
        }

        val viewModel = createViewModel(dependencies)
        advanceUntilIdle()

        viewModel.revokeToken("token-1")
        advanceUntilIdle()

        val state = viewModel.state.value as InviteTokensUiState.Success
        assertEquals(initialTokens, state.tokens)
        assertTrue(state.actionError != null)
    }

    @Test
    fun clearActionError_resetsField() = runTest {
        val dependencies = FakeInviteTokensDependencies().apply {
            tokensResult = ApiResult.Success(emptyList())
            createResult = ApiResult.Failure(ApiError.NetworkError)
        }

        val viewModel = createViewModel(dependencies)
        advanceUntilIdle()

        viewModel.createToken("Team", 5, "MEMBER", false)
        advanceUntilIdle()
        viewModel.clearActionError()

        val state = viewModel.state.value as InviteTokensUiState.Success
        assertEquals(null, state.actionError)
    }

    private fun createViewModel(
        dependencies: InviteTokensDependencies,
    ) = InviteTokensViewModel(
        savedStateHandle = SavedStateHandle(mapOf("serverId" to "srv-1")),
        dependencies = dependencies,
    )

    private class FakeInviteTokensDependencies : InviteTokensDependencies {
        private val server = Server(
            id = "srv-1",
            name = "Test Server",
            username = "@test",
            address = "https://server.example.com",
        )

        var tokensResult: ApiResult<List<InviteToken>> = ApiResult.Success(emptyList())
        var createResult: ApiResult<InviteToken> = ApiResult.Success(testToken(id = "created"))
        var revokeResult: ApiResult<Unit> = ApiResult.Success(Unit)
        val createCalls = mutableListOf<CreateCall>()

        override fun getServerById(serverId: String): Server = server

        override suspend fun getInviteTokens(serverAddress: String): ApiResult<List<InviteToken>> = tokensResult

        override suspend fun createInviteToken(
            serverAddress: String,
            label: String,
            maxUses: Int,
            grantedRole: String,
            requireApproval: Boolean,
        ): ApiResult<InviteToken> {
            createCalls += CreateCall(label, maxUses, grantedRole, requireApproval)
            return createResult
        }

        override suspend fun revokeInviteToken(serverAddress: String, tokenId: String): ApiResult<Unit> = revokeResult
    }

    private data class CreateCall(
        val label: String,
        val maxUses: Int,
        val grantedRole: String,
        val requireApproval: Boolean,
    )

    private companion object {
        fun testToken(
            id: String,
            label: String = "Team",
            maxUses: Int = 10,
            useCount: Int = 1,
        ) = InviteToken(
            id = id,
            code = "ABCD1234",
            label = label,
            maxUses = maxUses,
            useCount = useCount,
            grantedRole = UserRole.MEMBER,
            requireApproval = false,
            revoked = false,
            createdAt = "2026-03-16T10:00:00Z",
        )
    }
}
