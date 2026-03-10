package com.example.android.ui.screens.notifications

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.android.ui.components.AleAppButton
import com.example.android.ui.components.AleAppButtonSize
import com.example.android.ui.components.AleAppButtonVariant
import com.example.android.ui.components.AleAppCard
import com.example.android.ui.theme.AleAppTheme

/* ═══════════════════════════════════════════════════════════════════════════ */
/*  Data                                                                      */
/* ═══════════════════════════════════════════════════════════════════════════ */

enum class NotificationType { REQUEST_APPROVED, REQUEST_REJECTED, REQUEST_PENDING }

data class NotificationItem(
    val id: String,
    val type: NotificationType,
    val serverName: String,
    val message: String,
    val dateText: String,
    val read: Boolean,
)

/* ═══════════════════════════════════════════════════════════════════════════ */
/*  Mock data                                                                 */
/* ═══════════════════════════════════════════════════════════════════════════ */

internal val sampleNotifications = listOf(
    NotificationItem(
        id = "n1",
        type = NotificationType.REQUEST_REJECTED,
        serverName = "Creative Studio",
        message = "Ваша заявка на вступление была отклонена администратором",
        dateText = "15 марта, 14:30",
        read = false,
    ),
    NotificationItem(
        id = "n2",
        type = NotificationType.REQUEST_APPROVED,
        serverName = "Tech Community",
        message = "Ваша заявка на вступление была одобрена",
        dateText = "14 марта, 10:15",
        read = false,
    ),
    NotificationItem(
        id = "n3",
        type = NotificationType.REQUEST_PENDING,
        serverName = "Game Dev Hub",
        message = "Заявка отправлена, ожидайте решения администратора",
        dateText = "13 марта, 18:45",
        read = true,
    ),
    NotificationItem(
        id = "n4",
        type = NotificationType.REQUEST_APPROVED,
        serverName = "Design Lab",
        message = "Ваша заявка на вступление была одобрена",
        dateText = "12 марта, 09:00",
        read = true,
    ),
)

/* ═══════════════════════════════════════════════════════════════════════════ */
/*  NotificationsScreen                                                       */
/* ═══════════════════════════════════════════════════════════════════════════ */

@Composable
fun NotificationsScreen(
    notifications: List<NotificationItem> = sampleNotifications,
    onBack: () -> Unit = {},
    onMarkAsRead: (String) -> Unit = {},
    onClearAll: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val colors = AleAppTheme.colors

    Scaffold(
        containerColor = colors.background,
        topBar = {
            NotificationsTopBar(
                unreadCount = notifications.count { !it.read },
                showClearAll = notifications.isNotEmpty(),
                onBack = onBack,
                onClearAll = onClearAll,
            )
        },
    ) { padding ->
        if (notifications.isEmpty()) {
            EmptyState(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
            )
        } else {
            LazyColumn(
                modifier = modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(notifications, key = { it.id }) { notification ->
                    NotificationCard(
                        notification = notification,
                        onClick = {
                            if (!notification.read) onMarkAsRead(notification.id)
                        },
                    )
                }
            }
        }
    }
}

/* ═══════════════════════════════════════════════════════════════════════════ */
/*  Top bar                                                                   */
/* ═══════════════════════════════════════════════════════════════════════════ */

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NotificationsTopBar(
    unreadCount: Int,
    showClearAll: Boolean,
    onBack: () -> Unit,
    onClearAll: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = AleAppTheme.colors

    Column(modifier) {
        TopAppBar(
            title = {
                Column {
                    Text(
                        text = "Уведомления",
                        style = MaterialTheme.typography.headlineMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (unreadCount > 0) {
                        Text(
                            text = "$unreadCount непрочитанных",
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.mutedForeground,
                        )
                    }
                }
            },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Назад",
                    )
                }
            },
            actions = {
                if (showClearAll) {
                    AleAppButton(
                        onClick = onClearAll,
                        variant = AleAppButtonVariant.Outline,
                        size = AleAppButtonSize.Small,
                        modifier = Modifier.padding(end = 8.dp),
                    ) {
                        Text("Очистить все")
                    }
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = colors.card,
                titleContentColor = colors.foreground,
                navigationIconContentColor = colors.foreground,
            ),
        )
        HorizontalDivider(color = colors.border)
    }
}

