package com.callapp.android.ui.screens.call

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.PhoneDisabled
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.callapp.android.ui.components.PulsingAvatar
import com.callapp.android.ui.theme.AleAppTheme

/* ═══════════════════════════════════════════════════════════════════════════ */
/*  Data                                                                      */
/* ═══════════════════════════════════════════════════════════════════════════ */

enum class CallType(val label: String) {
    VOICE("Голосовой звонок"),
    VIDEO("Видеозвонок"),
}

/* ═══════════════════════════════════════════════════════════════════════════ */
/*  IncomingCallScreen                                                        */
/* ═══════════════════════════════════════════════════════════════════════════ */

@Composable
fun IncomingCallScreen(
    contactName: String,
    contactInitials: String = contactName.take(2).uppercase(),
    serverName: String = "",
    callType: CallType = CallType.VOICE,
    onAccept: () -> Unit = {},
    onDecline: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val colors = AleAppTheme.colors

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.background)
            .windowInsetsPadding(WindowInsets.statusBars)
            .padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(1.dp)
                .testTag("incoming_call_type_marker_${callType.name.lowercase()}"),
        )

        // ── Top: avatar + info ──────────────────────────────────────────────
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            // Pulsing avatar
            PulsingAvatar(
                initials = contactInitials,
                isPulsing = true,
                accentColor = colors.accent,
                backgroundColor = colors.secondary,
                textColor = colors.foreground,
            )

            Spacer(Modifier.height(16.dp))

            // Contact name
            Text(
                text = contactName,
                style = MaterialTheme.typography.displaySmall.copy(
                    fontWeight = FontWeight.Bold,
                ),
                color = colors.foreground,
                textAlign = TextAlign.Center,
            )

            // Server name
            if (serverName.isNotBlank()) {
                Spacer(Modifier.height(4.dp))
                Text(
                    text = serverName,
                    style = MaterialTheme.typography.titleMedium,
                    color = colors.mutedForeground,
                    textAlign = TextAlign.Center,
                )
            }

            Spacer(Modifier.height(12.dp))

            // Call type badge
            CallTypeBadge(callType = callType)

            Spacer(Modifier.height(32.dp))

            // "Входящий вызов..." text
            Text(
                text = "Входящий вызов...",
                style = MaterialTheme.typography.bodyLarge,
                color = colors.mutedForeground,
            )
        }

        // ── Bottom: accept/decline buttons ──────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(bottom = 32.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Decline
            ActionButton(
                label = "Отклонить",
                backgroundColor = colors.callDecline,
                iconTint = colors.destructiveForeground,
                labelColor = colors.foreground,
                icon = { tint ->
                    Icon(
                        imageVector = Icons.Filled.PhoneDisabled,
                        contentDescription = "Отклонить звонок",
                        tint = tint,
                        modifier = Modifier.size(32.dp),
                    )
                },
                onClick = onDecline,
                modifier = Modifier.testTag("incoming_call_decline_button"),
            )

            // Accept
            ActionButton(
                label = "Принять",
                backgroundColor = colors.callAccept,
                iconTint = colors.destructiveForeground,
                labelColor = colors.foreground,
                icon = { tint ->
                    Icon(
                        imageVector = Icons.Filled.Call,
                        contentDescription = "Принять звонок",
                        tint = tint,
                        modifier = Modifier.size(32.dp),
                    )
                },
                onClick = onAccept,
                modifier = Modifier.testTag("incoming_call_accept_button"),
            )
        }

        // Helper text
        Text(
            text = "Нажмите кнопку для ответа на вызов",
            style = MaterialTheme.typography.labelSmall,
            color = colors.mutedForeground,
            modifier = Modifier
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(bottom = 16.dp),
        )
    }
}

/* ═══════════════════════════════════════════════════════════════════════════ */
/*  CallTypeBadge                                                             */
/* ═══════════════════════════════════════════════════════════════════════════ */

@Composable
private fun CallTypeBadge(
    callType: CallType,
    modifier: Modifier = Modifier,
) {
    val colors = AleAppTheme.colors

    Surface(
        modifier = modifier.testTag("incoming_call_type_${callType.name.lowercase()}"),
        shape = RoundedCornerShape(50),
        color = colors.card,
        border = androidx.compose.foundation.BorderStroke(1.dp, colors.border),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Icon(
                imageVector = if (callType == CallType.VIDEO) Icons.Filled.Videocam
                else Icons.Filled.Call,
                contentDescription = null,
                tint = colors.primary,
                modifier = Modifier
                    .size(20.dp)
                    .testTag("incoming_call_icon_${callType.name.lowercase()}"),
            )
            Text(
                text = callType.label,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.Medium,
                ),
                color = colors.cardForeground,
                modifier = Modifier.testTag("incoming_call_type_${callType.name.lowercase()}"),
            )
        }
    }
}

