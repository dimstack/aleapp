package com.callapp.android.ui.screens.notifications

import android.content.res.Configuration
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.CallMissed
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.callapp.android.domain.model.Notification
import com.callapp.android.domain.model.NotificationType
import com.callapp.android.domain.model.Server
import com.callapp.android.ui.components.AleAppButton
import com.callapp.android.ui.components.AleAppButtonSize
import com.callapp.android.ui.components.AleAppButtonVariant
import com.callapp.android.ui.components.AleAppCard
import com.callapp.android.ui.theme.AleAppTheme
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun NotificationsScreen(
    notifications: List<Notification> = emptyList(),
    server: Server? = null,
    onBack: () -> Unit = {},
    onClearAll: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val colors = AleAppTheme.colors

    Scaffold(
        containerColor = colors.background,
        topBar = {
            NotificationsTopBar(
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
            val sections = notificationSections(notifications)
            LazyColumn(
                modifier = modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                sections.forEach { section ->
                    item(key = "header_${section.label}") {
                        NotificationDateHeader(text = section.label)
                    }
                    items(section.items, key = { it.id }) { item ->
                        NotificationCard(
                            item = item,
                            server = server,
                            onClick = {},
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NotificationsTopBar(
    showClearAll: Boolean,
    onBack: () -> Unit,
    onClearAll: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = AleAppTheme.colors

    Column(modifier) {
        TopAppBar(
            title = {
                Text(
                    text = "Уведомления",
                    style = MaterialTheme.typography.headlineMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
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

@Composable
private fun NotificationDateHeader(
    text: String,
    modifier: Modifier = Modifier,
) {
    val colors = AleAppTheme.colors

    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        HorizontalDivider(
            modifier = Modifier.weight(1f),
            color = colors.border,
        )
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
            color = colors.mutedForeground,
        )
        HorizontalDivider(
            modifier = Modifier.weight(1f),
            color = colors.border,
        )
    }
}

@Composable
private fun NotificationCard(
    item: NotificationListItem,
    server: Server?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    when (item.notification.type) {
        NotificationType.MISSED_CALL -> MissedCallNotificationCard(
            item = item,
            server = server,
            onClick = onClick,
            modifier = modifier,
        )

        else -> DefaultNotificationCard(
            item = item,
            onClick = onClick,
            modifier = modifier,
        )
    }
}

@Composable
private fun MissedCallNotificationCard(
    item: NotificationListItem,
    server: Server?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = AleAppTheme.colors
    val notification = item.notification
    val actorName = notification.actorDisplayName.orEmpty().ifBlank { "Неизвестный контакт" }
    val actorUsername = notification.actorUsername
        ?.trim()
        ?.takeIf { it.isNotBlank() && it != notification.actorUserId }
    val serverHandle = server?.username?.removePrefix("@").orEmpty().ifBlank {
        notification.serverName.lowercase(Locale.forLanguageTag("ru"))
    }

    val metadataText = buildAnnotatedString {
        if (actorUsername != null) {
            append(actorUsername)
            append(" • ")
        }
        appendInlineContent("serverIcon", "[server]")
        append(" ")
        append(serverHandle)
    }
    val metadataInlineContent = mapOf(
        "serverIcon" to InlineTextContent(
            placeholder = Placeholder(
                width = 18.sp,
                height = 18.sp,
                placeholderVerticalAlign = PlaceholderVerticalAlign.Center,
            ),
        ) {
            NotificationServerImage(
                name = server?.name ?: notification.serverName,
                imageUrl = server?.imageUrl,
            )
        },
    )

    AleAppCard(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            NotificationAvatar(
                name = actorName,
                avatarUrl = notification.actorAvatarUrl,
                modifier = Modifier.size(56.dp),
            )

            Spacer(Modifier.width(16.dp))

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    text = actorName,
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                    color = colors.cardForeground,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = metadataText,
                    inlineContent = metadataInlineContent,
                    modifier = Modifier.fillMaxWidth(),
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.mutedForeground,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = if (item.count > 1) {
                        "Пропущено вызовов: ${item.count}"
                    } else {
                        "Пропущенный вызов"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.destructive,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            Spacer(Modifier.width(12.dp))

            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Surface(
                    shape = CircleShape,
                    color = colors.destructive.copy(alpha = 0.12f),
                    contentColor = colors.destructive,
                ) {
                    Box(
                        modifier = Modifier.size(42.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.CallMissed,
                            contentDescription = null,
                            modifier = Modifier.size(22.dp),
                        )
                    }
                }

                Text(
                    text = formatNotificationTimestamp(notification.createdAt),
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Medium),
                    color = colors.mutedForeground,
                    maxLines = 1,
                )
            }
        }
    }
}

@Composable
private fun DefaultNotificationCard(
    item: NotificationListItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = AleAppTheme.colors
    val notification = item.notification

    AleAppCard(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.Top,
        ) {
            NotificationIcon(type = notification.type)

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = notificationTitle(notification.type),
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
                    color = colors.cardForeground,
                    modifier = Modifier.fillMaxWidth(),
                )

                Spacer(Modifier.height(6.dp))

                Text(
                    text = notification.message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.mutedForeground,
                )

                Spacer(Modifier.height(12.dp))

                NotificationMetaRow(
                    serverName = notification.serverName,
                    createdAt = notification.createdAt,
                )
            }
        }
    }
}

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

        NotificationType.REQUEST_DECLINED -> Triple(
            colors.destructive.copy(alpha = 0.1f),
            colors.destructive,
            Icons.Default.Cancel,
        )

        NotificationType.REQUEST_SENT -> Triple(
            colors.accent.copy(alpha = 0.1f),
            colors.accent,
            Icons.Default.Schedule,
        )

        NotificationType.INCOMING_CALL -> Triple(
            colors.statusOnline.copy(alpha = 0.1f),
            colors.statusOnline,
            Icons.Default.Notifications,
        )

        NotificationType.MISSED_CALL -> Triple(
            colors.destructive.copy(alpha = 0.1f),
            colors.destructive,
            Icons.AutoMirrored.Filled.CallMissed,
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

@Composable
private fun NotificationAvatar(
    name: String,
    avatarUrl: String?,
    modifier: Modifier = Modifier,
) {
    val colors = AleAppTheme.colors
    val initials = initialsOf(name)

    Surface(
        modifier = modifier,
        shape = CircleShape,
        color = colors.secondary,
    ) {
        if (!avatarUrl.isNullOrBlank()) {
            AsyncImage(
                model = avatarUrl,
                contentDescription = name,
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CircleShape),
                contentScale = ContentScale.Crop,
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(colors.secondary),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = initials,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = colors.cardForeground,
                )
            }
        }
    }
}

@Composable
private fun NotificationMetaRow(
    serverName: String,
    createdAt: String,
    modifier: Modifier = Modifier,
) {
    val colors = AleAppTheme.colors

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = serverName,
            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
            color = colors.mutedForeground,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(Modifier.width(12.dp))
        Text(
            text = formatNotificationTimestamp(createdAt),
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Medium),
            color = colors.mutedForeground,
            maxLines = 1,
        )
    }
}

@Composable
private fun NotificationServerImage(
    name: String,
    imageUrl: String?,
    modifier: Modifier = Modifier,
) {
    if (!imageUrl.isNullOrBlank()) {
        AsyncImage(
            model = imageUrl,
            contentDescription = name,
            modifier = modifier
                .size(18.dp)
                .clip(RoundedCornerShape(5.dp)),
            contentScale = ContentScale.Crop,
        )
    } else {
        Surface(
            modifier = modifier.size(18.dp),
            shape = RoundedCornerShape(5.dp),
            color = AleAppTheme.colors.secondary,
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = initialsOf(name).take(1),
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                    color = AleAppTheme.colors.cardForeground,
                )
            }
        }
    }
}

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
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
                    color = colors.mutedForeground,
                )

                Spacer(Modifier.height(4.dp))

                Text(
                    text = "Здесь будут появляться пропущенные звонки и изменения по вашим заявкам",
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.mutedForeground,
                )
            }
        }
    }
}

