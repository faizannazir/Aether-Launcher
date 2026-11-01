package com.aether.launcher.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.aether.launcher.viewmodel.LauncherState

@Composable
fun LauncherSettingsScreen(
    state: LauncherState,
    isDefaultLauncher: Boolean,
    onBack: () -> Unit,
    onSetDefaultLauncher: () -> Unit,
    onOpenSystemSettings: () -> Unit,
    onDarkThemeChange: (Boolean) -> Unit,
    onBlurChange: (Boolean) -> Unit,
    onBlurIntensityChange: (Float) -> Unit,
    onGridColumnsChange: (String) -> Unit
) {
    val foreground = if (state.isDarkTheme) Color(0xFFF2F4F6) else Color(0xFF17212B)
    val muted = foreground.copy(alpha = 0.62f)
    val background = if (state.isDarkTheme) Color(0xFF101419) else Color(0xFFF4F6F8)
    val card = if (state.isDarkTheme) Color(0xFF1B222A) else Color.White

    Column(
        Modifier.fillMaxSize().background(background).statusBarsPadding().navigationBarsPadding().padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.ArrowBack, "Back", tint = foreground, modifier = Modifier.clickable(onClick = onBack).padding(8.dp))
            Column(Modifier.weight(1f).padding(start = 8.dp)) {
                Text("Settings", style = MaterialTheme.typography.headlineSmall, color = foreground)
                Text("Aether Launcher", style = MaterialTheme.typography.bodySmall, color = muted)
            }
            Icon(Icons.Rounded.Settings, null, tint = muted)
        }
        Card(colors = CardDefaults.cardColors(containerColor = card), shape = RoundedCornerShape(22.dp)) {
            Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.CheckCircle, null, tint = if (isDefaultLauncher) Color(0xFF269B70) else muted)
                    Column(Modifier.weight(1f).padding(start = 12.dp)) {
                        Text(if (isDefaultLauncher) "Aether is your Home app" else "Choose Aether as Home app", color = foreground, style = MaterialTheme.typography.titleMedium)
                        Text(if (isDefaultLauncher) "Home button opens Aether" else "Android will ask you to confirm this change", color = muted, style = MaterialTheme.typography.bodySmall)
                    }
                }
                if (!isDefaultLauncher) Button(onClick = onSetDefaultLauncher, modifier = Modifier.fillMaxWidth()) { Text("Set as default launcher") }
            }
        }
        Text("APPEARANCE", color = muted, style = MaterialTheme.typography.labelMedium)
        Card(colors = CardDefaults.cardColors(containerColor = card), shape = RoundedCornerShape(22.dp)) {
            Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                PreferenceSwitch("Dark appearance", "Use a darker launcher surface", state.isDarkTheme, onDarkThemeChange, foreground, muted)
                PreferenceSwitch("Translucent surfaces", "Use blur only where supported", state.isBlurEnabled, onBlurChange, foreground, muted)
                Column {
                    Text("Surface strength: ${(state.blurIntensity * 100).toInt()}%", color = foreground)
                    Slider(state.blurIntensity, onBlurIntensityChange)
                }
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Home screen columns", color = foreground)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("3x5", "4x5", "5x5").forEach { grid ->
                            FilterChip(state.gridColumns == grid, { onGridColumnsChange(grid) }, label = { Text(grid.substringBefore("x")) })
                        }
                    }
                }
            }
        }
        Text("DEVICE", color = muted, style = MaterialTheme.typography.labelMedium)
        Card(colors = CardDefaults.cardColors(containerColor = card), shape = RoundedCornerShape(22.dp)) {
            Row(Modifier.fillMaxWidth().clickable(onClick = onOpenSystemSettings).padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.Tune, null, tint = foreground)
                Column(Modifier.weight(1f).padding(start = 12.dp)) {
                    Text("Android system settings", color = foreground)
                    Text("Open the device's actual Settings app", color = muted, style = MaterialTheme.typography.bodySmall)
                }
                Text("Open", color = MaterialTheme.colorScheme.primary)
            }
        }
        Text("Launcher preferences affect Aether screens. Android controls system Settings, Quick Settings, lock screen and gesture navigation.", color = muted, style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun PreferenceSwitch(
    title: String,
    subtitle: String,
    checked: Boolean,
    onChange: (Boolean) -> Unit,
    foreground: Color,
    muted: Color
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f).padding(end = 12.dp)) {
            Text(title, color = foreground, style = MaterialTheme.typography.titleSmall)
            Text(subtitle, color = muted, style = MaterialTheme.typography.bodySmall)
        }
        Switch(checked, onChange)
    }
}
