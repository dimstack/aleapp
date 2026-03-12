package com.callapp.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.navigation.compose.rememberNavController
import com.callapp.android.data.ServiceLocator
import com.callapp.android.data.SessionStore
import com.callapp.android.ui.navigation.AppNavGraph
import com.callapp.android.ui.screens.settings.UserStatus
import com.callapp.android.ui.theme.AleAppTheme
import com.callapp.android.webrtc.WebRtcFactory

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize WebRTC factory (app-scoped, survives across calls)
        WebRtcFactory.init(applicationContext)

        // Initialize session store and restore sessions
        val sessionStore = SessionStore(applicationContext)
        ServiceLocator.sessionStore = sessionStore
        restoreSessions(sessionStore)

        setContent {
            var isDarkTheme by remember { mutableStateOf(sessionStore.isDarkTheme) }
            var userStatus by remember {
                mutableStateOf(
                    try { UserStatus.valueOf(sessionStore.userStatus) }
                    catch (_: Exception) { UserStatus.ONLINE }
                )
            }

            // Update edge-to-edge based on theme
            enableEdgeToEdge(
                statusBarStyle = if (isDarkTheme) {
                    SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
                } else {
                    SystemBarStyle.light(
                        android.graphics.Color.TRANSPARENT,
                        android.graphics.Color.TRANSPARENT,
                    )
                },
                navigationBarStyle = if (isDarkTheme) {
                    SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
                } else {
                    SystemBarStyle.light(
                        android.graphics.Color.TRANSPARENT,
                        android.graphics.Color.TRANSPARENT,
                    )
                },
            )

            AleAppTheme(darkTheme = isDarkTheme) {
                val navController = rememberNavController()
                AppNavGraph(
                    navController = navController,
                    isDarkTheme = isDarkTheme,
                    userStatus = userStatus,
                    onThemeChange = {
                        isDarkTheme = it
                        sessionStore.isDarkTheme = it
                    },
                    onStatusChange = {
                        userStatus = it
                        sessionStore.userStatus = it.name
                    },
                )
            }
        }
    }

    private fun restoreSessions(sessionStore: SessionStore) {
        val sessions = sessionStore.getSessions()
        val connectionManager = ServiceLocator.connectionManager

        for ((address, session) in sessions) {
            connectionManager.restoreSession(address, session.sessionToken)
        }

        // Restore active server context
        val activeAddress = sessionStore.activeServerAddress
        if (activeAddress.isNotEmpty()) {
            ServiceLocator.activeServerAddress = activeAddress
            ServiceLocator.currentUserId = sessionStore.activeUserId
        }
    }
}
