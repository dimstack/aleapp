package com.callapp.android.ui.screens.profile

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.callapp.android.ui.components.AleAppButton
import com.callapp.android.ui.components.AleAppButtonSize
import com.callapp.android.ui.components.AleAppButtonVariant
import com.callapp.android.ui.components.AleAppCard
import com.callapp.android.ui.theme.AleAppTheme

/* ═══════════════════════════════════════════════════════════════════════════ */
/*  Data                                                                      */
/* ═══════════════════════════════════════════════════════════════════════════ */

data class UserProfileData(
    val userId: String,
    val name: String,
    val username: String,
    val serverName: String,
    val isAdmin: Boolean,
    val isFavorite: Boolean,
)

/* ═══════════════════════════════════════════════════════════════════════════ */
/*  Mock data                                                                 */
/* ═══════════════════════════════════════════════════════════════════════════ */

internal val sampleUserProfileFavorite = UserProfileData(
    userId = "u1",
    name = "Анна Смирнова",
    username = "anna_s",
    serverName = "Tech Community",
    isAdmin = false,
    isFavorite = true,
)

internal val sampleUserProfileNotFavorite = UserProfileData(
    userId = "u2",
    name = "Алексей Козлов",
    username = "alexey_k",
    serverName = "Tech Community",
    isAdmin = false,
    isFavorite = false,
)

/* ═══════════════════════════════════════════════════════════════════════════ */
/*  UserProfileScreen                                                         */
/* ═══════════════════════════════════════════════════════════════════════════ */

@Composable
fun UserProfileScreen(
    user: UserProfileData = sampleUserProfileNotFavorite,
    onBack: () -> Unit = {},
    onToggleFavorite: (String) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val colors = AleAppTheme.colors
    var isFavorite by remember { mutableStateOf(user.isFavorite) }

    Scaffold(
        containerColor = colors.background,
        topBar = {
            UserProfileTopBar(onBack = onBack)
        },
    ) { padding ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(32.dp))

            // ── Avatar ────────────────────────────────────────────────────
            ProfileAvatar(
                name = user.name,
                size = 128.dp,
            )

            Spacer(Modifier.height(16.dp))

            // ── Name + username ───────────────────────────────────────────
            Text(
                text = user.name,
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontWeight = FontWeight.SemiBold,
                ),
                color = colors.foreground,
            )

            Spacer(Modifier.height(4.dp))

            Text(
                text = "@${user.username}",
                style = MaterialTheme.typography.bodyMedium,
                color = colors.mutedForeground,
            )

            Spacer(Modifier.height(20.dp))

            // ── Favorite toggle button ────────────────────────────────────
            AleAppButton(
                onClick = {
                    isFavorite = !isFavorite
                    onToggleFavorite(user.userId)
                },
                variant = if (isFavorite) AleAppButtonVariant.Outline else AleAppButtonVariant.Primary,
                size = AleAppButtonSize.Default,
            ) {
                Icon(
                    imageVector = if (isFavorite) Icons.Default.Star else Icons.Default.StarBorder,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = if (isFavorite) colors.accent else colors.primaryForeground,
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = if (isFavorite) "Удалить из избранного" else "Добавить в избранное",
                )
            }

            Spacer(Modifier.height(24.dp))

            // ── Server + Role card ────────────────────────────────────────
            AleAppCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    // Server
                    Column {
                        Text(
                            text = "Сервер",
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontWeight = FontWeight.Medium,
                            ),
                            color = colors.mutedForeground,
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = user.serverName,
                            style = MaterialTheme.typography.bodyLarge,
                            color = colors.cardForeground,
                        )
                    }

                    // Role
                    Column {
                        Text(
                            text = "Роль",
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontWeight = FontWeight.Medium,
                            ),
                            color = colors.mutedForeground,
                        )
                        Spacer(Modifier.height(4.dp))
                        RoleBadge(isAdmin = user.isAdmin)
                    }
                }
            }

            Spacer(Modifier.height(32.dp))
        }
    }
}

/* ═══════════════════════════════════════════════════════════════════════════ */
/*  Top bar                                                                   */
/* ═══════════════════════════════════════════════════════════════════════════ */

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun UserProfileTopBar(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = AleAppTheme.colors

    Column(modifier) {
        TopAppBar(
            title = {
                Text(
                    text = "Профиль пользователя",
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
/*  Previews                                                                  */
/* ═══════════════════════════════════════════════════════════════════════════ */

@Preview(name = "UserProfile — not favorite, Light", showBackground = true, showSystemUi = true)
@Composable
private fun UserProfileNotFavLightPreview() {
    AleAppTheme(darkTheme = false) {
        UserProfileScreen(user = sampleUserProfileNotFavorite)
    }
}

@Preview(
    name = "UserProfile — not favorite, Dark",
    showBackground = true,
    showSystemUi = true,
    uiMode = Configuration.UI_MODE_NIGHT_YES,
)
@Composable
private fun UserProfileNotFavDarkPreview() {
    AleAppTheme(darkTheme = true) {
        UserProfileScreen(user = sampleUserProfileNotFavorite)
    }
}

@Preview(name = "UserProfile — favorite, Light", showBackground = true, showSystemUi = true)
@Composable
private fun UserProfileFavLightPreview() {
    AleAppTheme(darkTheme = false) {
        UserProfileScreen(user = sampleUserProfileFavorite)
    }
}

@Preview(
    name = "UserProfile — favorite, Dark",
    showBackground = true,
    showSystemUi = true,
    uiMode = Configuration.UI_MODE_NIGHT_YES,
)
@Composable
private fun UserProfileFavDarkPreview() {
    AleAppTheme(darkTheme = true) {
        UserProfileScreen(user = sampleUserProfileFavorite)
    }
}

@Preview(name = "UserProfileTopBar — Light", showBackground = true)
@Composable
private fun UserTopBarLightPreview() {
    AleAppTheme(darkTheme = false) {
        UserProfileTopBar(onBack = {})
    }
}

@Preview(name = "UserProfileTopBar — Dark", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun UserTopBarDarkPreview() {
    AleAppTheme(darkTheme = true) {
        UserProfileTopBar(onBack = {})
    }
}
