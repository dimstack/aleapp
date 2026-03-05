package com.example.android.ui.navigation

sealed class Route(val route: String) {
    data object Home : Route("home")
    data object ServerDetail : Route("server_detail/{serverId}") {
        fun createRoute(serverId: String) = "server_detail/$serverId"
    }
    data object Settings : Route("settings")
    data object Notifications : Route("notifications")
    data object Call : Route("call/{userId}") {
        fun createRoute(userId: String) = "call/$userId"
    }
    data object IncomingCall : Route("incoming_call/{userId}") {
        fun createRoute(userId: String) = "incoming_call/$userId"
    }
    data object JoinRequests : Route("join_requests/{serverId}") {
        fun createRoute(serverId: String) = "join_requests/$serverId"
    }
    data object ServerManagement : Route("server_management/{serverId}") {
        fun createRoute(serverId: String) = "server_management/$serverId"
    }
    data object MyProfile : Route("my_profile/{serverId}") {
        fun createRoute(serverId: String) = "my_profile/$serverId"
    }
    data object UserProfile : Route("user_profile/{serverId}/{userId}") {
        fun createRoute(serverId: String, userId: String) = "user_profile/$serverId/$userId"
    }
}
