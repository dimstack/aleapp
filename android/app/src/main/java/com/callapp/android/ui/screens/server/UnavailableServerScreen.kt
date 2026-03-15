package com.callapp.android.ui.screens.server

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.callapp.android.domain.model.Server
import com.callapp.android.domain.model.ServerAvailabilityStatus
import com.callapp.android.ui.preview.PreviewData
import com.callapp.android.ui.theme.AleAppTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UnavailableServerScreen(
    server: Server? = null,
    isLoading: Boolean = false,
    isRefreshing: Boolean = false,
    isRemoving: Boolean = false,
    onBack: () -> Unit = {},
    onRetry: () -> Unit = {},
    onRemove: () -> Unit = {},
) {
    val colors = AleAppTheme.colors

    Scaffold(
        containerColor = colors.background,
        topBar = {
            TopAppBar(
                title = { Text("Сервер недоступен") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = colors.card,
                    titleContentColor = colors.foreground,
                    navigationIconContentColor = colors.foreground,
                ),
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            if (isLoading) {
                CircularProgressIndicator(color = colors.primary)
            } else {
                Text(
                    text = server?.name ?: "Сервер",
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.SemiBold),
                    color = colors.foreground,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = server?.availabilityMessage
                        ?: "Не удалось подключиться к серверу. Проверьте подключение или удалите его из приложения.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = colors.mutedForeground,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(12.dp))
                server?.username?.takeIf { it.isNotBlank() }?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.mutedForeground,
                    )
                }
                Spacer(Modifier.height(32.dp))
                Button(
                    onClick = onRetry,
                    enabled = !isRefreshing && !isRemoving,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    if (isRefreshing) {
                        CircularProgressIndicator(
                            color = colors.primaryForeground,
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                        )
                    } else {
                        Icon(Icons.Default.Refresh, contentDescription = null)
                    }
                    Spacer(Modifier.width(8.dp))
                    Text("Перепроверить подключение")
                }
                Spacer(Modifier.height(12.dp))
                OutlinedButton(
                    onClick = onRemove,
                    enabled = !isRefreshing && !isRemoving,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(Icons.Default.DeleteOutline, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Удалить сервер")
                }
            }
        }
    }
}

@Preview(name = "Unavailable Server - Light", showBackground = true, showSystemUi = true)
@Composable
private fun UnavailableServerLightPreview() {
    AleAppTheme(darkTheme = false) {
        UnavailableServerScreen(
            server = PreviewData.serverTech.copy(
                availabilityStatus = ServerAvailabilityStatus.UNAVAILABLE,
                availabilityMessage = "Сервер недоступен",
            ),
        )
    }
}

@Preview(
    name = "Unavailable Server - Dark",
    showBackground = true,
    showSystemUi = true,
    uiMode = Configuration.UI_MODE_NIGHT_YES,
)
@Composable
private fun UnavailableServerDarkPreview() {
    AleAppTheme(darkTheme = true) {
        UnavailableServerScreen(
            server = PreviewData.serverTech.copy(
                availabilityStatus = ServerAvailabilityStatus.UNAVAILABLE,
                availabilityMessage = "Сервер недоступен",
            ),
        )
    }
}
