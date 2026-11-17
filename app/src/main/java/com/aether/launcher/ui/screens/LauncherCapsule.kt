package com.aether.launcher.ui.screens

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

@Composable
internal fun LauncherCapsule(drag: LauncherAppDrag, modifier: Modifier = Modifier) {
    val source = drag.source ?: return
    val shape = RoundedCornerShape(24.dp)
    Column(
        modifier.widthIn(min = 126.dp, max = 330.dp)
            .animateContentSize()
            .clip(shape)
            .background(Brush.verticalGradient(listOf(
                Color(0xFF1D1E21).copy(alpha = .88f),
                Color(0xFF08090B).copy(alpha = .85f)
            )))
            .border(1.dp, Color.White.copy(alpha = .18f), shape)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            if (drag.groupReady) "Drop to group apps" else "Place ${source.app.label}",
            color = Color.White,
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.padding(vertical = 3.dp),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            DragAction("Remove", drag, AppDropTarget.Remove)
            DragAction("App info", drag, AppDropTarget.Info)
        }
    }
}

@Composable
private fun DragAction(label: String, drag: LauncherAppDrag, target: AppDropTarget) {
    Box(
        Modifier.appDropTarget(drag, target)
            .background(
                if (drag.target == target) Color.White.copy(alpha = .25f) else Color.White.copy(alpha = .08f),
                RoundedCornerShape(16.dp)
            ).padding(horizontal = 18.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(label, color = Color.White, style = MaterialTheme.typography.labelMedium)
    }
}
