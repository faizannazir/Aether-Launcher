package com.aether.launcher.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.aether.launcher.data.model.GestureAction
import com.aether.launcher.data.model.HomeBackground
import com.aether.launcher.data.model.HomeGesture
import com.aether.launcher.data.model.HomeGrid
import com.aether.launcher.data.model.LauncherOptions
import com.aether.launcher.data.model.LauncherTheme

private enum class SettingsPage(val title: String) {
    HOME("Home"), GESTURES("Gestures"), ANDROID("Android")
}

internal enum class AndroidShortcut(val title: String) {
    SETTINGS("Android Settings"), INTERNET("Internet controls"), BLUETOOTH("Bluetooth"), NAVIGATION("System navigation"),
    DISPLAY("Display"), SOUND("Sound"), NOTIFICATIONS("Notifications"),
    ACCESSIBILITY("Accessibility"), WALLPAPER("Wallpaper & style")
}

@Composable
internal fun LauncherSettingsScreen(
    options: LauncherOptions,
    isDefaultLauncher: Boolean,
    onBack: () -> Unit,
    onSetDefaultLauncher: () -> Unit,
    onAndroidShortcut: (AndroidShortcut) -> Unit,
    onStarterLayout: () -> Unit,
    onUpdate: ((LauncherOptions) -> LauncherOptions) -> Unit
) {
    var selected by rememberSaveable { mutableStateOf(SettingsPage.HOME) }
    var confirmStarter by remember { mutableStateOf(false) }
    if (confirmStarter) AlertDialog(
        onDismissRequest = { confirmStarter = false },
        title = { Text("Apply starter layout?") },
        text = { Text("Replace Home pages, widgets and dock with the new starter layout. Your appearance and gesture preferences will be kept.") },
        confirmButton = {
            TextButton(onClick = { confirmStarter = false; onStarterLayout() }) { Text("Apply") }
        },
        dismissButton = { TextButton(onClick = { confirmStarter = false }) { Text("Cancel") } }
    )
    Column(
        Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface)
            .statusBarsPadding().navigationBarsPadding()
    ) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Back") }
            Text("Launcher settings", style = MaterialTheme.typography.headlineSmall)
        }
        TabRow(selectedTabIndex = selected.ordinal) {
            SettingsPage.entries.forEach { page ->
                Tab(selected = selected == page, onClick = { selected = page }, text = { Text(page.title) })
            }
        }
        key(selected) {
            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                when (selected) {
                    SettingsPage.HOME -> {
                        item {
                            SettingsGroup("Appearance") {
                                OptionRow("Theme", options.theme, LauncherTheme.entries, LauncherTheme::title) { value ->
                                    onUpdate { it.copy(theme = value) }
                                }
                                OptionRow("Background", options.background, HomeBackground.entries, HomeBackground::title) { value ->
                                    onUpdate { it.copy(background = value) }
                                }
                                SettingsSwitch("Translucent surfaces", options.glass) { value -> onUpdate { it.copy(glass = value) } }
                                if (options.glass) {
                                    OptionRow("Surface opacity", options.glassOpacity, listOf(0f, 0.25f, 0.5f, 0.75f, 1f),
                                        { "${(it * 100).toInt()}%" }) { value -> onUpdate { it.copy(glassOpacity = value) } }
                                }
                            }
                        }
                        item {
                            SettingsGroup("Home layout") {
                                OptionRow("Home grid", options.grid, HomeGrid.entries, HomeGrid::title) { value ->
                                    onUpdate { it.copy(grid = value) }
                                }
                                OptionRow("Home icon size", options.iconSize, listOf(36, 40, 46, 50, 56), { "$it dp" }) { value ->
                                    onUpdate { it.copy(iconSize = value) }
                                }
                                SettingsSwitch("App labels", options.showLabels) { value -> onUpdate { it.copy(showLabels = value) } }
                                SettingsSwitch("Clock and date", options.showClock) { value -> onUpdate { it.copy(showClock = value) } }
                                SettingsSwitch("Search bar", options.showSearch) { value -> onUpdate { it.copy(showSearch = value) } }
                                SettingsSwitch("Dock", options.showDock) { value -> onUpdate { it.copy(showDock = value) } }
                                TextButton(onClick = { confirmStarter = true }) { Text("Apply starter layout") }
                            }
                        }
                        item {
                            SettingsGroup("App drawer") {
                                OptionRow("Drawer columns", options.drawerColumns, listOf(3, 4, 5), Int::toString) { value ->
                                    onUpdate { it.copy(drawerColumns = value) }
                                }
                                SettingsSwitch("App categories", options.showCategories) { value -> onUpdate { it.copy(showCategories = value) } }
                            }
                        }
                    }
                    SettingsPage.GESTURES -> item {
                        SettingsGroup("Home gestures") {
                            HomeGesture.entries.forEach { gesture ->
                                OptionRow(gesture.title, options.actionFor(gesture), GestureAction.entries, GestureAction::title) { value ->
                                    onUpdate { it.copy(gestures = it.gestures + (gesture to value)) }
                                }
                            }
                            SettingsSwitch("Swipe up on an app for widgets", options.swipeForWidgets) { value ->
                                onUpdate { it.copy(swipeForWidgets = value) }
                            }
                            Text("Swipe sideways to change Home pages.", modifier = Modifier.padding(14.dp),
                                style = MaterialTheme.typography.bodySmall)
                        }
                    }
                    SettingsPage.ANDROID -> {
                        item {
                            SettingsGroup("Default Home app") {
                                Text(if (isDefaultLauncher) "Aether is active" else "Choose Aether in Android's Home app settings",
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp))
                                TextButton(onClick = onSetDefaultLauncher) { Text("Default Home settings") }
                            }
                        }
                        item {
                            SettingsGroup("Open Android controls") {
                                AndroidShortcut.entries.forEach { shortcut ->
                                    TextButton(onClick = { onAndroidShortcut(shortcut) }, modifier = Modifier.fillMaxWidth()) {
                                        Text(shortcut.title, modifier = Modifier.weight(1f))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingsGroup(title: String, content: @Composable ColumnScope.() -> Unit) {
    Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
        Column(Modifier.padding(vertical = 8.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(14.dp))
            content()
        }
    }
}

@Composable
private fun SettingsSwitch(title: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        Modifier.fillMaxWidth().heightIn(min = 52.dp)
            .toggleable(checked, role = Role.Switch, onValueChange = onChange).padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, Modifier.weight(1f))
        Switch(checked, onCheckedChange = null)
    }
}

@Composable
private fun <T> OptionRow(title: String, selected: T, options: List<T>, name: (T) -> String, onChange: (T) -> Unit) {
    var open by remember { mutableStateOf(false) }
    Box {
        TextButton(onClick = { open = true }, modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)) {
            Text(title, Modifier.weight(1f), color = MaterialTheme.colorScheme.onSurface)
            Text(name(selected), modifier = Modifier.padding(start = 10.dp))
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(name(option)) },
                    onClick = {
                        open = false
                        onChange(option)
                    }
                )
            }
        }
    }
}
