package com.example.android.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * Custom color palette for the "Old Money" design system.
 * Light theme = "Old Money", Dark theme = "Evening".
 */
@Immutable
data class AleAppColors(
    // Core
    val background: Color,
    val foreground: Color,
    val card: Color,
    val cardForeground: Color,

    // Primary
    val primary: Color,
    val primaryForeground: Color,

    // Secondary
    val secondary: Color,
    val secondaryForeground: Color,

    // Muted
    val muted: Color,
    val mutedForeground: Color,

    // Accent
    val accent: Color,
    val accentForeground: Color,

    // Destructive
    val destructive: Color,
    val destructiveForeground: Color,

    // Input & Controls
    val inputBackground: Color,
    val switchBackground: Color,
    val border: Color,
    val ring: Color,

    // Status
    val statusOnline: Color,
    val statusBusy: Color,
    val statusOffline: Color,

    // Call
    val callAccept: Color,
    val callDecline: Color,

    val isDark: Boolean,
)

// ── Light palette ("Old Money") ──────────────────────────────────────────────

val LightAleAppColors = AleAppColors(
    background          = Color(0xFFF8F6F1),
    foreground          = Color(0xFF2C3E50),
    card                = Color(0xFFFFFFFF),
    cardForeground      = Color(0xFF2C3E50),

    primary             = Color(0xFF1B3A52),
    primaryForeground   = Color(0xFFF8F6F1),

    secondary           = Color(0xFFE8E3D5),
    secondaryForeground = Color(0xFF2C3E50),

    muted               = Color(0xFFD4CCBB),
    mutedForeground     = Color(0xFF6B7280),

    accent              = Color(0xFFC9B896),
    accentForeground    = Color(0xFF2C3E50),

    destructive         = Color(0xFF8B4513),
    destructiveForeground = Color(0xFFF8F6F1),

    inputBackground     = Color(0xFFF5F1E8),
    switchBackground    = Color(0xFFD4CCBB),
    border              = Color(0x1F2C3E50), // rgba(44,62,80, 0.12)
    ring                = Color(0xFF1B3A52),

    statusOnline        = Color(0xFF4A7C59),
    statusBusy          = Color(0xFF8B6F47),
    statusOffline       = Color(0xFF8B8B8B),

    callAccept          = Color(0xFF4A7C59),
    callDecline         = Color(0xFF8B6F47),

    isDark              = false,
)

// ── Dark palette ("Evening") ─────────────────────────────────────────────────

val DarkAleAppColors = AleAppColors(
    background          = Color(0xFF1A1F28),
    foreground          = Color(0xFFE8E3D5),
    card                = Color(0xFF232933),
    cardForeground      = Color(0xFFE8E3D5),

    primary             = Color(0xFFC9B896),
    primaryForeground   = Color(0xFF1A1F28),

    secondary           = Color(0xFF2A313D),
    secondaryForeground = Color(0xFFE8E3D5),

    muted               = Color(0xFF3A4250),
    mutedForeground     = Color(0xFF9CA3AF),

    accent              = Color(0xFFA89968),
    accentForeground    = Color(0xFF1A1F28),

    destructive         = Color(0xFF9D7C5A),
    destructiveForeground = Color(0xFFF8F6F1),

    inputBackground     = Color(0xFF2A313D),
    switchBackground    = Color(0xFF3A4250),
    border              = Color(0x26C9B896), // rgba(201,184,150, 0.15)
    ring                = Color(0xFFC9B896),

    statusOnline        = Color(0xFF5A9670),
    statusBusy          = Color(0xFFA89968),
    statusOffline       = Color(0xFF6B7280),

    callAccept          = Color(0xFF5A9670),
    callDecline         = Color(0xFF9D7C5A),

    isDark              = true,
)

val LocalAleAppColors = staticCompositionLocalOf { LightAleAppColors }
