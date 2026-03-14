package com.callapp.android.calling

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class CallBootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            CallAvailabilityServiceController.sync(context)
        }
    }
}