private fun notificationTitle(type: NotificationType): String = when (type) {
    NotificationType.REQUEST_APPROVED -> "Заявка одобрена"
    NotificationType.REQUEST_DECLINED -> "Заявка отклонена"
    NotificationType.REQUEST_SENT -> "Заявка отправлена"
    NotificationType.INCOMING_CALL -> "Входящий вызов"
    NotificationType.MISSED_CALL -> "Пропущенный вызов"
}

private fun formatNotificationTimestamp(value: String): String {
    if (value.isBlank()) return ""
    return runCatching {
        val localDateTime = Instant.parse(value).atZone(ZoneId.systemDefault())
        localDateTime.format(DateTimeFormatter.ofPattern("HH:mm", Locale.forLanguageTag("ru")))
    }.getOrElse { value }
}

private fun notificationSections(notifications: List<Notification>): List<NotificationSection> {
    return notifications
        .groupBy { notificationDateKey(it.createdAt) }
        .toList()
        .sortedByDescending { (date, _) -> date }
        .map { (date, items) ->
            NotificationSection(
                label = formatNotificationDateHeader(date),
                items = aggregateNotifications(items),
            )
        }
}

private fun aggregateNotifications(notifications: List<Notification>): List<NotificationListItem> {
    val sorted = notifications.sortedByDescending { notificationSortInstant(it.createdAt) }
    val result = mutableListOf<NotificationListItem>()

    sorted.forEach { notification ->
        if (notification.type != NotificationType.MISSED_CALL) {
            result += NotificationListItem(notification = notification)
            return@forEach
        }

        val index = result.indexOfFirst { current ->
            current.notification.type == NotificationType.MISSED_CALL &&
                current.notification.actorUserId == notification.actorUserId &&
                current.notification.actorUsername == notification.actorUsername &&
                current.notification.actorDisplayName == notification.actorDisplayName &&
                current.notification.serverName == notification.serverName
        }

        if (index >= 0) {
            val current = result[index]
            result[index] = current.copy(count = current.count + 1)
        } else {
            result += NotificationListItem(notification = notification)
        }
    }

    return result
}

