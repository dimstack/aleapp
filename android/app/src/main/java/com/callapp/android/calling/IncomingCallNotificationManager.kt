package com.callapp.android.calling

import android.content.Context
import androidx.core.app.NotificationManagerCompat

object IncomingCallNotificationManager {
    fun notificationIdFor(serverAddress: String, userId: String): Int {
        return ("$serverAddress:$userId").hashCode()
    }

    fun dismiss(
        context: Context,
        notificationId: Int = 0,
        serverAddress: String? = null,
        userId: String? = null,
    ) {
        val resolvedNotificationId = when {
            notificationId != 0 -> notificationId
            !serverAddress.isNullOrBlank() && !userId.isNullOrBlank() -> {
                notificationIdFor(serverAddress, userId)
            }
            else -> return
        }

        NotificationManagerCompat.from(context).cancel(resolvedNotificationId)
    }
}
