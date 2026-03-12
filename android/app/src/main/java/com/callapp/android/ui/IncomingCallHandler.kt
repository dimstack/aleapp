package com.callapp.android.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import com.callapp.android.data.ServiceLocator
import com.callapp.android.network.signaling.ConnectionState
import com.callapp.android.network.signaling.SignalMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * Composable that listens for incoming CallRequest signals on all saved server
 * sessions and triggers navigation to IncomingCallScreen.
 *
 * Place this at a high level in the composition (e.g. alongside NavHost).
 */
@Composable
fun IncomingCallHandler(
    onIncomingCall: (serverAddress: String, userId: String, contactName: String, serverName: String) -> Unit,
) {
    val sessionStore = ServiceLocator.sessionStore
    val serverAddresses = sessionStore.getSessions().keys.sorted()

    DisposableEffect(serverAddresses) {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
        val connManager = ServiceLocator.connectionManager

        serverAddresses.forEach { serverAddress ->
            val signaling = connManager.getSignaling(serverAddress)

            if (signaling.connectionState.value == ConnectionState.Disconnected) {
                signaling.connect()
            }

            scope.launch {
                signaling.messages.collect { message ->
                    if (message is SignalMessage.CallRequest) {
                        onIncomingCall(
                            serverAddress,
                            message.fromUserId,
                            message.fromUserName,
                            message.fromServerName,
                        )
                    }
                }
            }
        }

        onDispose {
            scope.cancel()
        }
    }
}
