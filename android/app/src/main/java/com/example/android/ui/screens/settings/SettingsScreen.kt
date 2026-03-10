package com.example.android.ui.screens.settings

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material.icons.outlined.SettingsBrightness
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.android.ui.components.AleAppCard
import com.example.android.ui.components.AleAppTopBar
import com.example.android.ui.theme.AleAppTheme

/* ═══════════════════════════════════════════════════════════════════════════ */
/*  Data                                                                      */
/* ═══════════════════════════════════════════════════════════════════════════ */

enum class ThemeMode { LIGHT, DARK, SYSTEM }

enum class UserStatus(val label: String, val description: String) {
    ONLINE("В сети", "Вы доступны для звонков"),
    DO_NOT_DISTURB("Не беспокоить", "Не получать звонки"),
    INVISIBLE("Невидимый", "Показываться офлайн"),
}

/* ═══════════════════════════════════════════════════════════════════════════ */
/*  SettingsScreen                                                            */
/* ═══════════════════════════════════════════════════════════════════════════ */

@Composable
fun SettingsScreen(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    userStatus: UserStatus = UserStatus.ONLINE,
    onBack: () -> Unit = {},
    onThemeModeChange: (ThemeMode) -> Unit = {},
    onStatusChange: (UserStatus) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val colors = AleAppTheme.colors

    Scaffold(
        containerColor = colors.background,
        topBar = {
            AleAppTopBar(
                title = "Настройки",
                onBackClick = onBack,
            )
        },
    ) { padding ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            ThemeSection(
                themeMode = themeMode,
                onThemeModeChange = onThemeModeChange,
            )

            StatusSection(
                currentStatus = userStatus,
                onStatusChange = onStatusChange,
            )

            AboutSection()

            Spacer(Modifier.height(16.dp))
        }
    }
}

/* ═══════════════════════════════════════════════════════════════════════════ */
/*  ThemeSection                                                              */
/* ═══════════════════════════════════════════════════════════════════════════ */

@Composable
private fun ThemeSection(
    themeMode: ThemeMode,
    onThemeModeChange: (ThemeMode) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = AleAppTheme.colors

    AleAppCard(modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Outlined.LightMode,
                    contentDescription = null,
                    tint = colors.accent,
                    modifier = Modifier.size(20.dp),
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "Тема оформления",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                    ),
                    color = colors.cardForeground,
                )
            }

            Spacer(Modifier.height(16.dp))

            // Theme options
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                ThemeOption(
                    isSelected = themeMode == ThemeMode.LIGHT,
                    icon = {
                        Icon(
                            imageVector = Icons.Outlined.LightMode,
                            contentDescription = null,
                            tint = colors.primary,
                            modifier = Modifier.size(20.dp),
                        )
                    },
                    iconBackground = colors.secondary,
                    title = "Светлая",
                    subtitle = "Old Money",
                    onClick = { onThemeModeChange(ThemeMode.LIGHT) },
                )

                ThemeOption(
                    isSelected = themeMode == ThemeMode.DARK,
                    icon = {
                        Icon(
                            imageVector = Icons.Outlined.DarkMode,
                            contentDescription = null,
                            tint = colors.primaryForeground,
                            modifier = Modifier.size(20.dp),
                        )
                    },
                    iconBackground = colors.primary,
                    title = "Темная",
                    subtitle = "Evening",
                    onClick = { onThemeModeChange(ThemeMode.DARK) },
                )

                ThemeOption(
                    isSelected = themeMode == ThemeMode.SYSTEM,
                    icon = {
                        Icon(
                            imageVector = Icons.Outlined.SettingsBrightness,
                            contentDescription = null,
                            tint = colors.accent,
                            modifier = Modifier.size(20.dp),
                        )
                    },
                    iconBackground = colors.secondary,
                    title = "Системная",
                    subtitle = "Как в устройстве",
                    onClick = { onThemeModeChange(ThemeMode.SYSTEM) },
                )
            }
        }
    }
}

