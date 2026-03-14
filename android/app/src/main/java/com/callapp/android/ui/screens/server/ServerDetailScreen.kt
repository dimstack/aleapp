package com.callapp.android.ui.screens.server

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
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.callapp.android.domain.model.JoinRequest
import com.callapp.android.domain.model.Server
import com.callapp.android.domain.model.User
import com.callapp.android.domain.model.UserStatus
import com.callapp.android.ui.components.AleAppButton
import com.callapp.android.ui.components.AleAppButtonSize
import com.callapp.android.ui.components.AleAppButtonVariant
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

private val serverImagePalette = listOf(
    Color(0xFF3A5068), Color(0xFF5E4B6B), Color(0xFF4B6858), Color(0xFF6B5B4B),
)

private fun avatarColor(name: String): Color =
    avatarPalette[name.hashCode().absoluteValue % avatarPalette.size]

private fun serverImageColor(name: String): Color =
    serverImagePalette[name.hashCode().absoluteValue % serverImagePalette.size]

/* ═══════════════════════════════════════════════════════════════════════════ */
/*  ServerDetailScreen                                                        */
/* ═══════════════════════════════════════════════════════════════════════════ */

@Composable
fun ServerDetailScreen(
    server: Server = PreviewData.serverTech,
    membersState: UiState<List<User>> = UiState.Success(PreviewData.techMembers),
    isAdmin: Boolean = true,
    pendingRequests: List<JoinRequest> = PreviewData.joinRequests,
    onBack: () -> Unit = {},
    onCallClick: (userId: String, contactName: String) -> Unit = { _, _ -> },
    onProfileClick: () -> Unit = {},
    onContactClick: (String) -> Unit = {},
    onManageServer: () -> Unit = {},
    onViewRequests: () -> Unit = {},
    onRemoveMember: (String) -> Unit = {},
    onApproveRequest: (String) -> Unit = {},
    onDeclineRequest: (String) -> Unit = {},
    onRetry: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val colors = AleAppTheme.colors
    var searchQuery by remember { mutableStateOf("") }
    var isEditMode by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = colors.background,
        topBar = {
            ServerDetailTopBar(
                serverName = server.name,
                isAdmin = isAdmin,
                pendingRequestsCount = pendingRequests.size,
                onBack = onBack,
                onProfileClick = onProfileClick,
                onManageServer = onManageServer,
                onViewRequests = onViewRequests,
            )
        },
    ) { padding ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState()),
        ) {
            // ── Server info section ───────────────────────────────────────
            ServerInfoSection(server = server, isAdmin = isAdmin)

            // ── Members ───────────────────────────────────────────────────
            when (membersState) {
                is UiState.Loading -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 48.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator(color = colors.primary)
                    }
                }
                is UiState.Error -> {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 48.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(
                            text = membersState.message,
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
                is UiState.Success -> {
                    val members = membersState.data
                    val filteredMembers = remember(members, searchQuery) {
                        if (searchQuery.isBlank()) members
                        else members.filter {
                            it.name.lowercase().contains(searchQuery.lowercase())
                        }
                    }

                    SearchBar(
                        query = searchQuery,
                        onQueryChange = { searchQuery = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                    )

                    MembersSection(
                        members = filteredMembers,
                        isAdmin = isAdmin,
                        isEditMode = isEditMode,
                        onToggleEditMode = { isEditMode = !isEditMode },
                        onCallClick = onCallClick,
                        onContactClick = onContactClick,
                        onRemoveMember = onRemoveMember,
                    )
                }
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}

/* ═══════════════════════════════════════════════════════════════════════════ */
/*  Top Bar                                                                   */
/* ═══════════════════════════════════════════════════════════════════════════ */

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ServerDetailTopBar(
    serverName: String,
    isAdmin: Boolean,
    pendingRequestsCount: Int,
    onBack: () -> Unit,
    onProfileClick: () -> Unit,
    onManageServer: () -> Unit,
    onViewRequests: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = AleAppTheme.colors

    Column(modifier) {
        TopAppBar(
            title = {
                Text(
                    text = serverName,
                    style = MaterialTheme.typography.headlineMedium,
                    maxLines = 2,
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
                if (isAdmin) {
                    // Join requests button with badge
                    IconButton(onClick = onViewRequests) {
                        BadgedBox(
                            badge = {
                                if (pendingRequestsCount > 0) {
                                    Badge(
                                        containerColor = colors.accent,
                                        contentColor = colors.accentForeground,
                                    ) {
                                        Text(
                                            text = if (pendingRequestsCount > 9) "9+"
                                            else pendingRequestsCount.toString(),
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontWeight = FontWeight.Bold,
                                            ),
                                        )
                                    }
                                }
                            },
                        ) {
                            Icon(
                                imageVector = Icons.Default.PersonAdd,
                                contentDescription = "Заявки на вступление",
                            )
                        }
                    }

                    // Server settings button
                    IconButton(onClick = onManageServer) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Управление сервером",
                        )
                    }
                }

                // Profile button
                IconButton(onClick = onProfileClick) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = "Мой профиль",
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
/*  Server info section                                                       */
/* ═══════════════════════════════════════════════════════════════════════════ */

@Composable
private fun ServerInfoSection(
    server: Server,
    isAdmin: Boolean,
    modifier: Modifier = Modifier,
) {
    val colors = AleAppTheme.colors

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(colors.card)
            .padding(horizontal = 16.dp, vertical = 20.dp),
    ) {
        Row {
            // Server image (96dp rounded-2xl)
            ServerImage(
                name = server.name,
                size = 96.dp,
            )

            Spacer(Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = server.name,
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontWeight = FontWeight.Bold,
                    ),
                    color = colors.foreground,
                )

                Spacer(Modifier.height(2.dp))

                Text(
                    text = server.username,
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.mutedForeground,
                )

                Spacer(Modifier.height(8.dp))

                Text(
                    text = server.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.mutedForeground,
                )

                if (isAdmin) {
                    Spacer(Modifier.height(12.dp))
                    AdminBadge()
                }
            }
        }
    }

    HorizontalDivider(color = colors.border)
}

@Composable
private fun ServerImage(
    name: String,
    modifier: Modifier = Modifier,
    size: Dp = 96.dp,
) {
    val bgColor = serverImageColor(name)
    val initials = name.split(" ")
        .take(2)
        .mapNotNull { it.firstOrNull()?.uppercaseChar() }
        .joinToString("")

    Surface(
        modifier = modifier.size(size),
        shape = RoundedCornerShape(16.dp),
        color = bgColor,
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = initials,
                color = Color.White,
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                ),
            )
        }
    }
}

