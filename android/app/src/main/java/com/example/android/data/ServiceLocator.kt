package com.example.android.data

import com.example.android.network.ServerConnectionManager

object ServiceLocator {
    val connectionManager = ServerConnectionManager()
    val serverRepository by lazy { ServerRepository(connectionManager) }

    /** Address of the server the user is currently interacting with. */
    var activeServerAddress: String = ""
}
