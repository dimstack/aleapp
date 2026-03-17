package com.callapp.android.ui.screens.connect

import android.content.res.Configuration
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.callapp.android.ui.common.readPickedImage
import com.callapp.android.ui.components.AleAppButton
import com.callapp.android.ui.components.AleAppButtonSize
import com.callapp.android.ui.components.AleAppButtonVariant
import com.callapp.android.ui.components.AleAppCard
import com.callapp.android.ui.components.FormField
import com.callapp.android.ui.theme.AleAppTheme

@Composable
fun CreateProfileScreen(
    serverName: String = "New Server",
    initialUsername: String = "",
    initialName: String = "",
    initialPassword: String = "",
    initialConfirmPassword: String = "",
    initialAvatarUrl: String = "",
    triggerSubmitOnLaunch: Boolean = false,
    onUploadAvatar: (bytes: ByteArray, fileName: String, mimeType: String, onUploaded: (String) -> Unit) -> Unit =
        { _, _, _, _ -> },
    isUploadingImage: Boolean = false,
    uploadError: String? = null,
    onPhotoPickerRequest: (() -> Unit)? = null,
    onCreateProfile: (username: String, name: String, password: String, avatarUrl: String?) -> Unit =
        { _, _, _, _ -> },
    modifier: Modifier = Modifier,
) {
    val colors = AleAppTheme.colors
    val context = LocalContext.current

    var username by remember { mutableStateOf(initialUsername) }
    var name by remember { mutableStateOf(initialName) }
    var password by remember { mutableStateOf(initialPassword) }
    var confirmPassword by remember { mutableStateOf(initialConfirmPassword) }
    var avatarUrl by remember { mutableStateOf(initialAvatarUrl) }
    var error by remember { mutableStateOf<String?>(null) }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
    ) { uri ->
        val pickedImage = uri?.let { context.readPickedImage(it) } ?: return@rememberLauncherForActivityResult
        onUploadAvatar(
            pickedImage.bytes,
            pickedImage.fileName,
            pickedImage.mimeType,
        ) { uploadedUrl ->
            avatarUrl = uploadedUrl
        }
    }

    val requestPhotoPicker = {
        onPhotoPickerRequest?.invoke() ?: photoPickerLauncher.launch(
            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
        )
    }

    val submitProfile = {
        when {
            username.isBlank() -> error = "Username обязателен"
            !isValidUsername(username.trim()) ->
                error = "Username может содержать только буквы, цифры и подчёркивание"
            name.isBlank() -> error = "Имя обязательно"
            password.length < MIN_PASSWORD_LENGTH ->
                error = "Пароль должен содержать минимум $MIN_PASSWORD_LENGTH символов"
            password != confirmPassword ->
                error = "Пароли не совпадают"
            else -> {
                error = null
                onCreateProfile(
                    username.trim(),
                    name.trim(),
                    password,
                    avatarUrl.trim().ifEmpty { null },
                )
            }
        }
    }

    LaunchedEffect(triggerSubmitOnLaunch) {
        if (triggerSubmitOnLaunch) {
            submitProfile()
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
            AleAppCard(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(32.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp),
                ) {
                    ProfileHeader(serverName = serverName)

                    ProfileAvatar(
                        name = name,
                        avatarUrl = avatarUrl,
                        isUploadingImage = isUploadingImage,
                        onPickPhoto = requestPhotoPicker,
                    )

                    FormField(
                        label = "Username",
                        required = true,
                        value = username,
                        onValueChange = { username = it; error = null },
                        placeholder = "username",
                        singleLine = true,
                        prefix = "@ ",
                        testTag = "create_profile_username_input",
                        helperText = "Только буквы, цифры и подчёркивание",
                    )

                    FormField(
                        label = "Имя",
                        required = true,
                        value = name,
                        onValueChange = { name = it; error = null },
                        placeholder = "Введите ваше имя",
                        singleLine = true,
                        testTag = "create_profile_name_input",
                    )

                    FormField(
                        label = "Пароль",
                        required = true,
                        value = password,
                        onValueChange = { password = it; error = null },
                        placeholder = "Минимум 8 символов",
                        singleLine = true,
                        isPassword = true,
                        testTag = "create_profile_password_input",
                        helperText = "Пароль понадобится для входа с другого устройства",
                    )

                    FormField(
                        label = "Подтвердите пароль",
                        required = true,
                        value = confirmPassword,
                        onValueChange = { confirmPassword = it; error = null },
                        placeholder = "Повторите пароль",
                        singleLine = true,
                        isPassword = true,
                        testTag = "create_profile_confirm_password_input",
                    )

                    if (uploadError != null) {
                        ErrorCard(
                            message = uploadError,
                            testTag = "create_profile_upload_error",
                        )
                    }

                    if (error != null) {
                        ErrorCard(
                            message = error.orEmpty(),
                            testTag = "create_profile_error",
                        )
                    }

                    Spacer(Modifier.height(4.dp))

                    AleAppButton(
                        onClick = submitProfile,
                        variant = AleAppButtonVariant.Primary,
                        size = AleAppButtonSize.Large,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("create_profile_submit_button"),
                    ) {
                        Text("Создать профиль")
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
            ProfileHint(modifier = Modifier.fillMaxWidth())
        }
    }
}

private const val MIN_PASSWORD_LENGTH = 8

private val usernamePattern = Regex("^[a-zA-Z0-9_]+$")

private fun isValidUsername(username: String): Boolean = usernamePattern.matches(username)

@Composable
private fun ProfileHeader(
    serverName: String,
    modifier: Modifier = Modifier,
) {
    val colors = AleAppTheme.colors

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = "Создание профиля",
            style = MaterialTheme.typography.headlineLarge.copy(
                fontWeight = FontWeight.Bold,
            ),
            color = colors.foreground,
            textAlign = TextAlign.Center,
        )

        Spacer(Modifier.height(8.dp))

        Text(
            text = buildAnnotatedString {
                append("Создайте свой профиль для сервера ")
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

@Composable
private fun ProfileAvatar(
    name: String,
    avatarUrl: String,
    isUploadingImage: Boolean,
    onPickPhoto: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = AleAppTheme.colors
    val initials = if (name.isNotBlank()) {
        name.take(2).uppercase()
    } else {
        "??"
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box {
            if (avatarUrl.isNotBlank()) {
                AsyncImage(
                    model = avatarUrl,
                    contentDescription = "Аватар профиля",
                    modifier = Modifier
                        .size(96.dp)
                        .clip(CircleShape)
                        .testTag("create_profile_avatar")
                        .clickable(onClick = onPickPhoto),
                    contentScale = ContentScale.Crop,
                )
            } else {
                Surface(
                    modifier = Modifier
                        .size(96.dp)
                        .testTag("create_profile_avatar")
                        .clickable(onClick = onPickPhoto),
                    shape = CircleShape,
                    color = colors.primary,
                    contentColor = colors.primaryForeground,
                    border = BorderStroke(4.dp, colors.secondary),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = initials,
                            style = MaterialTheme.typography.headlineMedium.copy(
                                fontWeight = FontWeight.SemiBold,
                            ),
                            color = Color.White,
                        )
                    }
                }
            }

            Surface(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .offset(x = 2.dp, y = 2.dp)
                    .size(32.dp)
                    .clickable(onClick = onPickPhoto)
                    .testTag("create_profile_photo_button"),
                shape = CircleShape,
                color = colors.primary,
                contentColor = colors.primaryForeground,
                shadowElevation = 4.dp,
            ) {
                Box(contentAlignment = Alignment.Center) {
                    if (isUploadingImage) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            color = colors.primaryForeground,
                            strokeWidth = 2.dp,
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.CameraAlt,
                            contentDescription = "Изменить фото",
                            modifier = Modifier.size(16.dp),
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(8.dp))

        Text(
            text = "Загрузите фото профиля",
            style = MaterialTheme.typography.bodySmall,
            color = colors.mutedForeground,
        )
    }
}

@Composable
private fun ErrorCard(
    message: String,
    testTag: String,
    modifier: Modifier = Modifier,
) {
    val colors = AleAppTheme.colors

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        color = colors.destructive.copy(alpha = 0.1f),
        border = BorderStroke(1.dp, colors.destructive.copy(alpha = 0.2f)),
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodySmall,
            color = colors.destructive,
            modifier = Modifier
                .padding(12.dp)
                .testTag(testTag),
        )
    }
}

@Composable
private fun ProfileHint(modifier: Modifier = Modifier) {
    val colors = AleAppTheme.colors

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = colors.primary.copy(alpha = 0.1f),
        border = BorderStroke(1.dp, colors.primary.copy(alpha = 0.2f)),
    ) {
        Text(
            text = "\uD83D\uDCA1 Вы можете создать разные профили для разных серверов",
            style = MaterialTheme.typography.bodySmall,
            color = colors.mutedForeground,
            modifier = Modifier.padding(16.dp),
        )
    }
}

