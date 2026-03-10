package com.example.android.ui.navigation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.example.android.ui.components.AleAppButton
import com.example.android.ui.components.AleAppButtonVariant
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
import com.example.android.ui.screens.settings.SettingsScreen
import com.example.android.ui.screens.settings.ThemeMode
import com.example.android.ui.screens.settings.UserStatus

@Composable
fun AppNavGraph(
    navController: NavHostController,
    modifier: Modifier = Modifier,
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    userStatus: UserStatus = UserStatus.ONLINE,
    onThemeModeChange: (ThemeMode) -> Unit = {},
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
                onCallClick = { userId ->
                    navController.navigate(Route.Call.createRoute(userId))
                },
                onAddServerClick = {
                    // TODO: navigate to AddServer screen
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
                onCallClick = { userId ->
                    navController.navigate(Route.Call.createRoute(userId))
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

        composable(Route.Settings.route) {
            SettingsScreen(
                themeMode = themeMode,
                userStatus = userStatus,
                onBack = { navController.popBackStack() },
                onThemeModeChange = onThemeModeChange,
                onStatusChange = onStatusChange,
            )
        }

        composable(Route.Notifications.route) {
            StubScreen(
                title = "Уведомления",
                subtitle = "Список уведомлений",
                onBack = { navController.popBackStack() }
            )
        }

        composable(
            route = Route.Call.route,
            arguments = listOf(navArgument("userId") { type = NavType.StringType })
        ) { backStackEntry ->
            val userId = backStackEntry.arguments?.getString("userId") ?: ""
            StubScreen(
                title = "Звонок",
                subtitle = "userId: $userId",
                onBack = { navController.popBackStack() }
            )
        }

        composable(
            route = Route.IncomingCall.route,
            arguments = listOf(navArgument("userId") { type = NavType.StringType })
        ) { backStackEntry ->
            val userId = backStackEntry.arguments?.getString("userId") ?: ""
            StubScreen(
                title = "Входящий звонок",
                subtitle = "от userId: $userId",
                onBack = { navController.popBackStack() }
            )
        }
    }
}

@Composable
private fun StubScreen(
    title: String,
    subtitle: String,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(32.dp))
        AleAppButton(onClick = onBack, variant = AleAppButtonVariant.Secondary) { Text("Назад") }
    }
}
