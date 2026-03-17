package com.callapp.android.ui.screens.profile

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Edit
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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.callapp.android.ui.components.AleAppButton
import com.callapp.android.ui.components.AleAppButtonSize
import com.callapp.android.ui.components.AleAppButtonVariant
import com.callapp.android.ui.components.AleAppCard
import com.callapp.android.ui.common.readPickedImage
import com.callapp.android.ui.theme.AleAppTheme
import kotlin.math.absoluteValue

data class MyProfileData(
    val name: String,
    val username: String,
    val avatarUrl: String = "",
    val serverName: String,
    val isAdmin: Boolean,
)

internal val sampleProfileAdmin = MyProfileData(
    name = "Александр",
    username = "alex_tech",
    avatarUrl = "",
    serverName = "Tech Community",
    isAdmin = true,
)

internal val sampleProfileMember = MyProfileData(
    name = "Александр Дизайнер",
    username = "alex_creative",
    avatarUrl = "",
    serverName = "Creative Studio",
    isAdmin = false,
)

private val avatarPalette = listOf(
    Color(0xFF8B7355),
    Color(0xFF5B7B8F),
    Color(0xFF7B6B8D),
    Color(0xFF6B8E6B),
    Color(0xFF8B6F5E),
    Color(0xFF6B7B8B),
)

private fun avatarColor(name: String): Color =
    avatarPalette[name.hashCode().absoluteValue % avatarPalette.size]

