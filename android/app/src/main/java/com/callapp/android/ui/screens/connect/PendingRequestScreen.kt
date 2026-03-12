package com.callapp.android.ui.screens.connect

import android.content.res.Configuration
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import com.callapp.android.ui.theme.AleAppTheme

/* ═══════════════════════════════════════════════════════════════════════════ */
/*  Data                                                                      */
/* ═══════════════════════════════════════════════════════════════════════════ */

enum class RequestStatus { PENDING, APPROVED, REJECTED }

/* ═══════════════════════════════════════════════════════════════════════════ */
/*  PendingRequestScreen                                                      */
/* ═══════════════════════════════════════════════════════════════════════════ */

@Composable
fun PendingRequestScreen(
    serverName: String = "Server",
    userName: String = "user",
    status: RequestStatus = RequestStatus.PENDING,
    onBackToHome: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val colors = AleAppTheme.colors

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            AleAppCard(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    // Status icon
                    StatusIcon(status = status)

                    Spacer(Modifier.height(24.dp))

                    // Title
                    Text(
                        text = when (status) {
                            RequestStatus.PENDING -> "Заявка отправлена"
                            RequestStatus.APPROVED -> "Заявка одобрена!"
                            RequestStatus.REJECTED -> "Заявка отклонена"
                        },
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.Bold,
                        ),
                        color = colors.foreground,
                        textAlign = TextAlign.Center,
                    )

                    Spacer(Modifier.height(8.dp))

                    // Description
                    StatusDescription(
                        status = status,
                        serverName = serverName,
                    )

                    Spacer(Modifier.height(24.dp))

                    // Profile info box
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        color = colors.secondary,
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                        ) {
                            Text(
                                text = "Ваш профиль",
                                style = MaterialTheme.typography.bodySmall,
                                color = colors.mutedForeground,
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = userName,
                                style = MaterialTheme.typography.bodyLarge.copy(
                                    fontWeight = FontWeight.Medium,
                                ),
                                color = colors.cardForeground,
                            )
                        }
                    }

                    Spacer(Modifier.height(24.dp))

                    // Action button
                    AleAppButton(
                        onClick = onBackToHome,
                        variant = AleAppButtonVariant.Primary,
                        size = AleAppButtonSize.Large,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(
                                imageVector = Icons.Default.Home,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp),
                            )
                            Spacer(Modifier.width(8.dp))
                            Text("Вернуться на главную")
                        }
                    }

                    // Helper text (pending only)
                    if (status == RequestStatus.PENDING) {
                        Spacer(Modifier.height(16.dp))
                        Text(
                            text = "Проверить статус заявки можно в разделе уведомлений",
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.mutedForeground,
                            textAlign = TextAlign.Center,
                        )
                    }
                }
            }
        }
    }
}

/* ═══════════════════════════════════════════════════════════════════════════ */
/*  Status icon                                                               */
/* ═══════════════════════════════════════════════════════════════════════════ */

@Composable
private fun StatusIcon(
    status: RequestStatus,
    modifier: Modifier = Modifier,
) {
    val colors = AleAppTheme.colors

    val (bgColor, iconTint, icon) = when (status) {
        RequestStatus.PENDING -> Triple(
            colors.accent.copy(alpha = 0.2f),
            colors.accent,
            Icons.Default.Schedule,
        )
        RequestStatus.APPROVED -> Triple(
            colors.statusOnline.copy(alpha = 0.2f),
            colors.statusOnline,
            Icons.Default.CheckCircle,
        )
        RequestStatus.REJECTED -> Triple(
            colors.destructive.copy(alpha = 0.2f),
            colors.destructive,
            Icons.Default.Cancel,
        )
    }

    Surface(
        modifier = modifier.size(80.dp),
        shape = CircleShape,
        color = bgColor,
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(40.dp),
                tint = iconTint,
            )
        }
    }
}

/* ═══════════════════════════════════════════════════════════════════════════ */
/*  Status description                                                        */
/* ═══════════════════════════════════════════════════════════════════════════ */

