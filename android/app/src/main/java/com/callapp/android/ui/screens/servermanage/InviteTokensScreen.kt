package com.callapp.android.ui.screens.servermanage

import android.content.res.Configuration
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.callapp.android.domain.model.InviteToken
import com.callapp.android.domain.model.UserRole
import com.callapp.android.ui.components.AleAppButton
import com.callapp.android.ui.components.AleAppButtonSize
import com.callapp.android.ui.components.AleAppButtonVariant
import com.callapp.android.ui.components.AleAppCard
import com.callapp.android.ui.components.FormField
import com.callapp.android.ui.theme.AleAppTheme

/* ═══════════════════════════════════════════════════════════════════════════ */
/*  InviteTokensScreen                                                        */
/* ═══════════════════════════════════════════════════════════════════════════ */

@Composable
fun InviteTokensScreen(
    tokens: List<InviteToken> = emptyList(),
    serverAddress: String = "",
    isLoading: Boolean = false,
    onBack: () -> Unit = {},
    onCreateToken: (label: String, maxUses: Int, grantedRole: String, requireApproval: Boolean) -> Unit =
        { _, _, _, _ -> },
    onRevokeToken: (tokenId: String) -> Unit = {},
    onCopyToken: (fullToken: String) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val colors = AleAppTheme.colors
    var showCreateDialog by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = colors.background,
        topBar = {
            InviteTokensTopBar(onBack = onBack)
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showCreateDialog = true },
                containerColor = colors.primary,
                contentColor = colors.primaryForeground,
            ) {
                Icon(Icons.Default.Add, contentDescription = "Создать токен")
            }
        },
    ) { padding ->
        when {
            isLoading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator(color = colors.primary)
                }
            }
            tokens.isEmpty() -> {
                EmptyTokensState(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                )
            }
            else -> {
                LazyColumn(
                    modifier = modifier
                        .fillMaxSize()
                        .padding(padding),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(
                        start = 16.dp,
                        end = 16.dp,
                        top = 16.dp,
                        bottom = 88.dp,
                    ),
                ) {
                    items(tokens, key = { it.id }) { token ->
                        TokenCard(
                            token = token,
                            serverAddress = serverAddress,
                            onRevoke = { onRevokeToken(token.id) },
                            onCopy = {
                                val address = serverAddress
                                    .removePrefix("http://")
                                    .removePrefix("https://")
                                onCopyToken("$address/${token.code}")
                            },
                        )
                    }
                }
            }
        }
    }

    if (showCreateDialog) {
        CreateTokenDialog(
            onDismiss = { showCreateDialog = false },
            onCreate = { label, maxUses, role, approval ->
                onCreateToken(label, maxUses, role, approval)
                showCreateDialog = false
            },
        )
    }
}

