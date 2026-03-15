package com.callapp.android.ui.screens.home

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.callapp.android.domain.model.Server
import com.callapp.android.domain.model.ServerAvailabilityStatus
import com.callapp.android.domain.model.User
import com.callapp.android.domain.model.UserStatus
import com.callapp.android.ui.common.UiState
import com.callapp.android.ui.components.AleAppCard
import com.callapp.android.ui.preview.PreviewData
import com.callapp.android.ui.theme.AleAppTheme
import kotlin.math.absoluteValue

/* ═══════════════════════════════════════════════════════════════════════════ */
/*  Color helpers                                                             */
/* ═══════════════════════════════════════════════════════════════════════════ */

private val avatarPalette = listOf(
    Color(0xFF8B7355), Color(0xFF5B7B8F), Color(0xFF7B6B8D),
    Color(0xFF6B8E6B), Color(0xFF8B6F5E), Color(0xFF6B7B8B),
)

private val serverPalette = listOf(
    Color(0xFF3A5068), Color(0xFF5E4B6B), Color(0xFF4B6858), Color(0xFF6B5B4B),
)

private fun avatarColor(name: String): Color =
    avatarPalette[name.hashCode().absoluteValue % avatarPalette.size]

private fun serverImageColor(name: String): Color =
    serverPalette[name.hashCode().absoluteValue % serverPalette.size]

/* ═══════════════════════════════════════════════════════════════════════════ */
/*  HomeScreen                                                                */
/* ═══════════════════════════════════════════════════════════════════════════ */

