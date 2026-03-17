package com.callapp.android.ui.common

internal fun displayUsername(username: String): String {
    val trimmed = username.trim()
    if (trimmed.isEmpty()) return "@"
    return if (trimmed.startsWith("@")) trimmed else "@$trimmed"
}

internal fun editableUsername(username: String): String = username.trim().removePrefix("@")
