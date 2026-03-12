package com.callapp.android.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider

// ── Material 3 color scheme mappings ─────────────────────────────────────────

private val LightMaterialColors = lightColorScheme(
    primary            = LightAleAppColors.primary,
    onPrimary          = LightAleAppColors.primaryForeground,
    secondary          = LightAleAppColors.secondary,
    onSecondary        = LightAleAppColors.secondaryForeground,
    tertiary           = LightAleAppColors.accent,
    onTertiary         = LightAleAppColors.accentForeground,
    background         = LightAleAppColors.background,
    onBackground       = LightAleAppColors.foreground,
    surface            = LightAleAppColors.card,
    onSurface          = LightAleAppColors.cardForeground,
    surfaceVariant     = LightAleAppColors.secondary,
    onSurfaceVariant   = LightAleAppColors.mutedForeground,
    error              = LightAleAppColors.destructive,
    onError            = LightAleAppColors.destructiveForeground,
    outline            = LightAleAppColors.border,
    outlineVariant     = LightAleAppColors.muted,
)

private val DarkMaterialColors = darkColorScheme(
    primary            = DarkAleAppColors.primary,
    onPrimary          = DarkAleAppColors.primaryForeground,
    secondary          = DarkAleAppColors.secondary,
    onSecondary        = DarkAleAppColors.secondaryForeground,
    tertiary           = DarkAleAppColors.accent,
    onTertiary         = DarkAleAppColors.accentForeground,
    background         = DarkAleAppColors.background,
    onBackground       = DarkAleAppColors.foreground,
    surface            = DarkAleAppColors.card,
    onSurface          = DarkAleAppColors.cardForeground,
    surfaceVariant     = DarkAleAppColors.secondary,
    onSurfaceVariant   = DarkAleAppColors.mutedForeground,
    error              = DarkAleAppColors.destructive,
    onError            = DarkAleAppColors.destructiveForeground,
    outline            = DarkAleAppColors.border,
    outlineVariant     = DarkAleAppColors.muted,
)

// ── Theme composable ─────────────────────────────────────────────────────────

@Composable
fun AleAppTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val aleAppColors = if (darkTheme) DarkAleAppColors else LightAleAppColors
    val materialColors = if (darkTheme) DarkMaterialColors else LightMaterialColors

    CompositionLocalProvider(LocalAleAppColors provides aleAppColors) {
        MaterialTheme(
            colorScheme = materialColors,
            typography = Typography,
            content = content,
        )
    }
}

// ── Convenience accessor ─────────────────────────────────────────────────────

object AleAppTheme {
    val colors: AleAppColors
        @Composable
        get() = LocalAleAppColors.current
}
