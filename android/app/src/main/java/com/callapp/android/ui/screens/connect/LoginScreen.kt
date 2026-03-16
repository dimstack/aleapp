package com.callapp.android.ui.screens.connect

import android.content.res.Configuration
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Login
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.callapp.android.ui.components.AleAppButton
import com.callapp.android.ui.components.AleAppButtonSize
import com.callapp.android.ui.components.AleAppButtonVariant
import com.callapp.android.ui.components.AleAppCard
import com.callapp.android.ui.components.FormField
import com.callapp.android.ui.theme.AleAppTheme

/* ═══════════════════════════════════════════════════════════════════════════ */
/*  LoginScreen                                                              */
/* ═══════════════════════════════════════════════════════════════════════════ */

@Composable
fun LoginScreen(
    serverName: String = "Server",
    initialUsername: String = "",
    initialPassword: String = "",
    triggerSubmitOnLaunch: Boolean = false,
    isLoading: Boolean = false,
    externalError: String? = null,
    onLogin: (username: String, password: String) -> Unit = { _, _ -> },
    modifier: Modifier = Modifier,
) {
    val colors = AleAppTheme.colors

    var username by remember { mutableStateOf(initialUsername) }
    var password by remember { mutableStateOf(initialPassword) }
    var error by remember { mutableStateOf<String?>(null) }

    // Show external error from ViewModel
    val displayError = externalError ?: error

    LaunchedEffect(triggerSubmitOnLaunch, isLoading) {
        if (
            triggerSubmitOnLaunch &&
            !isLoading &&
            username.isNotBlank() &&
            password.isNotBlank()
        ) {
            error = null
            onLogin(username.trim(), password)
        }
    }

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
                    // Header
                    LoginHeader(serverName = serverName)

                    // Username field
                    FormField(
                        label = "Username",
                        required = true,
                        value = username,
                        onValueChange = { username = it; error = null },
                        placeholder = "username",
                        singleLine = true,
                        prefix = "@ ",
                        testTag = "login_username_input",
                        helperText = "Username, указанный при регистрации",
                    )

                    // Password field
                    FormField(
                        label = "Пароль",
                        required = true,
                        value = password,
                        onValueChange = { password = it; error = null },
                        placeholder = "Введите пароль",
                        singleLine = true,
                        isPassword = true,
                        testTag = "login_password_input",
                    )

                    // Error message
                    if (displayError != null) {
                        Surface(
                            modifier = Modifier.testTag("login_error"),
                            shape = RoundedCornerShape(8.dp),
                            color = colors.destructive.copy(alpha = 0.1f),
                            border = BorderStroke(
                                1.dp,
                                colors.destructive.copy(alpha = 0.2f),
                            ),
                        ) {
                            Text(
                                text = displayError,
                                style = MaterialTheme.typography.bodySmall,
                                color = colors.destructive,
                                modifier = Modifier.padding(12.dp),
                            )
                        }
                    }

                    // Login button
                    Spacer(Modifier.height(4.dp))
                    AleAppButton(
                        onClick = {
                            when {
                                username.isBlank() -> error = "Username обязателен"
                                password.isBlank() -> error = "Пароль обязателен"
                                else -> {
                                    error = null
                                    onLogin(username.trim(), password)
                                }
                            }
                        },
                        variant = AleAppButtonVariant.Primary,
                        size = AleAppButtonSize.Large,
                        enabled = !isLoading,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("login_submit_button"),
                    ) {
                        Text(if (isLoading) "Вход..." else "Войти")
                    }
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
                    text = "\uD83D\uDD12 Если вы забыли пароль, обратитесь к администратору сервера",
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.mutedForeground,
                    modifier = Modifier.padding(16.dp),
                )
            }
        }
    }
}

/* ═══════════════════════════════════════════════════════════════════════════ */
/*  LoginHeader                                                              */
/* ═══════════════════════════════════════════════════════════════════════════ */

@Composable
private fun LoginHeader(
    serverName: String,
    modifier: Modifier = Modifier,
) {
    val colors = AleAppTheme.colors

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Surface(
            modifier = Modifier.size(64.dp),
            shape = CircleShape,
            color = colors.primary.copy(alpha = 0.1f),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Login,
                    contentDescription = null,
                    modifier = Modifier.size(32.dp),
                    tint = colors.primary,
                )
            }
        }

        Spacer(Modifier.height(16.dp))

        Text(
            text = "Вход в аккаунт",
            style = MaterialTheme.typography.headlineLarge.copy(
                fontWeight = FontWeight.Bold,
            ),
            color = colors.foreground,
            textAlign = TextAlign.Center,
        )

        Spacer(Modifier.height(8.dp))

        Text(
            text = buildAnnotatedString {
                append("Войдите в существующий аккаунт на сервере ")
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
/*  Previews                                                                  */
/* ═══════════════════════════════════════════════════════════════════════════ */

@Preview(name = "Login — Light", showBackground = true, showSystemUi = true)
@Composable
private fun LoginLightPreview() {
    AleAppTheme(darkTheme = false) {
        Surface(color = AleAppTheme.colors.background) {
            LoginScreen(serverName = "Tech Community")
        }
    }
}

@Preview(
    name = "Login — Dark",
    showBackground = true,
    showSystemUi = true,
    uiMode = Configuration.UI_MODE_NIGHT_YES,
)
@Composable
private fun LoginDarkPreview() {
    AleAppTheme(darkTheme = true) {
        Surface(color = AleAppTheme.colors.background) {
            LoginScreen(serverName = "Game Dev Hub")
        }
    }
}

@Preview(name = "Login — Error", showBackground = true, showSystemUi = true)
@Composable
private fun LoginErrorPreview() {
    AleAppTheme(darkTheme = false) {
        Surface(color = AleAppTheme.colors.background) {
            LoginScreen(
                serverName = "Tech Community",
                externalError = "Неверный username или пароль",
            )
        }
    }
}

@Preview(name = "Login — Loading", showBackground = true, showSystemUi = true)
@Composable
private fun LoginLoadingPreview() {
    AleAppTheme(darkTheme = false) {
        Surface(color = AleAppTheme.colors.background) {
            LoginScreen(
                serverName = "Tech Community",
                isLoading = true,
            )
        }
    }
}

@Preview(name = "LoginHeader — Light", showBackground = true)
@Composable
private fun LoginHeaderPreview() {
    AleAppTheme(darkTheme = false) {
        Surface(color = AleAppTheme.colors.card) {
            LoginHeader(
                serverName = "Tech Community",
                modifier = Modifier.padding(24.dp),
            )
        }
    }
}
