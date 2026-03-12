package com.callapp.android.network.signaling

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

private val json = Json {
    ignoreUnknownKeys = true
    isLenient = true
}

@Serializable
sealed class SignalMessage {

    abstract val targetUserId: String

    @Serializable
    @SerialName("offer")
    data class Offer(
        val sdp: String,
        @SerialName("from_user_id") val fromUserId: String = "",
        @SerialName("target_user_id") override val targetUserId: String,
    ) : SignalMessage()

    @Serializable
    @SerialName("answer")
    data class Answer(
        val sdp: String,
        @SerialName("from_user_id") val fromUserId: String = "",
        @SerialName("target_user_id") override val targetUserId: String,
    ) : SignalMessage()

    @Serializable
    @SerialName("ice_candidate")
    data class IceCandidate(
        val candidate: String,
        @SerialName("sdp_mid") val sdpMid: String,
        @SerialName("sdp_m_line_index") val sdpMLineIndex: Int,
        @SerialName("from_user_id") val fromUserId: String = "",
        @SerialName("target_user_id") override val targetUserId: String,
    ) : SignalMessage()

    @Serializable
    @SerialName("call_request")
    data class CallRequest(
        @SerialName("from_user_id") val fromUserId: String = "",
        @SerialName("from_user_name") val fromUserName: String = "",
        @SerialName("from_server_name") val fromServerName: String = "",
        @SerialName("target_user_id") override val targetUserId: String,
    ) : SignalMessage()

    @Serializable
    @SerialName("call_response")
    data class CallResponse(
        val accepted: Boolean,
        @SerialName("from_user_id") val fromUserId: String,
        @SerialName("target_user_id") override val targetUserId: String,
    ) : SignalMessage()

    @Serializable
    @SerialName("call_end")
    data class CallEnd(
        @SerialName("target_user_id") override val targetUserId: String,
    ) : SignalMessage()

    @Serializable
    @SerialName("call_decline")
    data class CallDecline(
        @SerialName("target_user_id") override val targetUserId: String,
    ) : SignalMessage()

    @Serializable
    @SerialName("call_busy")
    data class CallBusy(
        @SerialName("target_user_id") override val targetUserId: String,
    ) : SignalMessage()

    @Serializable
    @SerialName("status_update")
    data class StatusUpdate(
        @SerialName("user_id") val userId: String,
        val status: String,
        @SerialName("target_user_id") override val targetUserId: String = "",
    ) : SignalMessage()

    fun toJson(): String = json.encodeToString(serializer(), this)

    companion object {
        fun fromJson(text: String): SignalMessage = json.decodeFromString(serializer(), text)
    }
}
