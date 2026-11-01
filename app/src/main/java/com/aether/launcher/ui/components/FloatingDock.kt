package com.aether.launcher.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.aether.launcher.data.model.AppItem
import com.aether.launcher.glass.GlassLevel
import com.aether.launcher.glass.glassSurface

@Composable
fun FloatingDock(
    dockApps: List<AppItem>,
    onAppClick: (AppItem) -> Unit,
    onAppDrawerClick: () -> Unit,
    isDarkTheme: Boolean = false,
    isBlurEnabled: Boolean = true,
    modifier: Modifier = Modifier
) {
    Row(
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .glassSurface(
                level = GlassLevel.BALANCED,
                isDarkTheme = isDarkTheme,
                blurEnabled = isBlurEnabled,
                shape = RoundedCornerShape(32.dp)
            )
            .padding(vertical = 10.dp, horizontal = 12.dp)
    ) {
        // App icons in dock (up to 4)
        dockApps.take(4).forEach { app ->
            SquircleIcon(
                label = app.label,
                iconDrawable = app.icon,
                onClick = { onAppClick(app) },
                showLabel = false,
                isDarkTheme = isDarkTheme,
                isBlurEnabled = isBlurEnabled,
                size = 50.dp
            )
        }

        // Dedicated App Drawer Button
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(50.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(if (isDarkTheme) Color(0x33FFFFFF) else Color(0x26FFFFFF))
                .semantics(mergeDescendants = true) {
                    role = Role.Button
                    contentDescription = "Open App Drawer"
                }
                .clickable(
                    onClick = onAppDrawerClick,
                    onClickLabel = "Open App Drawer"
                )
        ) {
            Icon(
                imageVector = Icons.Rounded.GridView,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(28.dp)
            )
        }
    }
}