@Preview(name = "CreateProfile - Light", showBackground = true, showSystemUi = true)
@Composable
private fun CreateProfileLightPreview() {
    AleAppTheme(darkTheme = false) {
        Surface(color = AleAppTheme.colors.background) {
            CreateProfileScreen(serverName = "New Server 5")
        }
    }
}

@Preview(
    name = "CreateProfile - Dark",
    showBackground = true,
    showSystemUi = true,
    uiMode = Configuration.UI_MODE_NIGHT_YES,
)
@Composable
private fun CreateProfileDarkPreview() {
    AleAppTheme(darkTheme = true) {
        Surface(color = AleAppTheme.colors.background) {
            CreateProfileScreen(serverName = "Tech Community")
        }
    }
}

@Preview(name = "ProfileAvatar - With Name", showBackground = true)
@Composable
private fun ProfileAvatarWithNamePreview() {
    AleAppTheme(darkTheme = false) {
        Surface(color = AleAppTheme.colors.card) {
            ProfileAvatar(
                name = "Александр",
                avatarUrl = "",
                isUploadingImage = false,
                onPickPhoto = {},
                modifier = Modifier.padding(24.dp),
            )
        }
    }
}

@Preview(name = "ProfileAvatar - Empty", showBackground = true)
@Composable
private fun ProfileAvatarEmptyPreview() {
    AleAppTheme(darkTheme = false) {
        Surface(color = AleAppTheme.colors.card) {
            ProfileAvatar(
                name = "",
                avatarUrl = "",
                isUploadingImage = false,
                onPickPhoto = {},
                modifier = Modifier.padding(24.dp),
            )
        }
    }
}

@Preview(name = "ProfileHeader - Light", showBackground = true)
@Composable
private fun ProfileHeaderPreview() {
    AleAppTheme(darkTheme = false) {
        Surface(color = AleAppTheme.colors.card) {
            ProfileHeader(
                serverName = "Tech Community",
                modifier = Modifier.padding(24.dp),
            )
        }
    }
}

@Preview(name = "ProfileHint - Light", showBackground = true)
@Composable
private fun ProfileHintLightPreview() {
    AleAppTheme(darkTheme = false) {
        Surface(color = AleAppTheme.colors.background) {
            ProfileHint(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
            )
        }
    }
}
