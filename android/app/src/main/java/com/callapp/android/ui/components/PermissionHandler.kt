package com.callapp.android.ui.components

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat

/**
 * Permissions required for audio calls.
 */
val audioCallPermissions: List<String> = buildList {
    add(Manifest.permission.RECORD_AUDIO)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        add(Manifest.permission.BLUETOOTH_CONNECT)
    }
}

/**
 * Permissions required for video calls (audio + camera).
 */
val videoCallPermissions: List<String> = buildList {
    addAll(audioCallPermissions)
    add(Manifest.permission.CAMERA)
}

fun hasPermissions(context: Context, permissions: List<String>): Boolean =
    permissions.all {
        ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
    }

/**
 * Composable that requests runtime permissions for calls.
 *
 * Checks if permissions are already granted — if yes, calls [onAllGranted] immediately.
 * Otherwise launches the system permission dialog.
 *
 * @param permissions List of permissions to request.
 * @param onAllGranted Called when all permissions are granted.
 * @param onDenied Called when any permission is denied.
 */
@Composable
fun RequestCallPermissions(
    permissions: List<String> = audioCallPermissions,
    onAllGranted: () -> Unit,
    onDenied: () -> Unit = {},
) {
    val context = LocalContext.current
    var alreadyGranted by remember {
        mutableStateOf(hasPermissions(context, permissions))
    }
    var showDeniedDialog by remember { mutableStateOf(false) }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        val allGranted = results.values.all { it }
        if (allGranted) {
            alreadyGranted = true
            onAllGranted()
        } else {
            showDeniedDialog = true
        }
    }

    LaunchedEffect(alreadyGranted) {
        if (alreadyGranted) {
            onAllGranted()
        } else {
            launcher.launch(permissions.toTypedArray())
        }
    }

    if (showDeniedDialog) {
        AlertDialog(
            onDismissRequest = {
                showDeniedDialog = false
                onDenied()
            },
            title = { Text("Разрешения необходимы") },
            text = {
                Text(
                    buildString {
                        append("Для совершения звонков необходим доступ к микрофону")
                        if (Manifest.permission.CAMERA in permissions) {
                            append(" и камере")
                        }
                        append(". Пожалуйста, предоставьте разрешения.")
                    },
                    style = MaterialTheme.typography.bodyMedium,
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    showDeniedDialog = false
                    launcher.launch(permissions.toTypedArray())
                }) {
                    Text("Повторить")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showDeniedDialog = false
                    onDenied()
                }) {
                    Text("Отмена")
                }
            },
        )
    }
}
