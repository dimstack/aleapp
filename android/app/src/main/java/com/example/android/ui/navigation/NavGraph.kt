package com.example.android.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.example.android.ui.screens.home.HomeScreen
import com.example.android.ui.screens.home.HomeViewModel
import com.example.android.ui.screens.server.ServerDetailScreen
import com.example.android.ui.screens.server.ServerDetailViewModel
import com.example.android.ui.screens.joinrequests.JoinRequestsScreen
import com.example.android.ui.screens.joinrequests.sampleRequests
import com.example.android.ui.screens.servermanage.ServerManagementScreen
import com.example.android.ui.screens.servermanage.ServerManageData
import com.example.android.ui.screens.servermanage.sampleManageData
import com.example.android.ui.screens.profile.MyProfileScreen
import com.example.android.ui.screens.profile.MyProfileData
import com.example.android.ui.screens.profile.UserProfileScreen
import com.example.android.ui.screens.profile.UserProfileData
import com.example.android.ui.screens.call.CallPhase
import com.example.android.ui.screens.call.CallScreen
import com.example.android.ui.screens.call.CallStatus
import com.example.android.ui.screens.call.CallViewModel
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
            val viewModel: HomeViewModel = viewModel()
            val favorites by viewModel.favorites.collectAsState()
            val servers by viewModel.servers.collectAsState()
            val notificationCount by viewModel.notificationCount.collectAsState()

            HomeScreen(
                favorites = favorites,
                servers = servers,
                notificationCount = notificationCount,
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
        ) {
            val viewModel: ServerDetailViewModel = viewModel()
            val server by viewModel.server.collectAsState()
            val members by viewModel.members.collectAsState()
            val isAdmin by viewModel.isAdmin.collectAsState()
            val pendingRequests by viewModel.pendingRequests.collectAsState()

            ServerDetailScreen(
                server = server,
                members = members,
                isAdmin = isAdmin,
                pendingRequests = pendingRequests,
                onBack = { navController.popBackStack() },
                onCallClick = { userId, contactName ->
                    navController.navigate(Route.Call.createRoute(userId, contactName))
                },
                onProfileClick = {
                    val serverId = server.id
                    navController.navigate(Route.MyProfile.createRoute(serverId))
                },
                onContactClick = { userId ->
                    val serverId = server.id
                    navController.navigate(Route.UserProfile.createRoute(serverId, userId))
                },
                onManageServer = {
                    val serverId = server.id
                    navController.navigate(Route.ServerManagement.createRoute(serverId))
                },
                onViewRequests = {
                    val serverId = server.id
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
        ) {
            val viewModel: CallViewModel = viewModel()
            val callPhase by viewModel.callPhase.collectAsState()
            val elapsedSeconds by viewModel.elapsedSeconds.collectAsState()
            val isMicOn by viewModel.isMicOn.collectAsState()
            val isCameraOn by viewModel.isCameraOn.collectAsState()

            if (callPhase == CallPhase.ENDED) {
                LaunchedEffect(Unit) { navController.popBackStack() }
            } else {
                val callStatus = when (callPhase) {
                    CallPhase.CALLING -> CallStatus.CALLING
                    CallPhase.RINGING -> CallStatus.RINGING
                    else -> CallStatus.CONNECTED
                }

                CallScreen(
                    contactName = viewModel.contactName,
                    callStatus = callStatus,
                    elapsedSeconds = elapsedSeconds,
                    isMicOn = isMicOn,
                    isCameraOn = isCameraOn,
                    onMicToggle = viewModel::toggleMic,
                    onCameraToggle = viewModel::toggleCamera,
                    onEndCall = viewModel::endCall,
                )
            }
        }

        composable(
            route = Route.IncomingCall.route,
            arguments = listOf(
                navArgument("userId") { type = NavType.StringType },
                navArgument("contactName") { type = NavType.StringType },
                navArgument("serverName") { type = NavType.StringType },
            )
        ) {
            val viewModel: CallViewModel = viewModel()
            val callPhase by viewModel.callPhase.collectAsState()
            val elapsedSeconds by viewModel.elapsedSeconds.collectAsState()
            val isMicOn by viewModel.isMicOn.collectAsState()
            val isCameraOn by viewModel.isCameraOn.collectAsState()

            when (callPhase) {
                CallPhase.ENDED -> {
                    LaunchedEffect(Unit) { navController.popBackStack() }
                }
                CallPhase.CONNECTED -> {
                    // After accepting: show active call UI
                    CallScreen(
                        contactName = viewModel.contactName,
                        callStatus = CallStatus.CONNECTED,
                        elapsedSeconds = elapsedSeconds,
                        isMicOn = isMicOn,
                        isCameraOn = isCameraOn,
                        onMicToggle = viewModel::toggleMic,
                        onCameraToggle = viewModel::toggleCamera,
                        onEndCall = viewModel::endCall,
                    )
                }
                else -> {
                    // INCOMING phase: show accept/decline
                    IncomingCallScreen(
                        contactName = viewModel.contactName,
                        serverName = viewModel.serverName ?: "",
                        onAccept = viewModel::acceptCall,
                        onDecline = viewModel::declineCall,
                    )
                }
            }
        }
    }
}