@Composable
private fun ThemeOption(
    isSelected: Boolean,
    icon: @Composable () -> Unit,
    iconBackground: androidx.compose.ui.graphics.Color,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = AleAppTheme.colors
    val borderColor = if (isSelected) colors.primary else colors.border
    val backgroundColor = if (isSelected) colors.primary.copy(alpha = 0.1f)
    else androidx.compose.ui.graphics.Color.Transparent

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                color = borderColor,
                shape = RoundedCornerShape(12.dp),
            )
            .clickable(onClick = onClick),
        color = backgroundColor,
        shape = RoundedCornerShape(12.dp),
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(
                modifier = Modifier.size(40.dp),
                shape = CircleShape,
                color = iconBackground,
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.fillMaxSize(),
                ) {
                    icon()
                }
            }

            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.Medium,
                    ),
                    color = colors.cardForeground,
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.mutedForeground,
                )
            }

            if (isSelected) {
                RadioIndicator()
            }
        }
    }
}

/* ═══════════════════════════════════════════════════════════════════════════ */
/*  StatusSection                                                             */
/* ═══════════════════════════════════════════════════════════════════════════ */

@Composable
private fun StatusSection(
    currentStatus: UserStatus,
    onStatusChange: (UserStatus) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = AleAppTheme.colors

    AleAppCard(modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(20.dp)
                        .clip(CircleShape)
                        .background(colors.statusOnline),
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "Статус",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                    ),
                    color = colors.cardForeground,
                )
            }

            Spacer(Modifier.height(16.dp))

            // Status options
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                UserStatus.entries.forEach { status ->
                    StatusOption(
                        status = status,
                        isSelected = currentStatus == status,
                        onClick = { onStatusChange(status) },
                    )
                }
            }
        }
    }
}

@Composable
private fun StatusOption(
    status: UserStatus,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = AleAppTheme.colors
    val dotColor = when (status) {
        UserStatus.ONLINE -> colors.statusOnline
        UserStatus.DO_NOT_DISTURB -> colors.statusBusy
        UserStatus.INVISIBLE -> colors.statusOffline
    }
    val borderColor = if (isSelected) colors.primary else colors.border
    val backgroundColor = if (isSelected) colors.primary.copy(alpha = 0.1f)
    else androidx.compose.ui.graphics.Color.Transparent

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                color = borderColor,
                shape = RoundedCornerShape(12.dp),
            )
            .clickable(onClick = onClick),
        color = backgroundColor,
        shape = RoundedCornerShape(12.dp),
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(16.dp)
                    .clip(CircleShape)
                    .background(dotColor),
            )

            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = status.label,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.Medium,
                    ),
                    color = colors.cardForeground,
                )
                Text(
                    text = status.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.mutedForeground,
                )
            }

            if (isSelected) {
                RadioIndicator()
            }
        }
    }
}

@Composable
private fun RadioIndicator(modifier: Modifier = Modifier) {
    val colors = AleAppTheme.colors

    Surface(
        modifier = modifier.size(20.dp),
        shape = CircleShape,
        color = colors.primary,
    ) {
        Box(contentAlignment = Alignment.Center) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(colors.primaryForeground),
            )
        }
    }
}

/* ═══════════════════════════════════════════════════════════════════════════ */
/*  AboutSection                                                              */
/* ═══════════════════════════════════════════════════════════════════════════ */