@Composable
private fun StatusDescription(
    status: RequestStatus,
    serverName: String,
    modifier: Modifier = Modifier,
) {
    val colors = AleAppTheme.colors

    val text = when (status) {
        RequestStatus.PENDING -> buildAnnotatedString {
            append("Ваша заявка на вступление в сервер ")
            withStyle(SpanStyle(fontWeight = FontWeight.SemiBold)) {
                append(serverName)
            }
            append(" отправлена администратору.\n\nВы получите уведомление, когда администратор рассмотрит вашу заявку.")
        }
        RequestStatus.APPROVED -> buildAnnotatedString {
            append("Администратор одобрил вашу заявку на вступление в сервер ")
            withStyle(SpanStyle(fontWeight = FontWeight.SemiBold)) {
                append(serverName)
            }
            append("!\n\nТеперь вы можете пользоваться всеми возможностями сервера.")
        }
        RequestStatus.REJECTED -> buildAnnotatedString {
            append("Администратор отклонил вашу заявку на вступление в сервер ")
            withStyle(SpanStyle(fontWeight = FontWeight.SemiBold)) {
                append(serverName)
            }
            append(".\n\nВы можете попробовать подать заявку снова позже.")
        }
    }

    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        color = colors.mutedForeground,
        textAlign = TextAlign.Center,
        modifier = modifier,
    )
}

/* ═══════════════════════════════════════════════════════════════════════════ */
/*  Previews                                                                  */
/* ═══════════════════════════════════════════════════════════════════════════ */

// ── Full screen ─────────────────────────────────────────────────────────────

@Preview(name = "Pending — Light", showBackground = true, showSystemUi = true)
@Composable
private fun PendingLightPreview() {
    AleAppTheme(darkTheme = false) {
        Surface(color = AleAppTheme.colors.background) {
            PendingRequestScreen(
                serverName = "Tech Community",
                userName = "@alex_tech",
                status = RequestStatus.PENDING,
            )
        }
    }
}

@Preview(
    name = "Pending — Dark",
    showBackground = true,
    showSystemUi = true,
    uiMode = Configuration.UI_MODE_NIGHT_YES,
)
@Composable
private fun PendingDarkPreview() {
    AleAppTheme(darkTheme = true) {
        Surface(color = AleAppTheme.colors.background) {
            PendingRequestScreen(
                serverName = "Tech Community",
                userName = "@alex_tech",
                status = RequestStatus.PENDING,
            )
        }
    }
}

@Preview(name = "Approved — Light", showBackground = true, showSystemUi = true)
@Composable
private fun ApprovedLightPreview() {
    AleAppTheme(darkTheme = false) {
        Surface(color = AleAppTheme.colors.background) {
            PendingRequestScreen(
                serverName = "Game Dev Hub",
                userName = "@dmitry_dev",
                status = RequestStatus.APPROVED,
            )
        }
    }
}

@Preview(name = "Rejected — Light", showBackground = true, showSystemUi = true)
@Composable
private fun RejectedLightPreview() {
    AleAppTheme(darkTheme = false) {
        Surface(color = AleAppTheme.colors.background) {
            PendingRequestScreen(
                serverName = "Creative Studio",
                userName = "@maria_v",
                status = RequestStatus.REJECTED,
            )
        }
    }
}

// ── Component previews ──────────────────────────────────────────────────────

@Preview(name = "StatusIcon — Pending", showBackground = true)
@Composable
private fun StatusIconPendingPreview() {
    AleAppTheme(darkTheme = false) {
        Surface(color = AleAppTheme.colors.card) {
            StatusIcon(
                status = RequestStatus.PENDING,
                modifier = Modifier.padding(24.dp),
            )
        }
    }
}

@Preview(name = "StatusIcon — Approved", showBackground = true)
@Composable
private fun StatusIconApprovedPreview() {
    AleAppTheme(darkTheme = false) {
        Surface(color = AleAppTheme.colors.card) {
            StatusIcon(
                status = RequestStatus.APPROVED,
                modifier = Modifier.padding(24.dp),
            )
        }
    }
}

@Preview(name = "StatusIcon — Rejected", showBackground = true)
@Composable
private fun StatusIconRejectedPreview() {
    AleAppTheme(darkTheme = false) {
        Surface(color = AleAppTheme.colors.card) {
            StatusIcon(
                status = RequestStatus.REJECTED,
                modifier = Modifier.padding(24.dp),
            )
        }
    }
}
