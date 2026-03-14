package com.callapp.android.calling

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.callapp.android.MainActivity
import com.callapp.android.R
import com.callapp.android.data.ServiceLocator
import com.callapp.android.data.SessionStore
import com.callapp.android.network.signaling.ConnectionState
import com.callapp.android.network.signaling.SignalMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class CallAvailabilityService : Service() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val sessionStore by lazy { ensureSessionStore() }
    private val listenerJobs = linkedMapOf<String, kotlinx.coroutines.Job>()
    private val activeIncomingNotificationIds = linkedSetOf<Int>()

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannels()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopSelf()
            return START_NOT_STICKY
        }

        val sessions = sessionStore.getSessions()
        if (sessions.isEmpty()) {
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
            return START_NOT_STICKY
        }

        restoreSessions()
        startForeground(SERVICE_NOTIFICATION_ID, buildServiceNotification(sessions.size))
        syncListeners(sessions.keys)
        return START_STICKY
    }

    override fun onDestroy() {
        listenerJobs.values.forEach { it.cancel() }
        listenerJobs.clear()
        scope.cancel()
        super.onDestroy()
    }

    private fun ensureSessionStore(): SessionStore {
        return try {
            ServiceLocator.sessionStore
        } catch (_: UninitializedPropertyAccessException) {
            SessionStore(applicationContext).also { ServiceLocator.sessionStore = it }
        }
    }

    private fun restoreSessions() {
        val connectionManager = ServiceLocator.connectionManager
        sessionStore.getSessions().forEach { (address, session) ->
            connectionManager.restoreSession(address, session.sessionToken)
        }
    }

    private fun syncListeners(serverAddresses: Set<String>) {
        val removedAddresses = listenerJobs.keys - serverAddresses
        removedAddresses.forEach { address ->
            listenerJobs.remove(address)?.cancel()
        }

        serverAddresses.forEach { address ->
            if (listenerJobs.containsKey(address)) return@forEach

            val signaling = ServiceLocator.connectionManager.getSignaling(address)
            if (signaling.connectionState.value == ConnectionState.Disconnected) {
                signaling.connect()
            }

            listenerJobs[address] = scope.launch {
                signaling.messages.collectLatest { message ->
                    handleSignalMessage(address, message)
                }
            }
        }
    }

    private fun handleSignalMessage(serverAddress: String, message: SignalMessage) {
        when (message) {
            is SignalMessage.CallRequest -> {
                val notificationId = notificationIdFor(serverAddress, message.fromUserId)
                val payload = IncomingCallPayload(
                    serverAddress = serverAddress,
                    userId = message.fromUserId,
                    contactName = message.fromUserName.ifBlank { "Неизвестный контакт" },
                    serverName = message.fromServerName,
                    notificationId = notificationId,
                )
                showIncomingCallNotification(payload)
            }

            is SignalMessage.CallEnd,
            is SignalMessage.CallDecline,
            is SignalMessage.CallBusy -> {
                cancelIncomingCallNotifications()
            }

            else -> Unit
        }
    }

    private fun buildServiceNotification(sessionCount: Int): Notification {
        return NotificationCompat.Builder(this, SERVICE_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.sym_action_call)
            .setContentTitle(getString(R.string.app_name))
            .setContentText(
                if (sessionCount == 1) {
                    "Ожидание входящих вызовов на 1 сервере"
                } else {
                    "Ожидание входящих вызовов на $sessionCount серверах"
                },
            )
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .build()
    }

    private fun showIncomingCallNotification(payload: IncomingCallPayload) {
        val launchIntent = IncomingCallIntentContract.putExtras(
            Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            payload,
        )

        val fullScreenIntent = PendingIntent.getActivity(
            this,
            payload.notificationId,
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val notification = NotificationCompat.Builder(this, CALL_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.sym_call_incoming)
            .setContentTitle(payload.contactName)
            .setContentText(
                payload.serverName.takeIf { it.isNotBlank() }?.let { "Входящий вызов • $it" }
                    ?: "Входящий вызов",
            )
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_CALL)
            .setAutoCancel(true)
            .setOngoing(true)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setContentIntent(fullScreenIntent)
            .setFullScreenIntent(fullScreenIntent, true)
            .build()

        activeIncomingNotificationIds += payload.notificationId
        NotificationManagerCompat.from(this).notify(payload.notificationId, notification)
    }

    private fun cancelIncomingCallNotifications() {
        val manager = NotificationManagerCompat.from(this)
        activeIncomingNotificationIds.forEach(manager::cancel)
        activeIncomingNotificationIds.clear()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(
            NotificationChannel(
                SERVICE_CHANNEL_ID,
                "Call availability",
                NotificationManager.IMPORTANCE_LOW,
            ).apply {
                description = "Поддерживает соединение для входящих вызовов"
            },
        )
        manager.createNotificationChannel(
            NotificationChannel(
                CALL_CHANNEL_ID,
                "Incoming calls",
                NotificationManager.IMPORTANCE_HIGH,
            ).apply {
                description = "Показывает входящие вызовы"
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            },
        )
    }

    private fun notificationIdFor(serverAddress: String, userId: String): Int {
        return ("$serverAddress:$userId").hashCode()
    }

    companion object {
        const val ACTION_START = "com.callapp.android.action.START_CALL_AVAILABILITY"
        const val ACTION_STOP = "com.callapp.android.action.STOP_CALL_AVAILABILITY"

        private const val SERVICE_CHANNEL_ID = "call_service_channel"
        private const val CALL_CHANNEL_ID = "incoming_call_channel"
        private const val SERVICE_NOTIFICATION_ID = 1001
    }
}
