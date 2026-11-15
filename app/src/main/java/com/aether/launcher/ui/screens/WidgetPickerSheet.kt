package com.aether.launcher.ui.screens

import android.appwidget.AppWidgetProviderInfo
import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Widgets
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
internal fun WidgetPickerSheet(
    providers: List<AppWidgetProviderInfo>,
    appName: String?,
    onProviderSelect: (AppWidgetProviderInfo) -> Unit,
    onClose: () -> Unit
) {
    val context = LocalContext.current
    Column(Modifier.fillMaxWidth().heightIn(max = 520.dp).padding(horizontal = 16.dp, vertical = 8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                appName?.let { "$it widgets" } ?: "Widgets & Glanceables",
                Modifier.weight(1f), style = MaterialTheme.typography.headlineSmall, maxLines = 2
            )
            IconButton(onClick = onClose) { Icon(Icons.Rounded.Close, "Close widgets") }
        }
        LazyColumn(
            contentPadding = PaddingValues(bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(providers, key = { it.provider.flattenToShortString() }) { provider ->
                val label by produceState("", provider) {
                    value = withContext(Dispatchers.IO) { provider.loadLabel(context.packageManager) }
                }
                val preview by produceState<Bitmap?>(null, provider, appName) {
                    if (appName != null) {
                        value = withContext(Dispatchers.IO) {
                            provider.loadPreviewImage(context, context.resources.displayMetrics.densityDpi)?.let { drawable ->
                                val width = drawable.intrinsicWidth.coerceAtLeast(1)
                                val height = drawable.intrinsicHeight.coerceAtLeast(1)
                                val scale = minOf(1f, 320f / maxOf(width, height))
                                drawable.toBitmap((width * scale).toInt().coerceAtLeast(1), (height * scale).toInt().coerceAtLeast(1))
                            }
                        }
                    }
                }
                Column(
                    Modifier.fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(16.dp))
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    val bitmap = remember(preview) { preview?.asImageBitmap() }
                    if (bitmap != null) {
                        Image(bitmap, contentDescription = null, modifier = Modifier.fillMaxWidth().height(112.dp),
                            contentScale = ContentScale.Fit)
                    }
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        if (bitmap == null) {
                            Icon(Icons.Rounded.Widgets, null, Modifier.padding(end = 10.dp))
                        }
                        Text(label, Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                        TextButton(onClick = { onProviderSelect(provider) }) { Text("Add") }
                    }
                }
            }
        }
    }
}
