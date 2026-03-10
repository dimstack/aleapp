package com.example.android.ui.screens.connect

import android.content.res.Configuration
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.android.ui.components.AleAppButton
import com.example.android.ui.components.AleAppButtonSize
import com.example.android.ui.components.AleAppButtonVariant
import com.example.android.ui.components.AleAppCard
import com.example.android.ui.components.FormField
import com.example.android.ui.theme.AleAppTheme

/* ═══════════════════════════════════════════════════════════════════════════ */
/*  CreateProfileScreen                                                       */
/* ═══════════════════════════════════════════════════════════════════════════ */

@Composable
fun CreateProfileScreen(
    serverName: String = "New Server",
    onCreateProfile: (username: String, name: String, avatarUrl: String?) -> Unit =
        { _, _, _ -> },
    modifier: Modifier = Modifier,
) {
    val colors = AleAppTheme.colors

    var username by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    var avatarUrl by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 32.dp),
        ) {
            // ── Main card ───────────────────────────────────────────────────
            AleAppCard(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(32.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp),
                ) {
                    // Title
                    ProfileHeader(serverName = serverName)

                    // Avatar
                    ProfileAvatar(name = name)

                    // Username field
                    FormField(
                        label = "Username",
                        required = true,
                        value = username,
                        onValueChange = { username = it; error = null },
                        placeholder = "username",
                        singleLine = true,
                        prefix = "@ ",
                        helperText = "Только буквы, цифры и подчёркивание",
                    )

                    // Name field
                    FormField(
                        label = "Имя",
                        required = true,
                        value = name,
                        onValueChange = { name = it; error = null },
                        placeholder = "Введите ваше имя",
                        singleLine = true,
                    )

                    // Avatar URL field
                    FormField(
                        label = "URL аватара",
                        required = false,
                        value = avatarUrl,
                        onValueChange = { avatarUrl = it },
                        placeholder = "https://example.com/avatar.jpg",
                        singleLine = true,
                    )

                    // Error message
                    if (error != null) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = colors.destructive.copy(alpha = 0.1f),
                            border = BorderStroke(
                                1.dp,
                                colors.destructive.copy(alpha = 0.2f),
                            ),
                        ) {
                            Text(
                                text = error!!,
                                style = MaterialTheme.typography.bodySmall,
                                color = colors.destructive,
                                modifier = Modifier.padding(12.dp),
                            )
                        }
                    }

                    // Create profile button
                    Spacer(Modifier.height(4.dp))
                    AleAppButton(
                        onClick = {
                            when {
                                username.isBlank() -> error = "Username обязателен"
                                !isValidUsername(username.trim()) ->
                                    error = "Username может содержать только буквы, цифры и подчёркивание"
                                name.isBlank() -> error = "Имя обязательно"
                                else -> {
                                    error = null
                                    onCreateProfile(
                                        username.trim(),
                                        name.trim(),
                                        avatarUrl.trim().ifEmpty { null },
                                    )
                                }
                            }
                        },
                        variant = AleAppButtonVariant.Primary,
                        size = AleAppButtonSize.Large,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text("Создать профиль")
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // ── Hint block ──────────────────────────────────────────────────
            ProfileHint(
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

/* ═══════════════════════════════════════════════════════════════════════════ */
/*  Validation                                                                */
/* ═══════════════════════════════════════════════════════════════════════════ */

private val usernamePattern = Regex("^[a-zA-Z0-9_]+$")

private fun isValidUsername(username: String): Boolean =
    usernamePattern.matches(username)

/* ═══════════════════════════════════════════════════════════════════════════ */
/*  Profile header                                                            */
/* ═══════════════════════════════════════════════════════════════════════════ */

@Composable
private fun ProfileHeader(
    serverName: String,
    modifier: Modifier = Modifier,
) {
    val colors = AleAppTheme.colors

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = "Создание профиля",
            style = MaterialTheme.typography.headlineLarge.copy(
                fontWeight = FontWeight.Bold,
            ),
            color = colors.foreground,
            textAlign = TextAlign.Center,
        )

        Spacer(Modifier.height(8.dp))

        Text(
            text = buildAnnotatedString {
                append("Создайте свой профиль для сервера ")
                withStyle(SpanStyle(fontWeight = FontWeight.SemiBold, color = colors.primary)) {
                    append(serverName)
                }
            },
            style = MaterialTheme.typography.bodyMedium,
            color = colors.mutedForeground,
            textAlign = TextAlign.Center,
        )
    }
}

