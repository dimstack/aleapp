package com.example.android.ui.screens.servermanage

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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Delete
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.android.ui.components.AleAppButton
import com.example.android.ui.components.AleAppButtonSize
import com.example.android.ui.components.AleAppButtonVariant
import com.example.android.ui.components.AleAppCard
import com.example.android.ui.components.FormField
import com.example.android.ui.theme.AleAppTheme
import kotlin.math.absoluteValue

/* ═══════════════════════════════════════════════════════════════════════════ */
/*  Data                                                                      */
/* ═══════════════════════════════════════════════════════════════════════════ */

data class ServerManageData(
    val id: String,
    val name: String,
    val username: String,
    val description: String,
    val imageUrl: String,
)

/* ═══════════════════════════════════════════════════════════════════════════ */
/*  Mock data                                                                 */
/* ═══════════════════════════════════════════════════════════════════════════ */

internal val sampleManageData = ServerManageData(
    id = "s1",
    name = "Tech Community",
    username = "tech_community",
    description = "Сообщество разработчиков и технических специалистов. Обсуждаем последние технологии и делимся опытом.",
    imageUrl = "https://images.unsplash.com/photo",
)

/* ═══════════════════════════════════════════════════════════════════════════ */
/*  Color helpers                                                             */
/* ═══════════════════════════════════════════════════════════════════════════ */

private val serverImagePalette = listOf(
    Color(0xFF3A5068), Color(0xFF5E4B6B), Color(0xFF4B6858), Color(0xFF6B5B4B),
)

private fun serverImageColor(name: String): Color =
    serverImagePalette[name.hashCode().absoluteValue % serverImagePalette.size]

/* ═══════════════════════════════════════════════════════════════════════════ */
/*  ServerManagementScreen                                                    */
/* ═══════════════════════════════════════════════════════════════════════════ */

@Composable
fun ServerManagementScreen(
    initial: ServerManageData = sampleManageData,
    onBack: () -> Unit = {},
    onSave: (name: String, username: String, description: String, imageUrl: String) -> Unit =
        { _, _, _, _ -> },
    onDeleteServer: () -> Unit = {},
    onInviteTokens: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val colors = AleAppTheme.colors

    var name by remember { mutableStateOf(initial.name) }
    var username by remember { mutableStateOf(initial.username) }
    var description by remember { mutableStateOf(initial.description) }
    var imageUrl by remember { mutableStateOf(initial.imageUrl) }
    var error by remember { mutableStateOf<String?>(null) }

    Scaffold(
        containerColor = colors.background,
        topBar = {
            ManageTopBar(onBack = onBack)
        },
    ) { padding ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState()),
        ) {
            Spacer(Modifier.height(24.dp))

            // ── Main form card ────────────────────────────────────────────
            AleAppCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp),
                ) {
                    // Server image preview + camera button
                    ServerImagePreview(name = name)

                    // Name field
                    FormField(
                        label = "Название сервера",
                        required = true,
                        value = name,
                        onValueChange = { name = it; error = null },
                        placeholder = "Введите название сервера",
                        singleLine = true,
                    )

                    // Username field
                    FormField(
                        label = "Username",
                        required = true,
                        value = username,
                        onValueChange = { username = it; error = null },
                        placeholder = "server_username",
                        singleLine = true,
                        prefix = "@ ",
                    )

                    // Description field
                    FormField(
                        label = "Описание",
                        required = true,
                        value = description,
                        onValueChange = { description = it; error = null },
                        placeholder = "Введите описание сервера",
                        singleLine = false,
                        minHeight = 96,
                    )

                    // Image URL field
                    FormField(
                        label = "URL изображения",
                        required = false,
                        value = imageUrl,
                        onValueChange = { imageUrl = it },
                        placeholder = "https://example.com/image.jpg",
                        singleLine = true,
                    )

                    // Error message
                    if (error != null) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = colors.destructive.copy(alpha = 0.1f),
                            border = BorderStroke(1.dp, colors.destructive.copy(alpha = 0.2f)),
                        ) {
                            Text(
                                text = error!!,
                                style = MaterialTheme.typography.bodySmall,
                                color = colors.destructive,
                                modifier = Modifier.padding(12.dp),
                            )
                        }
                    }

                    // Action buttons
                    Spacer(Modifier.height(4.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        AleAppButton(
                            onClick = {
                                when {
                                    name.isBlank() -> error = "Название обязательно"
                                    username.isBlank() -> error = "Username обязателен"
                                    description.isBlank() -> error = "Описание обязательно"
                                    else -> {
                                        error = null
                                        onSave(
                                            name.trim(),
                                            username.trim(),
                                            description.trim(),
                                            imageUrl.trim(),
                                        )
                                    }
                                }
                            },
                            variant = AleAppButtonVariant.Primary,
                            size = AleAppButtonSize.Large,
                            modifier = Modifier.weight(1f),
                        ) {
                            Text("Сохранить изменения")
                        }

                        AleAppButton(
                            onClick = onBack,
                            variant = AleAppButtonVariant.Outline,
                            size = AleAppButtonSize.Large,
                            modifier = Modifier.weight(1f),
                        ) {
                            Text("Отмена")
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // ── Invite tokens link ──────────────────────────────────────
            InviteTokensCard(
                onClick = onInviteTokens,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
            )

            Spacer(Modifier.height(16.dp))

            // ── Danger zone ───────────────────────────────────────────────
            DangerZone(
                onDeleteServer = onDeleteServer,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
            )

            Spacer(Modifier.height(32.dp))
        }
    }
}

