package com.aether.launcher.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.aether.launcher.data.model.AppItem
import com.aether.launcher.data.model.FolderItem
import com.aether.launcher.glass.GlassLevel
import com.aether.launcher.glass.glassSurface

@Composable
fun SmartFolder2x2(
    folder: FolderItem,
    installedApps: List<AppItem>,
    isDarkTheme: Boolean,
    isBlurEnabled: Boolean,
    onAppClick: (AppItem) -> Unit,
    onOpenFolderModal: () -> Unit,
    modifier: Modifier = Modifier
) {
    val textColor = if (isDarkTheme) Color.White else Color(0xFF191C1E)
    val folderApps = installedApps.filter { folder.appPackages.contains(it.packageName) }.take(4)

    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(4.dp)
            .glassSurface(
                level = GlassLevel.BALANCED,
                isDarkTheme = isDarkTheme,
                blurEnabled = isBlurEnabled,
                shape = RoundedCornerShape(26.dp)
            )
            .clickable(onClick = onOpenFolderModal)
            .padding(10.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = folder.title,
                style = MaterialTheme.typography.labelSmall,
                color = textColor,
                maxLines = 1
            )

            Spacer(modifier = Modifier.height(4.dp))

            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalArrangement = Arrangement.SpaceEvenly
            ) {
                items(folderApps) { app ->
                    SquircleIcon(
                        label = "",
                        iconDrawable = app.icon,
                iconBitmap = app.iconBitmap,
                        onClick = { onAppClick(app) },
                        showLabel = false,
                        isDarkTheme = isDarkTheme,
                        isBlurEnabled = isBlurEnabled,
                        size = 38.dp
                    )
                }
            }
        }
    }
}