/* ═══════════════════════════════════════════════════════════════════════════ */
/*  Top bar                                                                   */
/* ═══════════════════════════════════════════════════════════════════════════ */

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun InviteTokensTopBar(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = AleAppTheme.colors

    Column(modifier) {
        TopAppBar(
            title = {
                Text(
                    text = "Токены приглашений",
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
/*  Token card                                                                */
/* ═══════════════════════════════════════════════════════════════════════════ */

@Composable
private fun TokenCard(
    token: InviteToken,
    serverAddress: String,
    onRevoke: () -> Unit,
    onCopy: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = AleAppTheme.colors
    val alpha = if (token.revoked) 0.5f else 1f

    AleAppCard(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
        ) {
            // Header: label + revoked badge
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    text = token.label,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Medium,
                    ),
                    color = colors.foreground.copy(alpha = alpha),
                    modifier = Modifier.weight(1f),
                )

                if (token.revoked) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = colors.destructive.copy(alpha = 0.15f),
                    ) {
                        Text(
                            text = "Отозван",
                            style = MaterialTheme.typography.labelSmall,
                            color = colors.destructive,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                        )
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            // Code (monospace, copyable)
            Row(
                verticalAlignment = Alignment.CenterVertically,
            ) {
                val displayAddress = serverAddress
                    .removePrefix("http://")
                    .removePrefix("https://")

                Text(
                    text = "$displayAddress/${token.code}",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontFamily = FontFamily.Monospace,
                    ),
                    color = colors.primary.copy(alpha = alpha),
                    modifier = Modifier.weight(1f),
                )

                if (!token.revoked) {
                    IconButton(
                        onClick = onCopy,
                        modifier = Modifier.size(32.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = "Копировать",
                            modifier = Modifier.size(18.dp),
                            tint = colors.mutedForeground,
                        )
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            // Info badges
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                // Usage
                val usageText = if (token.maxUses == 0) {
                    "${token.useCount} / \u221E"
                } else {
                    "${token.useCount} / ${token.maxUses}"
                }
                TokenBadge(text = usageText)

                // Role
                val roleText = if (token.grantedRole == UserRole.ADMIN) "Админ" else "Участник"
                TokenBadge(text = roleText)

                // Approval
                val approvalText = if (token.requireApproval) "С одобрением" else "Без одобрения"
                TokenBadge(text = approvalText)
            }

            // Revoke button
            if (!token.revoked) {
                Spacer(Modifier.height(12.dp))
                AleAppButton(
                    onClick = onRevoke,
                    variant = AleAppButtonVariant.Outline,
                    size = AleAppButtonSize.Small,
                ) {
                    Text(
                        text = "Отозвать",
                        color = colors.destructive,
                    )
                }
            }
        }
    }
}

/* ═══════════════════════════════════════════════════════════════════════════ */
/*  Token badge                                                               */
/* ═══════════════════════════════════════════════════════════════════════════ */

@Composable
private fun TokenBadge(
    text: String,
    modifier: Modifier = Modifier,
) {
    val colors = AleAppTheme.colors

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(6.dp),
        color = colors.secondary,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = colors.mutedForeground,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
        )
    }
}

/* ═══════════════════════════════════════════════════════════════════════════ */
/*  Empty state                                                               */
/* ═══════════════════════════════════════════════════════════════════════════ */

@Composable
private fun EmptyTokensState(modifier: Modifier = Modifier) {
    val colors = AleAppTheme.colors

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "Нет токенов",
                style = MaterialTheme.typography.titleMedium,
                color = colors.mutedForeground,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "Создайте токен приглашения для новых участников",
                style = MaterialTheme.typography.bodySmall,
                color = colors.mutedForeground,
            )
        }
    }
}

/* ═══════════════════════════════════════════════════════════════════════════ */
/*  Create token dialog                                                       */
/* ═══════════════════════════════════════════════════════════════════════════ */

@Composable
private fun CreateTokenDialog(
    onDismiss: () -> Unit,
    onCreate: (label: String, maxUses: Int, grantedRole: String, requireApproval: Boolean) -> Unit,
) {
    val colors = AleAppTheme.colors

    var label by remember { mutableStateOf("") }
    var maxUsesText by remember { mutableStateOf("") }
    var isAdmin by remember { mutableStateOf(false) }
    var requireApproval by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = colors.card,
        titleContentColor = colors.foreground,
        textContentColor = colors.foreground,
        title = {
            Text(
                text = "Новый токен",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Medium,
                ),
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                FormField(
                    label = "Название",
                    required = true,
                    value = label,
                    onValueChange = { label = it; error = null },
                    placeholder = "Для команды дизайна",
                    singleLine = true,
                )

                FormField(
                    label = "Макс. использований",
                    required = false,
                    value = maxUsesText,
                    onValueChange = { maxUsesText = it.filter { ch -> ch.isDigit() } },
                    placeholder = "0 = без ограничений",
                    singleLine = true,
                )

                // Role switch
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Права администратора",
                            style = MaterialTheme.typography.bodyMedium,
                            color = colors.foreground,
                        )
                        Text(
                            text = "Пользователь получит роль админа",
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.mutedForeground,
                        )
                    }
                    Switch(
                        checked = isAdmin,
                        onCheckedChange = { isAdmin = it },
                        colors = SwitchDefaults.colors(
                            checkedTrackColor = colors.primary,
                            checkedThumbColor = colors.primaryForeground,
                        ),
                    )
                }

                // Require approval switch
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Требовать одобрение",
                            style = MaterialTheme.typography.bodyMedium,
                            color = colors.foreground,
                        )
                        Text(
                            text = "Заявка будет ждать подтверждения админа",
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.mutedForeground,
                        )
                    }
                    Switch(
                        checked = requireApproval,
                        onCheckedChange = { requireApproval = it },
                        colors = SwitchDefaults.colors(
                            checkedTrackColor = colors.primary,
                            checkedThumbColor = colors.primaryForeground,
                        ),
                    )
                }

                if (error != null) {
                    Text(
                        text = error!!,
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.destructive,
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (label.isBlank()) {
                        error = "Название обязательно"
                        return@TextButton
                    }
                    val maxUses = maxUsesText.toIntOrNull() ?: 0
                    val role = if (isAdmin) "ADMIN" else "MEMBER"
                    onCreate(label.trim(), maxUses, role, requireApproval)
                },
            ) {
                Text("Создать", color = colors.primary)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Отмена", color = colors.mutedForeground)
            }
        },
    )
}

