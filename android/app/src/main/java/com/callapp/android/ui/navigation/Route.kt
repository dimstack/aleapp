package com.callapp.android.ui.navigation

sealed class Route(val route: String) {
    data object Home : Route("home")
    data object AddServer : Route("add_server")
    data object AuthChoice : Route("auth_choice/{serverName}") {
        fun createRoute(serverName: String) =
            "auth_choice/${java.net.URLEncoder.encode(serverName, "UTF-8")}"
    }
    data object CreateProfile : Route("create_profile/{serverName}") {
        fun createRoute(serverName: String) =
            "create_profile/${java.net.URLEncoder.encode(serverName, "UTF-8")}"
    }
    data object Login : Route("login/{serverName}") {
        fun createRoute(serverName: String) =
            "login/${java.net.URLEncoder.encode(serverName, "UTF-8")}"
    }
    data object PendingRequest : Route("pending_request/{serverName}/{userName}") {
        fun createRoute(serverName: String, userName: String) =
            "pending_request/${java.net.URLEncoder.encode(serverName, "UTF-8")}/${java.net.URLEncoder.encode(userName, "UTF-8")}"
    }
    data object ServerDetail : Route("server_detail/{serverId}") {
        fun createRoute(serverId: String) = "server_detail/$serverId"
    }
    data object Settings : Route("settings")
    data object Notifications : Route("notifications/{serverId}") {
        fun createRoute(serverId: String) = "notifications/$serverId"
    }
    data object Call : Route("call/{serverAddress}/{userId}/{contactName}") {
        fun createRoute(serverAddress: String, userId: String, contactName: String) =
            "call/${java.net.URLEncoder.encode(serverAddress, "UTF-8")}/$userId/${java.net.URLEncoder.encode(contactName, "UTF-8")}"
    }
    data object IncomingCall : Route("incoming_call/{serverAddress}/{userId}/{contactName}/{serverName}") {
        fun createRoute(serverAddress: String, userId: String, contactName: String, serverName: String) =
            "incoming_call/${java.net.URLEncoder.encode(serverAddress, "UTF-8")}/$userId/${java.net.URLEncoder.encode(contactName, "UTF-8")}/${java.net.URLEncoder.encode(serverName, "UTF-8")}"
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