/* ═══════════════════════════════════════════════════════════════════════════ */
/*  ActionButton                                                              */
/* ═══════════════════════════════════════════════════════════════════════════ */

@Composable
private fun ActionButton(
    label: String,
    backgroundColor: Color,
    iconTint: Color,
    labelColor: Color,
    icon: @Composable (Color) -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Surface(
            modifier = Modifier
                .size(72.dp)
                .clip(CircleShape)
                .clickable(onClick = onClick),
            shape = CircleShape,
            color = backgroundColor,
            shadowElevation = 8.dp,
        ) {
            Box(contentAlignment = Alignment.Center) {
                icon(iconTint)
            }
        }

        Spacer(Modifier.height(12.dp))

        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge.copy(
                fontWeight = FontWeight.Medium,
            ),
            color = labelColor,
        )
    }
}

/* ═══════════════════════════════════════════════════════════════════════════ */
/*  Previews                                                                  */
/* ═══════════════════════════════════════════════════════════════════════════ */

@Preview(name = "IncomingCall — Light Voice", showBackground = true, showSystemUi = true)
@Composable
private fun IncomingCallLightVoicePreview() {
    AleAppTheme(darkTheme = false) {
        IncomingCallScreen(
            contactName = "Наталья Попова",
            serverName = "Creative Studio",
            callType = CallType.VOICE,
        )
    }
}

@Preview(name = "IncomingCall — Light Video", showBackground = true, showSystemUi = true)
@Composable
private fun IncomingCallLightVideoPreview() {
    AleAppTheme(darkTheme = false) {
        IncomingCallScreen(
            contactName = "Алексей Козлов",
            serverName = "Tech Community",
            callType = CallType.VIDEO,
        )
    }
}

@Preview(
    name = "IncomingCall — Dark Voice",
    showBackground = true,
    showSystemUi = true,
    uiMode = Configuration.UI_MODE_NIGHT_YES,
)
@Composable
private fun IncomingCallDarkVoicePreview() {
    AleAppTheme(darkTheme = true) {
        IncomingCallScreen(
            contactName = "Елена Иванова",
            serverName = "Music Production",
            callType = CallType.VOICE,
        )
    }
}

@Preview(
    name = "IncomingCall — Dark Video",
    showBackground = true,
    showSystemUi = true,
    uiMode = Configuration.UI_MODE_NIGHT_YES,
)
@Composable
private fun IncomingCallDarkVideoPreview() {
    AleAppTheme(darkTheme = true) {
        IncomingCallScreen(
            contactName = "Дмитрий Петров",
            serverName = "Game Dev Hub",
            callType = CallType.VIDEO,
        )
    }
}

@Preview(name = "CallTypeBadge — Voice", showBackground = true)
@Composable
private fun CallTypeBadgeVoicePreview() {
    AleAppTheme(darkTheme = false) {
        Surface(color = AleAppTheme.colors.background) {
            CallTypeBadge(
                callType = CallType.VOICE,
                modifier = Modifier.padding(16.dp),
            )
        }
    }
}

@Preview(name = "CallTypeBadge — Video", showBackground = true)
@Composable
private fun CallTypeBadgeVideoPreview() {
    AleAppTheme(darkTheme = false) {
        Surface(color = AleAppTheme.colors.background) {
            CallTypeBadge(
                callType = CallType.VIDEO,
                modifier = Modifier.padding(16.dp),
            )
        }
    }
}

@Preview(name = "ActionButton — Decline", showBackground = true)
@Composable
private fun ActionButtonDeclinePreview() {
    AleAppTheme(darkTheme = false) {
        val colors = AleAppTheme.colors
        Box(
            modifier = Modifier
                .background(colors.background)
                .padding(24.dp),
        ) {
            ActionButton(
                label = "Отклонить",
                backgroundColor = colors.callDecline,
                iconTint = colors.destructiveForeground,
                labelColor = colors.foreground,
                icon = { tint ->
                    Icon(Icons.Filled.PhoneDisabled, null, tint = tint, modifier = Modifier.size(32.dp))
                },
                onClick = {},
            )
        }
    }
}

@Preview(name = "ActionButton — Accept", showBackground = true)
@Composable
private fun ActionButtonAcceptPreview() {
    AleAppTheme(darkTheme = false) {
        val colors = AleAppTheme.colors
        Box(
            modifier = Modifier
                .background(colors.background)
                .padding(24.dp),
        ) {
            ActionButton(
                label = "Принять",
                backgroundColor = colors.callAccept,
                iconTint = colors.destructiveForeground,
                labelColor = colors.foreground,
                icon = { tint ->
                    Icon(Icons.Filled.Call, null, tint = tint, modifier = Modifier.size(32.dp))
                },
                onClick = {},
            )
        }
    }
}
