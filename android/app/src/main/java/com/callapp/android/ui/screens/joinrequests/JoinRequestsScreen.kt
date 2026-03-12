package com.callapp.android.ui.screens.joinrequests

import android.content.res.Configuration
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.callapp.android.ui.components.AleAppButton
import com.callapp.android.ui.components.AleAppButtonSize
import com.callapp.android.ui.components.AleAppButtonVariant
import com.callapp.android.ui.components.AleAppCard
import com.callapp.android.ui.theme.AleAppTheme
import kotlin.math.absoluteValue

/* ═══════════════════════════════════════════════════════════════════════════ */
/*  Data                                                                      */
/* ═══════════════════════════════════════════════════════════════════════════ */

data class JoinRequestItem(
    val id: String,
    val userName: String,
    val username: String,
    val dateText: String,
)

/* ═══════════════════════════════════════════════════════════════════════════ */
/*  Mock data                                                                 */
/* ═══════════════════════════════════════════════════════════════════════════ */

internal val sampleRequests = listOf(
    JoinRequestItem("r1", "Иван Петров", "@ivan_petrov", "2 марта в 10:30"),
    JoinRequestItem("r2", "Мария Сидорова", "@maria_sidorova", "3 марта в 09:15"),
)

/* ═══════════════════════════════════════════════════════════════════════════ */
/*  Color helpers                                                             */
/* ═══════════════════════════════════════════════════════════════════════════ */

private val avatarPalette = listOf(
    Color(0xFF8B7355), Color(0xFF5B7B8F), Color(0xFF7B6B8D),
    Color(0xFF6B8E6B), Color(0xFF8B6F5E), Color(0xFF6B7B8B),
)

private fun avatarColor(name: String): Color =
    avatarPalette[name.hashCode().absoluteValue % avatarPalette.size]

/* ═══════════════════════════════════════════════════════════════════════════ */
/*  JoinRequestsScreen                                                        */
/* ═══════════════════════════════════════════════════════════════════════════ */

@Composable
fun JoinRequestsScreen(
    serverName: String = "Tech Community",
    requests: List<JoinRequestItem> = sampleRequests,
    onBack: () -> Unit = {},
    onApprove: (String) -> Unit = {},
    onDecline: (String) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val colors = AleAppTheme.colors

    Scaffold(
        containerColor = colors.background,
        topBar = {
            JoinRequestsTopBar(
                serverName = serverName,
                onBack = onBack,
            )
        },
    ) { padding ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState()),
        ) {
            Spacer(Modifier.height(8.dp))

            // ── Section header: "Новые заявки [count]" ────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = "Новые заявки",
                    style = MaterialTheme.typography.titleLarge,
                    color = colors.foreground,
                )
                CountBadge(count = requests.size)
            }

            // ── Requests list or empty state ──────────────────────────────
            if (requests.isNotEmpty()) {
                AleAppCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                ) {
                    requests.forEachIndexed { index, request ->
                        JoinRequestRow(
                            request = request,
                            onApprove = { onApprove(request.id) },
                            onDecline = { onDecline(request.id) },
                        )
                        if (index < requests.lastIndex) {
                            HorizontalDivider(
                                color = colors.border,
                                modifier = Modifier.padding(horizontal = 16.dp),
                            )
                        }
                    }
                }
            } else {
                EmptyRequestsState(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                )
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}

