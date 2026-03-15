package com.callapp.android.data

import android.content.Context
import android.content.SharedPreferences
import com.callapp.android.calling.CallAvailabilityServiceController
import com.callapp.android.domain.model.Server
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString

private const val PREFS_NAME = "callapp_sessions"
private const val KEY_SESSIONS = "sessions"
private const val KEY_ACTIVE_ADDRESS = "active_address"
private const val KEY_ACTIVE_USER_ID = "active_user_id"
private const val KEY_PENDING_APPROVALS = "pending_approvals"
private const val KEY_DARK_THEME = "dark_theme"
private const val KEY_USER_STATUS = "user_status"

private val json = Json { ignoreUnknownKeys = true }

@Serializable
data class ServerSession(
    val serverAddress: String,
    val sessionToken: String,
    val userId: String,
    val serverName: String = "",
    val serverUsername: String = "",
    val serverId: String = "",
)

@Serializable
data class PendingApprovalSession(
    val serverAddress: String,
    val inviteToken: String,
    val username: String,
    val password: String,
    val serverName: String = "",
)

class SessionStore(context: Context) {
    private val appContext = context.applicationContext

    private val prefs: SharedPreferences =
        appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val _sessionsFlow = MutableStateFlow(readSessions())
    val sessionsFlow: StateFlow<Map<String, ServerSession>> = _sessionsFlow.asStateFlow()

    // ── Session management ────────────────────────────────────────────────

    fun saveSession(
        serverAddress: String,
        sessionToken: String,
        userId: String,
        serverName: String = "",
        serverUsername: String = "",
        serverId: String = "",
        setActive: Boolean = true,
    ) {
        val sessions = readSessions().toMutableMap()
        sessions[serverAddress] = ServerSession(
            serverAddress = serverAddress,
            sessionToken = sessionToken,
            userId = userId,
            serverName = serverName,
            serverUsername = serverUsername,
            serverId = serverId,
        )
        prefs.edit().apply {
            putString(KEY_SESSIONS, json.encodeToString(sessions))
            if (setActive) {
                putString(KEY_ACTIVE_ADDRESS, serverAddress)
                putString(KEY_ACTIVE_USER_ID, userId)
            }
        }.apply()
        _sessionsFlow.value = sessions.toMap()
        CallAvailabilityServiceController.sync(appContext)
    }

    fun getSessions(): Map<String, ServerSession> {
        return _sessionsFlow.value
    }

    private fun readSessions(): Map<String, ServerSession> {
        val raw = prefs.getString(KEY_SESSIONS, null) ?: return emptyMap()
        return try {
            json.decodeFromString<Map<String, ServerSession>>(raw)
        } catch (_: Exception) {
            emptyMap()
        }
    }

    fun getSession(serverAddress: String): ServerSession? =
        getSessions()[serverAddress]

    fun savePendingApproval(
        serverAddress: String,
        inviteToken: String,
        username: String,
        password: String,
        serverName: String = "",
    ) {
        val pending = getPendingApprovals().toMutableMap()
        pending[serverAddress] = PendingApprovalSession(
            serverAddress = serverAddress,
            inviteToken = inviteToken,
            username = username,
            password = password,
            serverName = serverName,
        )
        prefs.edit()
            .putString(KEY_PENDING_APPROVALS, json.encodeToString(pending))
            .apply()
    }

    fun getPendingApprovals(): Map<String, PendingApprovalSession> {
        val raw = prefs.getString(KEY_PENDING_APPROVALS, null) ?: return emptyMap()
        return try {
            json.decodeFromString<Map<String, PendingApprovalSession>>(raw)
        } catch (_: Exception) {
            emptyMap()
        }
    }

    fun removePendingApproval(serverAddress: String) {
        val pending = getPendingApprovals().toMutableMap()
        pending.remove(serverAddress)
        prefs.edit()
            .putString(KEY_PENDING_APPROVALS, json.encodeToString(pending))
            .apply()
    }

    fun removeSession(serverAddress: String) {
        val sessions = readSessions().toMutableMap()
        sessions.remove(serverAddress)
        val editor = prefs.edit()
            .putString(KEY_SESSIONS, json.encodeToString(sessions))

        if (activeServerAddress == serverAddress) {
            editor
                .putString(KEY_ACTIVE_ADDRESS, "")
                .putString(KEY_ACTIVE_USER_ID, "")
        }

        editor.apply()
        _sessionsFlow.value = sessions.toMap()
        CallAvailabilityServiceController.sync(appContext)
    }

    fun updateServerMetadata(
        serverAddress: String,
        serverId: String,
        serverName: String,
        serverUsername: String,
    ) {
        val existing = getSession(serverAddress) ?: return
        saveSession(
            serverAddress = serverAddress,
            sessionToken = existing.sessionToken,
            userId = existing.userId,
            serverName = serverName,
            serverUsername = serverUsername,
            serverId = serverId,
        )
    }

    /** Convert stored sessions to a list of Server domain objects. */
    fun getConnectedServers(): List<Server> =
        getSessions().values.map { session ->
            Server(
                id = session.serverId.ifEmpty { session.serverAddress },
                name = session.serverName.ifEmpty { session.serverAddress },
                username = session.serverUsername,
                address = session.serverAddress,
            )
        }

    // ── Active server ─────────────────────────────────────────────────────

    var activeServerAddress: String
        get() = prefs.getString(KEY_ACTIVE_ADDRESS, "") ?: ""
        set(value) { prefs.edit().putString(KEY_ACTIVE_ADDRESS, value).apply() }

    var activeUserId: String
        get() = prefs.getString(KEY_ACTIVE_USER_ID, "") ?: ""
        set(value) { prefs.edit().putString(KEY_ACTIVE_USER_ID, value).apply() }

    // ── Preferences ───────────────────────────────────────────────────────

    var isDarkTheme: Boolean
        get() = prefs.getBoolean(KEY_DARK_THEME, false)
        set(value) { prefs.edit().putBoolean(KEY_DARK_THEME, value).apply() }

    var userStatus: String
        get() = prefs.getString(KEY_USER_STATUS, "ONLINE") ?: "ONLINE"
        set(value) { prefs.edit().putString(KEY_USER_STATUS, value).apply() }
}
