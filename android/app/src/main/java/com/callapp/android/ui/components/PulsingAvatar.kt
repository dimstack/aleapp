package com.callapp.android.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.callapp.android.ui.theme.AleAppTheme

@Composable
fun PulsingAvatar(
    initials: String,
    isPulsing: Boolean,
    accentColor: Color,
    backgroundColor: Color,
    textColor: Color,
    modifier: Modifier = Modifier,
    avatarSize: Dp = 128.dp,
) {
    Box(
        modifier = modifier.size(avatarSize * 1.6f),
        contentAlignment = Alignment.Center,
    ) {
        if (isPulsing) {
            val infiniteTransition = rememberInfiniteTransition(label = "pulse")

            val scale1 by infiniteTransition.animateFloat(
                initialValue = 1f,
                targetValue = 1.5f,
                animationSpec = infiniteRepeatable(
                    animation = tween(durationMillis = 1500, easing = LinearEasing),
                    repeatMode = RepeatMode.Restart,
                ),
                label = "ring1_scale",
            )
            val alpha1 by infiniteTransition.animateFloat(
                initialValue = 0.4f,
                targetValue = 0f,
                animationSpec = infiniteRepeatable(
                    animation = tween(durationMillis = 1500, easing = LinearEasing),
                    repeatMode = RepeatMode.Restart,
                ),
                label = "ring1_alpha",
            )

            val scale2 by infiniteTransition.animateFloat(
                initialValue = 1f,
                targetValue = 1.5f,
                animationSpec = infiniteRepeatable(
                    animation = tween(
                        durationMillis = 1500,
                        delayMillis = 500,
                        easing = LinearEasing,
                    ),
                    repeatMode = RepeatMode.Restart,
                ),
                label = "ring2_scale",
            )
            val alpha2 by infiniteTransition.animateFloat(
                initialValue = 0.3f,
                targetValue = 0f,
                animationSpec = infiniteRepeatable(
                    animation = tween(
                        durationMillis = 1500,
                        delayMillis = 500,
                        easing = LinearEasing,
                    ),
                    repeatMode = RepeatMode.Restart,
                ),
                label = "ring2_alpha",
            )

            Box(
                modifier = Modifier
                    .size(avatarSize)
                    .graphicsLayer {
                        scaleX = scale1
                        scaleY = scale1
                        this.alpha = alpha1
                    }
                    .clip(CircleShape)
                    .background(accentColor),
            )

            Box(
                modifier = Modifier
                    .size(avatarSize)
                    .graphicsLayer {
                        scaleX = scale2
                        scaleY = scale2
                        this.alpha = alpha2
                    }
                    .clip(CircleShape)
                    .background(accentColor),
            )
        }

        Surface(
            modifier = Modifier
                .size(avatarSize)
                .border(width = 4.dp, color = accentColor, shape = CircleShape),
            shape = CircleShape,
            color = backgroundColor,
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = initials,
                    style = MaterialTheme.typography.headlineLarge,
                    color = textColor,
                )
            }
        }
    }
}

/* ═══════════════════════════════════════════════════════════════════════════ */
/*  Previews                                                                  */
/* ═══════════════════════════════════════════════════════════════════════════ */

@Preview(name = "PulsingAvatar — pulsing", showBackground = true)
@Composable
private fun PulsingAvatarPulsingPreview() {
    AleAppTheme(darkTheme = false) {
        Box(modifier = Modifier.background(AleAppTheme.colors.background)) {
            PulsingAvatar(
                initials = "АК",
                isPulsing = true,
                accentColor = AleAppTheme.colors.accent,
                backgroundColor = AleAppTheme.colors.secondary,
                textColor = AleAppTheme.colors.foreground,
            )
        }
    }
}

@Preview(name = "PulsingAvatar — static", showBackground = true)
@Composable
private fun PulsingAvatarStaticPreview() {
    AleAppTheme(darkTheme = false) {
        Box(modifier = Modifier.background(AleAppTheme.colors.background)) {
            PulsingAvatar(
                initials = "ЕИ",
                isPulsing = false,
                accentColor = AleAppTheme.colors.accent,
                backgroundColor = AleAppTheme.colors.secondary,
                textColor = AleAppTheme.colors.foreground,
            )
        }
    }
}