/* ═══════════════════════════════════════════════════════════════════════════ */
/*  Top bar                                                                   */
/* ═══════════════════════════════════════════════════════════════════════════ */

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun JoinRequestsTopBar(
    serverName: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = AleAppTheme.colors

    Column(modifier) {
        TopAppBar(
            title = {
                Column {
                    Text(
                        text = "Заявки на вступление",
                        style = MaterialTheme.typography.headlineMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = serverName,
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.mutedForeground,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
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
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = colors.card,
                titleContentColor = colors.foreground,
                navigationIconContentColor = colors.foreground,
                actionIconContentColor = colors.mutedForeground,
            ),
        )
        HorizontalDivider(color = colors.border)
    }
}

/* ═══════════════════════════════════════════════════════════════════════════ */
/*  Count badge                                                               */
/* ═══════════════════════════════════════════════════════════════════════════ */

@Composable
private fun CountBadge(
    count: Int,
    modifier: Modifier = Modifier,
) {
    val colors = AleAppTheme.colors

    Surface(
        modifier = modifier,
        shape = CircleShape,
        color = colors.accent.copy(alpha = 0.2f),
        contentColor = colors.accentForeground,
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
/*  Request row                                                               */
/* ═══════════════════════════════════════════════════════════════════════════ */

@Composable
private fun JoinRequestRow(
    request: JoinRequestItem,
    onApprove: () -> Unit,
    onDecline: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = AleAppTheme.colors

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Avatar
        RequestAvatar(
            name = request.userName,
            size = 56.dp,
        )

        Spacer(Modifier.width(12.dp))

        // Info: name, @username, date
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = request.userName,
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontWeight = FontWeight.Medium,
                ),
                color = colors.cardForeground,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = request.username,
                style = MaterialTheme.typography.bodySmall,
                color = colors.mutedForeground,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = request.dateText,
                style = MaterialTheme.typography.labelSmall,
                color = colors.mutedForeground,
            )
        }

        Spacer(Modifier.width(8.dp))

        // Action buttons
        Column(
            verticalArrangement = Arrangement.spacedBy(6.dp),
            horizontalAlignment = Alignment.End,
        ) {
            // Accept — green (status-online color)
            AleAppButton(
                onClick = onApprove,
                size = AleAppButtonSize.Small,
                variant = AleAppButtonVariant.Primary,
            ) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp),
                )
                Spacer(Modifier.width(4.dp))
                Text("Принять", style = MaterialTheme.typography.labelMedium)
            }

            // Decline — outlined destructive
            AleAppButton(
                onClick = onDecline,
                size = AleAppButtonSize.Small,
                variant = AleAppButtonVariant.Outline,
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp),
                    tint = colors.destructive,
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    text = "Отклонить",
                    style = MaterialTheme.typography.labelMedium,
                    color = colors.destructive,
                )
            }
        }
    }
}

/* ═══════════════════════════════════════════════════════════════════════════ */
/*  Avatar                                                                    */
/* ═══════════════════════════════════════════════════════════════════════════ */

@Composable
private fun RequestAvatar(
    name: String,
    modifier: Modifier = Modifier,
    size: Dp = 56.dp,
) {
    val bgColor = avatarColor(name)
    val initials = name.split(" ")
        .take(2)
        .mapNotNull { it.firstOrNull()?.uppercaseChar() }
        .joinToString("")

    Surface(
        modifier = modifier.size(size),
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
}

/* ═══════════════════════════════════════════════════════════════════════════ */
/*  Empty state                                                               */
/* ═══════════════════════════════════════════════════════════════════════════ */

@Composable
private fun EmptyRequestsState(
    modifier: Modifier = Modifier,
) {
    val colors = AleAppTheme.colors

    AleAppCard(modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 48.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            // Checkmark circle
            Surface(
                shape = CircleShape,
                color = colors.secondary,
                modifier = Modifier.size(64.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = colors.mutedForeground,
                        modifier = Modifier.size(32.dp),
                    )
                }
            }

            Text(
                text = "Новых заявок нет",
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.Medium,
                ),
                color = colors.mutedForeground,
            )

            Text(
                text = "Все заявки обработаны",
                style = MaterialTheme.typography.bodySmall,
                color = colors.mutedForeground,
            )
        }
    }
}

/* ═══════════════════════════════════════════════════════════════════════════ */
/*  Previews                                                                  */
/* ═══════════════════════════════════════════════════════════════════════════ */

// ── Full screen ─────────────────────────────────────────────────────────────

@Preview(name = "JoinRequests — Light", showBackground = true, showSystemUi = true)
@Composable
private fun JoinRequestsScreenLightPreview() {
    AleAppTheme(darkTheme = false) {
        JoinRequestsScreen(
            serverName = "Tech Community",
            requests = sampleRequests,
        )
    }
}

@Preview(
    name = "JoinRequests — Dark",
    showBackground = true,
    showSystemUi = true,
    uiMode = Configuration.UI_MODE_NIGHT_YES,
)
@Composable
private fun JoinRequestsScreenDarkPreview() {
    AleAppTheme(darkTheme = true) {
        JoinRequestsScreen(
            serverName = "Tech Community",
            requests = sampleRequests,
        )
    }
}