@Composable
fun HomeScreen(
    favoritesState: UiState<List<User>> = UiState.Success(PreviewData.favorites),
    serversState: UiState<List<Server>> = UiState.Success(PreviewData.servers),
    notificationCount: Int = 1,
    onServerClick: (Server) -> Unit = {},
    onSettingsClick: () -> Unit = {},
    onNotificationsClick: () -> Unit = {},
    onCallClick: (serverId: String, userId: String, contactName: String) -> Unit = { _, _, _ -> },
    onAddServerClick: () -> Unit = {},
    onContactClick: (serverId: String, userId: String) -> Unit = { _, _ -> },
    onRetry: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val colors = AleAppTheme.colors

    Scaffold(
        containerColor = colors.background,
        topBar = {
            HomeTopBar(
                notificationCount = notificationCount,
                onNotificationsClick = onNotificationsClick,
                onSettingsClick = onSettingsClick,
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAddServerClick,
                shape = CircleShape,
                containerColor = colors.primary,
                contentColor = colors.primaryForeground,
            ) {
                Icon(Icons.Default.Add, contentDescription = "Добавить сервер")
            }
        },
    ) { padding ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState()),
        ) {
            Spacer(Modifier.height(8.dp))

            // ── Избранные ────────────────────────────────────────────────
            DataSection(
                state = favoritesState,
                onRetry = onRetry,
            ) { favorites ->
                SectionHeader(title = "Избранные", count = favorites.size)
                AleAppCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                ) {
                    favorites.forEachIndexed { index, user ->
                        FavoriteContactRow(
                            user = user,
                            onCallClick = { onCallClick(user.serverId, user.id, user.name) },
                            onContactClick = { onContactClick(user.serverId, user.id) },
                        )
                        if (index < favorites.lastIndex) {
                            HorizontalDivider(
                                color = colors.border,
                                modifier = Modifier.padding(horizontal = 16.dp),
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(20.dp))

            // ── Серверы ──────────────────────────────────────────────────
            DataSection(
                state = serversState,
                onRetry = onRetry,
            ) { servers ->
                SectionHeader(title = "Серверы", count = servers.size)
                AleAppCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                ) {
                    servers.forEachIndexed { index, server ->
                        ServerRow(
                            server = server,
                            onClick = { onServerClick(server) },
                        )
                        if (index < servers.lastIndex) {
                            HorizontalDivider(
                                color = colors.border,
                                modifier = Modifier.padding(horizontal = 16.dp),
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(88.dp))
        }
    }
}

/* ═══════════════════════════════════════════════════════════════════════════ */
/*  DataSection — Loading / Error / Success                                  */
/* ═══════════════════════════════════════════════════════════════════════════ */

@Composable
private fun <T> DataSection(
    state: UiState<T>,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable (T) -> Unit,
) {
    val colors = AleAppTheme.colors

    when (state) {
        is UiState.Loading -> {
            Box(
                modifier = modifier
                    .fillMaxWidth()
                    .padding(vertical = 32.dp),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator(color = colors.primary)
            }
        }
        is UiState.Error -> {
            Column(
                modifier = modifier
                    .fillMaxWidth()
                    .padding(vertical = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = state.message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.mutedForeground,
                )
                TextButton(onClick = onRetry) {
                    Icon(
                        Icons.Default.Refresh,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(Modifier.width(4.dp))
                    Text("Повторить")
                }
            }
        }
        is UiState.Success -> content(state.data)
    }
}

/* ═══════════════════════════════════════════════════════════════════════════ */
/*  HomeTopBar                                                                */
/* ═══════════════════════════════════════════════════════════════════════════ */

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HomeTopBar(
    notificationCount: Int,
    onNotificationsClick: () -> Unit,
    onSettingsClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = AleAppTheme.colors

    Column(modifier) {
        TopAppBar(
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = CircleShape,
                        color = colors.primary,
                        modifier = Modifier.size(40.dp),
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.fillMaxSize(),
                        ) {
                            Icon(
                                imageVector = Icons.Default.Call,
                                contentDescription = null,
                                tint = colors.primaryForeground,
                                modifier = Modifier.size(24.dp),
                            )
                        }
                    }
                    Spacer(Modifier.width(12.dp))
                    Text(
                        text = "CallApp",
                        style = MaterialTheme.typography.headlineMedium,
                    )
                }
            },
            actions = {
                IconButton(onClick = onNotificationsClick) {
                    BadgedBox(
                        badge = {
                            if (notificationCount > 0) {
                                Badge(
                                    containerColor = colors.destructive,
                                    contentColor = colors.destructiveForeground,
                                ) {
                                    Text(
                                        text = if (notificationCount > 9) "9+"
                                        else notificationCount.toString(),
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = FontWeight.Bold,
                                        ),
                                    )
                                }
                            }
                        },
                    ) {
                        Icon(
                            Icons.Default.Notifications,
                            contentDescription = "Уведомления",
                        )
                    }
                }
                IconButton(onClick = onSettingsClick) {
                    Icon(Icons.Default.Settings, contentDescription = "Настройки")
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = colors.card,
                titleContentColor = colors.foreground,
                actionIconContentColor = colors.mutedForeground,
            ),
        )
        HorizontalDivider(color = colors.border)
    }
}

/* ═══════════════════════════════════════════════════════════════════════════ */
/*  SectionHeader + CountBadge                                                */
/* ═══════════════════════════════════════════════════════════════════════════ */

@Composable
private fun SectionHeader(
    title: String,
    count: Int,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            color = AleAppTheme.colors.foreground,
        )
        CountBadge(count = count)
    }
}

@Composable
private fun CountBadge(
    count: Int,
    modifier: Modifier = Modifier,
) {
    val colors = AleAppTheme.colors

    Surface(
        modifier = modifier,
        shape = CircleShape,
        color = colors.secondary,
        contentColor = colors.secondaryForeground,
    ) {
        Text(
            text = count.toString(),
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.Medium,
            ),
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
        )
    }
}

/* ═══════════════════════════════════════════════════════════════════════════ */
/*  FavoriteContactRow                                                        */
/* ═══════════════════════════════════════════════════════════════════════════ */

