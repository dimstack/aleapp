package com.example.android.ui.screens.home

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
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import com.example.android.ui.components.AleAppCard
import com.example.android.ui.theme.AleAppTheme
import kotlin.math.absoluteValue

/* ═══════════════════════════════════════════════════════════════════════════ */
/*  Data                                                                      */
/* ═══════════════════════════════════════════════════════════════════════════ */

enum class ContactStatus { ONLINE, OFFLINE, BUSY }

data class FavoriteContact(
    val id: String,
    val name: String,
    val username: String,
    val status: ContactStatus,
    val serverId: String = "",
)

data class ServerUiItem(
    val id: String,
    val name: String,
    val username: String,
)

// ── Hardcoded sample data ────────────────────────────────────────────────────

internal val sampleFavorites = listOf(
    FavoriteContact("u1", "Анна Смирнова", "@tech_community", ContactStatus.ONLINE, "s1"),
    FavoriteContact("u2", "Дмитрий Петров", "@creative_studio", ContactStatus.ONLINE, "s2"),
    FavoriteContact("u3", "Елена Иванова", "@music_prod", ContactStatus.OFFLINE, "s3"),
)

internal val sampleServers = listOf(
    ServerUiItem("s1", "Tech Community", "@tech_community"),
    ServerUiItem("s2", "Creative Studio", "@creative_studio"),
    ServerUiItem("s3", "Music Production", "@music_prod"),
    ServerUiItem("s4", "Game Dev Hub", "@gamedev_hub"),
)

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
    favorites: List<FavoriteContact> = sampleFavorites,
    servers: List<ServerUiItem> = sampleServers,
    notificationCount: Int = 1,
    onServerClick: (String) -> Unit = {},
    onSettingsClick: () -> Unit = {},
    onNotificationsClick: () -> Unit = {},
    onCallClick: (String) -> Unit = {},
    onAddServerClick: () -> Unit = {},
    onContactClick: (serverId: String, userId: String) -> Unit = { _, _ -> },
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
            SectionHeader(title = "Избранные", count = favorites.size)

            AleAppCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
            ) {
                favorites.forEachIndexed { index, contact ->
                    FavoriteContactRow(
                        contact = contact,
                        onCallClick = { onCallClick(contact.id) },
                        onContactClick = { onContactClick(contact.serverId, contact.id) },
                    )
                    if (index < favorites.lastIndex) {
                        HorizontalDivider(
                            color = colors.border,
                            modifier = Modifier.padding(horizontal = 16.dp),
                        )
                    }
                }
            }

            Spacer(Modifier.height(20.dp))

            // ── Серверы ──────────────────────────────────────────────────
            SectionHeader(title = "Серверы", count = servers.size)

            AleAppCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
            ) {
                servers.forEachIndexed { index, server ->
                    ServerRow(
                        server = server,
                        onClick = { onServerClick(server.id) },
                    )
                    if (index < servers.lastIndex) {
                        HorizontalDivider(
                            color = colors.border,
                            modifier = Modifier.padding(horizontal = 16.dp),
                        )
                    }
                }
            }

            Spacer(Modifier.height(88.dp))
        }
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
    contact: FavoriteContact,
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
            name = contact.name,
            status = contact.status,
            size = 56.dp,
        )

        Spacer(Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = contact.name,
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontWeight = FontWeight.Medium,
                ),
                color = colors.cardForeground,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = contact.username,
                style = MaterialTheme.typography.bodySmall,
                color = colors.mutedForeground,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }

        IconButton(onClick = onCallClick) {
            Icon(
                imageVector = Icons.Default.Phone,
                contentDescription = "Позвонить ${contact.name}",
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
    status: ContactStatus,
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
    status: ContactStatus,
    modifier: Modifier = Modifier,
) {
    val colors = AleAppTheme.colors
    val dotColor = when (status) {
        ContactStatus.ONLINE -> colors.statusOnline
        ContactStatus.BUSY -> colors.statusBusy
        ContactStatus.OFFLINE -> colors.statusOffline
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
    server: ServerUiItem,
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
                text = server.username,
                style = MaterialTheme.typography.bodySmall,
                color = colors.mutedForeground,
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
                contact = FavoriteContact("1", "Анна Смирнова", "@tech_community", ContactStatus.ONLINE),
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
                contact = FavoriteContact("2", "Елена Иванова", "@music_prod", ContactStatus.OFFLINE),
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
                contact = FavoriteContact("1", "Дмитрий Петров", "@creative_studio", ContactStatus.BUSY),
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
            ContactAvatar(name = "Анна Смирнова", status = ContactStatus.ONLINE)
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
                ContactAvatar(name = "Анна С", status = ContactStatus.ONLINE)
                ContactAvatar(name = "Дмитрий П", status = ContactStatus.BUSY)
                ContactAvatar(name = "Елена И", status = ContactStatus.OFFLINE)
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
                server = ServerUiItem("1", "Tech Community", "@tech_community"),
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
                server = ServerUiItem("2", "Creative Studio", "@creative_studio"),
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
                SectionHeader(title = "Избранные", count = sampleFavorites.size)
                AleAppCard(modifier = Modifier.fillMaxWidth()) {
                    sampleFavorites.forEachIndexed { index, contact ->
                        FavoriteContactRow(contact = contact, onCallClick = {})
                        if (index < sampleFavorites.lastIndex) {
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
                SectionHeader(title = "Серверы", count = sampleServers.size)
                AleAppCard(modifier = Modifier.fillMaxWidth()) {
                    sampleServers.forEachIndexed { index, server ->
                        ServerRow(server = server, onClick = {})
                        if (index < sampleServers.lastIndex) {
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
