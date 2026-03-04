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
            HomeStub(
                onOpenServer = { navController.navigate(Route.ServerDetail.createRoute("demo-server")) },
                onOpenSettings = { navController.navigate(Route.Settings.route) },
                onStartCall = { navController.navigate(Route.Call.createRoute("demo-user")) },
                onIncomingCall = { navController.navigate(Route.IncomingCall.createRoute("demo-caller")) }
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
private fun HomeStub(
    onOpenServer: () -> Unit,
    onOpenSettings: () -> Unit,
    onStartCall: () -> Unit,
    onIncomingCall: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "CallApp",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Главный экран",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(32.dp))

        AleAppButton(onClick = onOpenServer) { Text("Открыть сервер") }
        Spacer(modifier = Modifier.height(12.dp))
        AleAppButton(onClick = onOpenSettings, variant = AleAppButtonVariant.Secondary) { Text("Настройки") }
        Spacer(modifier = Modifier.height(12.dp))
        AleAppButton(onClick = onStartCall, variant = AleAppButtonVariant.Outline) { Text("Начать звонок") }
        Spacer(modifier = Modifier.height(12.dp))
        AleAppButton(onClick = onIncomingCall, variant = AleAppButtonVariant.Outline) { Text("Входящий звонок") }
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