@Composable
private fun AboutSection(modifier: Modifier = Modifier) {
    val colors = AleAppTheme.colors

    AleAppCard(modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Outlined.Info,
                    contentDescription = null,
                    tint = colors.primary,
                    modifier = Modifier.size(20.dp),
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "О приложении",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                    ),
                    color = colors.cardForeground,
                )
            }

            Spacer(Modifier.height(16.dp))

            // App info
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    modifier = Modifier.size(48.dp),
                    shape = RoundedCornerShape(14.dp),
                    color = colors.primary,
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Filled.Info,
                            contentDescription = null,
                            tint = colors.primaryForeground,
                            modifier = Modifier.size(28.dp),
                        )
                    }
                }

                Spacer(Modifier.width(12.dp))

                Column {
                    Text(
                        text = "CallApp",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                        ),
                        color = colors.cardForeground,
                    )
                    Text(
                        text = "Версия 1.0.0",
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.mutedForeground,
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            Text(
                text = "Приложение для онлайн звонков с организацией вокруг серверов. " +
                        "Общайтесь с коллегами и друзьями в удобном формате.",
                style = MaterialTheme.typography.bodyMedium,
                color = colors.mutedForeground,
            )

            Spacer(Modifier.height(16.dp))

            HorizontalDivider(color = colors.border)

            Spacer(Modifier.height(12.dp))

            // Links
            AboutLink(
                icon = {
                    // GitHub-style icon: primary circle
                    Surface(
                        modifier = Modifier.size(40.dp),
                        shape = CircleShape,
                        color = colors.primary,
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = "GH",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                ),
                                color = colors.primaryForeground,
                            )
                        }
                    }
                },
                title = "GitHub",
                subtitle = "Исходный код проекта",
                onClick = { /* TODO: open GitHub link */ },
            )

            Spacer(Modifier.height(8.dp))

            AboutLink(
                icon = {
                    // Telegram-style icon: accent circle
                    Surface(
                        modifier = Modifier.size(40.dp),
                        shape = CircleShape,
                        color = colors.accent,
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = "TG",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                ),
                                color = colors.accentForeground,
                            )
                        }
                    }
                },
                title = "Telegram",
                subtitle = "Новости и поддержка",
                onClick = { /* TODO: open Telegram link */ },
            )

            Spacer(Modifier.height(16.dp))

            HorizontalDivider(color = colors.border)

            Spacer(Modifier.height(12.dp))

            // Copyright
            Text(
                text = "\u00A9 2026 CallApp. Все права защищены.",
                style = MaterialTheme.typography.labelSmall,
                color = colors.mutedForeground,
                modifier = Modifier.fillMaxWidth(),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            )
        }
    }
}

@Composable
private fun AboutLink(
    icon: @Composable () -> Unit,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = AleAppTheme.colors

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        icon()

        Spacer(Modifier.width(12.dp))

        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.Medium,
                ),
                color = colors.cardForeground,
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = colors.mutedForeground,
            )
        }
    }
}

/* ═══════════════════════════════════════════════════════════════════════════ */
/*  Previews                                                                  */
/* ═══════════════════════════════════════════════════════════════════════════ */

@Preview(name = "SettingsScreen — Light", showBackground = true, showSystemUi = true)
@Composable
private fun SettingsScreenLightPreview() {
    AleAppTheme(darkTheme = false) {
        SettingsScreen(themeMode = ThemeMode.LIGHT)
    }
}

@Preview(
    name = "SettingsScreen — Dark",
    showBackground = true,
    showSystemUi = true,
    uiMode = Configuration.UI_MODE_NIGHT_YES,
)
@Composable
private fun SettingsScreenDarkPreview() {
    AleAppTheme(darkTheme = true) {
        SettingsScreen(themeMode = ThemeMode.DARK, userStatus = UserStatus.DO_NOT_DISTURB)
    }
}

@Preview(name = "SettingsScreen — System theme", showBackground = true, showSystemUi = true)
@Composable
private fun SettingsScreenSystemPreview() {
    AleAppTheme(darkTheme = false) {
        SettingsScreen(themeMode = ThemeMode.SYSTEM)
    }
}

@Preview(name = "ThemeSection — Light selected", showBackground = true)
@Composable
private fun ThemeSectionLightPreview() {
    AleAppTheme(darkTheme = false) {
        Surface(color = AleAppTheme.colors.background) {
            ThemeSection(
                themeMode = ThemeMode.LIGHT,
                onThemeModeChange = {},
                modifier = Modifier.padding(16.dp),
            )
        }
    }
}

@Preview(name = "ThemeSection — System selected", showBackground = true)
@Composable
private fun ThemeSectionSystemPreview() {
    AleAppTheme(darkTheme = false) {
        Surface(color = AleAppTheme.colors.background) {
            ThemeSection(
                themeMode = ThemeMode.SYSTEM,
                onThemeModeChange = {},
                modifier = Modifier.padding(16.dp),
            )
        }
    }
}