@Composable
private fun AdminBadge(modifier: Modifier = Modifier) {
    val colors = AleAppTheme.colors

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(50),
        color = colors.primary.copy(alpha = 0.15f),
        contentColor = colors.primary,
    ) {
        Text(
            text = "Вы администратор",
            style = MaterialTheme.typography.labelMedium.copy(
                fontWeight = FontWeight.Medium,
            ),
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
        )
    }
}

/* ═══════════════════════════════════════════════════════════════════════════ */
/*  Search bar                                                                */
/* ═══════════════════════════════════════════════════════════════════════════ */

@Composable
private fun SearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = AleAppTheme.colors

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = colors.inputBackground,
        border = androidx.compose.foundation.BorderStroke(1.dp, colors.border),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = null,
                tint = colors.mutedForeground,
                modifier = Modifier.size(20.dp),
            )

            Spacer(Modifier.width(8.dp))

            Box(modifier = Modifier.weight(1f)) {
                if (query.isEmpty()) {
                    Text(
                        text = "Поиск участников...",
                        style = MaterialTheme.typography.bodyLarge,
                        color = colors.mutedForeground,
                    )
                }
                BasicTextField(
                    value = query,
                    onValueChange = onQueryChange,
                    textStyle = MaterialTheme.typography.bodyLarge.copy(
                        color = colors.foreground,
                    ),
                    singleLine = true,
                    cursorBrush = SolidColor(colors.primary),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

/* ═══════════════════════════════════════════════════════════════════════════ */
/*  Members section                                                           */
/* ═══════════════════════════════════════════════════════════════════════════ */

@Composable
private fun MembersSection(
    members: List<User>,
    isAdmin: Boolean,
    isEditMode: Boolean,
    onToggleEditMode: () -> Unit,
    onCallClick: (userId: String, contactName: String) -> Unit,
    onContactClick: (String) -> Unit,
    onRemoveMember: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = AleAppTheme.colors

    Column(modifier = modifier) {
        // Section header: "Участники [count]" + edit pencil
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Участники",
                style = MaterialTheme.typography.titleLarge,
                color = colors.foreground,
            )

            Spacer(Modifier.width(8.dp))

            CountBadge(count = members.size)

            Spacer(Modifier.weight(1f))

            if (isAdmin) {
                IconButton(onClick = onToggleEditMode) {
                    if (isEditMode) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Отменить редактирование",
                                tint = colors.destructive,
                                modifier = Modifier.size(20.dp),
                            )
                        }
                    } else {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Редактировать участников",
                            tint = colors.mutedForeground,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                }
            }
        }

        // Members card
        if (members.isNotEmpty()) {
            AleAppCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
            ) {
                members.forEachIndexed { index, member ->
                    MemberRow(
                        member = member,
                        isEditMode = isEditMode,
                        onCallClick = { onCallClick(member.id, member.name) },
                        onContactClick = { onContactClick(member.id) },
                        onRemoveClick = { onRemoveMember(member.id) },
                    )
                    if (index < members.lastIndex) {
                        HorizontalDivider(
                            color = colors.border,
                            modifier = Modifier.padding(horizontal = 16.dp),
                        )
                    }
                }
            }
        } else {
            // Empty state
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 48.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "Участники не найдены",
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.mutedForeground,
                )
            }
        }
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
/*  Member row                                                                */
/* ═══════════════════════════════════════════════════════════════════════════ */

