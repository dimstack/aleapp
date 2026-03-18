package com.callapp.android.calling

import android.content.Intent
import com.callapp.android.MainActivity

object NotificationsIntentContract {
    private const val EXTRA_SERVER_ID = "notification_server_id"

    fun createIntent(serverId: String): Intent =
        Intent(MainActivity.ACTION_OPEN_NOTIFICATIONS).putExtra(EXTRA_SERVER_ID, serverId)

    fun fromIntent(intent: Intent?): String? {
        if (intent?.action != MainActivity.ACTION_OPEN_NOTIFICATIONS) return null
        return intent.getStringExtra(EXTRA_SERVER_ID)?.takeIf { it.isNotBlank() }
    }
}
