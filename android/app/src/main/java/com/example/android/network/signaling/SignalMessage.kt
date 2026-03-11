package com.example.android.network.signaling

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
        @SerialName("target_user_id") override val targetUserId: String,
    ) : SignalMessage()

    @Serializable
    @SerialName("answer")
    data class Answer(
        val sdp: String,
        @SerialName("target_user_id") override val targetUserId: String,
    ) : SignalMessage()

    @Serializable
    @SerialName("ice_candidate")
    data class IceCandidate(
        val candidate: String,
        @SerialName("sdp_mid") val sdpMid: String,
        @SerialName("sdp_m_line_index") val sdpMLineIndex: Int,
        @SerialName("target_user_id") override val targetUserId: String,
    ) : SignalMessage()

    @Serializable
    @SerialName("call_request")
    data class CallRequest(
        @SerialName("target_user_id") override val targetUserId: String,
    ) : SignalMessage()

    @Serializable
    @SerialName("call_response")
    data class CallResponse(
        val accepted: Boolean,
        @SerialName("from_user_id") val fromUserId: String,
        @SerialName("target_user_id") override val targetUserId: String,
    ) : SignalMessage()

    fun toJson(): String = json.encodeToString(serializer(), this)

    companion object {
        fun fromJson(text: String): SignalMessage = json.decodeFromString(serializer(), text)
    }
}
