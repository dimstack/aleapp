package com.callapp.android.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val DarkColorScheme = darkColorScheme(
    primary = EveningPrimary,
    onPrimary = EveningOnPrimary,
    secondary = EveningSecondary,
    background = EveningBackground,
    surface = EveningSurface,
    onSurface = EveningOnSurface,
    onBackground = EveningOnSurface,
)

private val LightColorScheme = lightColorScheme(
    primary = OldMoneyPrimary,
    onPrimary = OldMoneyOnPrimary,
    secondary = OldMoneySecondary,
    background = OldMoneyBackground,
    surface = OldMoneySurface,
    onSurface = OldMoneyOnSurface,
    onBackground = OldMoneyOnSurface,
)

@Composable
fun AndroidTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
