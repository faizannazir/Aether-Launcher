package com.aether.launcher.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun CustomizationSheet(
    canRemovePage: Boolean,
    onAddPage: () -> Unit,
    onRemovePage: () -> Unit,
    onWidgets: () -> Unit,
    onAddControls: () -> Unit,
    onWallpaper: () -> Unit,
    onSettings: () -> Unit,
    onApps: () -> Unit,
    onClose: () -> Unit
) {
    ModalBottomSheet(onDismissRequest = onClose) {
        Column(Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, bottom = 20.dp)) {
            Text("Edit Home", style = MaterialTheme.typography.headlineSmall, modifier = Modifier.padding(12.dp))
            TextButton(onClick = onWidgets) { Text("Add widgets") }
            TextButton(onClick = onAddControls) { Text("Add Android controls card") }
            TextButton(onClick = onWallpaper) { Text("Wallpaper & style") }
            TextButton(onClick = onAddPage) { Text("Add Home page") }
            if (canRemovePage) TextButton(onClick = onRemovePage) { Text("Remove empty page") }
            TextButton(onClick = onSettings) { Text("Launcher settings") }
            TextButton(onClick = onApps) { Text("All apps") }
        }
    }
}
