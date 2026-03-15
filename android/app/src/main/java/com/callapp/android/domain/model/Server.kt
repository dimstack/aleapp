package com.callapp.android.domain.model

enum class ServerAvailabilityStatus {
    UNKNOWN,
    CHECKING,
    AVAILABLE,
    UNAVAILABLE,
}

data class Server(
    val id: String,
    val name: String,
    val username: String,
    val description: String = "",
    val imageUrl: String? = null,
    val address: String = "",
    val availabilityStatus: ServerAvailabilityStatus = ServerAvailabilityStatus.UNKNOWN,
    val availabilityMessage: String? = null,
)
