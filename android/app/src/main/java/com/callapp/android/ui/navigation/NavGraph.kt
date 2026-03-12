package com.callapp.android.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.callapp.android.ui.IncomingCallHandler
import com.callapp.android.ui.components.RequestCallPermissions
import com.callapp.android.ui.components.videoCallPermissions
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.callapp.android.ui.screens.home.HomeScreen
import com.callapp.android.ui.screens.home.HomeViewModel
import com.callapp.android.ui.screens.server.ServerDetailScreen
import com.callapp.android.ui.screens.server.ServerDetailViewModel
import com.callapp.android.ui.screens.joinrequests.JoinRequestsScreen
import com.callapp.android.ui.screens.joinrequests.JoinRequestItem
import com.callapp.android.ui.screens.joinrequests.JoinRequestsViewModel
import com.callapp.android.ui.screens.servermanage.InviteTokensScreen
import com.callapp.android.ui.screens.servermanage.InviteTokensUiState
import com.callapp.android.ui.screens.servermanage.InviteTokensViewModel
import com.callapp.android.ui.screens.servermanage.ServerManagementScreen
import com.callapp.android.ui.screens.servermanage.ServerManagementViewModel
import com.callapp.android.ui.screens.profile.MyProfileScreen
import com.callapp.android.ui.screens.profile.MyProfileViewModel
import com.callapp.android.ui.screens.profile.UserProfileScreen
import com.callapp.android.ui.screens.profile.UserProfileViewModel
import com.callapp.android.ui.screens.call.CallPhase
import com.callapp.android.ui.screens.call.CallScreen
import com.callapp.android.ui.screens.call.CallStatus
import com.callapp.android.ui.screens.call.CallViewModel
import com.callapp.android.ui.screens.call.IncomingCallScreen
import com.callapp.android.ui.screens.connect.AddServerScreen
import com.callapp.android.ui.screens.connect.AuthChoiceScreen
import com.callapp.android.ui.screens.connect.ConnectUiState
import com.callapp.android.ui.screens.connect.ConnectViewModel
import com.callapp.android.ui.screens.connect.CreateProfileScreen
import com.callapp.android.ui.screens.connect.LoginScreen
import com.callapp.android.ui.screens.connect.PendingRequestScreen
import com.callapp.android.ui.screens.connect.RequestStatus
import com.callapp.android.ui.screens.notifications.NotificationsScreen
import com.callapp.android.ui.screens.notifications.NotificationsViewModel
import com.callapp.android.ui.screens.settings.SettingsScreen
import com.callapp.android.ui.screens.settings.UserStatus