@Composable
fun MyProfileScreen(
    profile: MyProfileData = sampleProfileAdmin,
    onBack: () -> Unit = {},
    onSaveProfile: (name: String, username: String, avatarUrl: String) -> Unit = { _, _, _ -> },
    onUploadAvatar: (bytes: ByteArray, fileName: String, mimeType: String, onUploaded: (String) -> Unit) -> Unit = { _, _, _, _ -> },
    isUploadingImage: Boolean = false,
    uploadError: String? = null,
    onPhotoPickerRequest: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val colors = AleAppTheme.colors
    val context = LocalContext.current

    var isEditing by remember { mutableStateOf(false) }
    var editName by remember { mutableStateOf(profile.name) }
    var editUsername by remember { mutableStateOf(profile.username) }
    var editAvatarUrl by remember { mutableStateOf(profile.avatarUrl) }
    var savedName by remember { mutableStateOf(profile.name) }
    var savedUsername by remember { mutableStateOf(profile.username) }
    var savedAvatarUrl by remember { mutableStateOf(profile.avatarUrl) }
    val visibleAvatarUrl = if (isEditing) editAvatarUrl else savedAvatarUrl
    val photoPickerLauncher = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        val pickedImage = uri?.let { context.readPickedImage(it) } ?: return@rememberLauncherForActivityResult
        onUploadAvatar(pickedImage.bytes, pickedImage.fileName, pickedImage.mimeType) { uploadedUrl ->
            editAvatarUrl = uploadedUrl
        }
    }

    LaunchedEffect(profile.avatarUrl) {
        if (!isEditing) {
            savedAvatarUrl = profile.avatarUrl
        }
    }

    fun requestPhotoPicker() {
        onPhotoPickerRequest?.invoke()
            ?: photoPickerLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
    }

    Scaffold(
        containerColor = colors.background,
        topBar = { ProfileTopBar(onBack = onBack) },
    ) { padding ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(32.dp))

            Box {
                ProfileAvatar(
                    name = savedName,
                    avatarUrl = visibleAvatarUrl,
                    size = 128.dp,
                )
                if (isEditing) {
                    Surface(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .size(32.dp),
                        shape = CircleShape,
                        color = colors.primary,
                        contentColor = colors.primaryForeground,
                        shadowElevation = 4.dp,
                        onClick = ::requestPhotoPicker,
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
            }

            Spacer(Modifier.height(16.dp))

            Text(
                text = savedName,
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.SemiBold),
                color = colors.foreground,
            )

            Spacer(Modifier.height(4.dp))

            Text(
                text = "@$savedUsername",
                style = MaterialTheme.typography.bodyMedium,
                color = colors.mutedForeground,
            )

            Spacer(Modifier.height(20.dp))

            if (!isEditing) {
                AleAppButton(
                    onClick = {
                        editName = savedName
                        editUsername = savedUsername
                        editAvatarUrl = savedAvatarUrl
                        isEditing = true
                    },
                    variant = AleAppButtonVariant.Primary,
                    size = AleAppButtonSize.Default,
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("Редактировать профиль")
                }
            }

            Spacer(Modifier.height(24.dp))

            AleAppCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    InfoField(
                        label = "Имя",
                        value = if (isEditing) editName else savedName,
                        isEditing = isEditing,
                        onValueChange = { editName = it },
                    )

                    InfoField(
                        label = "Username",
                        value = if (isEditing) editUsername else savedUsername,
                        isEditing = isEditing,
                        onValueChange = { editUsername = it },
                        prefix = "@",
                    )

                    if (isEditing) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                text = "Фото профиля",
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                                color = colors.mutedForeground,
                            )
                            AleAppButton(
                                onClick = ::requestPhotoPicker,
                                variant = AleAppButtonVariant.Outline,
                                size = AleAppButtonSize.Default,
                            ) {
                                Text("Изменить фото")
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            AleAppCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    Column {
                        Text(
                            text = "Сервер",
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                            color = colors.mutedForeground,
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = profile.serverName,
                            style = MaterialTheme.typography.bodyLarge,
                            color = colors.cardForeground,
                        )
                    }

                    Column {
                        Text(
                            text = "Роль",
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                            color = colors.mutedForeground,
                        )
                        Spacer(Modifier.height(4.dp))
                        RoleBadge(isAdmin = profile.isAdmin)
                    }
                }
            }

            if (isUploadingImage || !uploadError.isNullOrBlank()) {
                Spacer(Modifier.height(12.dp))
                UploadStatus(
                    isUploading = isUploadingImage,
                    error = uploadError,
                )
            }

            if (isEditing) {
                Spacer(Modifier.height(24.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    AleAppButton(
                        onClick = {
                            savedName = editName.trim()
                            savedUsername = editUsername.trim()
                            savedAvatarUrl = editAvatarUrl.trim()
                            onSaveProfile(savedName, savedUsername, savedAvatarUrl)
                            isEditing = false
                        },
                        variant = AleAppButtonVariant.Primary,
                        size = AleAppButtonSize.Large,
                        modifier = Modifier.weight(1f),
                    ) {
                        Text("Сохранить")
                    }

                    AleAppButton(
                        onClick = {
                            editName = savedName
                            editUsername = savedUsername
                            editAvatarUrl = savedAvatarUrl
                            isEditing = false
                        },
                        variant = AleAppButtonVariant.Outline,
                        size = AleAppButtonSize.Large,
                        modifier = Modifier.weight(1f),
                    ) {
                        Text("Отмена")
                    }
                }
            }

            Spacer(Modifier.height(32.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ProfileTopBar(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = AleAppTheme.colors

    Column(modifier) {
        TopAppBar(
            title = {
                Text(
                    text = "Мой профиль",
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
internal fun ProfileAvatar(
    name: String,
    avatarUrl: String = "",
    modifier: Modifier = Modifier,
    size: Dp = 128.dp,
) {
    val bgColor = avatarColor(name)
    val initials = name.split(" ")
        .take(2)
        .mapNotNull { it.firstOrNull()?.uppercaseChar() }
        .joinToString("")

    val colors = AleAppTheme.colors

    Surface(
        modifier = modifier.size(size),
        shape = CircleShape,
        color = bgColor,
        border = BorderStroke(4.dp, colors.secondary),
    ) {
        if (avatarUrl.isNotBlank()) {
            AsyncImage(
                model = avatarUrl,
                contentDescription = "Аватар профиля",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
        } else {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = initials,
                    color = Color.White,
                    style = MaterialTheme.typography.headlineLarge.copy(
                        fontWeight = FontWeight.SemiBold,
                        fontSize = (size.value * 0.25f).sp,
                    ),
                )
            }
        }
    }
}

@Composable
private fun InfoField(
    label: String,
    value: String,
    isEditing: Boolean,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    prefix: String? = null,
) {
    val colors = AleAppTheme.colors

    Column(modifier = modifier) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
            color = colors.mutedForeground,
        )

        Spacer(Modifier.height(4.dp))

        if (isEditing) {
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = colors.inputBackground,
                border = BorderStroke(1.dp, colors.border),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (prefix != null) {
                        Text(
                            text = prefix,
                            style = MaterialTheme.typography.bodyLarge,
                            color = colors.mutedForeground,
                        )
                    }
                    BasicTextField(
                        value = value,
                        onValueChange = onValueChange,
                        textStyle = MaterialTheme.typography.bodyLarge.copy(color = colors.foreground),
                        singleLine = true,
                        cursorBrush = SolidColor(colors.primary),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        } else {
            Text(
                text = if (prefix != null) "$prefix$value" else value,
                style = MaterialTheme.typography.bodyLarge,
                color = colors.cardForeground,
            )
        }
    }
}

@Composable
internal fun RoleBadge(
    isAdmin: Boolean,
    modifier: Modifier = Modifier,
) {
    val colors = AleAppTheme.colors

    if (isAdmin) {
        Surface(
            modifier = modifier,
            shape = RoundedCornerShape(50),
            color = colors.primary.copy(alpha = 0.15f),
            contentColor = colors.primary,
        ) {
            Text(
                text = "Администратор",
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Medium),
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
            )
        }
    } else {
        Surface(
            modifier = modifier,
            shape = RoundedCornerShape(50),
            color = colors.secondary,
            contentColor = colors.mutedForeground,
        ) {
            Text(
                text = "Участник",
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Medium),
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
            )
        }
    }
}

@Composable
private fun UploadStatus(
    isUploading: Boolean,
    error: String?,
) {
    val colors = AleAppTheme.colors

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
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

@Preview(name = "MyProfile Admin Light", showBackground = true, showSystemUi = true)
@Composable
private fun MyProfileAdminLightPreview() {
    AleAppTheme(darkTheme = false) {
        MyProfileScreen(profile = sampleProfileAdmin)
    }
}

@Preview(
    name = "MyProfile Admin Dark",
    showBackground = true,
    showSystemUi = true,
    uiMode = Configuration.UI_MODE_NIGHT_YES,
)
@Composable
private fun MyProfileAdminDarkPreview() {
    AleAppTheme(darkTheme = true) {
        MyProfileScreen(profile = sampleProfileAdmin)
    }
}

@Preview(name = "MyProfile Member Light", showBackground = true, showSystemUi = true)
@Composable
private fun MyProfileMemberLightPreview() {
    AleAppTheme(darkTheme = false) {
        MyProfileScreen(profile = sampleProfileMember)
    }
}

@Preview(
    name = "MyProfile Member Dark",
    showBackground = true,
    showSystemUi = true,
    uiMode = Configuration.UI_MODE_NIGHT_YES,
)
@Composable
private fun MyProfileMemberDarkPreview() {
    AleAppTheme(darkTheme = true) {
        MyProfileScreen(profile = sampleProfileMember)
    }
}

@Preview(name = "ProfileTopBar Light", showBackground = true)
@Composable
private fun TopBarLightPreview() {
    AleAppTheme(darkTheme = false) {
        ProfileTopBar(onBack = {})
    }
}

@Preview(name = "ProfileTopBar Dark", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun TopBarDarkPreview() {
    AleAppTheme(darkTheme = true) {
        ProfileTopBar(onBack = {})
    }
}

@Preview(name = "ProfileAvatar Light", showBackground = true)
@Composable
private fun AvatarLightPreview() {
    AleAppTheme(darkTheme = false) {
        Surface(color = AleAppTheme.colors.background, modifier = Modifier.padding(24.dp)) {
            ProfileAvatar(name = "Александр")
        }
    }
}

@Preview(name = "ProfileAvatar Dark", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun AvatarDarkPreview() {
    AleAppTheme(darkTheme = true) {
        Surface(color = AleAppTheme.colors.background, modifier = Modifier.padding(24.dp)) {
            ProfileAvatar(name = "Александр Дизайнер")
        }
    }
}

@Preview(name = "RoleBadge Light", showBackground = true)
@Composable
private fun RoleBadgeAdminPreview() {
    AleAppTheme(darkTheme = false) {
        Surface(color = AleAppTheme.colors.card, modifier = Modifier.padding(16.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                RoleBadge(isAdmin = true)
                RoleBadge(isAdmin = false)
            }
        }
    }
}

@Preview(name = "RoleBadge Dark", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun RoleBadgeDarkPreview() {
    AleAppTheme(darkTheme = true) {
        Surface(color = AleAppTheme.colors.card, modifier = Modifier.padding(16.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                RoleBadge(isAdmin = true)
                RoleBadge(isAdmin = false)
            }
        }
    }
}

@Preview(name = "InfoField View", showBackground = true)
@Composable
private fun InfoFieldViewPreview() {
    AleAppTheme(darkTheme = false) {
        Surface(color = AleAppTheme.colors.card) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                InfoField(label = "Имя", value = "Александр", isEditing = false, onValueChange = {})
                InfoField(label = "Username", value = "alex_tech", isEditing = false, onValueChange = {}, prefix = "@")
            }
        }
    }
}

@Preview(name = "InfoField Edit", showBackground = true)
@Composable
private fun InfoFieldEditPreview() {
    AleAppTheme(darkTheme = false) {
        Surface(color = AleAppTheme.colors.card) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                InfoField(label = "Имя", value = "Александр", isEditing = true, onValueChange = {})
                InfoField(label = "Username", value = "alex_tech", isEditing = true, onValueChange = {}, prefix = "@")
            }
        }
    }
}
