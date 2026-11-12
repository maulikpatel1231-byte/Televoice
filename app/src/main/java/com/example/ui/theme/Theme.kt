package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val TeleVoiceDarkColorScheme = darkColorScheme(
    primary = NeonPink,
    onPrimary = Color.White,
    primaryContainer = Color(0xFF4C082A),
    onPrimaryContainer = NeonPinkLight,
    secondary = NeonPurple,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFF381559),
    onSecondaryContainer = Color(0xFFE9D5FF),
    tertiary = TelegramBlue,
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFF0F3B55),
    onTertiaryContainer = Color(0xFFBAE6FD),
    background = StudioDarkBg,
    onBackground = TextPrimary,
    surface = StudioCardBg,
    onSurface = TextPrimary,
    surfaceVariant = StudioCardVariant,
    onSurfaceVariant = TextSecondary,
    outline = StudioBorder
)

@Composable
fun TeleVoiceTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = TeleVoiceDarkColorScheme,
        typography = Typography,
        content = content
    )
}
