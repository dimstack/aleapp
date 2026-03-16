package com.callapp.server.repository

import com.callapp.server.config.DatabaseConfig
import com.callapp.server.config.ServerConfig
import com.callapp.server.database.DatabaseFactory
import com.callapp.server.database.MigrationRunner
import com.callapp.server.database.ServerBootstrap
import com.callapp.server.models.JoinRequestStatus
import com.callapp.server.models.NotificationType
import com.callapp.server.models.Role
import com.callapp.server.models.UserStatus
import java.io.File
import java.sql.DriverManager
import java.util.UUID
import javax.sql.DataSource
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class RepositoryCoverageTest {

    private val databaseFile = File("build/repository-coverage-${UUID.randomUUID()}.db")
    private val dataSource: DataSource = DatabaseFactory(
        DatabaseConfig(
            path = databaseFile.absolutePath,
            maximumPoolSize = 1,
        ),
    ).createDataSource()

    private val userRepository = UserRepository(dataSource)
    private val joinRequestRepository = JoinRequestRepository(dataSource)
    private val favoriteRepository = FavoriteRepository(dataSource)
    private val serverRepository = ServerRepository(dataSource)

    init {
        MigrationRunner(dataSource).run()
        ServerBootstrap(
            dataSource = dataSource,
            config = ServerConfig(
                id = "server-1",
                name = "CallApp Test",
                username = "@callapp",
                description = "Coverage test server",
                imageUrl = "https://example.com/server.png",
            ),
        ).ensureServerRow()
    }

    @AfterTest
    fun tearDown() {
        databaseFile.delete()
    }

    @Test
    fun userRepositoryCreatesUpdatesFindsAndDeletesUsers() {
        val user = userRepository.createUser(
            id = "user-1",
            username = "@anna",
            displayName = "Anna",
            passwordHash = "hash-1",
            avatarUrl = "https://example.com/anna.png",
            role = Role.ADMIN,
            serverId = "server-1",
            isApproved = true,
        )

        assertEquals("@anna", user.username)
        assertEquals(Role.ADMIN, user.role)
        assertEquals(UserStatus.ONLINE, user.status)

        val byId = userRepository.findById("user-1")
        val byUsername = userRepository.findByUsername("server-1", "@anna")
        val allUsers = userRepository.listByServer("server-1")

        assertEquals("Anna", byId?.displayName)
        assertEquals("user-1", byUsername?.id)
        assertEquals(1, allUsers.size)

        val updated = userRepository.updateUser(
            userId = "user-1",
            name = "Anna Updated",
            username = "@anna.updated",
            avatarUrl = "https://example.com/anna-updated.png",
            status = UserStatus.DO_NOT_DISTURB,
        )

        assertEquals("Anna Updated", updated?.displayName)
        assertEquals("@anna.updated", updated?.username)
        assertEquals(UserStatus.DO_NOT_DISTURB, updated?.status)

        userRepository.deleteUser("user-1")

        assertNull(userRepository.findById("user-1"))
    }

    @Test
    fun userRepositoryTracksPendingJoinRequests() {
        seedInviteToken(id = "invite-1", token = "JOIN1234")

        userRepository.createJoinRequest(
            id = "request-1",
            username = "@pending",
            displayName = "Pending User",
            passwordHash = "hash-pending",
            avatarUrl = null,
            inviteTokenId = "invite-1",
            serverId = "server-1",
            requestedRole = Role.MEMBER,
        )

        val pending = userRepository.findPendingJoinRequest("server-1", "@pending")

        assertNotNull(pending)
        assertEquals("request-1", pending.id)
        assertEquals(JoinRequestStatus.PENDING, pending.status)
    }

    @Test
    fun joinRequestRepositoryCreatesApprovesAndDeclinesRequests() {
        createUser(id = "reviewer-1", username = "@reviewer", displayName = "Reviewer")
        createUser(id = "approved-1", username = "@approved", displayName = "Approved User")
        seedInviteToken(id = "invite-2", token = "APPROVE12")

        val created = joinRequestRepository.create(
            username = "@candidate",
            displayName = "Candidate User",
            passwordHash = "hash-candidate",
            avatarUrl = null,
            inviteTokenId = "invite-2",
            serverId = "server-1",
            requestedRole = Role.MEMBER,
        )

        assertEquals(JoinRequestStatus.PENDING, created.status)
        assertTrue(joinRequestRepository.listPending("server-1").any { it.id == created.id })

        val pendingDetails = joinRequestRepository.findPendingById(created.id)
        assertEquals("@candidate", pendingDetails?.username)

        joinRequestRepository.approve(
            requestId = created.id,
            reviewerId = "reviewer-1",
            userId = "approved-1",
        )

        val approvedSummary = joinRequestRepository.findSummaryById(created.id)
        assertEquals(JoinRequestStatus.APPROVED, approvedSummary?.status)
        assertEquals(1, countNotificationsFor("approved-1", NotificationType.REQUEST_APPROVED))

        val declined = joinRequestRepository.create(
            username = "@declined",
            displayName = "Declined User",
            passwordHash = "hash-declined",
            avatarUrl = "https://example.com/declined.png",
            inviteTokenId = "invite-2",
            serverId = "server-1",
            requestedRole = Role.ADMIN,
        )

        joinRequestRepository.decline(
            requestId = declined.id,
            reviewerId = "reviewer-1",
        )

        assertEquals(JoinRequestStatus.DECLINED, joinRequestRepository.findSummaryById(declined.id)?.status)
        assertNull(joinRequestRepository.findPendingById(declined.id))
    }

    @Test
    fun favoriteRepositoryAddsListsAndRemovesFavorites() {
        createUser(id = "user-1", username = "@anna", displayName = "Anna")
        createUser(id = "user-2", username = "@boris", displayName = "Boris")
        createUser(id = "user-3", username = "@carol", displayName = "Carol")

        favoriteRepository.addFavorite("user-1", "user-2")
        favoriteRepository.addFavorite("user-1", "user-3")
        favoriteRepository.addFavorite("user-1", "user-2")

        val favorites = favoriteRepository.listFavorites("user-1", "server-1")

        assertEquals(listOf("@boris", "@carol"), favorites.map { it.username })

        favoriteRepository.removeFavorite("user-1", "user-2")

        assertEquals(listOf("@carol"), favoriteRepository.listFavorites("user-1", "server-1").map { it.username })
    }

    @Test
    fun serverRepositoryReadsUpdatesAndDeletesCurrentServerData() {
        createUser(id = "user-10", username = "@owner", displayName = "Owner")
        createUser(id = "user-11", username = "@friend", displayName = "Friend")
        seedInviteToken(id = "invite-3", token = "SERVER12")
        joinRequestRepository.create(
            username = "@newbie",
            displayName = "Newbie",
            passwordHash = "hash-newbie",
            avatarUrl = null,
            inviteTokenId = "invite-3",
            serverId = "server-1",
            requestedRole = Role.MEMBER,
        )
        favoriteRepository.addFavorite("user-10", "user-11")
        seedNotification(userId = "user-10", type = NotificationType.REQUEST_SENT)
        seedLoginAttempt(username = "@owner")

        val current = serverRepository.getCurrentServer()
        assertEquals("CallApp Test", current?.name)
        assertEquals("@callapp", current?.username)

        val updated = serverRepository.update(
            name = "Updated Server",
            username = "@updated",
            description = "Updated description",
            imageUrl = "https://example.com/updated.png",
        )

        assertEquals("Updated Server", updated?.name)
        assertEquals("@updated", updated?.username)
        assertEquals("Updated description", updated?.description)
        assertEquals("https://example.com/updated.png", updated?.imageUrl)

        serverRepository.deleteCurrentServer()

        assertNull(serverRepository.getCurrentServer())
        assertEquals(0, countRows("users"))
        assertEquals(0, countRows("favorites"))
        assertEquals(0, countRows("notifications"))
        assertEquals(0, countRows("join_requests"))
        assertEquals(0, countRows("invite_tokens"))
        assertEquals(0, countRows("login_attempts"))
    }

    private fun createUser(
        id: String,
        username: String,
        displayName: String,
        role: Role = Role.MEMBER,
        status: UserStatus = UserStatus.ONLINE,
    ) {
        dataSource.connection.use { connection ->
            connection.prepareStatement(
                """
                INSERT INTO users(
                    id, username, display_name, password_hash, avatar_url, role, status,
                    server_id, is_approved, created_at, updated_at
                )
                VALUES (?, ?, ?, ?, NULL, ?, ?, 'server-1', 1,
                        strftime('%Y-%m-%dT%H:%M:%fZ', 'now'),
                        strftime('%Y-%m-%dT%H:%M:%fZ', 'now'))
                """.trimIndent(),
            ).use { statement ->
                statement.setString(1, id)
                statement.setString(2, username)
                statement.setString(3, displayName)
                statement.setString(4, "hash-$id")
                statement.setString(5, role.name)
                statement.setString(6, status.name)
                statement.executeUpdate()
            }
        }
    }

    private fun seedInviteToken(id: String, token: String) {
        dataSource.connection.use { connection ->
            connection.prepareStatement(
                """
                INSERT INTO invite_tokens(
                    id, token, label, server_id, created_by, max_uses, current_uses, granted_role,
                    require_approval, expires_at, is_revoked, created_at
                )
                VALUES (?, ?, 'Coverage token', 'server-1', NULL, 0, 0, 'MEMBER', 0, NULL, 0,
                        strftime('%Y-%m-%dT%H:%M:%fZ', 'now'))
                """.trimIndent(),
            ).use { statement ->
                statement.setString(1, id)
                statement.setString(2, token)
                statement.executeUpdate()
            }
        }
    }

    private fun seedNotification(userId: String, type: NotificationType) {
        dataSource.connection.use { connection ->
            connection.prepareStatement(
                """
                INSERT INTO notifications(id, user_id, type, server_name, message, is_read, created_at)
                VALUES (?, ?, ?, 'CallApp Test', 'message', 0, strftime('%Y-%m-%dT%H:%M:%fZ', 'now'))
                """.trimIndent(),
            ).use { statement ->
                statement.setString(1, UUID.randomUUID().toString())
                statement.setString(2, userId)
                statement.setString(3, type.name)
                statement.executeUpdate()
            }
        }
    }

    private fun seedLoginAttempt(username: String) {
        dataSource.connection.use { connection ->
            connection.prepareStatement(
                """
                INSERT INTO login_attempts(server_id, username, failed_attempts, locked_until, updated_at)
                VALUES ('server-1', ?, 2, NULL, strftime('%Y-%m-%dT%H:%M:%fZ', 'now'))
                """.trimIndent(),
            ).use { statement ->
                statement.setString(1, username)
                statement.executeUpdate()
            }
        }
    }

    private fun countNotificationsFor(userId: String, type: NotificationType): Int {
        dataSource.connection.use { connection ->
            connection.prepareStatement(
                "SELECT COUNT(*) FROM notifications WHERE user_id = ? AND type = ?",
            ).use { statement ->
                statement.setString(1, userId)
                statement.setString(2, type.name)
                statement.executeQuery().use { resultSet ->
                    check(resultSet.next())
                    return resultSet.getInt(1)
                }
            }
        }
    }

    private fun countRows(table: String): Int =
        DriverManager.getConnection("jdbc:sqlite:${databaseFile.absolutePath}").use { connection ->
            connection.prepareStatement("SELECT COUNT(*) FROM $table").use { statement ->
                statement.executeQuery().use { resultSet ->
                    check(resultSet.next())
                    resultSet.getInt(1)
                }
            }
        }
}