@Composable
private fun MemberRow(
    member: User,
    isEditMode: Boolean,
    onCallClick: () -> Unit,
    onContactClick: () -> Unit,
    onRemoveClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = AleAppTheme.colors

    val statusLabel = when (member.status) {
        UserStatus.ONLINE -> "В сети"
        UserStatus.INVISIBLE -> "Не в сети"
        UserStatus.DO_NOT_DISTURB -> "Занят"
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onContactClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        MemberAvatar(
            name = member.name,
            status = member.status,
            size = 56.dp,
        )

        Spacer(Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = member.name,
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontWeight = FontWeight.Medium,
                ),
                color = colors.cardForeground,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = statusLabel,
                style = MaterialTheme.typography.bodySmall,
                color = colors.mutedForeground,
                maxLines = 1,
            )
        }

        if (isEditMode) {
            IconButton(onClick = onRemoveClick) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Удалить ${member.name}",
                    tint = colors.destructive,
                )
            }
        } else {
            IconButton(onClick = onCallClick) {
                Icon(
                    imageVector = Icons.Default.Phone,
                    contentDescription = "Позвонить ${member.name}",
                    tint = colors.primary,
                )
            }
        }
    }
}

/* ═══════════════════════════════════════════════════════════════════════════ */
/*  Member avatar with status dot                                             */
/* ═══════════════════════════════════════════════════════════════════════════ */

