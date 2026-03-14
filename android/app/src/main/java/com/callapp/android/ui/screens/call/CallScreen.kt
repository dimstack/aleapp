package com.callapp.android.ui.screens.call

import android.content.res.Configuration
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.VideocamOff
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.callapp.android.ui.components.PulsingAvatar
import com.callapp.android.ui.theme.AleAppTheme
import io.getstream.webrtc.android.compose.VideoRenderer
import io.getstream.webrtc.android.compose.VideoScalingType
import org.webrtc.EglBase
import org.webrtc.RendererCommon
import org.webrtc.VideoTrack
import kotlin.math.roundToInt

/* ═══════════════════════════════════════════════════════════════════════════ */
/*  Data                                                                      */
/* ═══════════════════════════════════════════════════════════════════════════ */

enum class CallStatus(val label: String) {
    CALLING("Вызов..."),
    RINGING("Звонок..."),
    CONNECTED("Подключено"),
}

/* ═══════════════════════════════════════════════════════════════════════════ */
/*  WebRTC Video Renderer                                                     */
/* ═══════════════════════════════════════════════════════════════════════════ */

@Composable
private fun WebRtcVideoRenderer(
    videoTrack: VideoTrack,
    eglBase: EglBase,
    modifier: Modifier = Modifier,
    scalingType: RendererCommon.ScalingType = RendererCommon.ScalingType.SCALE_ASPECT_FILL,
    mirror: Boolean = false,
) {
    val rendererEvents = remember {
        object : RendererCommon.RendererEvents {
            override fun onFirstFrameRendered() = Unit

            override fun onFrameResolutionChanged(
                videoWidth: Int,
                videoHeight: Int,
                rotation: Int,
            ) = Unit
        }
    }

    VideoRenderer(
        videoTrack = videoTrack,
        eglBaseContext = eglBase.eglBaseContext,
        videoScalingType = scalingType.toVideoScalingType(),
        onTextureViewCreated = { renderer ->
            renderer.setMirror(mirror)
        },
        rendererEvents = rendererEvents,
        modifier = modifier,
    )
}

/* ═══════════════════════════════════════════════════════════════════════════ */
/*  CallScreen                                                                */
/* ═══════════════════════════════════════════════════════════════════════════ */

@Composable
fun CallScreen(
    contactName: String,
    contactInitials: String = contactName.take(2).uppercase(),
    callStatus: CallStatus = CallStatus.CALLING,
    elapsedSeconds: Int = 0,
    isMicOn: Boolean = true,
    isCameraOn: Boolean = false,
    localVideoTrack: VideoTrack? = null,
    remoteVideoTrack: VideoTrack? = null,
    eglBase: EglBase? = null,
    onMicToggle: () -> Unit = {},
    onCameraToggle: () -> Unit = {},
    onSwitchCamera: () -> Unit = {},
    onEndCall: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val colors = AleAppTheme.colors
    var showInfo by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(colors.background),
    ) {
        // ── Remote video (fullscreen background) ─────────────────────────
        if (remoteVideoTrack != null && eglBase != null) {
            key("remote-video-renderer") {
                WebRtcVideoRenderer(
                    videoTrack = remoteVideoTrack,
                    eglBase = eglBase,
                    modifier = Modifier.fillMaxSize(),
                    scalingType = RendererCommon.ScalingType.SCALE_ASPECT_FILL,
                )
            }
        }

        // ── Main UI overlay ──────────────────────────────────────────────
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.statusBars),
        ) {
            // ── Header ───────────────────────────────────────────────────
            Header(
                callStatus = callStatus,
                elapsedSeconds = elapsedSeconds,
                contentColor = if (remoteVideoTrack != null) Color.White else colors.foreground,
                onInfoClick = { showInfo = !showInfo },
            )

            // ── Center: avatar + name (hidden when remote video is active)
            if (remoteVideoTrack == null) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    PulsingAvatar(
                        initials = contactInitials,
                        isPulsing = callStatus != CallStatus.CONNECTED,
                        accentColor = colors.accent,
                        backgroundColor = colors.secondary,
                        textColor = colors.foreground,
                    )

                    Spacer(Modifier.height(24.dp))

                    Text(
                        text = contactName,
                        style = MaterialTheme.typography.headlineLarge.copy(
                            fontWeight = FontWeight.SemiBold,
                        ),
                        color = colors.foreground,
                        textAlign = TextAlign.Center,
                    )

                    Spacer(Modifier.height(8.dp))

                    Text(
                        text = callStatus.label,
                        style = MaterialTheme.typography.bodyLarge,
                        color = colors.mutedForeground,
                    )
                }
            } else {
                Spacer(Modifier.weight(1f))
            }

            // ── Controls ─────────────────────────────────────────────────
            ControlBar(
                isMicOn = isMicOn,
                isCameraOn = isCameraOn,
                colors = colors,
                hasRemoteVideo = remoteVideoTrack != null,
                onMicToggle = onMicToggle,
                onCameraToggle = onCameraToggle,
                onSwitchCamera = onSwitchCamera,
                onEndCall = onEndCall,
                modifier = Modifier.windowInsetsPadding(WindowInsets.navigationBars),
            )
        }

        // ── Local video PiP (draggable, top-end) ─────────────────────────
        if (localVideoTrack != null && eglBase != null && isCameraOn) {
            key("local-video-renderer") {
                DraggableLocalVideo(
                    videoTrack = localVideoTrack,
                    eglBase = eglBase,
                    modifier = Modifier
                        .windowInsetsPadding(WindowInsets.statusBars)
                        .padding(top = 72.dp, end = 16.dp)
                        .align(Alignment.TopEnd),
                )
            }
        }

        // ── Info panel overlay ───────────────────────────────────────────
        if (showInfo) {
            InfoPanel(
                contactName = contactName,
                elapsedSeconds = elapsedSeconds,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .windowInsetsPadding(WindowInsets.statusBars)
                    .padding(top = 64.dp, end = 16.dp),
            )
        }
    }
}

