package com.example.android.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.example.android.ui.screens.home.HomeScreen
import com.example.android.ui.screens.server.ServerDetailScreen
import com.example.android.ui.screens.server.sampleMembersCreative
import com.example.android.ui.screens.server.sampleMembers
import com.example.android.ui.screens.server.sampleJoinRequests
import com.example.android.ui.screens.server.sampleServerAdmin
import com.example.android.ui.screens.server.sampleServerRegular
import com.example.android.ui.screens.joinrequests.JoinRequestsScreen
import com.example.android.ui.screens.joinrequests.sampleRequests
import com.example.android.ui.screens.servermanage.ServerManagementScreen
import com.example.android.ui.screens.servermanage.ServerManageData
import com.example.android.ui.screens.servermanage.sampleManageData
import com.example.android.ui.screens.profile.MyProfileScreen
import com.example.android.ui.screens.profile.MyProfileData
import com.example.android.ui.screens.profile.UserProfileScreen
import com.example.android.ui.screens.profile.UserProfileData
import com.example.android.ui.screens.call.CallScreen
import com.example.android.ui.screens.call.IncomingCallScreen
import com.example.android.ui.screens.connect.AddServerScreen
import com.example.android.ui.screens.connect.CreateProfileScreen
import com.example.android.ui.screens.connect.PendingRequestScreen
import com.example.android.ui.screens.connect.RequestStatus
import com.example.android.ui.screens.notifications.NotificationsScreen
import com.example.android.ui.screens.settings.SettingsScreen
import com.example.android.ui.screens.settings.UserStatus

