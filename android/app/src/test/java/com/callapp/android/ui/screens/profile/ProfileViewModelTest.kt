package com.callapp.android.ui.screens.profile

import androidx.lifecycle.SavedStateHandle
import com.callapp.android.domain.model.Server
import com.callapp.android.domain.model.User
import com.callapp.android.domain.model.UserRole
import com.callapp.android.network.result.ApiError
import com.callapp.android.network.result.ApiResult
import com.callapp.android.ui.screens.connect.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ProfileViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun loadProfile_success() = runTest {
        val dependencies = FakeMyProfileDependencies()

        val viewModel = createMyProfileViewModel(dependencies)
        advanceUntilIdle()

        val profile = viewModel.state.value.profile
        assertEquals("Alex", profile?.name)
        assertEquals("@alex", profile?.username)
        assertEquals("", profile?.avatarUrl)
        assertNull(viewModel.state.value.error)
    }

    @Test
    fun loadProfile_error() = runTest {
        val dependencies = FakeMyProfileDependencies().apply {
            userResult = ApiResult.Failure(ApiError.NetworkError)
        }

        val viewModel = createMyProfileViewModel(dependencies)
        advanceUntilIdle()

        assertTrue(viewModel.state.value.error != null)
        assertNull(viewModel.state.value.profile)
    }

    @Test
    fun updateProfile_success() = runTest {
        val dependencies = FakeMyProfileDependencies().apply {
            updateUserResult = ApiResult.Success(
                testUser(
                    name = "Updated Alex",
                    username = "@updated",
                    avatarUrl = "https://example.com/avatar.jpg",
                ),
            )
        }

        val viewModel = createMyProfileViewModel(dependencies)
        advanceUntilIdle()

        viewModel.saveProfile(
            name = "Updated Alex",
            username = "@updated",
            avatarUrl = "https://example.com/avatar.jpg",
        )
        advanceUntilIdle()

        val profile = viewModel.state.value.profile
        assertEquals("Updated Alex", profile?.name)
        assertEquals("@updated", profile?.username)
        assertEquals("https://example.com/avatar.jpg", profile?.avatarUrl)
        assertTrue(viewModel.state.value.saveSuccess)
        assertEquals("https://example.com/avatar.jpg", dependencies.lastAvatarUrl)
    }

    @Test
    fun updateProfile_networkError() = runTest {
        val dependencies = FakeMyProfileDependencies().apply {
            updateUserResult = ApiResult.Failure(ApiError.NetworkError)
        }

        val viewModel = createMyProfileViewModel(dependencies)
        advanceUntilIdle()

        viewModel.saveProfile(
            name = "Updated Alex",
            username = "@updated",
            avatarUrl = "https://example.com/avatar.jpg",
        )
        advanceUntilIdle()

        assertTrue(viewModel.state.value.saveError != null)
        assertFalse(viewModel.state.value.saveSuccess)
    }

    @Test
    fun uploadProfileImage_success_invokesCallback() = runTest {
        val dependencies = FakeMyProfileDependencies().apply {
            uploadImageResult = ApiResult.Success("https://server.example.com/uploads/profile/test.jpg")
        }
        val viewModel = createMyProfileViewModel(dependencies)
        advanceUntilIdle()

        var uploadedUrl: String? = null
        viewModel.uploadProfileImage(
            bytes = byteArrayOf(1, 2, 3),
            fileName = "avatar.jpg",
            mimeType = "image/jpeg",
        ) { uploadedUrl = it }
        advanceUntilIdle()

        assertEquals("https://server.example.com/uploads/profile/test.jpg", uploadedUrl)
        assertFalse(viewModel.state.value.isUploadingImage)
        assertNull(viewModel.state.value.uploadError)
    }

    @Test
    fun loadProfile_missingServer_setsError() = runTest {
        val dependencies = FakeMyProfileDependencies().apply {
            server = null
        }

        val viewModel = createMyProfileViewModel(dependencies)
        advanceUntilIdle()

        assertTrue(viewModel.state.value.error != null)
        assertNull(viewModel.state.value.profile)
    }

    @Test
    fun addToFavorites_success() = runTest {
        val dependencies = FakeUserProfileDependencies().apply {
            favoritesResult = ApiResult.Success(emptyList())
        }

        val viewModel = createUserProfileViewModel(dependencies)
        advanceUntilIdle()

        viewModel.addToFavorites()
        advanceUntilIdle()

        assertTrue(viewModel.state.value.user?.isFavorite == true)
        assertEquals(1, dependencies.addFavoriteCalls)
    }

    @Test
    fun removeFromFavorites_success() = runTest {
        val favoriteUser = testUser(id = "user-2", name = "Maria", username = "@maria")
        val dependencies = FakeUserProfileDependencies().apply {
            favoritesResult = ApiResult.Success(listOf(favoriteUser))
        }

        val viewModel = createUserProfileViewModel(dependencies)
        advanceUntilIdle()
        assertTrue(viewModel.state.value.user?.isFavorite == true)

        viewModel.removeFromFavorites()
        advanceUntilIdle()

        assertFalse(viewModel.state.value.user?.isFavorite == true)
        assertEquals(1, dependencies.removeFavoriteCalls)
    }

    @Test
    fun addToFavorites_networkError() = runTest {
        val dependencies = FakeUserProfileDependencies().apply {
            favoritesResult = ApiResult.Success(emptyList())
            addFavoriteResult = ApiResult.Failure(ApiError.NetworkError)
        }

        val viewModel = createUserProfileViewModel(dependencies)
        advanceUntilIdle()

        viewModel.addToFavorites()
        advanceUntilIdle()

        assertFalse(viewModel.state.value.user?.isFavorite == true)
        assertTrue(viewModel.state.value.error != null)
    }

    @Test
    fun loadUser_failure() = runTest {
        val dependencies = FakeUserProfileDependencies().apply {
            userResult = ApiResult.Failure(ApiError.NetworkError)
        }

        val viewModel = createUserProfileViewModel(dependencies)
        advanceUntilIdle()

        assertTrue(viewModel.state.value.error != null)
        assertNull(viewModel.state.value.user)
    }

    @Test
    fun loadUser_missingServer_setsError() = runTest {
        val dependencies = FakeUserProfileDependencies().apply {
            server = null
        }

        val viewModel = createUserProfileViewModel(dependencies)
        advanceUntilIdle()

        assertTrue(viewModel.state.value.error != null)
        assertNull(viewModel.state.value.user)
    }

    @Test
    fun favoritesLoadFailure_keepsProfileLoaded() = runTest {
        val dependencies = FakeUserProfileDependencies().apply {
            favoritesResult = ApiResult.Failure(ApiError.NetworkError)
        }

        val viewModel = createUserProfileViewModel(dependencies)
        advanceUntilIdle()

        assertEquals("Maria", viewModel.state.value.user?.name)
        assertFalse(viewModel.state.value.user?.isFavorite == true)
    }

    @Test
    fun removeFromFavorites_networkError_rollsBack() = runTest {
        val favoriteUser = testUser(id = "user-2", name = "Maria", username = "@maria")
        val dependencies = FakeUserProfileDependencies().apply {
            favoritesResult = ApiResult.Success(listOf(favoriteUser))
            removeFavoriteResult = ApiResult.Failure(ApiError.NetworkError)
        }

        val viewModel = createUserProfileViewModel(dependencies)
        advanceUntilIdle()

        viewModel.removeFromFavorites()
        advanceUntilIdle()

        assertTrue(viewModel.state.value.user?.isFavorite == true)
        assertTrue(viewModel.state.value.error != null)
    }

    private fun createMyProfileViewModel(
        dependencies: MyProfileDependencies,
    ) = MyProfileViewModel(
        savedStateHandle = SavedStateHandle(mapOf("serverId" to "srv-1")),
        dependencies = dependencies,
    )

    private fun createUserProfileViewModel(
        dependencies: UserProfileDependencies,
    ) = UserProfileViewModel(
        savedStateHandle = SavedStateHandle(
            mapOf(
                "serverId" to "srv-1",
                "userId" to "user-2",
            ),
        ),
        dependencies = dependencies,
    )

    private class FakeMyProfileDependencies : MyProfileDependencies {
        var server: Server? = Server(
            id = "srv-1",
            name = "Test Server",
            username = "@test",
            address = "https://server.example.com",
        )

        var userResult: ApiResult<User> = ApiResult.Success(testUser())
        var updateUserResult: ApiResult<User> = ApiResult.Success(testUser())
        var uploadImageResult: ApiResult<String> = ApiResult.Success("https://server.example.com/uploads/profile/default.jpg")
        var lastAvatarUrl: String? = null

        override fun getServerById(serverId: String): Server? = server

        override fun currentUserId(): String = "user-1"

        override suspend fun getUser(serverAddress: String, userId: String): ApiResult<User> = userResult

        override suspend fun updateUser(
            serverAddress: String,
            userId: String,
            name: String,
            username: String,
            avatarUrl: String,
        ): ApiResult<User> {
            lastAvatarUrl = avatarUrl
            return updateUserResult
        }

        override suspend fun uploadProfileImage(
            serverAddress: String,
            bytes: ByteArray,
            fileName: String,
            mimeType: String,
        ): ApiResult<String> = uploadImageResult
    }

    private class FakeUserProfileDependencies : UserProfileDependencies {
        var server: Server? = Server(
            id = "srv-1",
            name = "Test Server",
            username = "@test",
            address = "https://server.example.com",
        )

        var userResult: ApiResult<User> =
            ApiResult.Success(testUser(id = "user-2", name = "Maria", username = "@maria"))
        var favoritesResult: ApiResult<List<User>> = ApiResult.Success(emptyList())
        var addFavoriteResult: ApiResult<Unit> = ApiResult.Success(Unit)
        var removeFavoriteResult: ApiResult<Unit> = ApiResult.Success(Unit)
        var addFavoriteCalls = 0
        var removeFavoriteCalls = 0

        override fun getServerById(serverId: String): Server? = server

        override suspend fun getUser(serverAddress: String, userId: String): ApiResult<User> = userResult

        override suspend fun getFavorites(serverAddress: String): ApiResult<List<User>> = favoritesResult

        override suspend fun addFavorite(serverAddress: String, userId: String): ApiResult<Unit> {
            addFavoriteCalls += 1
            return addFavoriteResult
        }

        override suspend fun removeFavorite(serverAddress: String, userId: String): ApiResult<Unit> {
            removeFavoriteCalls += 1
            return removeFavoriteResult
        }
    }

    private companion object {
        fun testUser(
            id: String = "user-1",
            name: String = "Alex",
            username: String = "@alex",
            role: UserRole = UserRole.MEMBER,
            avatarUrl: String? = null,
        ) = User(
            id = id,
            name = name,
            username = username,
            avatarUrl = avatarUrl,
            role = role,
            serverId = "srv-1",
        )
    }
}
