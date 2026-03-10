package com.example.android.domain.model

enum class CallType { AUDIO, VIDEO }

sealed class CallState {
    data object Idle : CallState()

    data class Outgoing(
        val targetUserId: String,
        val targetUserName: String,
        val callType: CallType = CallType.AUDIO,
    ) : CallState()

    data class Incoming(
        val callerUserId: String,
        val callerName: String,
        val serverName: String,
        val callType: CallType = CallType.VIDEO,
    ) : CallState()

    data class Active(
        val remoteUserId: String,
        val remoteName: String,
        val callType: CallType = CallType.AUDIO,
        val durationSeconds: Int = 0,
    ) : CallState()

    data object Ended : CallState()
}
