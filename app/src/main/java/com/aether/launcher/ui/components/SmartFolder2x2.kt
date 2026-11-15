package com.aether.launcher.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.aether.launcher.data.model.AppItem
import com.aether.launcher.data.model.FolderItem

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
    val folderApps = folder.appPackages.take(4).mapNotNull { id -> installedApps.firstOrNull { it.packageName == id } }
    Column(
        modifier = modifier.fillMaxSize()
            .clip(RoundedCornerShape(22.dp))
            .background(if (isBlurEnabled) Color.Black.copy(alpha = 0.36f) else Color(0xFF252422))
            .padding(horizontal = 8.dp, vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        folderApps.chunked(2).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                row.forEach { app ->
                    SquircleIcon(app, onClick = { onAppClick(app) }, showLabel = false, isDarkTheme = isDarkTheme, size = 32.dp)
                }
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth().heightIn(min = 36.dp)
                .clip(RoundedCornerShape(12.dp)).clickable(onClick = onOpenFolderModal)
                .semantics { contentDescription = "Open folder ${folder.title}" },
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(folder.title, color = Color.White, style = MaterialTheme.typography.labelMedium,
                maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false))
            Icon(Icons.Rounded.KeyboardArrowDown, null, Modifier.size(16.dp), tint = Color.White)
        }
    }
}