@Composable
fun AppNavGraph(
    navController: NavHostController,
    modifier: Modifier = Modifier,
    isDarkTheme: Boolean = false,
    userStatus: UserStatus = UserStatus.ONLINE,
    onThemeChange: (Boolean) -> Unit = {},
    onStatusChange: (UserStatus) -> Unit = {},
) {
    NavHost(
        navController = navController,
        startDestination = Route.Home.route,
        modifier = modifier
    ) {
        composable(Route.Home.route) {
            HomeScreen(
                onServerClick = { serverId ->
                    navController.navigate(Route.ServerDetail.createRoute(serverId))
                },
                onSettingsClick = {
                    navController.navigate(Route.Settings.route)
                },
                onNotificationsClick = {
                    navController.navigate(Route.Notifications.route)
                },
                onCallClick = { userId, contactName ->
                    navController.navigate(Route.Call.createRoute(userId, contactName))
                },
                onAddServerClick = {
                    navController.navigate(Route.AddServer.route)
                },
                onContactClick = { serverId, userId ->
                    navController.navigate(Route.UserProfile.createRoute(serverId, userId))
                },
            )
        }

        composable(
            route = Route.ServerDetail.route,
            arguments = listOf(navArgument("serverId") { type = NavType.StringType })
        ) { backStackEntry ->
            val serverId = backStackEntry.arguments?.getString("serverId") ?: ""

            // Pick mock data based on serverId
            val isAdmin = serverId == "s1" || serverId == "s3"
            val server = when (serverId) {
                "s1" -> sampleServerAdmin
                "s2" -> sampleServerRegular
                else -> sampleServerAdmin.copy(id = serverId, name = "Server $serverId")
            }
            val members = if (serverId == "s2") sampleMembersCreative else sampleMembers
            val requests = if (isAdmin) sampleJoinRequests else emptyList()

            ServerDetailScreen(
                server = server,
                members = members,
                isAdmin = isAdmin,
                pendingRequests = requests,
                onBack = { navController.popBackStack() },
                onCallClick = { userId, contactName ->
                    navController.navigate(Route.Call.createRoute(userId, contactName))
                },
                onProfileClick = {
                    navController.navigate(Route.MyProfile.createRoute(serverId))
                },
                onContactClick = { userId ->
                    navController.navigate(Route.UserProfile.createRoute(serverId, userId))
                },
                onManageServer = {
                    navController.navigate(Route.ServerManagement.createRoute(serverId))
                },
                onViewRequests = {
                    navController.navigate(Route.JoinRequests.createRoute(serverId))
                },
            )
        }

        composable(
            route = Route.JoinRequests.route,
            arguments = listOf(navArgument("serverId") { type = NavType.StringType })
        ) { backStackEntry ->
            val serverId = backStackEntry.arguments?.getString("serverId") ?: ""
            val serverName = when (serverId) {
                "s1" -> "Tech Community"
                "s2" -> "Creative Studio"
                "s3" -> "Game Dev Hub"
                else -> "Server"
            }

            JoinRequestsScreen(
                serverName = serverName,
                requests = sampleRequests,
                onBack = { navController.popBackStack() },
                onApprove = { requestId ->
                    // TODO: handle approve
                },
                onDecline = { requestId ->
                    // TODO: handle decline
                },
            )
        }

        composable(
            route = Route.ServerManagement.route,
            arguments = listOf(navArgument("serverId") { type = NavType.StringType })
        ) { backStackEntry ->
            val serverId = backStackEntry.arguments?.getString("serverId") ?: ""
            val manageData = when (serverId) {
                "s1" -> sampleManageData
                "s2" -> ServerManageData(
                    id = "s2",
                    name = "Creative Studio",
                    username = "creative_studio",
                    description = "Творческая студия для дизайнеров, художников и креативных профессионалов.",
                    imageUrl = "",
                )
                else -> sampleManageData.copy(id = serverId)
            }

            ServerManagementScreen(
                initial = manageData,
                onBack = { navController.popBackStack() },
                onSave = { _, _, _, _ ->
                    navController.popBackStack()
                },
                onDeleteServer = {
                    // TODO: handle delete — pop to Home
                },
            )
        }

        composable(
            route = Route.MyProfile.route,
            arguments = listOf(navArgument("serverId") { type = NavType.StringType })
        ) { backStackEntry ->
            val serverId = backStackEntry.arguments?.getString("serverId") ?: ""
            val isAdmin = serverId == "s1" || serverId == "s3"
            val serverName = when (serverId) {
                "s1" -> "Tech Community"
                "s2" -> "Creative Studio"
                "s3" -> "Game Dev Hub"
                else -> "Server"
            }

            MyProfileScreen(
                profile = MyProfileData(
                    name = if (serverId == "s2") "Александр Дизайнер" else "Александр",
                    username = if (serverId == "s2") "alex_creative" else "alex_tech",
                    serverName = serverName,
                    isAdmin = isAdmin,
                ),
                onBack = { navController.popBackStack() },
                onSaveProfile = { _, _ ->
                    // TODO: handle save
                },
            )
        }

        composable(
            route = Route.UserProfile.route,
            arguments = listOf(
                navArgument("serverId") { type = NavType.StringType },
                navArgument("userId") { type = NavType.StringType },
            )
        ) { backStackEntry ->
            val serverId = backStackEntry.arguments?.getString("serverId") ?: ""
            val userId = backStackEntry.arguments?.getString("userId") ?: ""
            val serverName = when (serverId) {
                "s1" -> "Tech Community"
                "s2" -> "Creative Studio"
                "s3" -> "Game Dev Hub"
                else -> "Server"
            }

            // Mock: pick user data based on userId
            val userData = when (userId) {
                "u1" -> UserProfileData("u1", "Анна Смирнова", "anna_s", serverName, false, true)
                "u2" -> UserProfileData("u2", "Алексей Козлов", "alexey_k", serverName, false, false)
                "u3" -> UserProfileData("u3", "Сергей Новиков", "sergey_n", serverName, false, false)
                "u4" -> UserProfileData("u4", "Дмитрий Петров", "dmitry_p", serverName, false, true)
                "u5" -> UserProfileData("u5", "Мария Волкова", "maria_v", serverName, false, false)
                "u6" -> UserProfileData("u6", "Наталья Попова", "natasha_p", serverName, false, false)
                else -> UserProfileData(userId, "Пользователь", "user", serverName, false, false)
            }

            UserProfileScreen(
                user = userData,
                onBack = { navController.popBackStack() },
                onToggleFavorite = { _ ->
                    // TODO: handle toggle favorite
                },
            )
        }

        // ── Onboarding flow ─────────────────────────────────────────────────

        composable(Route.AddServer.route) {
            AddServerScreen(
                onBack = { navController.popBackStack() },
                onConnect = { ip, apiKey ->
                    // Mock: derive a server name from the IP for now
                    val serverName = "Server ${ip.substringBefore(":")}"
                    navController.navigate(Route.CreateProfile.createRoute(serverName))
                },
            )
        }

        composable(
            route = Route.CreateProfile.route,
            arguments = listOf(navArgument("serverName") { type = NavType.StringType })
        ) { backStackEntry ->
            val encodedName = backStackEntry.arguments?.getString("serverName") ?: ""
            val serverName = java.net.URLDecoder.decode(encodedName, "UTF-8")

            CreateProfileScreen(
                serverName = serverName,
                onCreateProfile = { username, name, _ ->
                    navController.navigate(
                        Route.PendingRequest.createRoute(serverName, "@$username")
                    )
                },
            )
        }

        composable(
            route = Route.PendingRequest.route,
            arguments = listOf(
                navArgument("serverName") { type = NavType.StringType },
                navArgument("userName") { type = NavType.StringType },
            )
        ) { backStackEntry ->
            val encodedServer = backStackEntry.arguments?.getString("serverName") ?: ""
            val serverName = java.net.URLDecoder.decode(encodedServer, "UTF-8")
            val encodedUser = backStackEntry.arguments?.getString("userName") ?: ""
            val userName = java.net.URLDecoder.decode(encodedUser, "UTF-8")

            PendingRequestScreen(
                serverName = serverName,
                userName = userName,
                status = RequestStatus.PENDING,
                onBackToHome = {
                    navController.navigate(Route.Home.route) {
                        popUpTo(Route.Home.route) { inclusive = true }
                    }
                },
            )
        }

        composable(Route.Settings.route) {
            SettingsScreen(
                isDarkTheme = isDarkTheme,
                userStatus = userStatus,
                onBack = { navController.popBackStack() },
                onThemeChange = onThemeChange,
                onStatusChange = onStatusChange,
            )
        }

        composable(Route.Notifications.route) {
            NotificationsScreen(
                onBack = { navController.popBackStack() },
                onMarkAsRead = { notificationId ->
                    // TODO: handle mark as read
                },
                onClearAll = {
                    // TODO: handle clear all
                },
            )
        }

        composable(
            route = Route.Call.route,
            arguments = listOf(
                navArgument("userId") { type = NavType.StringType },
                navArgument("contactName") { type = NavType.StringType },
            )
        ) { backStackEntry ->
            val encodedName = backStackEntry.arguments?.getString("contactName") ?: ""
            val contactName = java.net.URLDecoder.decode(encodedName, "UTF-8")

            CallScreen(
                contactName = contactName,
                onEndCall = { navController.popBackStack() },
            )
        }

        composable(
            route = Route.IncomingCall.route,
            arguments = listOf(
                navArgument("userId") { type = NavType.StringType },
                navArgument("contactName") { type = NavType.StringType },
                navArgument("serverName") { type = NavType.StringType },
            )
        ) { backStackEntry ->
            val encodedName = backStackEntry.arguments?.getString("contactName") ?: ""
            val contactName = java.net.URLDecoder.decode(encodedName, "UTF-8")
            val encodedServer = backStackEntry.arguments?.getString("serverName") ?: ""
            val serverName = java.net.URLDecoder.decode(encodedServer, "UTF-8")

            IncomingCallScreen(
                contactName = contactName,
                serverName = serverName,
                onAccept = { navController.popBackStack() },
                onDecline = { navController.popBackStack() },
            )
        }
    }
}