/* ═══════════════════════════════════════════════════════════════════════════ */
/*  Create token form (for standalone preview)                                */
/* ═══════════════════════════════════════════════════════════════════════════ */

@Composable
private fun CreateTokenFormContent(modifier: Modifier = Modifier) {
    val colors = AleAppTheme.colors

    var label by remember { mutableStateOf("Для команды дизайна") }
    var maxUsesText by remember { mutableStateOf("10") }
    var isAdmin by remember { mutableStateOf(false) }
    var requireApproval by remember { mutableStateOf(true) }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            text = "Новый токен",
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Medium),
            color = colors.foreground,
        )

        FormField(
            label = "Название",
            required = true,
            value = label,
            onValueChange = { label = it },
            placeholder = "Для команды дизайна",
            singleLine = true,
        )

        FormField(
            label = "Макс. использований",
            required = false,
            value = maxUsesText,
            onValueChange = { maxUsesText = it.filter { ch -> ch.isDigit() } },
            placeholder = "0 = без ограничений",
            singleLine = true,
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Права администратора", style = MaterialTheme.typography.bodyMedium, color = colors.foreground)
                Text("Пользователь получит роль админа", style = MaterialTheme.typography.bodySmall, color = colors.mutedForeground)
            }
            Switch(
                checked = isAdmin,
                onCheckedChange = { isAdmin = it },
                colors = SwitchDefaults.colors(checkedTrackColor = colors.primary, checkedThumbColor = colors.primaryForeground),
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Требовать одобрение", style = MaterialTheme.typography.bodyMedium, color = colors.foreground)
                Text("Заявка будет ждать подтверждения админа", style = MaterialTheme.typography.bodySmall, color = colors.mutedForeground)
            }
            Switch(
                checked = requireApproval,
                onCheckedChange = { requireApproval = it },
                colors = SwitchDefaults.colors(checkedTrackColor = colors.primary, checkedThumbColor = colors.primaryForeground),
            )
        }

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            AleAppButton(
                onClick = {},
                variant = AleAppButtonVariant.Primary,
                size = AleAppButtonSize.Default,
                modifier = Modifier.weight(1f),
            ) { Text("Создать") }

            AleAppButton(
                onClick = {},
                variant = AleAppButtonVariant.Outline,
                size = AleAppButtonSize.Default,
                modifier = Modifier.weight(1f),
            ) { Text("Отмена") }
        }
    }
}

/* ═══════════════════════════════════════════════════════════════════════════ */
/*  Sample data                                                               */
/* ═══════════════════════════════════════════════════════════════════════════ */

internal val sampleTokens = listOf(
    InviteToken(
        id = "t1",
        code = "ABC12345",
        label = "Для команды дизайна",
        maxUses = 10,
        useCount = 3,
        grantedRole = UserRole.MEMBER,
        requireApproval = false,
        revoked = false,
        createdAt = "2025-01-15",
    ),
    InviteToken(
        id = "t2",
        code = "XYZ98765",
        label = "Админ-доступ",
        maxUses = 1,
        useCount = 1,
        grantedRole = UserRole.ADMIN,
        requireApproval = true,
        revoked = false,
        createdAt = "2025-01-10",
    ),
    InviteToken(
        id = "t3",
        code = "OLD54321",
        label = "Старый токен",
        maxUses = 5,
        useCount = 5,
        grantedRole = UserRole.MEMBER,
        requireApproval = false,
        revoked = true,
        createdAt = "2024-12-01",
    ),
)

/* ═══════════════════════════════════════════════════════════════════════════ */
/*  Previews                                                                  */
/* ═══════════════════════════════════════════════════════════════════════════ */

@Preview(name = "InviteTokens — Light", showBackground = true, showSystemUi = true)
@Composable
private fun InviteTokensLightPreview() {
    AleAppTheme(darkTheme = false) {
        InviteTokensScreen(
            tokens = sampleTokens,
            serverAddress = "preview.callapp.example",
        )
    }
}

