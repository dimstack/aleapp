package com.callapp.android.data

import androidx.test.core.app.ApplicationProvider
import app.cash.turbine.test
import com.callapp.android.network.ServerConnectionManager
import com.callapp.android.network.result.ApiError
import com.callapp.android.network.result.ApiResult
import com.callapp.android.testutil.InMemorySharedPreferences
import kotlin.system.measureTimeMillis
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ServerRepositoryTest {

    private lateinit var connectionManager: ServerConnectionManager
    private lateinit var repository: ServerRepository
    private lateinit var sessionStore: SessionStore
    private val servers = mutableListOf<MockWebServer>()

    @Before
    fun setUp() {
        ApplicationProvider.getApplicationContext<android.content.Context>()
        connectionManager = ServerConnectionManager()
        repository = ServerRepository(connectionManager)
        sessionStore = SessionStore.createForTests(InMemorySharedPreferences())
        ServiceLocator.sessionStore = sessionStore
        ServiceLocator.activeServerAddress = ""
        ServiceLocator.currentUserId = ""
    }

    @After
    fun tearDown() {
        servers.forEach { it.shutdown() }
        servers.clear()
        ServiceLocator.activeServerAddress = ""
        ServiceLocator.currentUserId = ""
    }

    @Test
    fun connect_success() = runTest {
        val server = createServer()
        server.enqueueJson(
            """
            {
              "session_token": "connect-token",
              "status": "joined",
              "user": {
                "id": "user-1",
                "username": "@alex",
                "display_name": "Alex"
              },
              "server": {
                "id": "srv-1",
                "name": "Alpha",
                "username": "@alpha"
              }
            }
            """.trimIndent(),
        )

        val result = repository.connect(server.baseUrl(), "VALID1234")

        assertTrue(result is ApiResult.Success)
        val savedSession = sessionStore.getSession(server.baseUrl())
        requireNotNull(savedSession)
        assertEquals("connect-token", savedSession.sessionToken)
        assertEquals("user-1", savedSession.userId)
        assertEquals("Alpha", savedSession.serverName)
        assertEquals(server.baseUrl(), ServiceLocator.activeServerAddress)
    }

    @Test
    fun connect_invalidToken() = runTest {
        val server = createServer()
        server.enqueue(
            MockResponse()
                .setResponseCode(401)
                .setHeader("Content-Type", "application/json")
                .setBody("""{"code":"invite_token_invalid","message":"Invite token is invalid"}"""),
        )

        val result = repository.connect(server.baseUrl(), "BAD1234")

        assertTrue(result is ApiResult.Failure)
        assertFalse(sessionStore.getSessions().containsKey(server.baseUrl()))
    }

    @Test
    fun login_success() = runTest {
        val server = createServer()
        sessionStore.saveSession(
            serverAddress = server.baseUrl(),
            sessionToken = "old-token",
            userId = "user-1",
            serverName = "Alpha",
            serverUsername = "@alpha",
            serverId = "srv-1",
        )
        connectionManager.restoreSession(server.baseUrl(), "old-token")
        server.enqueueJson(
            """
            {
              "session_token": "new-token",
              "status": "joined",
              "user": {
                "id": "user-1",
                "username": "@alex",
                "display_name": "Alex"
              },
              "server": {
                "id": "srv-1",
                "name": "Alpha",
                "username": "@alpha"
              }
            }
            """.trimIndent(),
        )

        val result = repository.login(server.baseUrl(), "INVITE123", "@alex", "password123")

        assertTrue(result is ApiResult.Success)
        assertEquals("new-token", sessionStore.getSession(server.baseUrl())?.sessionToken)
    }

    @Test
    fun getUsers_multipleSessions() = runTest {
        val firstServer = createServer()
        val secondServer = createServer()
        connectionManager.restoreSession(firstServer.baseUrl(), "token-1")
        connectionManager.restoreSession(secondServer.baseUrl(), "token-2")
        firstServer.enqueueJson(
            """
            [
              {
                "id": "user-1",
                "username": "@anna",
                "display_name": "Anna",
                "is_online": true
              }
            ]
            """.trimIndent(),
        )
        secondServer.enqueueJson(
            """
            [
              {
                "id": "user-2",
                "username": "@boris",
                "display_name": "Boris",
                "is_online": false
              }
            ]
            """.trimIndent(),
        )

        val firstResult = repository.getUsers(firstServer.baseUrl())
        val secondResult = repository.getUsers(secondServer.baseUrl())

        require(firstResult is ApiResult.Success)
        require(secondResult is ApiResult.Success)
        assertEquals("@anna", firstResult.data.single().username)
        assertEquals("@boris", secondResult.data.single().username)
        assertEquals("Bearer token-1", firstServer.takeRequest().getHeader("Authorization"))
        assertEquals("Bearer token-2", secondServer.takeRequest().getHeader("Authorization"))
    }

    @Test
    fun refreshAvailability_serverResponds() = runTest {
        val server = createServer()
        sessionStore.saveSession(
            serverAddress = server.baseUrl(),
            sessionToken = "token-1",
            userId = "user-1",
        )
        server.enqueueJson(
            """
            {
              "id": "srv-1",
              "name": "Alpha",
              "username": "@alpha"
            }
            """.trimIndent(),
        )

        val availability = repository.refreshServerAvailability(server.baseUrl())

        assertEquals(com.callapp.android.domain.model.ServerAvailabilityStatus.AVAILABLE, availability.status)
        assertEquals(
            com.callapp.android.domain.model.ServerAvailabilityStatus.AVAILABLE,
            repository.availabilityByAddress.value[server.baseUrl()]?.status,
        )
    }

    @Test
    fun refreshAvailability_serverDown() = runTest {
        val address = "http://127.0.0.1:9"
        sessionStore.saveSession(
            serverAddress = address,
            sessionToken = "token-1",
            userId = "user-1",
        )

        val availability = repository.refreshServerAvailability(address)

        assertEquals(com.callapp.android.domain.model.ServerAvailabilityStatus.UNAVAILABLE, availability.status)
        assertEquals(
            com.callapp.android.domain.model.ServerAvailabilityStatus.UNAVAILABLE,
            repository.availabilityByAddress.value[address]?.status,
        )
    }

    @Test
    fun refreshAvailability_invalidSessionClearsSavedServerSession() = runTest {
        val server = createServer()
        sessionStore.saveSession(
            serverAddress = server.baseUrl(),
            sessionToken = "token-1",
            userId = "user-1",
        )
        ServiceLocator.activeServerAddress = server.baseUrl()
        ServiceLocator.currentUserId = "user-1"
        connectionManager.restoreSession(server.baseUrl(), "token-1")
        server.enqueue(
            MockResponse()
                .setResponseCode(401)
                .setHeader("Content-Type", "application/json")
                .setBody("""{"code":"unauthorized","message":"User session is invalid"}"""),
        )

        val availability = repository.refreshServerAvailability(server.baseUrl())

        assertEquals(com.callapp.android.domain.model.ServerAvailabilityStatus.UNAVAILABLE, availability.status)
        assertEquals(null, sessionStore.getSession(server.baseUrl()))
        assertEquals("", ServiceLocator.activeServerAddress)
        assertEquals("", ServiceLocator.currentUserId)
        assertEquals(null, repository.availabilityByAddress.value[server.baseUrl()])
    }

    @Test
    fun refreshAvailability_allServers() = runTest {
        val firstServer = createServer()
        val secondServer = createServer()
        sessionStore.saveSession(firstServer.baseUrl(), "token-1", "user-1")
        sessionStore.saveSession(secondServer.baseUrl(), "token-2", "user-2", setActive = false)
        firstServer.enqueue(
            MockResponse()
                .setHeader("Content-Type", "application/json")
                .setBodyDelay(400, java.util.concurrent.TimeUnit.MILLISECONDS)
                .setBody("""{"id":"srv-1","name":"Alpha","username":"@alpha"}"""),
        )
        secondServer.enqueue(
            MockResponse()
                .setHeader("Content-Type", "application/json")
                .setBodyDelay(400, java.util.concurrent.TimeUnit.MILLISECONDS)
                .setBody("""{"id":"srv-2","name":"Beta","username":"@beta"}"""),
        )

        val elapsedMs = measureTimeMillis {
            repository.refreshConnectedServersAvailability()
        }

        assertTrue("Expected parallel refresh, elapsed=$elapsedMs ms", elapsedMs < 750)
        assertEquals(
            com.callapp.android.domain.model.ServerAvailabilityStatus.AVAILABLE,
            repository.availabilityByAddress.value[firstServer.baseUrl()]?.status,
        )
        assertEquals(
            com.callapp.android.domain.model.ServerAvailabilityStatus.AVAILABLE,
            repository.availabilityByAddress.value[secondServer.baseUrl()]?.status,
        )
    }

    @Test
    fun processPendingApprovals_approved() = runTest {
        val server = createServer()
        sessionStore.savePendingApproval(
            serverAddress = server.baseUrl(),
            inviteToken = "INVITE123",
            username = "@alex",
            password = "password123",
            serverName = "Alpha",
        )
        server.enqueueJson(
            """
            {
              "session_token": "approved-token",
              "status": "joined",
              "user": {
                "id": "user-1",
                "username": "@alex",
                "display_name": "Alex"
              },
              "server": {
                "id": "srv-1",
                "name": "Alpha",
                "username": "@alpha"
              }
            }
            """.trimIndent(),
        )

        repository.processPendingApprovals()

        assertFalse(sessionStore.getPendingApprovals().containsKey(server.baseUrl()))
        assertEquals("approved-token", sessionStore.getSession(server.baseUrl())?.sessionToken)
    }

    @Test
    fun processPendingApprovals_stillPending() = runTest {
        val server = createServer()
        sessionStore.savePendingApproval(
            serverAddress = server.baseUrl(),
            inviteToken = "INVITE123",
            username = "@alex",
            password = "password123",
            serverName = "Alpha",
        )
        server.enqueueJson(
            """
            {
              "session_token": "pending-token",
              "status": "pending",
              "user": {
                "id": "user-1",
                "username": "@alex",
                "display_name": "Alex"
              },
              "server": {
                "id": "srv-1",
                "name": "Alpha",
                "username": "@alpha"
              }
            }
            """.trimIndent(),
        )

        repository.processPendingApprovals()

        assertTrue(sessionStore.getPendingApprovals().containsKey(server.baseUrl()))
        assertFalse(sessionStore.getSessions().containsKey(server.baseUrl()))
        assertEquals("pending-token", connectionManager.getClient(server.baseUrl()).sessionToken)
    }

    @Test
    fun processPendingApprovals_declined() = runTest {
        val server = createServer()
        sessionStore.savePendingApproval(
            serverAddress = server.baseUrl(),
            inviteToken = "INVITE123",
            username = "@alex",
            password = "password123",
            serverName = "Alpha",
        )
        server.enqueue(
            MockResponse()
                .setResponseCode(403)
                .setHeader("Content-Type", "application/json")
                .setBody("""{"code":"forbidden","message":"Request declined"}"""),
        )

        repository.pendingApprovalEvents.test {
            repository.processPendingApprovals()

            val event = awaitItem()
            require(event is PendingApprovalEvent.Declined)
            assertEquals(server.baseUrl(), event.serverAddress)
            assertEquals("Alpha", event.serverName)
            assertFalse(sessionStore.getPendingApprovals().containsKey(server.baseUrl()))
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun processPendingApprovals_networkError_keepsPending() = runTest {
        val address = "http://127.0.0.1:9"
        sessionStore.savePendingApproval(
            serverAddress = address,
            inviteToken = "INVITE123",
            username = "@alex",
            password = "password123",
            serverName = "Alpha",
        )

        repository.processPendingApprovals()

        assertTrue(sessionStore.getPendingApprovals().containsKey(address))
        assertFalse(sessionStore.getSessions().containsKey(address))
    }

    private fun createServer(): MockWebServer =
        MockWebServer().also {
            it.start()
            servers += it
        }

    private fun MockWebServer.baseUrl(): String = url("/").toString().removeSuffix("/")

    private fun MockWebServer.enqueueJson(body: String) {
        enqueue(
            MockResponse()
                .setHeader("Content-Type", "application/json")
                .setBody(body),
        )
    }
}
