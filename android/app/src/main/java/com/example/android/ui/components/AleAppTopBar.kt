package com.example.android.ui.components

import android.content.res.Configuration
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.RowScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.example.android.ui.theme.AleAppTheme

/**
 * Верхняя панель CallApp.
 *
 * Из Header.tsx / экранных хедеров:
 *  - border-b  bg-card  shadow-sm
 *  - Заголовок: text-2xl font-semibold text-foreground  (headlineMedium)
 *  - Кнопка «Назад»: ArrowBack, text-foreground
 *  - Иконки действий: text-muted-foreground
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AleAppTopBar(
    title: String,
    modifier: Modifier = Modifier,
    onBackClick: (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
) {
    val colors = AleAppTheme.colors

    Column(modifier = modifier) {
        TopAppBar(
            title = {
                Text(
                    text = title,
                    style = MaterialTheme.typography.headlineMedium,
                )
            },
            navigationIcon = {
                if (onBackClick != null) {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Назад",
                        )
                    }
                }
            },
            actions = actions,
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = colors.card,
                scrolledContainerColor = colors.card,
                titleContentColor = colors.foreground,
                navigationIconContentColor = colors.foreground,
                actionIconContentColor = colors.mutedForeground,
            ),
        )
        HorizontalDivider(color = colors.border)
    }
}

// ── Previews ─────────────────────────────────────────────────────────────────

@Preview(name = "TopBar — с кнопкой Назад", showBackground = true)
@Composable
private fun TopBarWithBackPreview() {
    AleAppTheme(darkTheme = false) {
        AleAppTopBar(
            title = "Мой профиль",
            onBackClick = {},
        )
    }
}

@Preview(name = "TopBar — с действиями", showBackground = true)
@Composable
private fun TopBarWithActionsPreview() {
    AleAppTheme(darkTheme = false) {
        AleAppTopBar(
            title = "CallApp",
            actions = {
                IconButton(onClick = {}) {
                    BadgedBox(badge = { Badge { Text("3") } }) {
                        Icon(Icons.Default.Notifications, contentDescription = "Уведомления")
                    }
                }
                IconButton(onClick = {}) {
                    Icon(Icons.Default.Settings, contentDescription = "Настройки")
                }
            },
        )
    }
}

@Preview(name = "TopBar — Dark", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun TopBarDarkPreview() {
    AleAppTheme(darkTheme = true) {
        AleAppTopBar(
            title = "Настройки",
            onBackClick = {},
            actions = {
                IconButton(onClick = {}) {
                    Icon(Icons.Default.Settings, contentDescription = "Настройки")
                }
            },
        )
    }
}
