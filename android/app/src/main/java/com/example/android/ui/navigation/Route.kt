package com.example.android.ui.navigation

sealed class Route(val route: String) {
    data object Home : Route("home")
    data object AddServer : Route("add_server")
    data object CreateProfile : Route("create_profile/{serverName}") {
        fun createRoute(serverName: String) =
            "create_profile/${java.net.URLEncoder.encode(serverName, "UTF-8")}"
    }
    data object PendingRequest : Route("pending_request/{serverName}/{userName}") {
        fun createRoute(serverName: String, userName: String) =
            "pending_request/${java.net.URLEncoder.encode(serverName, "UTF-8")}/${java.net.URLEncoder.encode(userName, "UTF-8")}"
    }
    data object ServerDetail : Route("server_detail/{serverId}") {
        fun createRoute(serverId: String) = "server_detail/$serverId"
    }
    data object Settings : Route("settings")
    data object Notifications : Route("notifications")
    data object Call : Route("call/{userId}/{contactName}") {
        fun createRoute(userId: String, contactName: String) =
            "call/$userId/${java.net.URLEncoder.encode(contactName, "UTF-8")}"
    }
    data object IncomingCall : Route("incoming_call/{userId}/{contactName}/{serverName}") {
        fun createRoute(userId: String, contactName: String, serverName: String) =
            "incoming_call/$userId/${java.net.URLEncoder.encode(contactName, "UTF-8")}/${java.net.URLEncoder.encode(serverName, "UTF-8")}"
    }
    data object JoinRequests : Route("join_requests/{serverId}") {
        fun createRoute(serverId: String) = "join_requests/$serverId"
    }
    data object ServerManagement : Route("server_management/{serverId}") {
        fun createRoute(serverId: String) = "server_management/$serverId"
    }
    data object InviteTokens : Route("invite_tokens/{serverId}") {
        fun createRoute(serverId: String) = "invite_tokens/$serverId"
    }
    data object MyProfile : Route("my_profile/{serverId}") {
        fun createRoute(serverId: String) = "my_profile/$serverId"
    }
    data object UserProfile : Route("user_profile/{serverId}/{userId}") {
        fun createRoute(serverId: String, userId: String) = "user_profile/$serverId/$userId"
    }
}
