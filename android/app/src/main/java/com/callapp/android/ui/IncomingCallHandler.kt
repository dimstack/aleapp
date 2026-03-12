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
 * Composable that listens for incoming CallRequest signals on the active server's
 * signaling connection and triggers navigation to IncomingCallScreen.
 *
 * Place this at a high level in the composition (e.g. alongside NavHost).
 */
@Composable
fun IncomingCallHandler(
    onIncomingCall: (userId: String, contactName: String, serverName: String) -> Unit,
) {
    DisposableEffect(Unit) {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
        val connManager = ServiceLocator.connectionManager
        val serverAddress = ServiceLocator.activeServerAddress

        if (serverAddress.isNotEmpty()) {
            val signaling = connManager.getSignaling(serverAddress)

            // Ensure signaling is connected
            if (signaling.connectionState.value == ConnectionState.Disconnected) {
                signaling.connect()
            }

            scope.launch {
                signaling.messages.collect { message ->
                    if (message is SignalMessage.CallRequest) {
                        onIncomingCall(
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