/* ═══════════════════════════════════════════════════════════════════════════ */
/*  Notification card                                                         */
/* ═══════════════════════════════════════════════════════════════════════════ */

@Composable
private fun NotificationCard(
    notification: NotificationItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = AleAppTheme.colors

    AleAppCard(
        modifier = modifier
            .fillMaxWidth()
            .alpha(if (notification.read) 0.6f else 1f),
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
        ) {
            // Icon
            NotificationIcon(type = notification.type)

            Spacer(Modifier.width(16.dp))

            // Content
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top,
                ) {
                    Text(
                        text = when (notification.type) {
                            NotificationType.REQUEST_APPROVED -> "Заявка одобрена"
                            NotificationType.REQUEST_REJECTED -> "Заявка отклонена"
                            NotificationType.REQUEST_PENDING -> "Заявка отправлена"
                        },
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontWeight = FontWeight.Medium,
                        ),
                        color = colors.cardForeground,
                        modifier = Modifier.weight(1f),
                    )

                    // Unread indicator
                    if (!notification.read) {
                        Spacer(Modifier.width(8.dp))
                        Surface(
                            modifier = Modifier.size(8.dp),
                            shape = CircleShape,
                            color = colors.primary,
                        ) {}
                    }
                }

                Spacer(Modifier.height(4.dp))

                Text(
                    text = notification.message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.mutedForeground,
                )

                Spacer(Modifier.height(8.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text(
                        text = notification.serverName,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Medium,
                        ),
                        color = colors.mutedForeground,
                    )
                    Text(
                        text = notification.dateText,
                        style = MaterialTheme.typography.labelSmall,
                        color = colors.mutedForeground,
                    )
                }
            }
        }
    }
}

/* ═══════════════════════════════════════════════════════════════════════════ */
/*  Notification icon                                                         */
/* ═══════════════════════════════════════════════════════════════════════════ */

@Composable
private fun NotificationIcon(
    type: NotificationType,
    modifier: Modifier = Modifier,
) {
    val colors = AleAppTheme.colors

    val (bgColor, iconTint, icon) = when (type) {
        NotificationType.REQUEST_APPROVED -> Triple(
            colors.statusOnline.copy(alpha = 0.1f),
            colors.statusOnline,
            Icons.Default.CheckCircle,
        )
        NotificationType.REQUEST_REJECTED -> Triple(
            colors.destructive.copy(alpha = 0.1f),
            colors.destructive,
            Icons.Default.Cancel,
        )
        NotificationType.REQUEST_PENDING -> Triple(
            colors.accent.copy(alpha = 0.1f),
            colors.accent,
            Icons.Default.Schedule,
        )
    }

    Surface(
        modifier = modifier.size(48.dp),
        shape = CircleShape,
        color = bgColor,
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                tint = iconTint,
            )
        }
    }
}

/* ═══════════════════════════════════════════════════════════════════════════ */
/*  Empty state                                                               */
/* ═══════════════════════════════════════════════════════════════════════════ */

@Composable
private fun EmptyState(modifier: Modifier = Modifier) {
    val colors = AleAppTheme.colors

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center,
    ) {
        AleAppCard(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(48.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Surface(
                    modifier = Modifier.size(64.dp),
                    shape = CircleShape,
                    color = colors.secondary,
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Notifications,
                            contentDescription = null,
                            modifier = Modifier.size(32.dp),
                            tint = colors.mutedForeground,
                        )
                    }
                }

                Spacer(Modifier.height(16.dp))

                Text(
                    text = "Уведомлений нет",
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontWeight = FontWeight.Medium,
                    ),
                    color = colors.mutedForeground,
                )

                Spacer(Modifier.height(4.dp))

                Text(
                    text = "Здесь будут отображаться уведомления о статусе ваших заявок",
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.mutedForeground,
                )
            }
        }
    }
}

