package com.callapp.android.calling

import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import com.callapp.android.data.SessionStore

object CallAvailabilityServiceController {
    fun sync(context: Context) {
        val appContext = context.applicationContext
        val hasSessions = SessionStore(appContext).getSessions().isNotEmpty()
        val serviceIntent = Intent(appContext, CallAvailabilityService::class.java)

        if (hasSessions) {
            ContextCompat.startForegroundService(
                appContext,
                serviceIntent.setAction(CallAvailabilityService.ACTION_START),
            )
        } else {
            appContext.stopService(serviceIntent)
        }
    }
}
