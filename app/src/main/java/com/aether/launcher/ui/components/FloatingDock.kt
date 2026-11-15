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
import com.aether.launcher.ui.screens.AppMenu
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
    isDarkTheme: Boolean = false,
    isBlurEnabled: Boolean = true,
    glassOpacity: Float,
    iconSize: Dp,
    modifier: Modifier = Modifier
) {
    Row(
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(if (isBlurEnabled) Color.Black.copy(alpha = 0.20f + 0.27f * glassOpacity) else Color(0xFF252422))
            .padding(vertical = 8.dp, horizontal = 8.dp)
    ) {
        dockApps.take(4).forEach { app ->
            var menuOpen by remember(app.packageName) { mutableStateOf(false) }
            Box {
                SquircleIcon(
                    app = app,
                    onClick = { actions.launch(app) },
                    onLongClick = { menuOpen = true },
                    showLabel = false,
                    isDarkTheme = isDarkTheme,
                    size = iconSize
                )
                AppMenu(menuOpen, { menuOpen = false }, app, actions, inDock = true, dockOnly = true)
            }
        }

    }
}