@Preview(name = "ThemeSection — Dark selected", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun ThemeSectionDarkPreview() {
    AleAppTheme(darkTheme = true) {
        Surface(color = AleAppTheme.colors.background) {
            ThemeSection(
                themeMode = ThemeMode.DARK,
                onThemeModeChange = {},
                modifier = Modifier.padding(16.dp),
            )
        }
    }
}

@Preview(name = "StatusSection — Online", showBackground = true)
@Composable
private fun StatusSectionOnlinePreview() {
    AleAppTheme(darkTheme = false) {
        Surface(color = AleAppTheme.colors.background) {
            StatusSection(
                currentStatus = UserStatus.ONLINE,
                onStatusChange = {},
                modifier = Modifier.padding(16.dp),
            )
        }
    }
}

@Preview(name = "StatusSection — DND", showBackground = true)
@Composable
private fun StatusSectionDndPreview() {
    AleAppTheme(darkTheme = false) {
        Surface(color = AleAppTheme.colors.background) {
            StatusSection(
                currentStatus = UserStatus.DO_NOT_DISTURB,
                onStatusChange = {},
                modifier = Modifier.padding(16.dp),
            )
        }
    }
}

@Preview(name = "StatusSection — Dark", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun StatusSectionDarkPreview() {
    AleAppTheme(darkTheme = true) {
        Surface(color = AleAppTheme.colors.background) {
            StatusSection(
                currentStatus = UserStatus.INVISIBLE,
                onStatusChange = {},
                modifier = Modifier.padding(16.dp),
            )
        }
    }
}

@Preview(name = "AboutSection — Light", showBackground = true)
@Composable
private fun AboutSectionLightPreview() {
    AleAppTheme(darkTheme = false) {
        Surface(color = AleAppTheme.colors.background) {
            AboutSection(modifier = Modifier.padding(16.dp))
        }
    }
}

@Preview(name = "AboutSection — Dark", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun AboutSectionDarkPreview() {
    AleAppTheme(darkTheme = true) {
        Surface(color = AleAppTheme.colors.background) {
            AboutSection(modifier = Modifier.padding(16.dp))
        }
    }
}

@Preview(name = "StatusOption — selected", showBackground = true)
@Composable
private fun StatusOptionSelectedPreview() {
    AleAppTheme(darkTheme = false) {
        Surface(color = AleAppTheme.colors.background) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                StatusOption(
                    status = UserStatus.ONLINE,
                    isSelected = true,
                    onClick = {},
                )
                StatusOption(
                    status = UserStatus.DO_NOT_DISTURB,
                    isSelected = false,
                    onClick = {},
                )
            }
        }
    }
}

@Preview(name = "ThemeOption — variants", showBackground = true)
@Composable
private fun ThemeOptionPreview() {
    AleAppTheme(darkTheme = false) {
        val colors = AleAppTheme.colors
        Surface(color = AleAppTheme.colors.background) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                ThemeOption(
                    isSelected = true,
                    icon = {
                        Icon(
                            Icons.Outlined.LightMode, null,
                            tint = colors.primary,
                            modifier = Modifier.size(20.dp),
                        )
                    },
                    iconBackground = colors.secondary,
                    title = "Светлая",
                    subtitle = "Old Money",
                    onClick = {},
                )
                ThemeOption(
                    isSelected = false,
                    icon = {
                        Icon(
                            Icons.Outlined.DarkMode, null,
                            tint = colors.primaryForeground,
                            modifier = Modifier.size(20.dp),
                        )
                    },
                    iconBackground = colors.primary,
                    title = "Темная",
                    subtitle = "Evening",
                    onClick = {},
                )
                ThemeOption(
                    isSelected = false,
                    icon = {
                        Icon(
                            Icons.Outlined.SettingsBrightness, null,
                            tint = colors.accent,
                            modifier = Modifier.size(20.dp),
                        )
                    },
                    iconBackground = colors.secondary,
                    title = "Системная",
                    subtitle = "Как в устройстве",
                    onClick = {},
                )
            }
        }
    }
}
