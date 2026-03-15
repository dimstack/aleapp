package com.callapp.android.data

import com.callapp.android.network.ServerConnectionManager

object ServiceLocator {
    val connectionManager = ServerConnectionManager()
    val serverRepository by lazy { ServerRepository(connectionManager) }
    lateinit var sessionStore: SessionStore

    /** Address of the server the user is currently interacting with. */
    var activeServerAddress: String = ""

    /** ID of the current user on the active server. Set after auth/login. */
    var currentUserId: String = ""

    fun clearServerSession(serverAddress: String) {
        connectionManager.removeClient(serverAddress)
        serverRepository.clearAvailability(serverAddress)

        try {
            sessionStore.removeSession(serverAddress)
            sessionStore.removePendingApproval(serverAddress)
        } catch (_: UninitializedPropertyAccessException) {
            // Ignore in previews/tests.
        }

        if (activeServerAddress == serverAddress) {
            activeServerAddress = ""
            currentUserId = ""
            try {
                sessionStore.activeServerAddress = ""
                sessionStore.activeUserId = ""
            } catch (_: UninitializedPropertyAccessException) {
                // Ignore in previews/tests.
            }
        }
    }
}
