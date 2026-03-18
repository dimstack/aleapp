package com.callapp.android

import android.Manifest
import android.app.KeyguardManager
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
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
    private val keyguardManager by lazy { getSystemService(KeyguardManager::class.java) }
    private var incomingCallUiVisible = false
    private var openedFromIncomingCallWhileLocked = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        updateIncomingCallLaunchState(intent)
        updateIncomingCallWindowBehavior(intent)

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
                    onIncomingCallUiVisibilityChanged = ::setIncomingCallUiVisible,
                    onIncomingCallFlowFinished = ::handleIncomingCallFlowFinished,
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
        updateIncomingCallLaunchState(intent)
        updateIncomingCallWindowBehavior(intent)
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

    private fun updateIncomingCallWindowBehavior(intent: Intent?) {
        configureIncomingCallWindowBehavior(
            IncomingCallWindowBehavior.shouldEnable(intent) || incomingCallUiVisible,
        )
    }

    private fun updateIncomingCallLaunchState(intent: Intent?) {
        if (IncomingCallWindowBehavior.shouldEnable(intent) && keyguardManager.isKeyguardLockedCompat()) {
            openedFromIncomingCallWhileLocked = true
        }
    }

    private fun configureIncomingCallWindowBehavior(enabled: Boolean) {
        IncomingCallWindowBehavior.apply(
            enabled = enabled,
            sdkInt = Build.VERSION.SDK_INT,
            setShowWhenLocked = ::setShowWhenLocked,
            setTurnScreenOn = ::setTurnScreenOn,
            addLegacyFlags = window::addFlags,
            clearLegacyFlags = window::clearFlags,
        )
    }

    private fun setIncomingCallUiVisible(visible: Boolean) {
        if (incomingCallUiVisible == visible) return
        incomingCallUiVisible = visible
        updateIncomingCallWindowBehavior(intent)
    }

    private fun handleIncomingCallFlowFinished() {
        setIncomingCallUiVisible(false)
        if (IncomingCallWindowBehavior.shouldCloseActivityAfterIncomingCallEnds(
                openedFromIncomingCallWhileLocked = openedFromIncomingCallWhileLocked,
                isKeyguardLocked = keyguardManager.isKeyguardLockedCompat(),
            )
        ) {
            openedFromIncomingCallWhileLocked = false
            moveTaskToBack(true)
            finish()
            return
        }
        openedFromIncomingCallWhileLocked = false
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
        return IncomingCallIntentContract.fromIntent(intent)
    }

    private fun extractNotificationsServerId(intent: Intent?): String? =
        NotificationsIntentContract.fromIntent(intent)

    companion object {
        const val ACTION_OPEN_NOTIFICATIONS = "com.callapp.android.action.OPEN_NOTIFICATIONS"
    }
}

internal object IncomingCallWindowBehavior {
    private const val LEGACY_FLAGS =
        WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
            WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON

    fun shouldEnable(intent: Intent?): Boolean = IncomingCallIntentContract.fromIntent(intent) != null

    fun shouldCloseActivityAfterIncomingCallEnds(
        openedFromIncomingCallWhileLocked: Boolean,
        isKeyguardLocked: Boolean,
    ): Boolean = openedFromIncomingCallWhileLocked && isKeyguardLocked

    fun apply(
        enabled: Boolean,
        sdkInt: Int,
        setShowWhenLocked: (Boolean) -> Unit,
        setTurnScreenOn: (Boolean) -> Unit,
        addLegacyFlags: (Int) -> Unit,
        clearLegacyFlags: (Int) -> Unit,
    ) {
        if (sdkInt >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(enabled)
            setTurnScreenOn(enabled)
        } else if (enabled) {
            addLegacyFlags(LEGACY_FLAGS)
        } else {
            clearLegacyFlags(LEGACY_FLAGS)
        }
    }
}

private fun KeyguardManager?.isKeyguardLockedCompat(): Boolean = this?.isKeyguardLocked == true
