package com.aether.launcher.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.aether.launcher.data.model.HomeLayoutItem
import com.aether.launcher.ui.screens.AndroidShortcut

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun HarmonyServiceWidget(
    item: HomeLayoutItem.ServiceWidget,
    translucent: Boolean,
    onShortcut: (AndroidShortcut) -> Unit,
    onRemove: () -> Unit
) {
    var confirmRemoval by remember { mutableStateOf(false) }
    BoxWithConstraints(
        Modifier.fillMaxSize().clip(RoundedCornerShape(22.dp))
            .background(if (translucent) Color.Black.copy(alpha = 0.38f) else Color(0xFF252422))
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        val wide = maxWidth > 205.dp
        Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center) {
            Text(
                item.title, color = Color.White, style = MaterialTheme.typography.titleSmall,
                maxLines = 1, overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth().heightIn(min = 26.dp)
                    .combinedClickable(
                        onClick = { onShortcut(AndroidShortcut.SETTINGS) },
                        onLongClick = { confirmRemoval = true },
                        onLongClickLabel = "Remove controls card"
                    ).padding(start = 6.dp)
            )
            if (wide) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Control("Internet", AndroidShortcut.INTERNET, onShortcut, Modifier.weight(1f))
                    Control("Sound", AndroidShortcut.SOUND, onShortcut, Modifier.weight(1f))
                }
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Control("Display", AndroidShortcut.DISPLAY, onShortcut, Modifier.weight(1f))
                    Control("Navigation", AndroidShortcut.NAVIGATION, onShortcut, Modifier.weight(1f))
                }
            } else {
                Control("Internet", AndroidShortcut.INTERNET, onShortcut)
                Control("Navigation", AndroidShortcut.NAVIGATION, onShortcut)
            }
        }
    }
    if (confirmRemoval) {
        AlertDialog(
            onDismissRequest = { confirmRemoval = false },
            title = { Text("Remove controls card?") },
            confirmButton = { TextButton(onClick = onRemove) { Text("Remove") } },
            dismissButton = { TextButton(onClick = { confirmRemoval = false }) { Text("Cancel") } }
        )
    }
}

@Composable
private fun Control(title: String, shortcut: AndroidShortcut, onShortcut: (AndroidShortcut) -> Unit, modifier: Modifier = Modifier) {
    TextButton(
        onClick = { onShortcut(shortcut) },
        modifier = modifier.height(40.dp),
        contentPadding = PaddingValues(horizontal = 6.dp)
    ) {
        Text(title, color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}
