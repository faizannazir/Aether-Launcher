package com.aether.launcher.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFFE8D7BC),
    onPrimary = Color(0xFF30271B),
    primaryContainer = Color(0xFF42382B),
    onPrimaryContainer = Color(0xFFFFE9CB),
    background = Color(0xFF171714),
    surface = Color(0xFF171714),
    surfaceVariant = Color(0xFF252420),
    onSurface = Color(0xFFECE9E2),
    onSurfaceVariant = Color(0xFFC8C3B9),
    outline = Color(0xFF8A857A)
)

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF584936),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFF1E0C8),
    onPrimaryContainer = Color(0xFF2F271D),
    background = Color(0xFFFAF9F5),
    surface = Color(0xFFFAF9F5),
    surfaceVariant = Color(0xFFF0EDE6),
    onSurface = Color(0xFF23221E),
    onSurfaceVariant = Color(0xFF57534B),
    outline = Color(0xFF827A6E)
)

@Composable
fun AetherLauncherTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = AetherTypography,
        content = content
    )
}
