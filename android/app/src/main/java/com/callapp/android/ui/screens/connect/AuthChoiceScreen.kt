package com.callapp.android.ui.screens.connect

import android.content.res.Configuration
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.automirrored.filled.Login
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.callapp.android.ui.components.AleAppCard
import com.callapp.android.ui.theme.AleAppTheme

/* ═══════════════════════════════════════════════════════════════════════════ */
/*  AuthChoiceScreen                                                         */
/* ═══════════════════════════════════════════════════════════════════════════ */

@Composable
fun AuthChoiceScreen(
    serverName: String = "Server",
    onCreateAccount: () -> Unit = {},
    onLogin: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val colors = AleAppTheme.colors

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
                    verticalArrangement = Arrangement.spacedBy(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    // Server icon
                    Surface(
                        modifier = Modifier.size(64.dp),
                        shape = CircleShape,
                        color = colors.primary.copy(alpha = 0.1f),
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Dns,
                                contentDescription = null,
                                modifier = Modifier.size(32.dp),
                                tint = colors.primary,
                            )
                        }
                    }

                    // Title
                    Text(
                        text = "Добро пожаловать",
                        style = MaterialTheme.typography.headlineLarge.copy(
                            fontWeight = FontWeight.Bold,
                        ),
                        color = colors.foreground,
                        textAlign = TextAlign.Center,
                    )

                    // Subtitle with server name
                    Text(
                        text = buildAnnotatedString {
                            append("Вы подключаетесь к серверу ")
                            withStyle(SpanStyle(fontWeight = FontWeight.SemiBold, color = colors.primary)) {
                                append(serverName)
                            }
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.mutedForeground,
                        textAlign = TextAlign.Center,
                    )

                    Spacer(Modifier.height(4.dp))

                    // Create account option
                    AuthChoiceCard(
                        icon = Icons.Default.PersonAdd,
                        title = "Создать аккаунт",
                        description = "Зарегистрировать новый профиль на сервере",
                        onClick = onCreateAccount,
                        modifier = Modifier.testTag("auth_choice_create_account"),
                    )

                    // Login option
                    AuthChoiceCard(
                        icon = Icons.AutoMirrored.Filled.Login,
                        title = "Войти в существующий",
                        description = "Войти с помощью username и пароля",
                        onClick = onLogin,
                        modifier = Modifier.testTag("auth_choice_login"),
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            // ── Hint block ──────────────────────────────────────────────────
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                color = colors.primary.copy(alpha = 0.1f),
                border = BorderStroke(1.dp, colors.primary.copy(alpha = 0.2f)),
            ) {
                Text(
                    text = "\uD83D\uDD12 Для входа в существующий аккаунт вам понадобятся username и пароль, " +
                        "которые вы указали при регистрации",
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.mutedForeground,
                    modifier = Modifier.padding(16.dp),
                )
            }
        }
    }
}

/* ═══════════════════════════════════════════════════════════════════════════ */
/*  AuthChoiceCard                                                           */
/* ═══════════════════════════════════════════════════════════════════════════ */

@Composable
private fun AuthChoiceCard(
    icon: ImageVector,
    title: String,
    description: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = AleAppTheme.colors

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        color = colors.inputBackground,
        border = BorderStroke(1.dp, colors.border),
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(
                modifier = Modifier.size(48.dp),
                shape = CircleShape,
                color = colors.primary.copy(alpha = 0.1f),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp),
                        tint = colors.primary,
                    )
                }
            }

            Spacer(Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Medium,
                    ),
                    color = colors.foreground,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = description,
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

@Preview(name = "AuthChoice — Light", showBackground = true, showSystemUi = true)
@Composable
private fun AuthChoiceLightPreview() {
    AleAppTheme(darkTheme = false) {
        Surface(color = AleAppTheme.colors.background) {
            AuthChoiceScreen(serverName = "Tech Community")
        }
    }
}

@Preview(
    name = "AuthChoice — Dark",
    showBackground = true,
    showSystemUi = true,
    uiMode = Configuration.UI_MODE_NIGHT_YES,
)
@Composable
private fun AuthChoiceDarkPreview() {
    AleAppTheme(darkTheme = true) {
        Surface(color = AleAppTheme.colors.background) {
            AuthChoiceScreen(serverName = "Game Dev Hub")
        }
    }
}

@Preview(name = "AuthChoiceCard — Light", showBackground = true)
@Composable
private fun AuthChoiceCardPreview() {
    AleAppTheme(darkTheme = false) {
        Surface(color = AleAppTheme.colors.card) {
            AuthChoiceCard(
                icon = Icons.Default.PersonAdd,
                title = "Создать аккаунт",
                description = "Зарегистрировать новый профиль на сервере",
                onClick = {},
                modifier = Modifier.padding(16.dp),
            )
        }
    }
}