@Composable
private fun FavoriteContactRow(
    user: User,
    onCallClick: () -> Unit,
    onContactClick: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val colors = AleAppTheme.colors

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onContactClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ContactAvatar(
            name = user.name,
            status = user.status,
            size = 56.dp,
        )

        Spacer(Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = user.name,
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontWeight = FontWeight.Medium,
                ),
                color = colors.cardForeground,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = user.username,
                style = MaterialTheme.typography.bodySmall,
                color = colors.mutedForeground,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }

        IconButton(onClick = onCallClick) {
            Icon(
                imageVector = Icons.Default.Phone,
                contentDescription = "Позвонить ${user.name}",
                tint = colors.primary,
            )
        }
    }
}

/* ═══════════════════════════════════════════════════════════════════════════ */
/*  ContactAvatar + StatusDot                                                 */
/* ═══════════════════════════════════════════════════════════════════════════ */

@Composable
private fun ContactAvatar(
    name: String,
    status: UserStatus,
    modifier: Modifier = Modifier,
    size: Dp = 56.dp,
) {
    val bgColor = avatarColor(name)
    val initials = name.split(" ")
        .take(2)
        .mapNotNull { it.firstOrNull()?.uppercaseChar() }
        .joinToString("")

    Box(modifier = modifier.size(size)) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            shape = CircleShape,
            color = bgColor,
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = initials,
                    color = Color.White,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        fontSize = (size.value * 0.3f).sp,
                    ),
                )
            }
        }

        StatusDot(
            status = status,
            modifier = Modifier.align(Alignment.BottomStart),
        )
    }
}

@Composable
private fun StatusDot(
    status: UserStatus,
    modifier: Modifier = Modifier,
) {
    val colors = AleAppTheme.colors
    val dotColor = when (status) {
        UserStatus.ONLINE -> colors.statusOnline
        UserStatus.DO_NOT_DISTURB -> colors.statusBusy
        UserStatus.INVISIBLE -> colors.statusOffline
    }

    Box(
        modifier = modifier
            .size(16.dp)
            .clip(CircleShape)
            .background(colors.card)
            .padding(2.dp)
            .clip(CircleShape)
            .background(dotColor),
    )
}

/* ═══════════════════════════════════════════════════════════════════════════ */
/*  ServerRow + ServerImage                                                   */
/* ═══════════════════════════════════════════════════════════════════════════ */

@Composable
private fun ServerRow(
    server: Server,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = AleAppTheme.colors

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ServerImage(name = server.name, size = 56.dp)

        Spacer(Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = server.name,
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontWeight = FontWeight.SemiBold,
                ),
                color = colors.cardForeground,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = serverSubtitle(server),
                style = MaterialTheme.typography.bodySmall,
                color = if (server.availabilityStatus == ServerAvailabilityStatus.UNAVAILABLE) {
                    colors.destructive
                } else {
                    colors.mutedForeground
                },
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }

        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = colors.mutedForeground,
        )
    }
}

private fun serverSubtitle(server: Server): String = when (server.availabilityStatus) {
    ServerAvailabilityStatus.UNAVAILABLE -> server.availabilityMessage ?: "Сервер недоступен"
    ServerAvailabilityStatus.CHECKING -> server.availabilityMessage ?: "Проверка подключения..."
    else -> server.username
}

@Composable
private fun ServerImage(
    name: String,
    modifier: Modifier = Modifier,
    size: Dp = 56.dp,
) {
    val bgColor = serverImageColor(name)
    val initials = name.split(" ")
        .take(2)
        .mapNotNull { it.firstOrNull()?.uppercaseChar() }
        .joinToString("")

    Surface(
        modifier = modifier.size(size),
        shape = RoundedCornerShape(12.dp),
        color = bgColor,
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = initials,
                color = Color.White,
                style = MaterialTheme.typography.titleSmall.copy(
                    fontWeight = FontWeight.SemiBold,
                ),
            )
        }
    }
}

