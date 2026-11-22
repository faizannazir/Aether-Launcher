package com.aether.launcher.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.unit.Dp
import com.aether.launcher.ui.screens.AppActions
import com.aether.launcher.ui.screens.AppDropTarget
import com.aether.launcher.ui.screens.LauncherAppDrag
import com.aether.launcher.ui.screens.appDropTarget
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.aether.launcher.data.model.AppItem

@Composable
internal fun FloatingDock(
    dockApps: List<AppItem>,
    actions: AppActions,
    drag: LauncherAppDrag,
    isDarkTheme: Boolean = false,
    isBlurEnabled: Boolean = true,
    glassOpacity: Float,
    iconSize: Dp,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(24.dp)
    val edge = if (isBlurEnabled) glassEdge(shape, darkSurface = true) else Modifier
    Row(
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth().appDropTarget(drag, AppDropTarget.Dock)
            .padding(horizontal = 20.dp)
            .clip(shape)
            .background(if (isBlurEnabled) Color.Black.copy(alpha = 0.20f + 0.27f * glassOpacity) else Color(0xFF252422))
            .then(edge)
            .padding(vertical = 8.dp, horizontal = 8.dp)
    ) {
        dockApps.take(4).forEach { app ->
            SquircleIcon(
                app = app,
                onClick = { actions.launch(app) },
                onLongClick = { actions.startDrag(app, null, true) },
                showLabel = false,
                isDarkTheme = isDarkTheme,
                size = iconSize
            )
        }

    }
}
