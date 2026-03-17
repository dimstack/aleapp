package com.callapp.android.ui.common

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns

data class PickedImage(
    val bytes: ByteArray,
    val fileName: String,
    val mimeType: String,
)

fun Context.readPickedImage(uri: Uri): PickedImage? {
    val mimeType = contentResolver.getType(uri)?.takeIf { it.startsWith("image/") } ?: return null
    val fileName = queryFileName(uri) ?: "image"
    val bytes = contentResolver.openInputStream(uri)?.use { it.readBytes() } ?: return null
    return PickedImage(bytes = bytes, fileName = fileName, mimeType = mimeType)
}

private fun Context.queryFileName(uri: Uri): String? {
    contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
        if (cursor.moveToFirst()) {
            val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (index >= 0) {
                return cursor.getString(index)
            }
        }
    }
    return null
}