@Composable
private fun MemberAvatar(
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
/*  Join requests section                                                     */
/* ═══════════════════════════════════════════════════════════════════════════ */

@Composable
fun JoinRequestsSection(
    requests: List<JoinRequest>,
    onApprove: (String) -> Unit,
    onDecline: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = AleAppTheme.colors

    Column(modifier = modifier) {
        // Section header
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
            // Empty state
            AleAppCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 48.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
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
    }
}

@Composable
private fun JoinRequestRow(
    request: JoinRequest,
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

        // Name + username + date
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
                text = request.createdAt,
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
            AleAppButton(
                onClick = onApprove,
                size = AleAppButtonSize.Small,
                variant = AleAppButtonVariant.Primary,
                modifier = Modifier,
            ) {
                val btnColors = AleAppTheme.colors
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp),
                    tint = btnColors.primaryForeground,
                )
                Spacer(Modifier.width(4.dp))
                Text("Принять", style = MaterialTheme.typography.labelMedium)
            }

            AleAppButton(
                onClick = onDecline,
                size = AleAppButtonSize.Small,
                variant = AleAppButtonVariant.Outline,
                modifier = Modifier,
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
/*  Previews                                                                  */
/* ═══════════════════════════════════════════════════════════════════════════ */

// ── Full screen previews ────────────────────────────────────────────────────

@Preview(name = "ServerDetail — Admin Light", showBackground = true, showSystemUi = true)
@Composable
private fun ServerDetailAdminLightPreview() {
    AleAppTheme(darkTheme = false) {
        ServerDetailScreen(
            server = PreviewData.serverTech,
            membersState = UiState.Success(PreviewData.techMembers),
            isAdmin = true,
            pendingRequests = PreviewData.joinRequests,
        )
    }
}

@Preview(
    name = "ServerDetail — Admin Dark",
    showBackground = true,
    showSystemUi = true,
    uiMode = Configuration.UI_MODE_NIGHT_YES,
)
@Composable
private fun ServerDetailAdminDarkPreview() {
    AleAppTheme(darkTheme = true) {
        ServerDetailScreen(
            server = PreviewData.serverTech,
            membersState = UiState.Success(PreviewData.techMembers),
            isAdmin = true,
            pendingRequests = PreviewData.joinRequests,
        )
    }
}

@Preview(name = "ServerDetail — Regular User Light", showBackground = true, showSystemUi = true)
@Composable
private fun ServerDetailRegularLightPreview() {
    AleAppTheme(darkTheme = false) {
        ServerDetailScreen(
            server = PreviewData.serverCreative,
            membersState = UiState.Success(PreviewData.creativeMembers),
            isAdmin = false,
            pendingRequests = emptyList(),
        )
    }
}

@Preview(
    name = "ServerDetail — Regular User Dark",
    showBackground = true,
    showSystemUi = true,
    uiMode = Configuration.UI_MODE_NIGHT_YES,
)
@Composable
private fun ServerDetailRegularDarkPreview() {
    AleAppTheme(darkTheme = true) {
        ServerDetailScreen(
            server = PreviewData.serverCreative,
            membersState = UiState.Success(PreviewData.creativeMembers),
            isAdmin = false,
            pendingRequests = emptyList(),
        )
    }
}

@Preview(name = "ServerDetail — Loading", showBackground = true, showSystemUi = true)
@Composable
private fun ServerDetailLoadingPreview() {
    AleAppTheme(darkTheme = false) {
        ServerDetailScreen(
            server = PreviewData.serverTech,
            membersState = UiState.Loading,
            isAdmin = false,
            pendingRequests = emptyList(),
        )
    }
}

@Preview(name = "ServerDetail — Error", showBackground = true, showSystemUi = true)
@Composable
private fun ServerDetailErrorPreview() {
    AleAppTheme(darkTheme = false) {
        ServerDetailScreen(
            server = PreviewData.serverTech,
            membersState = UiState.Error("Нет соединения с сервером"),
            isAdmin = false,
            pendingRequests = emptyList(),
        )
    }
}

// ── Component previews ──────────────────────────────────────────────────────

@Preview(name = "TopBar — Admin with requests", showBackground = true)
@Composable
private fun TopBarAdminPreview() {
    AleAppTheme(darkTheme = false) {
        ServerDetailTopBar(
            serverName = "Tech Community",
            isAdmin = true,
            pendingRequestsCount = 2,
            onBack = {},
            onProfileClick = {},
            onManageServer = {},
            onViewRequests = {},
        )
    }
}

@Preview(name = "TopBar — Regular user", showBackground = true)
@Composable
private fun TopBarRegularPreview() {
    AleAppTheme(darkTheme = false) {
        ServerDetailTopBar(
            serverName = "Creative Studio",
            isAdmin = false,
            pendingRequestsCount = 0,
            onBack = {},
            onProfileClick = {},
            onManageServer = {},
            onViewRequests = {},
        )
    }
}

@Preview(name = "TopBar — Admin Dark", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun TopBarAdminDarkPreview() {
    AleAppTheme(darkTheme = true) {
        ServerDetailTopBar(
            serverName = "Tech Community",
            isAdmin = true,
            pendingRequestsCount = 5,
            onBack = {},
            onProfileClick = {},
            onManageServer = {},
            onViewRequests = {},
        )
    }
}

@Preview(name = "ServerInfo — Admin", showBackground = true)
@Composable
private fun ServerInfoAdminPreview() {
    AleAppTheme(darkTheme = false) {
        Surface(color = AleAppTheme.colors.background) {
            ServerInfoSection(server = PreviewData.serverTech, isAdmin = true)
        }
    }
}

@Preview(name = "ServerInfo — Regular", showBackground = true)
@Composable
private fun ServerInfoRegularPreview() {
    AleAppTheme(darkTheme = false) {
        Surface(color = AleAppTheme.colors.background) {
            ServerInfoSection(server = PreviewData.serverCreative, isAdmin = false)
        }
    }
}

@Preview(name = "ServerInfo — Dark", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun ServerInfoDarkPreview() {
    AleAppTheme(darkTheme = true) {
        Surface(color = AleAppTheme.colors.background) {
            ServerInfoSection(server = PreviewData.serverTech, isAdmin = true)
        }
    }
}

@Preview(name = "SearchBar — Light", showBackground = true)
@Composable
private fun SearchBarPreview() {
    AleAppTheme(darkTheme = false) {
        Surface(color = AleAppTheme.colors.background) {
            SearchBar(
                query = "",
                onQueryChange = {},
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
            )
        }
    }
}

@Preview(name = "SearchBar — With text", showBackground = true)
@Composable
private fun SearchBarFilledPreview() {
    AleAppTheme(darkTheme = false) {
        Surface(color = AleAppTheme.colors.background) {
            SearchBar(
                query = "Анна",
                onQueryChange = {},
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
            )
        }
    }
}

@Preview(name = "SearchBar — Dark", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun SearchBarDarkPreview() {
    AleAppTheme(darkTheme = true) {
        Surface(color = AleAppTheme.colors.background) {
            SearchBar(
                query = "",
                onQueryChange = {},
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
            )
        }
    }
}

@Preview(name = "MemberRow — Online", showBackground = true)
@Composable
private fun MemberRowOnlinePreview() {
    AleAppTheme(darkTheme = false) {
        Surface(color = AleAppTheme.colors.card) {
            MemberRow(
                member = PreviewData.userAnna,
                isEditMode = false,
                onCallClick = {},
                onContactClick = {},
                onRemoveClick = {},
            )
        }
    }
}

@Preview(name = "MemberRow — Offline", showBackground = true)
@Composable
private fun MemberRowOfflinePreview() {
    AleAppTheme(darkTheme = false) {
        Surface(color = AleAppTheme.colors.card) {
            MemberRow(
                member = PreviewData.userMaria,
                isEditMode = false,
                onCallClick = {},
                onContactClick = {},
                onRemoveClick = {},
            )
        }
    }
}

@Preview(name = "MemberRow — Edit mode", showBackground = true)
@Composable
private fun MemberRowEditModePreview() {
    AleAppTheme(darkTheme = false) {
        Surface(color = AleAppTheme.colors.card) {
            MemberRow(
                member = PreviewData.userAnna,
                isEditMode = true,
                onCallClick = {},
                onContactClick = {},
                onRemoveClick = {},
            )
        }
    }
}

@Preview(name = "MemberRow — Dark", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun MemberRowDarkPreview() {
    AleAppTheme(darkTheme = true) {
        Surface(color = AleAppTheme.colors.card) {
            MemberRow(
                member = User(
                    id = "u2", name = "Алексей Козлов", username = "@alexey_k",
                    status = UserStatus.DO_NOT_DISTURB, serverId = "s1",
                ),
                isEditMode = false,
                onCallClick = {},
                onContactClick = {},
                onRemoveClick = {},
            )
        }
    }
}

@Preview(name = "MemberAvatar — all statuses", showBackground = true)
@Composable
private fun MemberAvatarStatusesPreview() {
    AleAppTheme(darkTheme = false) {
        Surface(color = AleAppTheme.colors.card) {
            Row(
                modifier = Modifier.padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                MemberAvatar(name = "Анна С", status = UserStatus.ONLINE)
                MemberAvatar(name = "Алексей К", status = UserStatus.DO_NOT_DISTURB)
                MemberAvatar(name = "Мария В", status = UserStatus.INVISIBLE)
            }
        }
    }
}

@Preview(name = "AdminBadge — Light", showBackground = true)
@Composable
private fun AdminBadgeLightPreview() {
    AleAppTheme(darkTheme = false) {
        Surface(color = AleAppTheme.colors.card, modifier = Modifier.padding(16.dp)) {
            AdminBadge()
        }
    }
}

@Preview(name = "AdminBadge — Dark", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun AdminBadgeDarkPreview() {
    AleAppTheme(darkTheme = true) {
        Surface(color = AleAppTheme.colors.card, modifier = Modifier.padding(16.dp)) {
            AdminBadge()
        }
    }
}

@Preview(name = "Members card — полная", showBackground = true)
@Composable
private fun MembersCardPreview() {
    AleAppTheme(darkTheme = false) {
        Surface(color = AleAppTheme.colors.background) {
            MembersSection(
                members = PreviewData.techMembers,
                isAdmin = true,
                isEditMode = false,
                onToggleEditMode = {},
                onCallClick = { _, _ -> },
                onContactClick = {},
                onRemoveMember = {},
            )
        }
    }
}

@Preview(name = "Members card — empty", showBackground = true)
@Composable
private fun MembersEmptyPreview() {
    AleAppTheme(darkTheme = false) {
        Surface(color = AleAppTheme.colors.background) {
            MembersSection(
                members = emptyList(),
                isAdmin = false,
                isEditMode = false,
                onToggleEditMode = {},
                onCallClick = { _, _ -> },
                onContactClick = {},
                onRemoveMember = {},
            )
        }
    }
}

@Preview(name = "JoinRequests — with items", showBackground = true)
@Composable
private fun JoinRequestsPreview() {
    AleAppTheme(darkTheme = false) {
        Surface(color = AleAppTheme.colors.background) {
            JoinRequestsSection(
                requests = PreviewData.joinRequests,
                onApprove = {},
                onDecline = {},
            )
        }
    }
}

@Preview(name = "JoinRequests — Dark", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun JoinRequestsDarkPreview() {
    AleAppTheme(darkTheme = true) {
        Surface(color = AleAppTheme.colors.background) {
            JoinRequestsSection(
                requests = PreviewData.joinRequests,
                onApprove = {},
                onDecline = {},
            )
        }
    }
}

@Preview(name = "JoinRequests — empty", showBackground = true)
@Composable
private fun JoinRequestsEmptyPreview() {
    AleAppTheme(darkTheme = false) {
        Surface(color = AleAppTheme.colors.background) {
            JoinRequestsSection(
                requests = emptyList(),
                onApprove = {},
                onDecline = {},
            )
        }
    }
}

@Preview(name = "JoinRequestRow — single", showBackground = true)
@Composable
private fun JoinRequestRowPreview() {
    AleAppTheme(darkTheme = false) {
        Surface(color = AleAppTheme.colors.card) {
            JoinRequestRow(
                request = PreviewData.joinRequests.first(),
                onApprove = {},
                onDecline = {},
            )
        }
    }
}

@Preview(name = "ServerImage — sizes", showBackground = true)
@Composable
private fun ServerImagePreview() {
    AleAppTheme(darkTheme = false) {
        Surface(color = AleAppTheme.colors.background) {
            Row(
                modifier = Modifier.padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                ServerImage(name = "Tech Community", size = 64.dp)
                ServerImage(name = "Creative Studio", size = 96.dp)
            }
        }
    }
}
