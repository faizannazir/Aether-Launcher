package com.aether.launcher.glass

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.Brush
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

enum class GlassLevel(
    val lightColor: Color,
    val darkColor: Color,
    val lightFallback: Color,
    val darkFallback: Color,
    val specularBorderLight: Color,
    val specularBorderDark: Color,
    val elevation: Dp
) {
    SUBTLE(
        lightColor = Color(0x40FFFFFF),       // 25% white
        darkColor = Color(0x261E293B),        // 15% slate
        lightFallback = Color(0xF2F1F5F9),    // 95% light
        darkFallback = Color(0xF20F172A),     // 95% dark
        specularBorderLight = Color(0x66FFFFFF),
        specularBorderDark = Color(0x33FFFFFF),
        elevation = 2.dp
    ),
    BALANCED(
        lightColor = Color(0x59FFFFFF),       // 35% white
        darkColor = Color(0x401E293B),        // 25% slate
        lightFallback = Color(0xF8F8FAFC),
        darkFallback = Color(0xF80F172A),
        specularBorderLight = Color(0x99FFFFFF),
        specularBorderDark = Color(0x40FFFFFF),
        elevation = 6.dp
    ),
    STRONG(
        lightColor = Color(0x80FFFFFF),       // 50% white
        darkColor = Color(0x660F172A),        // 40% slate
        lightFallback = Color(0xFFFFFFFF),
        darkFallback = Color(0xFF0F172A),
        specularBorderLight = Color(0xCCFFFFFF),
        specularBorderDark = Color(0x4DFFFFFF),
        elevation = 12.dp
    )
}

@Composable
fun Modifier.glassSurface(
    level: GlassLevel = GlassLevel.BALANCED,
    isDarkTheme: Boolean = false,
    blurEnabled: Boolean = true,
    shape: Shape = RoundedCornerShape(28.dp),
    borderWidth: Dp = 1.dp,
    opacity: Float = 0.75f
): Modifier {
    val surface = MaterialTheme.colorScheme.surface
    val tint = MaterialTheme.colorScheme.primary
    val alpha = (if (isDarkTheme) .48f else .34f) + opacity.coerceIn(0f, 1f) * .32f
    val backgroundColor = surface.copy(alpha = alpha)
    val activeBg = if (blurEnabled) backgroundColor else surface
    val borderColor = Color.White.copy(alpha = if (isDarkTheme) .20f else .48f)
    val shadow = if (blurEnabled) Modifier.shadow(level.elevation, shape, clip = false) else Modifier
    val sheen = if (blurEnabled) Modifier.background(
        Brush.verticalGradient(listOf(Color.White.copy(alpha = .09f), Color.Transparent, tint.copy(alpha = .025f))),
        shape
    ) else Modifier

    return this.then(shadow)
        .clip(shape)
        .background(activeBg, shape)
        .then(sheen)
        .border(borderWidth, borderColor, shape)
}