/* ═══════════════════════════════════════════════════════════════════════════ */
/*  DraggableLocalVideo                                                       */
/* ═══════════════════════════════════════════════════════════════════════════ */

@Composable
private fun DraggableLocalVideo(
    videoTrack: VideoTrack,
    eglBase: EglBase,
    modifier: Modifier = Modifier,
) {
    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }

    Box(
        modifier = modifier
            .offset { IntOffset(offsetX.roundToInt(), offsetY.roundToInt()) }
            .width(120.dp)
            .aspectRatio(3f / 4f)
            .clip(RoundedCornerShape(12.dp))
            .background(Color.Black.copy(alpha = 0.18f))
            .pointerInput(Unit) {
                detectDragGestures { change, dragAmount ->
                    change.consume()
                    offsetX += dragAmount.x
                    offsetY += dragAmount.y
                }
            },
    ) {
        WebRtcVideoRenderer(
            videoTrack = videoTrack,
            eglBase = eglBase,
            modifier = Modifier.fillMaxSize(),
            scalingType = RendererCommon.ScalingType.SCALE_ASPECT_FIT,
            mirror = true,
        )
    }
}

/* ═══════════════════════════════════════════════════════════════════════════ */
/*  Header                                                                    */
/* ═══════════════════════════════════════════════════════════════════════════ */

@Composable
private fun Header(
    callStatus: CallStatus,
    elapsedSeconds: Int,
    contentColor: Color,
    onInfoClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column {
            Text(
                text = callStatus.label,
                style = MaterialTheme.typography.bodySmall,
                color = contentColor.copy(alpha = 0.8f),
            )
            if (callStatus == CallStatus.CONNECTED) {
                Text(
                    text = formatDuration(elapsedSeconds),
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Medium,
                    ),
                    color = contentColor,
                )
            }
        }

        Surface(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .clickable(onClick = onInfoClick),
            shape = CircleShape,
            color = contentColor.copy(alpha = 0.1f),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Filled.Info,
                    contentDescription = "Информация о звонке",
                    tint = contentColor,
                    modifier = Modifier.size(20.dp),
                )
            }
        }
    }
}

/* ═══════════════════════════════════════════════════════════════════════════ */
/*  InfoPanel                                                                 */
/* ═══════════════════════════════════════════════════════════════════════════ */

@Composable
private fun InfoPanel(
    contactName: String,
    elapsedSeconds: Int,
    modifier: Modifier = Modifier,
) {
    val colors = AleAppTheme.colors

    Surface(
        modifier = modifier.width(260.dp),
        shape = RoundedCornerShape(12.dp),
        color = colors.card,
        shadowElevation = 8.dp,
        border = BorderStroke(1.dp, colors.border),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Информация о звонке",
                style = MaterialTheme.typography.titleSmall.copy(
                    fontWeight = FontWeight.SemiBold,
                ),
                color = colors.cardForeground,
            )

            Spacer(Modifier.height(12.dp))

            InfoRow("Контакт:", contactName, colors.mutedForeground, colors.cardForeground)
            Spacer(Modifier.height(8.dp))
            InfoRow("Длительность:", formatDuration(elapsedSeconds), colors.mutedForeground, colors.cardForeground)
            Spacer(Modifier.height(8.dp))
            InfoRow(
                label = "Качество:",
                value = "Отличное",
                labelColor = colors.mutedForeground,
                valueColor = colors.statusOnline,
            )
        }
    }
}

