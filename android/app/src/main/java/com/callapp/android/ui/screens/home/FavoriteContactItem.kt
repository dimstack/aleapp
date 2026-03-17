package com.callapp.android.ui.screens.home

import com.callapp.android.domain.model.Server
import com.callapp.android.domain.model.ServerAvailabilityStatus
import com.callapp.android.domain.model.User

data class FavoriteContactItem(
    val user: User,
    val serverName: String,
    val serverUsername: String,
    val serverImageUrl: String? = null,
    val serverAvailabilityStatus: ServerAvailabilityStatus = ServerAvailabilityStatus.AVAILABLE,
    val serverAvailabilityMessage: String? = null,
) {
    val isUnavailable: Boolean
        get() = serverAvailabilityStatus == ServerAvailabilityStatus.UNAVAILABLE
}

fun User.toFavoriteContactItem(
    server: Server,
    unavailableMessage: String? = null,
): FavoriteContactItem = FavoriteContactItem(
    user = this,
    serverName = server.name,
    serverUsername = server.username,
    serverImageUrl = server.imageUrl,
    serverAvailabilityStatus = server.availabilityStatus,
    serverAvailabilityMessage = unavailableMessage
        ?: if (server.availabilityStatus == ServerAvailabilityStatus.UNAVAILABLE) {
            server.availabilityMessage
        } else {
            null
        },
)