/* ═══════════════════════════════════════════════════════════════════════════ */
/*  Previews                                                                  */
/* ═══════════════════════════════════════════════════════════════════════════ */

// ── Full screen ─────────────────────────────────────────────────────────────

@Preview(name = "Notifications — Light", showBackground = true, showSystemUi = true)
@Composable
private fun NotificationsLightPreview() {
    AleAppTheme(darkTheme = false) {
        NotificationsScreen()
    }
}

@Preview(
    name = "Notifications — Dark",
    showBackground = true,
    showSystemUi = true,
    uiMode = Configuration.UI_MODE_NIGHT_YES,
)
@Composable
private fun NotificationsDarkPreview() {
    AleAppTheme(darkTheme = true) {
        NotificationsScreen()
    }
}

@Preview(name = "Notifications — Empty", showBackground = true, showSystemUi = true)
@Composable
private fun NotificationsEmptyPreview() {
    AleAppTheme(darkTheme = false) {
        NotificationsScreen(notifications = emptyList())
    }
}

@Preview(
    name = "Notifications — Empty Dark",
    showBackground = true,
    showSystemUi = true,
    uiMode = Configuration.UI_MODE_NIGHT_YES,
)
@Composable
private fun NotificationsEmptyDarkPreview() {
    AleAppTheme(darkTheme = true) {
        NotificationsScreen(notifications = emptyList())
    }
}

// ── Component previews ──────────────────────────────────────────────────────

@Preview(name = "NotificationCard — unread", showBackground = true)
@Composable
private fun NotificationCardUnreadPreview() {
    AleAppTheme(darkTheme = false) {
        Surface(color = AleAppTheme.colors.background) {
            NotificationCard(
                notification = sampleNotifications[0],
                onClick = {},
                modifier = Modifier.padding(16.dp),
            )
        }
    }
}

@Preview(name = "NotificationCard — read", showBackground = true)
@Composable
private fun NotificationCardReadPreview() {
    AleAppTheme(darkTheme = false) {
        Surface(color = AleAppTheme.colors.background) {
            NotificationCard(
                notification = sampleNotifications[2],
                onClick = {},
                modifier = Modifier.padding(16.dp),
            )
        }
    }
}

@Preview(name = "NotificationIcon — all types", showBackground = true)
@Composable
private fun NotificationIconPreview() {
    AleAppTheme(darkTheme = false) {
        Surface(color = AleAppTheme.colors.card) {
            Row(
                modifier = Modifier.padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                NotificationIcon(type = NotificationType.REQUEST_APPROVED)
                NotificationIcon(type = NotificationType.REQUEST_REJECTED)
                NotificationIcon(type = NotificationType.REQUEST_PENDING)
            }
        }
    }
}

@Preview(name = "EmptyState — Light", showBackground = true)
@Composable
private fun EmptyStateLightPreview() {
    AleAppTheme(darkTheme = false) {
        EmptyState(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
        )
    }
}

@Preview(name = "TopBar — with unread", showBackground = true)
@Composable
private fun TopBarUnreadPreview() {
    AleAppTheme(darkTheme = false) {
        NotificationsTopBar(
            unreadCount = 3,
            showClearAll = true,
            onBack = {},
            onClearAll = {},
        )
    }
}

@Preview(name = "TopBar — no unread", showBackground = true)
@Composable
private fun TopBarNoUnreadPreview() {
    AleAppTheme(darkTheme = false) {
        NotificationsTopBar(
            unreadCount = 0,
            showClearAll = true,
            onBack = {},
            onClearAll = {},
        )
    }
}