@Composable
private fun InfoRow(
    label: String,
    value: String,
    labelColor: Color,
    valueColor: Color,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = labelColor,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
            color = valueColor,
        )
    }
}

/* ═══════════════════════════════════════════════════════════════════════════ */
/*  ControlBar                                                                */
/* ═══════════════════════════════════════════════════════════════════════════ */

@Composable
private fun ControlBar(
    isMicOn: Boolean,
    isCameraOn: Boolean,
    colors: com.callapp.android.ui.theme.AleAppColors,
    hasRemoteVideo: Boolean = false,
    onMicToggle: () -> Unit,
    onCameraToggle: () -> Unit,
    onSwitchCamera: () -> Unit,
    onEndCall: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // When remote video is active, use semi-transparent white buttons for contrast
    val buttonBg = if (hasRemoteVideo) {
        Color.White.copy(alpha = 0.2f)
    } else {
        colors.foreground.copy(alpha = 0.1f)
    }
    val buttonBgDim = if (hasRemoteVideo) {
        Color.White.copy(alpha = 0.1f)
    } else {
        colors.foreground.copy(alpha = 0.05f)
    }
    val iconColor = if (hasRemoteVideo) Color.White else colors.foreground

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 48.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
            // Mic toggle
            ControlButton(
                icon = if (isMicOn) Icons.Filled.Mic else Icons.Filled.MicOff,
                contentDescription = if (isMicOn) "Выключить микрофон" else "Включить микрофон",
                backgroundColor = if (isMicOn) buttonBg else colors.callDecline,
                iconTint = if (isMicOn) iconColor else colors.destructiveForeground,
                onClick = onMicToggle,
            )

            // Camera toggle
            ControlButton(
                icon = if (isCameraOn) Icons.Filled.Videocam else Icons.Filled.VideocamOff,
                contentDescription = if (isCameraOn) "Выключить камеру" else "Включить камеру",
                backgroundColor = if (isCameraOn) buttonBg else buttonBgDim,
                iconTint = iconColor,
                onClick = onCameraToggle,
            )

            // Switch camera
            ControlButton(
                icon = Icons.Filled.Cameraswitch,
                contentDescription = "Переключить камеру",
                backgroundColor = buttonBg,
                iconTint = iconColor,
                onClick = onSwitchCamera,
            )

            // End call
            ControlButton(
                icon = Icons.Filled.CallEnd,
                contentDescription = "Завершить звонок",
                backgroundColor = colors.callDecline,
                iconTint = colors.destructiveForeground,
                onClick = onEndCall,
            )
        }
    }
}

/* ═══════════════════════════════════════════════════════════════════════════ */
/*  ControlButton                                                             */
/* ═══════════════════════════════════════════════════════════════════════════ */

@Composable
private fun ControlButton(
    icon: ImageVector,
    contentDescription: String,
    backgroundColor: Color,
    iconTint: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier
            .size(56.dp)
            .clip(CircleShape)
            .clickable(onClick = onClick),
        shape = CircleShape,
        color = backgroundColor,
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                tint = iconTint,
                modifier = Modifier.size(24.dp),
            )
        }
    }
}

/* ═══════════════════════════════════════════════════════════════════════════ */
/*  Utility                                                                   */
/* ═══════════════════════════════════════════════════════════════════════════ */

private fun formatDuration(totalSeconds: Int): String {
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%02d:%02d".format(minutes, seconds)
}

private fun RendererCommon.ScalingType.toVideoScalingType(): VideoScalingType = when (this) {
    RendererCommon.ScalingType.SCALE_ASPECT_FIT -> VideoScalingType.SCALE_ASPECT_FIT
    RendererCommon.ScalingType.SCALE_ASPECT_FILL -> VideoScalingType.SCALE_ASPECT_FILL
    RendererCommon.ScalingType.SCALE_ASPECT_BALANCED -> VideoScalingType.SCALE_ASPECT_BALANCED
}

/* ═══════════════════════════════════════════════════════════════════════════ */
/*  Previews                                                                  */
/* ═══════════════════════════════════════════════════════════════════════════ */

