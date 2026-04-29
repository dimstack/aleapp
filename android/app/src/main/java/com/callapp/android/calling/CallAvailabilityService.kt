package com.callapp.android.calling

import android.app.Notification as AndroidNotification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.callapp.android.MainActivity
import com.callapp.android.R
import com.callapp.android.data.ServiceLocator
import com.callapp.android.data.ServerSession
import com.callapp.android.data.SessionStore
import com.callapp.android.domain.model.Notification as AppNotification
import com.callapp.android.network.result.ApiResult
import com.callapp.android.network.signaling.ConnectionState
import com.callapp.android.network.signaling.SignalMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class CallAvailabilityService : Service() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val sessionStore by lazy { ensureSessionStore() }
    private val missedCallTracker by lazy {
        MissedCallNotificationTracker(
            getSharedPreferences(PUSH_PREFS_NAME, MODE_PRIVATE),
        )
    }
    private val listenerJobs = linkedMapOf<String, kotlinx.coroutines.Job>()
    private val activeIncomingNotificationIds = linkedSetOf<Int>()
    private var notificationsPollingJob: kotlinx.coroutines.Job? = null

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
        syncNotificationsPolling(sessions.values)
        return START_STICKY
    }

    override fun onDestroy() {
        notificationsPollingJob?.cancel()
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

    private fun syncNotificationsPolling(sessions: Collection<ServerSession>) {
        notificationsPollingJob?.cancel()
        if (sessions.isEmpty()) return

        val snapshots = sessions.toList()
        notificationsPollingJob = scope.launch {
            while (isActive) {
                snapshots.forEach { session ->
                    pollNotifications(session)
                }
                delay(NOTIFICATION_POLL_INTERVAL_MS)
            }
        }
    }

    private suspend fun pollNotifications(session: ServerSession) {
        val client = ServiceLocator.connectionManager.restoreSession(
            session.serverAddress,
            session.sessionToken,
        )
        val result = client.getNotifications()
        val notifications = when (result) {
            is ApiResult.Success -> result.data
            is ApiResult.Failure -> return
        }
        val freshMissedCalls = missedCallTracker.consumeNew(session.serverAddress, notifications)
        freshMissedCalls.forEach { notification ->
            showMissedCallNotification(session, notification)
        }
    }

    private fun handleSignalMessage(serverAddress: String, message: SignalMessage) {
        when (message) {
            is SignalMessage.CallRequest -> {
                val notificationId = IncomingCallNotificationManager.notificationIdFor(
                    serverAddress = serverAddress,
                    userId = message.fromUserId,
                )
                val payload = IncomingCallPayload(
                    serverAddress = serverAddress,
                    userId = message.fromUserId,
                    contactName = message.fromUserName.ifBlank { "Неизвестный контакт" },
                    serverName = message.fromServerName,
                    notificationId = notificationId,
                )
                if (!AppForegroundTracker.isStarted) {
                    showIncomingCallNotification(payload)
                }
            }

            is SignalMessage.CallEnd,
            is SignalMessage.CallDecline,
            is SignalMessage.CallBusy -> {
                cancelIncomingCallNotifications()
            }

            else -> Unit
        }
    }

    private fun buildServiceNotification(sessionCount: Int): AndroidNotification {
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
        val ringtoneUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
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
            .setSound(ringtoneUri)
            .build()

        activeIncomingNotificationIds += payload.notificationId
        NotificationManagerCompat.from(this).notify(payload.notificationId, notification)
    }

    private fun showMissedCallNotification(
        session: ServerSession,
        notification: AppNotification,
    ) {
        val serverId = session.serverId.ifBlank { session.serverAddress }
        val launchIntent = NotificationsIntentContract.createIntent(serverId).apply {
            setClass(this@CallAvailabilityService, MainActivity::class.java)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val contentIntent = PendingIntent.getActivity(
            this,
            missedNotificationIdFor(session.serverAddress, notification.id),
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val contactName = notification.actorDisplayName?.takeIf { it.isNotBlank() } ?: "Неизвестный контакт"
        val contentText = session.serverName.takeIf { it.isNotBlank() }
            ?.let { "Пропущенный вызов • $it" }
            ?: "Пропущенный вызов"

        val systemNotification = NotificationCompat.Builder(this, ALERTS_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.sym_call_missed)
            .setContentTitle(contactName)
            .setContentText(contentText)
            .setStyle(NotificationCompat.BigTextStyle().bigText(contentText))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_CALL)
            .setAutoCancel(true)
            .setContentIntent(contentIntent)
            .build()

        NotificationManagerCompat.from(this).notify(
            missedNotificationIdFor(session.serverAddress, notification.id),
            systemNotification,
        )
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
                lockscreenVisibility = AndroidNotification.VISIBILITY_PUBLIC
                setSound(
                    RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE),
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_NOTIFICATION_RINGTONE)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build(),
                )
            },
        )
        manager.createNotificationChannel(
            NotificationChannel(
                ALERTS_CHANNEL_ID,
                "Notifications",
                NotificationManager.IMPORTANCE_HIGH,
            ).apply {
                description = "Показывает пропущенные вызовы и другие важные уведомления"
                lockscreenVisibility = AndroidNotification.VISIBILITY_PRIVATE
            },
        )
    }

    private fun missedNotificationIdFor(serverAddress: String, notificationId: String): Int {
        return ("missed:$serverAddress:$notificationId").hashCode()
    }

    companion object {
        const val ACTION_START = "com.callapp.android.action.START_CALL_AVAILABILITY"
        const val ACTION_STOP = "com.callapp.android.action.STOP_CALL_AVAILABILITY"

        private const val SERVICE_CHANNEL_ID = "call_service_channel"
        private const val CALL_CHANNEL_ID = "incoming_call_channel_v2"
        private const val ALERTS_CHANNEL_ID = "app_alerts_channel"
        private const val SERVICE_NOTIFICATION_ID = 1001
        private const val NOTIFICATION_POLL_INTERVAL_MS = 30_000L
        private const val PUSH_PREFS_NAME = "callapp_notification_pushes"
    }
}