@Composable
fun AppNavGraph(
    navController: NavHostController,
    modifier: Modifier = Modifier,
    isDarkTheme: Boolean = false,
    userStatus: UserStatus = UserStatus.ONLINE,
    onThemeChange: (Boolean) -> Unit = {},
    onStatusChange: (UserStatus) -> Unit = {},
) {
    // Shared ConnectViewModel scoped to the nav graph for the onboarding flow
    val connectViewModel: ConnectViewModel = viewModel()

    // Listen for incoming calls globally and navigate to IncomingCallScreen
    IncomingCallHandler { userId, contactName, serverName ->
        val encodedName = java.net.URLEncoder.encode(contactName, "UTF-8")
        val encodedServer = java.net.URLEncoder.encode(serverName, "UTF-8")
        navController.navigate(
            Route.IncomingCall.createRoute(userId, encodedName, encodedServer)
        )
    }

    NavHost(
        navController = navController,
        startDestination = Route.Home.route,
        modifier = modifier
    ) {
        composable(Route.Home.route) {
            val viewModel: HomeViewModel = viewModel()
            val favoritesState by viewModel.favoritesState.collectAsState()
            val serversState by viewModel.serversState.collectAsState()
            val notificationCount by viewModel.notificationCount.collectAsState()

            HomeScreen(
                favoritesState = favoritesState,
                serversState = serversState,
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
                    connectViewModel.resetState()
                    navController.navigate(Route.AddServer.route)
                },
                onContactClick = { serverId, userId ->
                    navController.navigate(Route.UserProfile.createRoute(serverId, userId))
                },
                onRetry = viewModel::loadData,
            )
        }

        composable(
            route = Route.ServerDetail.route,
            arguments = listOf(navArgument("serverId") { type = NavType.StringType })
        ) {
            val viewModel: ServerDetailViewModel = viewModel()
            val server by viewModel.server.collectAsState()
            val membersState by viewModel.membersState.collectAsState()
            val isAdmin by viewModel.isAdmin.collectAsState()
            val pendingRequests by viewModel.pendingRequests.collectAsState()

            ServerDetailScreen(
                server = server,
                membersState = membersState,
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
                onRetry = viewModel::loadMembers,
            )
        }

        composable(
            route = Route.JoinRequests.route,
            arguments = listOf(navArgument("serverId") { type = NavType.StringType })
        ) {
            val viewModel: JoinRequestsViewModel = viewModel()
            val state by viewModel.state.collectAsState()

            JoinRequestsScreen(
                serverName = state.serverName,
                requests = state.requests.map { req ->
                    JoinRequestItem(
                        id = req.id,
                        userName = req.userName,
                        username = req.username,
                        dateText = req.createdAt,
                    )
                },
                onBack = { navController.popBackStack() },
                onApprove = { requestId -> viewModel.approve(requestId) },
                onDecline = { requestId -> viewModel.decline(requestId) },
            )
        }

        composable(
            route = Route.ServerManagement.route,
            arguments = listOf(navArgument("serverId") { type = NavType.StringType })
        ) {
            val viewModel: ServerManagementViewModel = viewModel()
            val state by viewModel.state.collectAsState()
            val manageData = state.data

            LaunchedEffect(state.saveSuccess) {
                if (state.saveSuccess) navController.popBackStack()
            }
            LaunchedEffect(state.deleteSuccess) {
                if (state.deleteSuccess) {
                    navController.navigate(Route.Home.route) {
                        popUpTo(Route.Home.route) { inclusive = true }
                    }
                }
            }

            if (manageData != null) {
                ServerManagementScreen(
                    initial = manageData,
                    onBack = { navController.popBackStack() },
                    onSave = { name, username, description, imageUrl ->
                        viewModel.save(name, username, description, imageUrl)
                    },
                    onDeleteServer = { viewModel.deleteServer() },
                    onInviteTokens = {
                        navController.navigate(Route.InviteTokens.createRoute(manageData.id))
                    },
                )
            }
        }

        composable(
            route = Route.InviteTokens.route,
            arguments = listOf(navArgument("serverId") { type = NavType.StringType })
        ) {
            val viewModel: InviteTokensViewModel = viewModel()
            val state by viewModel.state.collectAsState()
            val isCreating by viewModel.isCreating.collectAsState()

            val tokens = when (val s = state) {
                is InviteTokensUiState.Success -> s.tokens
                else -> emptyList()
            }
            val actionError = (state as? InviteTokensUiState.Success)?.actionError

            InviteTokensScreen(
                tokens = tokens,
                serverAddress = "",
                onBack = { navController.popBackStack() },
                onCreateToken = { label, maxUses, role, approval ->
                    viewModel.createToken(label, maxUses, role, approval)
                },
                onRevokeToken = { tokenId -> viewModel.revokeToken(tokenId) },
                onCopyToken = { _ -> /* clipboard handled in Screen */ },
            )
        }

        composable(
            route = Route.MyProfile.route,
            arguments = listOf(navArgument("serverId") { type = NavType.StringType })
        ) {
            val viewModel: MyProfileViewModel = viewModel()
            val state by viewModel.state.collectAsState()
            val profile = state.profile

            if (profile != null) {
                MyProfileScreen(
                    profile = profile,
                    onBack = { navController.popBackStack() },
                    onSaveProfile = { name, username ->
                        viewModel.saveProfile(name, username)
                    },
                )
            }
        }

        composable(
            route = Route.UserProfile.route,
            arguments = listOf(
                navArgument("serverId") { type = NavType.StringType },
                navArgument("userId") { type = NavType.StringType },
            )
        ) {
            val viewModel: UserProfileViewModel = viewModel()
            val state by viewModel.state.collectAsState()
            val user = state.user

            if (user != null) {
                UserProfileScreen(
                    user = user,
                    onBack = { navController.popBackStack() },
                    onToggleFavorite = { _ -> viewModel.toggleFavorite() },
                )
            }
        }

        // ── Onboarding flow (driven by ConnectViewModel) ─────────────────────

        composable(Route.AddServer.route) {
            val connectState by connectViewModel.state.collectAsState()

            // React to ConnectViewModel state changes
            LaunchedEffect(connectState) {
                when (val state = connectState) {
                    is ConnectUiState.AuthChoice -> {
                        navController.navigate(
                            Route.AuthChoice.createRoute(state.serverName)
                        )
                    }
                    is ConnectUiState.Joined -> {
                        navController.navigate(Route.Home.route) {
                            popUpTo(Route.Home.route) { inclusive = true }
                        }
                    }
                    is ConnectUiState.Pending -> {
                        navController.navigate(
                            Route.PendingRequest.createRoute(state.serverName, state.userName)
                        )
                    }
                    else -> {} // Idle, Loading, Error handled in AddServerScreen
                }
            }

            val isLoading = connectState is ConnectUiState.Loading
            val errorMessage = (connectState as? ConnectUiState.Error)?.message

            AddServerScreen(
                onBack = {
                    connectViewModel.resetState()
                    navController.popBackStack()
                },
                onConnect = { token ->
                    connectViewModel.connectWithToken(token)
                },
                isLoading = isLoading,
                errorMessage = errorMessage,
            )
        }

        composable(
            route = Route.AuthChoice.route,
            arguments = listOf(navArgument("serverName") { type = NavType.StringType })
        ) { backStackEntry ->
            val encodedName = backStackEntry.arguments?.getString("serverName") ?: ""
            val serverName = java.net.URLDecoder.decode(encodedName, "UTF-8")

            AuthChoiceScreen(
                serverName = serverName,
                onCreateAccount = {
                    navController.navigate(Route.CreateProfile.createRoute(serverName))
                },
                onLogin = {
                    navController.navigate(Route.Login.createRoute(serverName))
                },
            )
        }

        composable(
            route = Route.CreateProfile.route,
            arguments = listOf(navArgument("serverName") { type = NavType.StringType })
        ) { backStackEntry ->
            val encodedName = backStackEntry.arguments?.getString("serverName") ?: ""
            val serverName = java.net.URLDecoder.decode(encodedName, "UTF-8")
            val connectState by connectViewModel.state.collectAsState()

            // React to state changes after profile creation
            LaunchedEffect(connectState) {
                when (connectState) {
                    is ConnectUiState.Joined -> {
                        navController.navigate(Route.Home.route) {
                            popUpTo(Route.Home.route) { inclusive = true }
                        }
                    }
                    is ConnectUiState.Pending -> {
                        val state = connectState as ConnectUiState.Pending
                        navController.navigate(
                            Route.PendingRequest.createRoute(state.serverName, state.userName)
                        )
                    }
                    else -> {}
                }
            }

            CreateProfileScreen(
                serverName = serverName,
                onCreateProfile = { username, name, password, _ ->
                    connectViewModel.createProfile(username, name, password)
                },
            )
        }

        composable(
            route = Route.Login.route,
            arguments = listOf(navArgument("serverName") { type = NavType.StringType })
        ) { backStackEntry ->
            val encodedName = backStackEntry.arguments?.getString("serverName") ?: ""
            val serverName = java.net.URLDecoder.decode(encodedName, "UTF-8")
            val connectState by connectViewModel.state.collectAsState()

            // React to state changes after login
            LaunchedEffect(connectState) {
                when (connectState) {
                    is ConnectUiState.Joined -> {
                        navController.navigate(Route.Home.route) {
                            popUpTo(Route.Home.route) { inclusive = true }
                        }
                    }
                    else -> {}
                }
            }

            val errorMessage = (connectState as? ConnectUiState.LoginError)?.message

            LoginScreen(
                serverName = serverName,
                onLogin = { username, password ->
                    connectViewModel.login(username, password)
                },
                externalError = errorMessage,
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
            val viewModel: NotificationsViewModel = viewModel()
            val state by viewModel.state.collectAsState()

            NotificationsScreen(
                notifications = state.notifications,
                onBack = { navController.popBackStack() },
                onMarkAsRead = { notificationId -> viewModel.markAsRead(notificationId) },
                onClearAll = { viewModel.clearAll() },
            )
        }

        composable(
            route = Route.Call.route,
            arguments = listOf(
                navArgument("userId") { type = NavType.StringType },
                navArgument("contactName") { type = NavType.StringType },
            )
        ) {
            var permissionsGranted by remember { mutableStateOf(false) }

            RequestCallPermissions(
                permissions = videoCallPermissions,
                onAllGranted = { permissionsGranted = true },
                onDenied = { navController.popBackStack() },
            )

            if (permissionsGranted) {
                val viewModel: CallViewModel = viewModel()
                val callPhase by viewModel.callPhase.collectAsState()
                val elapsedSeconds by viewModel.elapsedSeconds.collectAsState()
                val isMicOn by viewModel.isMicOn.collectAsState()
                val isCameraOn by viewModel.isCameraOn.collectAsState()
                val localVideoTrack by viewModel.localVideoTrack.collectAsState()
                val remoteVideoTrack by viewModel.remoteVideoTrack.collectAsState()

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
                        localVideoTrack = localVideoTrack,
                        remoteVideoTrack = remoteVideoTrack,
                        eglBase = viewModel.eglBase,
                        onMicToggle = viewModel::toggleMic,
                        onCameraToggle = viewModel::toggleCamera,
                        onSwitchCamera = viewModel::switchCamera,
                        onEndCall = viewModel::endCall,
                    )
                }
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
            var permissionsGranted by remember { mutableStateOf(false) }

            RequestCallPermissions(
                permissions = videoCallPermissions,
                onAllGranted = { permissionsGranted = true },
                onDenied = { navController.popBackStack() },
            )

            if (permissionsGranted) {
                val viewModel: CallViewModel = viewModel()
                val callPhase by viewModel.callPhase.collectAsState()
                val elapsedSeconds by viewModel.elapsedSeconds.collectAsState()
                val isMicOn by viewModel.isMicOn.collectAsState()
                val isCameraOn by viewModel.isCameraOn.collectAsState()
                val localVideoTrack by viewModel.localVideoTrack.collectAsState()
                val remoteVideoTrack by viewModel.remoteVideoTrack.collectAsState()

                when (callPhase) {
                    CallPhase.ENDED -> {
                        LaunchedEffect(Unit) { navController.popBackStack() }
                    }
                    CallPhase.CONNECTED -> {
                        CallScreen(
                            contactName = viewModel.contactName,
                            callStatus = CallStatus.CONNECTED,
                            elapsedSeconds = elapsedSeconds,
                            isMicOn = isMicOn,
                            isCameraOn = isCameraOn,
                            localVideoTrack = localVideoTrack,
                            remoteVideoTrack = remoteVideoTrack,
                            eglBase = viewModel.eglBase,
                            onMicToggle = viewModel::toggleMic,
                            onCameraToggle = viewModel::toggleCamera,
                            onSwitchCamera = viewModel::switchCamera,
                            onEndCall = viewModel::endCall,
                        )
                    }
                    else -> {
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
}
