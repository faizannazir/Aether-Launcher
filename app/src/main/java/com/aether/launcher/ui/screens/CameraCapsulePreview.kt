package com.aether.launcher.ui.screens

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.aether.launcher.notification.cameraCutoutBounds
import com.aether.launcher.notification.cameraCapsuleGeometry

@Composable
internal fun CameraCapsulePreview(onSetup: () -> Unit, modifier: Modifier = Modifier) {
    val view = LocalView.current
    val density = LocalDensity.current
    val camera = if (Build.VERSION.SDK_INT >= 28) cameraCutoutBounds(view.rootWindowInsets?.displayCutout) else null
    val screenWidth = view.width.takeIf { it > 0 } ?: view.resources.displayMetrics.widthPixels
    val geometry = cameraCapsuleGeometry(screenWidth, density.density, camera)
    val width = with(density) { geometry.width.toDp() }
    val height = with(density) { geometry.height.toDp() }
    val offset = with(density) { geometry.offsetX.toDp() }
    val top = with(density) { geometry.top.toDp() }
    Box(
        modifier.offset(x = offset, y = top).size(width, height)
            .background(Color(0xFF07080A), RoundedCornerShape(50))
            .border(1.dp, Color.White.copy(alpha = .16f), RoundedCornerShape(50))
            .clickable(onClick = onSetup)
            .semantics { contentDescription = "Camera capsule setup" }
    )
}
