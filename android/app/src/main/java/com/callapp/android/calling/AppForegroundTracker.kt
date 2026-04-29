package com.callapp.android.calling

object AppForegroundTracker {
    @Volatile
    var isStarted: Boolean = false
        private set

    fun onActivityStarted() {
        isStarted = true
    }

    fun onActivityStopped() {
        isStarted = false
    }
}
