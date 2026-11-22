package com.aether.launcher.ui.components

import androidx.compose.foundation.border
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp

@Composable
internal fun glassEdge(shape: Shape, darkSurface: Boolean): Modifier {
    val edge = remember(darkSurface) {
        val top = Color.White.copy(alpha = if (darkSurface) .23f else .92f)
        val bottom = if (darkSurface) Color.White.copy(alpha = .07f) else Color.Black.copy(alpha = .08f)
        Brush.verticalGradient(listOf(top, bottom))
    }
    return Modifier.border(1.dp, edge, shape)
}
