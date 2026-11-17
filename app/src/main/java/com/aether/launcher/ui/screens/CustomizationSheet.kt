package com.aether.launcher.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.PopupProperties
import kotlin.math.roundToInt

@Composable
internal fun CustomizationSheet(
    anchor: Offset?,
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
    val margin = with(LocalDensity.current) { 12.dp.roundToPx() }
    val position = remember(anchor, margin) {
        object : PopupPositionProvider {
            override fun calculatePosition(
                anchorBounds: IntRect,
                windowSize: IntSize,
                layoutDirection: LayoutDirection,
                popupContentSize: IntSize
            ): IntOffset {
                val x = anchor?.x?.roundToInt() ?: windowSize.width / 2
                val y = anchor?.y?.roundToInt() ?: windowSize.height / 2
                return IntOffset(
                    (x - popupContentSize.width / 2).coerceIn(margin, (windowSize.width - popupContentSize.width - margin).coerceAtLeast(margin)),
                    (y - popupContentSize.height / 2).coerceIn(margin, (windowSize.height - popupContentSize.height - margin).coerceAtLeast(margin))
                )
            }
        }
    }
    Popup(
        popupPositionProvider = position,
        onDismissRequest = onClose,
        properties = PopupProperties(focusable = true)
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 4.dp,
            shadowElevation = 12.dp,
            modifier = Modifier.width(240.dp)
        ) {
            Column {
                DropdownMenuItem(text = { Text("Add widgets") }, onClick = onWidgets)
                DropdownMenuItem(text = { Text("Android controls card") }, onClick = onAddControls)
                DropdownMenuItem(text = { Text("Wallpaper & style") }, onClick = onWallpaper)
                DropdownMenuItem(text = { Text("Add Home page") }, onClick = onAddPage)
                if (canRemovePage) DropdownMenuItem(text = { Text("Remove empty page") }, onClick = onRemovePage)
                DropdownMenuItem(text = { Text("Launcher settings") }, onClick = onSettings)
                DropdownMenuItem(text = { Text("All apps") }, onClick = onApps)
            }
        }
    }
}
