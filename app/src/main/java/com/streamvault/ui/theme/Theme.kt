package com.streamvault.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.streamvault.R

// ── Palette ─────────────────────────────────────────────────────────────────
object SVColors {
    val Background     = Color(0xFF08090C)
    val Surface        = Color(0xFF0E1118)
    val SurfaceVar     = Color(0xFF161C26)
    val SurfaceHigh    = Color(0xFF1D2535)
    val Border         = Color(0xFF1E2A3A)

    val Cyan           = Color(0xFF00E5FF)
    val CyanDim        = Color(0xFF0099CC)
    val Violet         = Color(0xFF7C3AED)
    val VioletLight    = Color(0xFFA78BFA)

    val TextPrimary    = Color(0xFFE2E8F0)
    val TextSecondary  = Color(0xFF94A3B8)
    val TextMuted      = Color(0xFF64748B)

    val Success        = Color(0xFF10B981)
    val Danger         = Color(0xFFEF4444)
    val Warning        = Color(0xFFF59E0B)

    val PlayerBg       = Color(0xFF000000)
    val ControlOverlay = Color(0xCC000000)
    val SeekBar        = Color(0xFF00E5FF)
    val SeekBarBg      = Color(0x33FFFFFF)
    val SeekBarBuffer  = Color(0x55FFFFFF)
}

private val DarkColorScheme = darkColorScheme(
    primary          = SVColors.Cyan,
    onPrimary        = Color(0xFF000000),
    primaryContainer = SVColors.SurfaceHigh,
    secondary        = SVColors.Violet,
    onSecondary      = Color(0xFFFFFFFF),
    background       = SVColors.Background,
    onBackground     = SVColors.TextPrimary,
    surface          = SVColors.Surface,
    onSurface        = SVColors.TextPrimary,
    surfaceVariant   = SVColors.SurfaceVar,
    onSurfaceVariant = SVColors.TextSecondary,
    outline          = SVColors.Border,
    error            = SVColors.Danger,
)

// ── Typography (Google Sans style, Pixel feel) ───────────────────────────────
val SVTypography = Typography(
    displayLarge  = TextStyle(fontWeight = FontWeight.W800, fontSize = 32.sp, lineHeight = 38.sp, letterSpacing = (-0.5).sp),
    displayMedium = TextStyle(fontWeight = FontWeight.W700, fontSize = 26.sp, lineHeight = 32.sp),
    titleLarge    = TextStyle(fontWeight = FontWeight.W700, fontSize = 20.sp, lineHeight = 26.sp),
    titleMedium   = TextStyle(fontWeight = FontWeight.W600, fontSize = 16.sp, lineHeight = 22.sp, letterSpacing = 0.15.sp),
    titleSmall    = TextStyle(fontWeight = FontWeight.W600, fontSize = 14.sp, lineHeight = 20.sp),
    bodyLarge     = TextStyle(fontWeight = FontWeight.Normal, fontSize = 16.sp, lineHeight = 24.sp),
    bodyMedium    = TextStyle(fontWeight = FontWeight.Normal, fontSize = 14.sp, lineHeight = 20.sp),
    bodySmall     = TextStyle(fontWeight = FontWeight.Normal, fontSize = 12.sp, lineHeight = 16.sp),
    labelLarge    = TextStyle(fontWeight = FontWeight.W600, fontSize = 13.sp, lineHeight = 18.sp, letterSpacing = 0.1.sp),
    labelSmall    = TextStyle(fontWeight = FontWeight.W500, fontSize = 11.sp, lineHeight = 16.sp, letterSpacing = 0.5.sp),
)

@Composable
fun StreamVaultTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography  = SVTypography,
        content     = content
    )
}