/* ═══════════════════════════════════════════════════════════════════════════ */
/*  Top bar                                                                   */
/* ═══════════════════════════════════════════════════════════════════════════ */

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ManageTopBar(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = AleAppTheme.colors

    Column(modifier) {
        TopAppBar(
            title = {
                Text(
                    text = "Управление сервером",
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
/*  Server image preview                                                      */
/* ═══════════════════════════════════════════════════════════════════════════ */

@Composable
private fun ServerImagePreview(
    name: String,
    modifier: Modifier = Modifier,
) {
    val colors = AleAppTheme.colors
    val bgColor = serverImageColor(name)
    val initials = name.split(" ")
        .take(2)
        .mapNotNull { it.firstOrNull()?.uppercaseChar() }
        .joinToString("")

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box {
            // Server image placeholder
            Surface(
                modifier = Modifier.size(128.dp),
                shape = RoundedCornerShape(16.dp),
                color = bgColor,
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = initials,
                        color = Color.White,
                        style = MaterialTheme.typography.headlineLarge.copy(
                            fontWeight = FontWeight.SemiBold,
                        ),
                    )
                }
            }

            // Camera button overlay
            Surface(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .offset(x = 4.dp, y = 4.dp)
                    .size(32.dp),
                shape = CircleShape,
                color = colors.primary,
                contentColor = colors.primaryForeground,
                shadowElevation = 4.dp,
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.CameraAlt,
                        contentDescription = "Изменить фото",
                        modifier = Modifier.size(16.dp),
                    )
                }
            }
        }

        Spacer(Modifier.height(8.dp))

        Text(
            text = "Изображение сервера",
            style = MaterialTheme.typography.bodySmall,
            color = colors.mutedForeground,
        )
    }
}

/* ═══════════════════════════════════════════════════════════════════════════ */
/*  Invite tokens card                                                        */
/* ═══════════════════════════════════════════════════════════════════════════ */

@Composable
private fun InviteTokensCard(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = AleAppTheme.colors

    AleAppCard(
        modifier = modifier,
        onClick = onClick,
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Токены приглашений",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Medium,
                    ),
                    color = colors.foreground,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "Управление токенами для приглашения новых участников",
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.mutedForeground,
                )
            }
            Spacer(Modifier.width(8.dp))
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = null,
                tint = colors.mutedForeground,
                modifier = Modifier
                    .size(20.dp)
                    .graphicsLayer { rotationZ = 180f },
            )
        }
    }
}

/* ═══════════════════════════════════════════════════════════════════════════ */
/*  Danger zone                                                               */
/* ═══════════════════════════════════════════════════════════════════════════ */