@Preview(
    name = "InviteTokens — Dark",
    showBackground = true,
    showSystemUi = true,
    uiMode = Configuration.UI_MODE_NIGHT_YES,
)
@Composable
private fun InviteTokensDarkPreview() {
    AleAppTheme(darkTheme = true) {
        InviteTokensScreen(
            tokens = sampleTokens,
            serverAddress = "preview.callapp.example",
        )
    }
}

@Preview(name = "InviteTokens — Empty", showBackground = true, showSystemUi = true)
@Composable
private fun InviteTokensEmptyPreview() {
    AleAppTheme(darkTheme = false) {
        InviteTokensScreen(
            tokens = emptyList(),
            serverAddress = "preview.callapp.example",
        )
    }
}

@Preview(name = "InviteTokens — Loading", showBackground = true, showSystemUi = true)
@Composable
private fun InviteTokensLoadingPreview() {
    AleAppTheme(darkTheme = false) {
        InviteTokensScreen(
            tokens = emptyList(),
            isLoading = true,
            serverAddress = "preview.callapp.example",
        )
    }
}

@Preview(name = "TokenCard — Light", showBackground = true)
@Composable
private fun TokenCardLightPreview() {
    AleAppTheme(darkTheme = false) {
        Surface(color = AleAppTheme.colors.background) {
            TokenCard(
                token = sampleTokens[0],
                serverAddress = "preview.callapp.example",
                onRevoke = {},
                onCopy = {},
                modifier = Modifier.padding(16.dp),
            )
        }
    }
}

@Preview(name = "TokenCard — Revoked", showBackground = true)
@Composable
private fun TokenCardRevokedPreview() {
    AleAppTheme(darkTheme = false) {
        Surface(color = AleAppTheme.colors.background) {
            TokenCard(
                token = sampleTokens[2],
                serverAddress = "preview.callapp.example",
                onRevoke = {},
                onCopy = {},
                modifier = Modifier.padding(16.dp),
            )
        }
    }
}

@Preview(name = "TokenCard — Dark", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun TokenCardDarkPreview() {
    AleAppTheme(darkTheme = true) {
        Surface(color = AleAppTheme.colors.background) {
            TokenCard(
                token = sampleTokens[1],
                serverAddress = "preview.callapp.example",
                onRevoke = {},
                onCopy = {},
                modifier = Modifier.padding(16.dp),
            )
        }
    }
}

@Preview(name = "CreateTokenDialog — Light", showBackground = true)
@Composable
private fun CreateTokenDialogPreview() {
    AleAppTheme(darkTheme = false) {
        CreateTokenDialog(
            onDismiss = {},
            onCreate = { _, _, _, _ -> },
        )
    }
}

@Preview(
    name = "CreateTokenDialog — Dark",
    showBackground = true,
    uiMode = Configuration.UI_MODE_NIGHT_YES,
)
@Composable
private fun CreateTokenDialogDarkPreview() {
    AleAppTheme(darkTheme = true) {
        CreateTokenDialog(
            onDismiss = {},
            onCreate = { _, _, _, _ -> },
        )
    }
}

@Preview(name = "CreateTokenForm — Content Only", showBackground = true, widthDp = 360)
@Composable
private fun CreateTokenFormPreview() {
    AleAppTheme(darkTheme = false) {
        Surface(color = AleAppTheme.colors.card) {
            CreateTokenFormContent(modifier = Modifier.padding(24.dp))
        }
    }
}

@Preview(
    name = "CreateTokenForm — Dark",
    showBackground = true,
    widthDp = 360,
    uiMode = Configuration.UI_MODE_NIGHT_YES,
)
@Composable
private fun CreateTokenFormDarkPreview() {
    AleAppTheme(darkTheme = true) {
        Surface(color = AleAppTheme.colors.card) {
            CreateTokenFormContent(modifier = Modifier.padding(24.dp))
        }
    }
}

@Preview(name = "TokenBadge — Light", showBackground = true)
@Composable
private fun TokenBadgePreview() {
    AleAppTheme(darkTheme = false) {
        Surface(color = AleAppTheme.colors.background) {
            Row(
                modifier = Modifier.padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                TokenBadge(text = "3 / 10")
                TokenBadge(text = "Участник")
                TokenBadge(text = "Без одобрения")
            }
        }
    }
}
