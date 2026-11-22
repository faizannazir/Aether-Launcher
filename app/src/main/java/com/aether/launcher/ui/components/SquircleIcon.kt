package com.aether.launcher.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Android
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aether.launcher.data.AppIconCache
import com.aether.launcher.data.model.AppItem

internal val LocalAppIcons = compositionLocalOf<AppIconCache?> { null }

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SquircleIcon(
    app: AppItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onLongClick: (() -> Unit)? = null,
    size: Dp = 48.dp,
    showLabel: Boolean = true,
    isDarkTheme: Boolean = false,
    labelColor: Color = Color.White
) {
    val icons = LocalAppIcons.current
    val pixelSize = with(LocalDensity.current) { 48.dp.roundToPx().coerceIn(48, 160) }
    val bitmap by produceState(icons?.cached(app, pixelSize), app, pixelSize) {
        value = icons?.cached(app, pixelSize) ?: icons?.load(app, pixelSize)
    }

    val accessibleDescription = "Launch ${app.label}"

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .semantics(mergeDescendants = true) {
                role = Role.Button
                contentDescription = accessibleDescription
            }
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick,
                onClickLabel = accessibleDescription
            )
            .padding(2.dp)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(size)
        ) {
            val image = remember(bitmap) { bitmap?.asImageBitmap() }
            if (image != null) {
                Image(
                    bitmap = image,
                    contentDescription = null,
                    filterQuality = FilterQuality.Low,
                    modifier = Modifier.size(size)
                )
            } else {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(size)
                        .clip(RoundedCornerShape(16.dp))
                        .background(if (isDarkTheme) Color(0x33FFFFFF) else Color(0x1FFFFFFF))
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Android,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(size * 0.65f)
                    )
                }
            }
        }

        if (showLabel) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = app.label,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                ),
                color = labelColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )
        }
    }
}
