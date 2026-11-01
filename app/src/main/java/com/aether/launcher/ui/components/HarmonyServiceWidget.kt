package com.aether.launcher.ui.components

import android.content.Intent
import android.provider.Settings
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Widgets
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aether.launcher.data.model.HomeLayoutItem
import com.aether.launcher.glass.GlassLevel
import com.aether.launcher.glass.glassSurface

@Composable
fun HarmonyServiceWidget(
    widgetItem: HomeLayoutItem.ServiceWidget,
    isDarkTheme: Boolean,
    isBlurEnabled: Boolean,
    onToggleFocus: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val textColor = if (isDarkTheme) Color.White else Color(0xFF191C1E)
    val accentColor = if (isDarkTheme) Color(0xFF00FBFF) else Color(0xFF0284C7)

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
            .padding(14.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Rounded.Widgets,
                        contentDescription = "Service Control",
                        tint = accentColor,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "  SERVICE WIDGET",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = textColor.copy(alpha = 0.9f)
                    )
                }

                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(accentColor, RoundedCornerShape(4.dp))
                )
            }

            Column {
                Text(
                    text = widgetItem.title,
                    style = MaterialTheme.typography.titleSmall.copy(fontSize = 15.sp, fontWeight = FontWeight.SemiBold),
                    color = textColor,
                    maxLines = 1
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = widgetItem.subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = textColor.copy(alpha = 0.75f),
                    maxLines = 1
                )
            }

            // Quick Control Action Buttons
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Focus Toggle Button (Launcher Focus State)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier
                        .weight(1f)
                        .clickable(onClick = onToggleFocus)
                        .glassSurface(
                            level = GlassLevel.SUBTLE,
                            isDarkTheme = isDarkTheme,
                            blurEnabled = isBlurEnabled,
                            shape = RoundedCornerShape(12.dp)
                        )
                        .padding(horizontal = 6.dp, vertical = 6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.CheckCircle,
                        contentDescription = "Launcher Focus Mode",
                        tint = if (widgetItem.isFocusActive) accentColor else textColor.copy(alpha = 0.4f),
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = if (widgetItem.isFocusActive) " Focus ON" else " Focus OFF",
                        style = MaterialTheme.typography.labelSmall,
                        color = textColor
                    )
                }

                // System Settings Launcher Button
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier
                        .weight(1f)
                        .clickable {
                            try {
                                val intent = Intent(Settings.ACTION_SETTINGS).apply {
                                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                }
                                context.startActivity(intent)
                            } catch (e: Exception) {
                                Toast.makeText(context, "Unable to open Settings", Toast.LENGTH_SHORT).show()
                            }
                        }
                        .glassSurface(
                            level = GlassLevel.SUBTLE,
                            isDarkTheme = isDarkTheme,
                            blurEnabled = isBlurEnabled,
                            shape = RoundedCornerShape(12.dp)
                        )
                        .padding(horizontal = 6.dp, vertical = 6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Settings,
                        contentDescription = "Settings",
                        tint = textColor.copy(alpha = 0.9f),
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = " Settings",
                        style = MaterialTheme.typography.labelSmall,
                        color = textColor
                    )
                }
            }
        }
    }
}