private fun notificationDateKey(value: String): LocalDate =
    runCatching { Instant.parse(value).atZone(ZoneId.systemDefault()).toLocalDate() }
        .getOrElse { LocalDate.MIN }

private fun notificationSortInstant(value: String): Instant =
    runCatching { Instant.parse(value) }.getOrElse { Instant.EPOCH }

private fun formatNotificationDateHeader(date: LocalDate): String {
    if (date == LocalDate.MIN) return "Без даты"
    val today = LocalDate.now(ZoneId.systemDefault())
    val yesterday = today.minusDays(1)
    return when (date) {
        today -> "Сегодня"
        yesterday -> "Вчера"
        else -> date.format(DateTimeFormatter.ofPattern("d MMMM", Locale.forLanguageTag("ru")))
    }
}

private fun initialsOf(name: String): String {
    val parts = name.trim()
        .split(Regex("\\s+"))
        .filter { it.isNotBlank() }
        .take(2)
    if (parts.isEmpty()) return "?"
    return parts.joinToString("") { it.take(1).uppercase(Locale.forLanguageTag("ru")) }
}

private data class NotificationSection(
    val label: String,
    val items: List<NotificationListItem>,
)

private data class NotificationListItem(
    val notification: Notification,
    val count: Int = 1,
) {
    val id: String = if (count > 1) {
        buildString {
            append("agg:")
            append(notification.actorUserId ?: notification.actorDisplayName ?: notification.id)
            append(":")
            append(notification.serverName)
            append(":")
            append(notificationDateKey(notification.createdAt))
        }
    } else {
        notification.id
    }
}

private val previewNotifications = listOf(
    Notification(
        id = "n1",
        type = NotificationType.MISSED_CALL,
        serverName = "Горилла",
        message = "Missed call from Макак",
        actorUserId = "user-1",
        actorUsername = "@makak",
        actorDisplayName = "Макак",
        isRead = false,
        createdAt = "2026-03-18T19:51:00Z",
    ),
    Notification(
        id = "n1b",
        type = NotificationType.MISSED_CALL,
        serverName = "Горилла",
        message = "Missed call from Макак",
        actorUserId = "user-1",
        actorUsername = "@makak",
        actorDisplayName = "Макак",
        isRead = false,
        createdAt = "2026-03-18T19:45:00Z",
    ),
    Notification(
        id = "n2",
        type = NotificationType.REQUEST_APPROVED,
        serverName = "Tech Community",
        message = "Ваша заявка одобрена",
        isRead = false,
        createdAt = "2026-03-14T10:15:00Z",
    ),
)

@Preview(name = "Notifications - Light", showBackground = true, showSystemUi = true)
@Composable
private fun NotificationsLightPreview() {
    AleAppTheme(darkTheme = false) {
        NotificationsScreen(
            notifications = previewNotifications,
            server = Server(
                id = "srv-1",
                name = "Горилла",
                username = "@gorilla",
            ),
        )
    }
}

@Preview(
    name = "Notifications - Dark",
    showBackground = true,
    showSystemUi = true,
    uiMode = Configuration.UI_MODE_NIGHT_YES,
)
@Composable
private fun NotificationsDarkPreview() {
    AleAppTheme(darkTheme = true) {
        NotificationsScreen(
            notifications = previewNotifications,
            server = Server(
                id = "srv-1",
                name = "Горилла",
                username = "@gorilla",
            ),
        )
    }
}

@Preview(name = "Notifications - Empty", showBackground = true, showSystemUi = true)
@Composable
private fun NotificationsEmptyPreview() {
    AleAppTheme(darkTheme = false) {
        NotificationsScreen(notifications = emptyList())
    }
}