/* ═══════════════════════════════════════════════════════════════════════════ */
/*  Previews                                                                  */
/* ═══════════════════════════════════════════════════════════════════════════ */

@Preview(name = "HomeScreen — Light", showBackground = true, showSystemUi = true)
@Composable
private fun HomeScreenLightPreview() {
    AleAppTheme(darkTheme = false) {
        HomeScreen()
    }
}

@Preview(
    name = "HomeScreen — Dark",
    showBackground = true,
    showSystemUi = true,
    uiMode = Configuration.UI_MODE_NIGHT_YES,
)
@Composable
private fun HomeScreenDarkPreview() {
    AleAppTheme(darkTheme = true) {
        HomeScreen()
    }
}

@Preview(name = "HomeScreen — Loading", showBackground = true, showSystemUi = true)
@Composable
private fun HomeScreenLoadingPreview() {
    AleAppTheme(darkTheme = false) {
        HomeScreen(
            favoritesState = UiState.Loading,
            serversState = UiState.Loading,
        )
    }
}

@Preview(name = "HomeScreen — Error", showBackground = true, showSystemUi = true)
@Composable
private fun HomeScreenErrorPreview() {
    AleAppTheme(darkTheme = false) {
        HomeScreen(
            favoritesState = UiState.Error("Не удалось загрузить данные"),
            serversState = UiState.Error("Не удалось загрузить данные"),
        )
    }
}

@Preview(name = "HomeTopBar — Light", showBackground = true)
@Composable
private fun HomeTopBarLightPreview() {
    AleAppTheme(darkTheme = false) {
        HomeTopBar(
            notificationCount = 3,
            onNotificationsClick = {},
            onSettingsClick = {},
        )
    }
}

