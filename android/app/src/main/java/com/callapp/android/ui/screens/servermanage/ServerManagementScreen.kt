package com.callapp.android.ui.screens.servermanage

import android.content.res.Configuration
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Delete
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.callapp.android.ui.components.AleAppButton
import com.callapp.android.ui.components.AleAppButtonSize
import com.callapp.android.ui.components.AleAppButtonVariant
import com.callapp.android.ui.components.AleAppCard
import com.callapp.android.ui.components.FormField
import com.callapp.android.ui.common.displayUsername
import com.callapp.android.ui.common.editableUsername
import com.callapp.android.ui.common.readPickedImage
import com.callapp.android.ui.theme.AleAppTheme
import kotlin.math.absoluteValue

data class ServerManageData(
    val id: String,
    val name: String,
    val username: String,
    val description: String = "",
    val imageUrl: String,
)

internal val sampleManageData = ServerManageData(
    id = "s1",
    name = "Tech Community",
    username = "tech_community",
    description = "Сообщество разработчиков и технических специалистов. Обсуждаем технологии и делимся опытом.",
    imageUrl = "",
)

private val serverImagePalette = listOf(
    Color(0xFF3A5068),
    Color(0xFF5E4B6B),
    Color(0xFF4B6858),
    Color(0xFF6B5B4B),
)

private fun serverImageColor(name: String): Color =
    serverImagePalette[name.hashCode().absoluteValue % serverImagePalette.size]

@Composable
fun ServerManagementScreen(
    initial: ServerManageData = sampleManageData,
    onBack: () -> Unit = {},
    onSave: (name: String, username: String, description: String?, imageUrl: String) -> Unit = { _, _, _, _ -> },
    onUploadImage: (bytes: ByteArray, fileName: String, mimeType: String, onUploaded: (String) -> Unit) -> Unit = { _, _, _, _ -> },
    isUploadingImage: Boolean = false,
    uploadError: String? = null,
    onInviteTokens: () -> Unit = {},
    onPhotoPickerRequest: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val colors = AleAppTheme.colors
    val context = LocalContext.current

    var name by remember { mutableStateOf(initial.name) }
    var username by remember { mutableStateOf(editableUsername(initial.username)) }
    var description by remember { mutableStateOf(initial.description) }
    var imageUrl by remember { mutableStateOf(initial.imageUrl) }
    var error by remember { mutableStateOf<String?>(null) }
    val photoPickerLauncher = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        val pickedImage = uri?.let { context.readPickedImage(it) } ?: return@rememberLauncherForActivityResult
        onUploadImage(pickedImage.bytes, pickedImage.fileName, pickedImage.mimeType) { uploadedUrl ->
            imageUrl = uploadedUrl
        }
    }

    LaunchedEffect(initial.imageUrl) {
        imageUrl = initial.imageUrl
    }

    LaunchedEffect(initial.username) {
        username = editableUsername(initial.username)
    }

    fun requestPhotoPicker() {
        onPhotoPickerRequest?.invoke()
            ?: photoPickerLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
    }

    Scaffold(
        containerColor = colors.background,
        topBar = { ManageTopBar(onBack = onBack) },
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
                    ServerImagePreview(
                        name = name,
                        imageUrl = imageUrl,
                        onChangeImageClick = ::requestPhotoPicker,
                    )

                    FormField(
                        label = "Название сервера",
                        required = true,
                        value = name,
                        onValueChange = { name = it; error = null },
                        placeholder = "Введите название сервера",
                        singleLine = true,
                    )

                    FormField(
                        label = "Username",
                        required = true,
                        value = username,
                        onValueChange = { username = editableUsername(it); error = null },
                        placeholder = "server_username",
                        singleLine = true,
                        prefix = "@ ",
                    )

                    FormField(
                        label = "Описание",
                        required = false,
                        value = description,
                        onValueChange = { description = it; error = null },
                        placeholder = "Введите описание сервера",
                        singleLine = false,
                        minHeight = 96,
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
                                modifier = Modifier.padding(12.dp),
                            )
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        AleAppButton(
                            onClick = {
                                when {
                                    name.isBlank() -> error = "Название обязательно"
                                    username.isBlank() -> error = "Username обязателен"
                                    else -> {
                                        error = null
                                        onSave(
                                            name.trim(),
                                            displayUsername(username),
                                            description.trim().takeIf { it.isNotEmpty() },
                                            imageUrl.trim(),
                                        )
                                    }
                                }
                            },
                            variant = AleAppButtonVariant.Primary,
                            size = AleAppButtonSize.Large,
                            modifier = Modifier.weight(1f),
                        ) {
                            Text("Сохранить изменения")
                        }

                        AleAppButton(
                            onClick = onBack,
                            variant = AleAppButtonVariant.Outline,
                            size = AleAppButtonSize.Large,
                            modifier = Modifier.weight(1f),
                        ) {
                            Text("Отмена")
                        }
                    }
                }
            }

            if (isUploadingImage || !uploadError.isNullOrBlank()) {
                Spacer(Modifier.height(12.dp))
                UploadStatus(
                    error = uploadError,
                    isUploading = isUploadingImage,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                )
            }

            Spacer(Modifier.height(16.dp))

            InviteTokensCard(
                onClick = onInviteTokens,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
            )

            Spacer(Modifier.height(16.dp))

            DangerZone(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
            )

            Spacer(Modifier.height(32.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ManageTopBar(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = AleAppTheme.colors

    Column(modifier) {
        TopAppBar(
            title = {
                Text(
                    text = "Управление сервером",
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
private fun ServerImagePreview(
    name: String,
    imageUrl: String,
    onChangeImageClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = AleAppTheme.colors
    val bgColor = serverImageColor(name)
    val initials = name.split(" ")
        .take(2)
        .mapNotNull { it.firstOrNull()?.uppercaseChar() }
        .joinToString("")

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box {
            Surface(
                modifier = Modifier.size(128.dp),
                shape = RoundedCornerShape(16.dp),
                color = bgColor,
            ) {
                if (imageUrl.isNotBlank()) {
                    AsyncImage(
                        model = imageUrl,
                        contentDescription = "Изображение сервера",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                    )
                } else {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = initials,
                            color = Color.White,
                            style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.SemiBold),
                        )
                    }
                }
            }

            Surface(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .offset(x = 4.dp, y = 4.dp)
                    .size(32.dp),
                shape = CircleShape,
                color = colors.primary,
                contentColor = colors.primaryForeground,
                shadowElevation = 4.dp,
                onClick = onChangeImageClick,
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
            text = "Изображение сервера",
            style = MaterialTheme.typography.bodySmall,
            color = colors.mutedForeground,
        )
    }
}

@Composable
private fun InviteTokensCard(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = AleAppTheme.colors

    AleAppCard(
        modifier = modifier,
        onClick = onClick,
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Токены приглашений",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Medium),
                    color = colors.foreground,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "Управление токенами для приглашения новых участников",
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.mutedForeground,
                )
            }
            Spacer(Modifier.width(8.dp))
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = null,
                tint = colors.mutedForeground,
                modifier = Modifier
                    .size(20.dp)
                    .graphicsLayer { rotationZ = 180f },
            )
        }
    }
}

