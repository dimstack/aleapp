package com.callapp.android

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.navigation.compose.rememberNavController
import com.callapp.android.calling.CallAvailabilityServiceController
import com.callapp.android.calling.IncomingCallIntentContract
import com.callapp.android.calling.IncomingCallPayload
import com.callapp.android.calling.NotificationsIntentContract
import com.callapp.android.data.ServiceLocator
import com.callapp.android.data.SessionStore
import com.callapp.android.ui.navigation.AppNavGraph
import com.callapp.android.ui.screens.settings.UserStatus
import com.callapp.android.ui.theme.AleAppTheme
import com.callapp.android.webrtc.WebRtcFactory

class MainActivity : ComponentActivity() {
    private val notificationsPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { }
    private val pendingIncomingCallState = mutableStateOf<IncomingCallPayload?>(null)
    private val pendingNotificationsServerIdState = mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize WebRTC factory (app-scoped, survives across calls)
        WebRtcFactory.init(applicationContext)

        // Initialize session store and restore sessions
        val sessionStore = SessionStore(applicationContext)
        ServiceLocator.sessionStore = sessionStore
        restoreSessions(sessionStore)
        CallAvailabilityServiceController.sync(applicationContext)
        requestNotificationsPermissionIfNeeded()
        pendingIncomingCallState.value = extractIncomingCall(intent)
        pendingNotificationsServerIdState.value = extractNotificationsServerId(intent)

        setContent {
            var isDarkTheme by remember { mutableStateOf(sessionStore.isDarkTheme) }
            var userStatus by remember {
                mutableStateOf(
                    try { UserStatus.valueOf(sessionStore.userStatus) }
                    catch (_: Exception) { UserStatus.ONLINE }
                )
            }
            val pendingIncomingCall = pendingIncomingCallState.value
            val pendingNotificationsServerId = pendingNotificationsServerIdState.value

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
                    pendingIncomingCall = pendingIncomingCall,
                    pendingNotificationsServerId = pendingNotificationsServerId,
                    onIncomingCallConsumed = { pendingIncomingCallState.value = null },
                    onNotificationsDestinationConsumed = { pendingNotificationsServerIdState.value = null },
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

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        pendingIncomingCallState.value = extractIncomingCall(intent)
        pendingNotificationsServerIdState.value = extractNotificationsServerId(intent)
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

    private fun requestNotificationsPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        if (
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS,
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        notificationsPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
    }

    private fun extractIncomingCall(intent: Intent?): IncomingCallPayload? {
        val payload = IncomingCallIntentContract.fromIntent(intent) ?: return null
        if (payload.notificationId != 0) {
            NotificationManagerCompat.from(this).cancel(payload.notificationId)
        }
        return payload
    }

    private fun extractNotificationsServerId(intent: Intent?): String? =
        NotificationsIntentContract.fromIntent(intent)

    companion object {
        const val ACTION_OPEN_NOTIFICATIONS = "com.callapp.android.action.OPEN_NOTIFICATIONS"
    }
}