@Preview(name = "HomeTopBar — Dark", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun HomeTopBarDarkPreview() {
    AleAppTheme(darkTheme = true) {
        HomeTopBar(
            notificationCount = 1,
            onNotificationsClick = {},
            onSettingsClick = {},
        )
    }
}

@Preview(name = "HomeTopBar — без уведомлений", showBackground = true)
@Composable
private fun HomeTopBarNoBadgePreview() {
    AleAppTheme(darkTheme = false) {
        HomeTopBar(
            notificationCount = 0,
            onNotificationsClick = {},
            onSettingsClick = {},
        )
    }
}

@Preview(name = "SectionHeader — Light", showBackground = true)
@Composable
private fun SectionHeaderPreview() {
    AleAppTheme(darkTheme = false) {
        Surface(color = AleAppTheme.colors.background) {
            SectionHeader(title = "Избранные", count = 3)
        }
    }
}

@Preview(name = "SectionHeader — Dark", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun SectionHeaderDarkPreview() {
    AleAppTheme(darkTheme = true) {
        Surface(color = AleAppTheme.colors.background) {
            SectionHeader(title = "Серверы", count = 4)
        }
    }
}

@Preview(name = "FavoriteContactRow — Online", showBackground = true)
@Composable
private fun FavoriteOnlinePreview() {
    AleAppTheme(darkTheme = false) {
        Surface(color = AleAppTheme.colors.card) {
            FavoriteContactRow(
                user = PreviewData.userAnna,
                onCallClick = {},
            )
        }
    }
}

@Preview(name = "FavoriteContactRow — Offline", showBackground = true)
@Composable
private fun FavoriteOfflinePreview() {
    AleAppTheme(darkTheme = false) {
        Surface(color = AleAppTheme.colors.card) {
            FavoriteContactRow(
                user = PreviewData.userMaria,
                onCallClick = {},
            )
        }
    }
}

@Preview(name = "FavoriteContactRow — Dark", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun FavoriteDarkPreview() {
    AleAppTheme(darkTheme = true) {
        Surface(color = AleAppTheme.colors.card) {
            FavoriteContactRow(
                user = PreviewData.userDmitry,
                onCallClick = {},
            )
        }
    }
}

@Preview(name = "ContactAvatar — Online", showBackground = true)
@Composable
private fun AvatarOnlinePreview() {
    AleAppTheme(darkTheme = false) {
        Surface(
            color = AleAppTheme.colors.card,
            modifier = Modifier.padding(16.dp),
        ) {
            ContactAvatar(name = "Анна Смирнова", status = UserStatus.ONLINE)
        }
    }
}

@Preview(name = "ContactAvatar — все статусы", showBackground = true)
@Composable
private fun AvatarStatusesPreview() {
    AleAppTheme(darkTheme = false) {
        Surface(color = AleAppTheme.colors.card) {
            Row(
                modifier = Modifier.padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                ContactAvatar(name = "Анна С", status = UserStatus.ONLINE)
                ContactAvatar(name = "Дмитрий П", status = UserStatus.DO_NOT_DISTURB)
                ContactAvatar(name = "Елена И", status = UserStatus.INVISIBLE)
            }
        }
    }
}

@Preview(name = "ServerRow — Light", showBackground = true)
@Composable
private fun ServerRowPreview() {
    AleAppTheme(darkTheme = false) {
        Surface(color = AleAppTheme.colors.card) {
            ServerRow(
                server = PreviewData.serverTech,
                onClick = {},
            )
        }
    }
}

@Preview(name = "ServerRow — Dark", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun ServerRowDarkPreview() {
    AleAppTheme(darkTheme = true) {
        Surface(color = AleAppTheme.colors.card) {
            ServerRow(
                server = PreviewData.serverCreative,
                onClick = {},
            )
        }
    }
}

@Preview(name = "ServerImage — варианты", showBackground = true)
@Composable
private fun ServerImagesPreview() {
    AleAppTheme(darkTheme = false) {
        Surface(color = AleAppTheme.colors.background) {
            Row(
                modifier = Modifier.padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                ServerImage(name = "Tech Community")
                ServerImage(name = "Creative Studio")
                ServerImage(name = "Music Production")
                ServerImage(name = "Game Dev Hub")
            }
        }
    }
}

@Preview(name = "CountBadge", showBackground = true)
@Composable
private fun CountBadgePreview() {
    AleAppTheme(darkTheme = false) {
        Surface(color = AleAppTheme.colors.background) {
            Row(
                modifier = Modifier.padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                CountBadge(count = 3)
                CountBadge(count = 12)
            }
        }
    }
}

@Preview(name = "Favorites card — полная", showBackground = true)
@Composable
private fun FavoritesCardPreview() {
    AleAppTheme(darkTheme = false) {
        Surface(color = AleAppTheme.colors.background) {
            Column(modifier = Modifier.padding(16.dp)) {
                SectionHeader(title = "Избранные", count = PreviewData.favorites.size)
                AleAppCard(modifier = Modifier.fillMaxWidth()) {
                    PreviewData.favorites.forEachIndexed { index, user ->
                        FavoriteContactRow(user = user, onCallClick = {})
                        if (index < PreviewData.favorites.lastIndex) {
                            HorizontalDivider(
                                color = AleAppTheme.colors.border,
                                modifier = Modifier.padding(horizontal = 16.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Preview(name = "Servers card — полная", showBackground = true)
@Composable
private fun ServersCardPreview() {
    AleAppTheme(darkTheme = false) {
        Surface(color = AleAppTheme.colors.background) {
            Column(modifier = Modifier.padding(16.dp)) {
                SectionHeader(title = "Серверы", count = PreviewData.servers.size)
                AleAppCard(modifier = Modifier.fillMaxWidth()) {
                    PreviewData.servers.forEachIndexed { index, server ->
                        ServerRow(server = server, onClick = {})
                        if (index < PreviewData.servers.lastIndex) {
                            HorizontalDivider(
                                color = AleAppTheme.colors.border,
                                modifier = Modifier.padding(horizontal = 16.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}
