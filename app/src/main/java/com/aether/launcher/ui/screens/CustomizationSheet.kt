package com.aether.launcher.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.aether.launcher.glass.GlassLevel
import com.aether.launcher.glass.glassSurface

@Composable
fun CustomizationSheet(
    state: com.aether.launcher.viewmodel.LauncherState,
    onDarkThemeToggle: (Boolean) -> Unit,
    onBlurToggle: (Boolean) -> Unit,
    onBlurIntensityChange: (Float) -> Unit,
    onClose: () -> Unit
) {
    val textColor = if (state.isDarkTheme) Color.White else Color(0xFF191C1E)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .glassSurface(
                level = GlassLevel.STRONG,
                isDarkTheme = state.isDarkTheme,
                blurEnabled = state.isBlurEnabled,
                shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)
            )
            .padding(24.dp)
    ) {
        Column {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Customization & Styles",
                    style = MaterialTheme.typography.headlineMedium,
                    color = textColor,
                    modifier = Modifier.weight(1f)
                )

                Icon(
                    imageVector = Icons.Rounded.Close,
                    contentDescription = "Close",
                    tint = textColor,
                    modifier = Modifier.clickable(onClick = onClose)
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Dark Theme Toggle Row
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Dark Glass Theme", style = MaterialTheme.typography.bodyLarge, color = textColor)
                    Text("Apply deep obsidian glass surfaces", style = MaterialTheme.typography.labelSmall, color = textColor.copy(alpha = 0.6f))
                }
                Switch(checked = state.isDarkTheme, onCheckedChange = onDarkThemeToggle)
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Glass Surface Effects Toggle Row
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Glass Translucency Effects", style = MaterialTheme.typography.bodyLarge, color = textColor)
                    Text("Translucent glass material with specular edge highlights", style = MaterialTheme.typography.labelSmall, color = textColor.copy(alpha = 0.6f))
                }
                Switch(checked = state.isBlurEnabled, onCheckedChange = onBlurToggle)
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Glass Surface Intensity Slider
            Text(
                text = "Glass Surface Opacity: ${(state.blurIntensity * 100).toInt()}%",
                style = MaterialTheme.typography.bodyMedium,
                color = textColor
            )
            Slider(
                value = state.blurIntensity,
                onValueChange = onBlurIntensityChange,
                valueRange = 0f..1f
            )

            Spacer(modifier = Modifier.height(20.dp))

            Button(
                onClick = onClose,
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Done")
            }
        }
    }
}
