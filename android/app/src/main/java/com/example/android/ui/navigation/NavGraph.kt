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

@Composable
fun AppNavGraph(
    navController: NavHostController,
    modifier: Modifier = Modifier
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
            )
        }

        composable(
            route = Route.ServerDetail.route,
            arguments = listOf(navArgument("serverId") { type = NavType.StringType })
        ) { backStackEntry ->
            val serverId = backStackEntry.arguments?.getString("serverId") ?: ""
            StubScreen(
                title = "Сервер",
                subtitle = "serverId: $serverId",
                onBack = { navController.popBackStack() }
            )
        }

        composable(Route.Settings.route) {
            StubScreen(
                title = "Настройки",
                subtitle = "Тема, статус, о приложении",
                onBack = { navController.popBackStack() }
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