@Composable
private fun UploadStatus(
    error: String?,
    isUploading: Boolean,
    modifier: Modifier = Modifier,
) {
    val colors = AleAppTheme.colors

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = if (error.isNullOrBlank()) colors.secondary else colors.destructive.copy(alpha = 0.1f),
        border = BorderStroke(
            1.dp,
            if (error.isNullOrBlank()) colors.border else colors.destructive.copy(alpha = 0.2f),
        ),
    ) {
        Text(
            text = when {
                isUploading -> "Загружаем изображение..."
                !error.isNullOrBlank() -> error
                else -> ""
            },
            color = if (error.isNullOrBlank()) colors.foreground else colors.destructive,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
        )
    }
}

@Composable
private fun DangerZone(
    modifier: Modifier = Modifier,
) {
    val colors = AleAppTheme.colors

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = colors.destructive.copy(alpha = 0.1f),
        border = BorderStroke(1.dp, colors.destructive.copy(alpha = 0.2f)),
    ) {
        Row(modifier = Modifier.padding(20.dp)) {
            Icon(
                imageVector = Icons.Default.Delete,
                contentDescription = null,
                tint = colors.destructive,
                modifier = Modifier
                    .size(20.dp)
                    .padding(top = 2.dp),
            )

            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Опасная зона",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = colors.destructive,
                )

                Spacer(Modifier.height(4.dp))

                Text(
                    text = "Удаление сервера временно отключено в этой сборке. Доступно только обновление метаданных сервера и управление invite-токенами.",
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.mutedForeground,
                )
            }
        }
    }
}

@Preview(name = "ServerManagement Light", showBackground = true, showSystemUi = true)
@Composable
private fun ServerManagementLightPreview() {
    AleAppTheme(darkTheme = false) {
        ServerManagementScreen()
    }
}

@Preview(
    name = "ServerManagement Dark",
    showBackground = true,
    showSystemUi = true,
    uiMode = Configuration.UI_MODE_NIGHT_YES,
)
@Composable
private fun ServerManagementDarkPreview() {
    AleAppTheme(darkTheme = true) {
        ServerManagementScreen()
    }
}

@Preview(name = "ServerManagement Empty", showBackground = true, showSystemUi = true)
@Composable
private fun ServerManagementEmptyPreview() {
    AleAppTheme(darkTheme = false) {
        ServerManagementScreen(
            initial = ServerManageData(
                id = "new",
                name = "",
                username = "",
                description = "",
                imageUrl = "",
            ),
        )
    }
}

@Preview(name = "DangerZone Light", showBackground = true)
@Composable
private fun DangerZoneLightPreview() {
    AleAppTheme(darkTheme = false) {
        Surface(color = AleAppTheme.colors.background) {
            DangerZone(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
            )
        }
    }
}

@Preview(name = "DangerZone Dark", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun DangerZoneDarkPreview() {
    AleAppTheme(darkTheme = true) {
        Surface(color = AleAppTheme.colors.background) {
            DangerZone(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
            )
        }
    }
}