@Preview(name = "CallScreen — Light Calling", showBackground = true, showSystemUi = true)
@Composable
private fun CallScreenLightCallingPreview() {
    AleAppTheme(darkTheme = false) {
        CallScreen(
            contactName = "Алексей Козлов",
            callStatus = CallStatus.CALLING,
        )
    }
}

@Preview(name = "CallScreen — Light Connected", showBackground = true, showSystemUi = true)
@Composable
private fun CallScreenLightConnectedPreview() {
    AleAppTheme(darkTheme = false) {
        CallScreen(
            contactName = "Елена Иванова",
            callStatus = CallStatus.CONNECTED,
            elapsedSeconds = 143,
        )
    }
}

@Preview(name = "CallScreen — Light Mic off", showBackground = true, showSystemUi = true)
@Composable
private fun CallScreenLightMicOffPreview() {
    AleAppTheme(darkTheme = false) {
        CallScreen(
            contactName = "Дмитрий Петров",
            callStatus = CallStatus.CONNECTED,
            elapsedSeconds = 37,
            isMicOn = false,
        )
    }
}

@Preview(
    name = "CallScreen — Dark Calling",
    showBackground = true,
    showSystemUi = true,
    uiMode = Configuration.UI_MODE_NIGHT_YES,
)
@Composable
private fun CallScreenDarkCallingPreview() {
    AleAppTheme(darkTheme = true) {
        CallScreen(
            contactName = "Алексей Козлов",
            callStatus = CallStatus.CALLING,
        )
    }
}

@Preview(
    name = "CallScreen — Dark Connected",
    showBackground = true,
    showSystemUi = true,
    uiMode = Configuration.UI_MODE_NIGHT_YES,
)
@Composable
private fun CallScreenDarkConnectedPreview() {
    AleAppTheme(darkTheme = true) {
        CallScreen(
            contactName = "Елена Иванова",
            callStatus = CallStatus.CONNECTED,
            elapsedSeconds = 83,
        )
    }
}

@Preview(name = "CallScreen — Light Camera on", showBackground = true, showSystemUi = true)
@Composable
private fun CallScreenLightCameraOnPreview() {
    AleAppTheme(darkTheme = false) {
        CallScreen(
            contactName = "Алексей Козлов",
            callStatus = CallStatus.CONNECTED,
            elapsedSeconds = 45,
            isCameraOn = true,
        )
    }
}

@Preview(name = "ControlBar — Light", showBackground = true)
@Composable
private fun ControlBarLightPreview() {
    AleAppTheme(darkTheme = false) {
        Box(modifier = Modifier.background(AleAppTheme.colors.background)) {
            ControlBar(
                isMicOn = true,
                isCameraOn = false,
                colors = AleAppTheme.colors,
                onMicToggle = {},
                onCameraToggle = {},
                onSwitchCamera = {},
                onEndCall = {},
            )
        }
    }
}

@Preview(name = "ControlBar — Dark Mic off", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun ControlBarDarkMicOffPreview() {
    AleAppTheme(darkTheme = true) {
        Box(modifier = Modifier.background(AleAppTheme.colors.background)) {
            ControlBar(
                isMicOn = false,
                isCameraOn = true,
                colors = AleAppTheme.colors,
                onMicToggle = {},
                onCameraToggle = {},
                onSwitchCamera = {},
                onEndCall = {},
            )
        }
    }
}

@Preview(name = "ControlButton — End call", showBackground = true)
@Composable
private fun ControlButtonEndCallPreview() {
    AleAppTheme(darkTheme = false) {
        val colors = AleAppTheme.colors
        Box(
            modifier = Modifier
                .background(colors.background)
                .padding(16.dp),
        ) {
            ControlButton(
                icon = Icons.Filled.CallEnd,
                contentDescription = "Завершить",
                backgroundColor = colors.callDecline,
                iconTint = colors.destructiveForeground,
                onClick = {},
            )
        }
    }
}

@Preview(name = "InfoPanel — Light", showBackground = true)
@Composable
private fun InfoPanelLightPreview() {
    AleAppTheme(darkTheme = false) {
        Surface(color = AleAppTheme.colors.background) {
            InfoPanel(
                contactName = "Алексей Козлов",
                elapsedSeconds = 143,
                modifier = Modifier.padding(16.dp),
            )
        }
    }
}

@Preview(name = "InfoPanel — Dark", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun InfoPanelDarkPreview() {
    AleAppTheme(darkTheme = true) {
        Surface(color = AleAppTheme.colors.background) {
            InfoPanel(
                contactName = "Елена Иванова",
                elapsedSeconds = 37,
                modifier = Modifier.padding(16.dp),
            )
        }
    }
}
