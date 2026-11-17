package com.aether.launcher.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import android.os.Build
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.ui.platform.LocalContext
import com.aether.launcher.data.model.LauncherPalette
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.material3.LocalContentColor
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFFE8D7BC),
    onPrimary = Color(0xFF30271B),
    primaryContainer = Color(0xFF42382B),
    onPrimaryContainer = Color(0xFFFFE9CB),
    background = Color(0xFF17191D),
    onBackground = Color(0xFFECEEF2),
    surface = Color(0xFF17191D),
    surfaceVariant = Color(0xFF24272D),
    onSurface = Color(0xFFECEEF2),
    onSurfaceVariant = Color(0xFFC1C6D0),
    outline = Color(0xFF9098A5)
)

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF584936),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFF1E0C8),
    onPrimaryContainer = Color(0xFF2F271D),
    background = Color(0xFFF7F8FA),
    onBackground = Color(0xFF1D2026),
    surface = Color(0xFFF7F8FA),
    surfaceVariant = Color(0xFFECEEF3),
    onSurface = Color(0xFF1D2026),
    onSurfaceVariant = Color(0xFF4D5560),
    outline = Color(0xFF747C88)
)

@Composable
fun AetherLauncherTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    palette: LauncherPalette = LauncherPalette.WALLPAPER,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val neutral = if (darkTheme) DarkColorScheme else LightColorScheme
    val colorScheme = when {
        palette == LauncherPalette.WALLPAPER && Build.VERSION.SDK_INT >= 31 ->
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        palette == LauncherPalette.SAGE -> neutral.copy(
            primary = if (darkTheme) Color(0xFFBCD6BF) else Color(0xFF385A43),
            onPrimary = if (darkTheme) Color(0xFF203828) else Color.White,
            primaryContainer = if (darkTheme) Color(0xFF2A4030) else Color(0xFFD6E8D6),
            onPrimaryContainer = if (darkTheme) Color(0xFFD6E8D6) else Color(0xFF203828),
            secondary = if (darkTheme) Color(0xFFC0CDBF) else Color(0xFF4F6354),
            onSecondary = if (darkTheme) Color(0xFF25352A) else Color.White,
            secondaryContainer = if (darkTheme) Color(0xFF374B3D) else Color(0xFFD3E7D7),
            onSecondaryContainer = if (darkTheme) Color(0xFFDBE8DA) else Color(0xFF1D3525),
            surfaceTint = if (darkTheme) Color(0xFFBCD6BF) else Color(0xFF385A43)
        )
        palette == LauncherPalette.ORCHID -> neutral.copy(
            primary = if (darkTheme) Color(0xFFE3C5E7) else Color(0xFF6B426D),
            onPrimary = if (darkTheme) Color(0xFF3C2540) else Color.White,
            primaryContainer = if (darkTheme) Color(0xFF48304D) else Color(0xFFF0DDF2),
            onPrimaryContainer = if (darkTheme) Color(0xFFF0DDF2) else Color(0xFF3C2540),
            secondary = if (darkTheme) Color(0xFFD5C3D7) else Color(0xFF68576C),
            onSecondary = if (darkTheme) Color(0xFF362A3A) else Color.White,
            secondaryContainer = if (darkTheme) Color(0xFF4C3F50) else Color(0xFFEEDCF0),
            onSecondaryContainer = if (darkTheme) Color(0xFFEEDCF0) else Color(0xFF302034),
            surfaceTint = if (darkTheme) Color(0xFFE3C5E7) else Color(0xFF6B426D)
        )
        else -> neutral.copy(
            primary = if (darkTheme) Color(0xFFE0E2E8) else Color(0xFF424852),
            onPrimary = if (darkTheme) Color(0xFF242931) else Color.White,
            primaryContainer = if (darkTheme) Color(0xFF30343B) else Color(0xFFE2E5EA),
            onPrimaryContainer = if (darkTheme) Color(0xFFE2E5EA) else Color(0xFF252930),
            secondary = if (darkTheme) Color(0xFFC5CBD5) else Color(0xFF59616D),
            onSecondary = if (darkTheme) Color(0xFF2E343E) else Color.White,
            secondaryContainer = if (darkTheme) Color(0xFF414752) else Color(0xFFDFE5EF),
            onSecondaryContainer = if (darkTheme) Color(0xFFE1E7F0) else Color(0xFF26303A),
            surfaceTint = if (darkTheme) Color(0xFFE0E2E8) else Color(0xFF424852)
        )
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = AetherTypography
    ) {
        CompositionLocalProvider(LocalContentColor provides colorScheme.onSurface, content = content)
    }
}
