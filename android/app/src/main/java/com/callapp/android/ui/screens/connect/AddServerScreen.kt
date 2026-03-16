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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Dns
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.callapp.android.ui.components.AleAppButton
import com.callapp.android.ui.components.AleAppButtonSize
import com.callapp.android.ui.components.AleAppButtonVariant
import com.callapp.android.ui.components.AleAppCard
import com.callapp.android.ui.components.FormField
import com.callapp.android.ui.theme.AleAppTheme

@Composable
fun AddServerScreen(
    onBack: () -> Unit = {},
    onConnect: (token: String) -> Unit = {},
    isLoading: Boolean = false,
    errorMessage: String? = null,
    modifier: Modifier = Modifier,
) {
    val colors = AleAppTheme.colors

    var token by remember { mutableStateOf("") }
    var localError by remember { mutableStateOf<String?>(null) }
    val error = errorMessage ?: localError

    Scaffold(
        containerColor = colors.background,
        topBar = { AddServerTopBar(onBack = onBack) },
    ) { padding ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState()),
        ) {
            Spacer(Modifier.height(24.dp))

            AleAppCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp),
                ) {
                    ServerIconHeader()

                    FormField(
                        label = "Токен приглашения",
                        required = true,
                        value = token,
                        onValueChange = { token = it; localError = null },
                        placeholder = "server.example.com:3000/ABCD1234",
                        singleLine = true,
                        testTag = "add_server_token_input",
                    )

                    error?.let {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = colors.destructive.copy(alpha = 0.1f),
                            border = BorderStroke(1.dp, colors.destructive.copy(alpha = 0.2f)),
                        ) {
                            Text(
                                text = it,
                                style = MaterialTheme.typography.bodySmall,
                                color = colors.destructive,
                                modifier = Modifier
                                    .padding(12.dp)
                                    .testTag("add_server_error"),
                            )
                        }
                    }

                    Spacer(Modifier.height(4.dp))

                    AleAppButton(
                        onClick = {
                            when {
                                token.isBlank() -> localError = "Токен приглашения обязателен"
                                !isValidInviteToken(token.trim()) -> {
                                    localError = "Неверный формат токена. Ожидается: server:port/CODE"
                                }

                                else -> {
                                    localError = null
                                    onConnect(token.trim())
                                }
                            }
                        },
                        enabled = !isLoading,
                        variant = AleAppButtonVariant.Primary,
                        size = AleAppButtonSize.Large,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("add_server_submit_button"),
                    ) {
                        Text("Подключиться")
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            InfoBlock(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
            )

            Spacer(Modifier.height(32.dp))
        }
    }
}

private val inviteTokenPattern = Regex("""^[a-zA-Z0-9._:-]+/[a-zA-Z0-9]+$""")

private fun isValidInviteToken(token: String): Boolean =
    inviteTokenPattern.matches(token)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddServerTopBar(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = AleAppTheme.colors

    Column(modifier) {
        TopAppBar(
            title = {
                Text(
                    text = "Подключение к серверу",
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

@Composable
private fun ServerIconHeader(modifier: Modifier = Modifier) {
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
                    imageVector = Icons.Default.Dns,
                    contentDescription = null,
                    modifier = Modifier.size(32.dp),
                    tint = colors.primary,
                )
            }
        }

        Spacer(Modifier.height(12.dp))

        Text(
            text = "Вставьте токен приглашения, полученный от администратора сервера.",
            style = MaterialTheme.typography.bodyMedium,
            color = colors.mutedForeground,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun InfoBlock(modifier: Modifier = Modifier) {
    val colors = AleAppTheme.colors

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = colors.primary.copy(alpha = 0.1f),
        border = BorderStroke(1.dp, colors.primary.copy(alpha = 0.2f)),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Как получить токен",
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Medium),
                color = colors.cardForeground,
            )

            Spacer(Modifier.height(8.dp))

            val items = listOf(
                "Попросите администратора сервера создать для вас токен приглашения.",
                "Используйте полный формат: server.example.com:3000/ABCD1234.",
                "Токен может быть одноразовым или многоразовым.",
            )
            items.forEach { item ->
                Text(
                    text = "• $item",
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.mutedForeground,
                    modifier = Modifier.padding(vertical = 2.dp),
                )
            }
        }
    }
}

@Preview(name = "AddServer - Light", showBackground = true, showSystemUi = true)
@Composable
private fun AddServerLightPreview() {
    AleAppTheme(darkTheme = false) {
        AddServerScreen()
    }
}

@Preview(
    name = "AddServer - Dark",
    showBackground = true,
    showSystemUi = true,
    uiMode = Configuration.UI_MODE_NIGHT_YES,
)
@Composable
private fun AddServerDarkPreview() {
    AleAppTheme(darkTheme = true) {
        AddServerScreen()
    }
}

@Preview(name = "ServerIconHeader - Light", showBackground = true)
@Composable
private fun ServerIconHeaderLightPreview() {
    AleAppTheme(darkTheme = false) {
        Surface(color = AleAppTheme.colors.card) {
            ServerIconHeader(modifier = Modifier.padding(24.dp))
        }
    }
}

@Preview(name = "InfoBlock - Light", showBackground = true)
@Composable
private fun InfoBlockLightPreview() {
    AleAppTheme(darkTheme = false) {
        Surface(color = AleAppTheme.colors.background) {
            InfoBlock(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
            )
        }
    }
}

@Preview(name = "InfoBlock - Dark", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun InfoBlockDarkPreview() {
    AleAppTheme(darkTheme = true) {
        Surface(color = AleAppTheme.colors.background) {
            InfoBlock(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
            )
        }
    }
}
