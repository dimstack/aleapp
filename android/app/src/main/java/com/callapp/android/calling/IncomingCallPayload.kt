package com.callapp.android.calling

import android.content.Intent

data class IncomingCallPayload(
    val serverAddress: String,
    val userId: String,
    val contactName: String,
    val serverName: String,
    val notificationId: Int = 0,
)

object IncomingCallIntentContract {
    private const val EXTRA_SERVER_ADDRESS = "incoming_call_server_address"
    private const val EXTRA_USER_ID = "incoming_call_user_id"
    private const val EXTRA_CONTACT_NAME = "incoming_call_contact_name"
    private const val EXTRA_SERVER_NAME = "incoming_call_server_name"
    private const val EXTRA_NOTIFICATION_ID = "incoming_call_notification_id"

    fun putExtras(intent: Intent, payload: IncomingCallPayload): Intent = intent.apply {
        putExtra(EXTRA_SERVER_ADDRESS, payload.serverAddress)
        putExtra(EXTRA_USER_ID, payload.userId)
        putExtra(EXTRA_CONTACT_NAME, payload.contactName)
        putExtra(EXTRA_SERVER_NAME, payload.serverName)
        putExtra(EXTRA_NOTIFICATION_ID, payload.notificationId)
    }

    fun fromIntent(intent: Intent?): IncomingCallPayload? {
        intent ?: return null

        val serverAddress = intent.getStringExtra(EXTRA_SERVER_ADDRESS).orEmpty()
        val userId = intent.getStringExtra(EXTRA_USER_ID).orEmpty()
        val contactName = intent.getStringExtra(EXTRA_CONTACT_NAME).orEmpty()
        val serverName = intent.getStringExtra(EXTRA_SERVER_NAME).orEmpty()
        val notificationId = intent.getIntExtra(EXTRA_NOTIFICATION_ID, 0)

        if (serverAddress.isBlank() || userId.isBlank() || contactName.isBlank()) {
            return null
        }

        return IncomingCallPayload(
            serverAddress = serverAddress,
            userId = userId,
            contactName = contactName,
            serverName = serverName,
            notificationId = notificationId,
        )
    }
}