@Composable
private fun DangerZone(
    onDeleteServer: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = AleAppTheme.colors

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = colors.destructive.copy(alpha = 0.1f),
        border = BorderStroke(1.dp, colors.destructive.copy(alpha = 0.2f)),
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
        ) {
            Icon(
                imageVector = Icons.Default.Delete,
                contentDescription = null,
                tint = colors.destructive,
                modifier = Modifier
                    .size(20.dp)
                    .padding(top = 2.dp),
            )

            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Опасная зона",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                    ),
                    color = colors.destructive,
                )

                Spacer(Modifier.height(4.dp))

                Text(
                    text = "Удаление сервера приведет к потере всех данных. Это действие нельзя отменить.",
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.mutedForeground,
                )

                Spacer(Modifier.height(12.dp))

                AleAppButton(
                    onClick = onDeleteServer,
                    variant = AleAppButtonVariant.Outline,
                    size = AleAppButtonSize.Default,
                ) {
                    Text(
                        text = "Удалить сервер",
                        color = colors.destructive,
                    )
                }
            }
        }
    }
}

/* ═══════════════════════════════════════════════════════════════════════════ */
/*  Previews                                                                  */
/* ═══════════════════════════════════════════════════════════════════════════ */

// ── Full screen ─────────────────────────────────────────────────────────────

@Preview(name = "ServerManagement — Light", showBackground = true, showSystemUi = true)
@Composable
private fun ServerManagementLightPreview() {
    AleAppTheme(darkTheme = false) {
        ServerManagementScreen()
    }
}

@Preview(
    name = "ServerManagement — Dark",
    showBackground = true,
    showSystemUi = true,
    uiMode = Configuration.UI_MODE_NIGHT_YES,
)
@Composable
private fun ServerManagementDarkPreview() {
    AleAppTheme(darkTheme = true) {
        ServerManagementScreen()
    }
}

@Preview(name = "ServerManagement — Empty fields", showBackground = true, showSystemUi = true)
@Composable
private fun ServerManagementEmptyPreview() {
    AleAppTheme(darkTheme = false) {
        ServerManagementScreen(
            initial = ServerManageData(
                id = "new",
                name = "",
                username = "",
                description = "",
                imageUrl = "",
            ),
        )
    }
}

// ── Component previews ──────────────────────────────────────────────────────

@Preview(name = "TopBar — Light", showBackground = true)
@Composable
private fun TopBarLightPreview() {
    AleAppTheme(darkTheme = false) {
        ManageTopBar(onBack = {})
    }
}

@Preview(name = "TopBar — Dark", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun TopBarDarkPreview() {
    AleAppTheme(darkTheme = true) {
        ManageTopBar(onBack = {})
    }
}

@Preview(name = "ServerImagePreview — Light", showBackground = true)
@Composable
private fun ServerImagePreviewLight() {
    AleAppTheme(darkTheme = false) {
        Surface(color = AleAppTheme.colors.card) {
            ServerImagePreview(
                name = "Tech Community",
                modifier = Modifier.padding(24.dp),
            )
        }
    }
}

@Preview(name = "ServerImagePreview — Dark", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun ServerImagePreviewDark() {
    AleAppTheme(darkTheme = true) {
        Surface(color = AleAppTheme.colors.card) {
            ServerImagePreview(
                name = "Creative Studio",
                modifier = Modifier.padding(24.dp),
            )
        }
    }
}

@Preview(name = "DangerZone — Light", showBackground = true)
@Composable
private fun DangerZoneLightPreview() {
    AleAppTheme(darkTheme = false) {
        Surface(color = AleAppTheme.colors.background) {
            DangerZone(
                onDeleteServer = {},
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
            )
        }
    }
}

@Preview(name = "DangerZone — Dark", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun DangerZoneDarkPreview() {
    AleAppTheme(darkTheme = true) {
        Surface(color = AleAppTheme.colors.background) {
            DangerZone(
                onDeleteServer = {},
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
            )
        }
    }
}