/* ═══════════════════════════════════════════════════════════════════════════ */
/*  Profile avatar                                                            */
/* ═══════════════════════════════════════════════════════════════════════════ */

@Composable
private fun ProfileAvatar(
    name: String,
    modifier: Modifier = Modifier,
) {
    val colors = AleAppTheme.colors
    val initials = if (name.isNotBlank()) {
        name.take(2).uppercase()
    } else {
        "??"
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box {
            // Avatar circle with ring
            Surface(
                modifier = Modifier.size(96.dp),
                shape = CircleShape,
                color = colors.primary,
                contentColor = colors.primaryForeground,
                border = BorderStroke(4.dp, colors.secondary),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = initials,
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                        ),
                        color = Color.White,
                    )
                }
            }

            // Camera button overlay
            Surface(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .offset(x = 2.dp, y = 2.dp)
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
            text = "Загрузите фото профиля",
            style = MaterialTheme.typography.bodySmall,
            color = colors.mutedForeground,
        )
    }
}

/* ═══════════════════════════════════════════════════════════════════════════ */
/*  Hint block                                                                */
/* ═══════════════════════════════════════════════════════════════════════════ */

@Composable
private fun ProfileHint(modifier: Modifier = Modifier) {
    val colors = AleAppTheme.colors

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = colors.primary.copy(alpha = 0.1f),
        border = BorderStroke(1.dp, colors.primary.copy(alpha = 0.2f)),
    ) {
        Text(
            text = "\uD83D\uDCA1 Вы можете создать разные профили для разных серверов",
            style = MaterialTheme.typography.bodySmall,
            color = colors.mutedForeground,
            modifier = Modifier.padding(16.dp),
        )
    }
}

/* ═══════════════════════════════════════════════════════════════════════════ */
/*  Previews                                                                  */
/* ═══════════════════════════════════════════════════════════════════════════ */

@Preview(name = "CreateProfile — Light", showBackground = true, showSystemUi = true)
@Composable
private fun CreateProfileLightPreview() {
    AleAppTheme(darkTheme = false) {
        Surface(color = AleAppTheme.colors.background) {
            CreateProfileScreen(serverName = "New Server 5")
        }
    }
}

@Preview(
    name = "CreateProfile — Dark",
    showBackground = true,
    showSystemUi = true,
    uiMode = Configuration.UI_MODE_NIGHT_YES,
)
@Composable
private fun CreateProfileDarkPreview() {
    AleAppTheme(darkTheme = true) {
        Surface(color = AleAppTheme.colors.background) {
            CreateProfileScreen(serverName = "Tech Community")
        }
    }
}

@Preview(name = "ProfileAvatar — with name", showBackground = true)
@Composable
private fun ProfileAvatarWithNamePreview() {
    AleAppTheme(darkTheme = false) {
        Surface(color = AleAppTheme.colors.card) {
            ProfileAvatar(
                name = "Александр",
                modifier = Modifier.padding(24.dp),
            )
        }
    }
}

@Preview(name = "ProfileAvatar — empty", showBackground = true)
@Composable
private fun ProfileAvatarEmptyPreview() {
    AleAppTheme(darkTheme = false) {
        Surface(color = AleAppTheme.colors.card) {
            ProfileAvatar(
                name = "",
                modifier = Modifier.padding(24.dp),
            )
        }
    }
}

@Preview(name = "ProfileHeader — Light", showBackground = true)
@Composable
private fun ProfileHeaderPreview() {
    AleAppTheme(darkTheme = false) {
        Surface(color = AleAppTheme.colors.card) {
            ProfileHeader(
                serverName = "Tech Community",
                modifier = Modifier.padding(24.dp),
            )
        }
    }
}

@Preview(name = "ProfileHint — Light", showBackground = true)
@Composable
private fun ProfileHintLightPreview() {
    AleAppTheme(darkTheme = false) {
        Surface(color = AleAppTheme.colors.background) {
            ProfileHint(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
            )
        }
    }
}