@Preview(name = "JoinRequests — Empty Light", showBackground = true, showSystemUi = true)
@Composable
private fun JoinRequestsScreenEmptyLightPreview() {
    AleAppTheme(darkTheme = false) {
        JoinRequestsScreen(
            serverName = "Game Dev Hub",
            requests = emptyList(),
        )
    }
}

@Preview(
    name = "JoinRequests — Empty Dark",
    showBackground = true,
    showSystemUi = true,
    uiMode = Configuration.UI_MODE_NIGHT_YES,
)
@Composable
private fun JoinRequestsScreenEmptyDarkPreview() {
    AleAppTheme(darkTheme = true) {
        JoinRequestsScreen(
            serverName = "Game Dev Hub",
            requests = emptyList(),
        )
    }
}

@Preview(name = "JoinRequests — Many requests", showBackground = true, showSystemUi = true)
@Composable
private fun JoinRequestsScreenManyPreview() {
    AleAppTheme(darkTheme = false) {
        JoinRequestsScreen(
            serverName = "Tech Community",
            requests = listOf(
                JoinRequestItem("r1", "Иван Петров", "@ivan_petrov", "2 марта в 10:30"),
                JoinRequestItem("r2", "Мария Сидорова", "@maria_sidorova", "3 марта в 09:15"),
                JoinRequestItem("r3", "Олег Козлов", "@oleg_kozlov", "3 марта в 14:22"),
                JoinRequestItem("r4", "Елена Новикова", "@elena_n", "4 марта в 08:00"),
            ),
        )
    }
}

// ── Component previews ──────────────────────────────────────────────────────

@Preview(name = "TopBar — Light", showBackground = true)
@Composable
private fun TopBarLightPreview() {
    AleAppTheme(darkTheme = false) {
        JoinRequestsTopBar(
            serverName = "Tech Community",
            onBack = {},
        )
    }
}

@Preview(name = "TopBar — Dark", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun TopBarDarkPreview() {
    AleAppTheme(darkTheme = true) {
        JoinRequestsTopBar(
            serverName = "Tech Community",
            onBack = {},
        )
    }
}

@Preview(name = "RequestRow — Light", showBackground = true)
@Composable
private fun RequestRowLightPreview() {
    AleAppTheme(darkTheme = false) {
        Surface(color = AleAppTheme.colors.card) {
            JoinRequestRow(
                request = JoinRequestItem("r1", "Иван Петров", "@ivan_petrov", "2 марта в 10:30"),
                onApprove = {},
                onDecline = {},
            )
        }
    }
}

@Preview(name = "RequestRow — Dark", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun RequestRowDarkPreview() {
    AleAppTheme(darkTheme = true) {
        Surface(color = AleAppTheme.colors.card) {
            JoinRequestRow(
                request = JoinRequestItem("r2", "Мария Сидорова", "@maria_sidorova", "3 марта в 09:15"),
                onApprove = {},
                onDecline = {},
            )
        }
    }
}

@Preview(name = "EmptyState — Light", showBackground = true)
@Composable
private fun EmptyStateLightPreview() {
    AleAppTheme(darkTheme = false) {
        Surface(color = AleAppTheme.colors.background) {
            EmptyRequestsState(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
            )
        }
    }
}

@Preview(name = "EmptyState — Dark", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun EmptyStateDarkPreview() {
    AleAppTheme(darkTheme = true) {
        Surface(color = AleAppTheme.colors.background) {
            EmptyRequestsState(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
            )
        }
    }
}

@Preview(name = "RequestAvatar — variants", showBackground = true)
@Composable
private fun RequestAvatarPreview() {
    AleAppTheme(darkTheme = false) {
        Surface(color = AleAppTheme.colors.card) {
            Row(
                modifier = Modifier.padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                RequestAvatar(name = "Иван Петров")
                RequestAvatar(name = "Мария Сидорова")
                RequestAvatar(name = "Олег Козлов")
            }
        }
    }
}

@Preview(name = "CountBadge — variants", showBackground = true)
@Composable
private fun CountBadgePreview() {
    AleAppTheme(darkTheme = false) {
        Surface(color = AleAppTheme.colors.background) {
            Row(
                modifier = Modifier.padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                CountBadge(count = 2)
                CountBadge(count = 0)
                CountBadge(count = 15)
            }
        }
    }
}
