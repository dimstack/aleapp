package com.example.android.data

import com.example.android.network.ServerConnectionManager

object ServiceLocator {
    val connectionManager = ServerConnectionManager()
    val serverRepository by lazy { ServerRepository(connectionManager) }
}
