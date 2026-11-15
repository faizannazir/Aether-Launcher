package com.aether.launcher.ui.screens

import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import com.aether.launcher.data.model.AppItem
import com.aether.launcher.data.model.HomeLayoutItem

internal data class AppActions(
    val launch: (AppItem) -> Unit,
    val info: (AppItem) -> Unit,
    val toggleDock: (AppItem) -> Unit,
    val pin: (AppItem) -> Unit,
    val remove: (HomeLayoutItem) -> Unit,
    val group: (HomeLayoutItem.App) -> Unit
)

@Composable
internal fun AppMenu(
    open: Boolean,
    close: () -> Unit,
    app: AppItem,
    actions: AppActions,
    inDock: Boolean,
    shortcut: HomeLayoutItem.App? = null,
    canGroup: Boolean = false,
    dockOnly: Boolean = false
) {
    fun perform(action: () -> Unit) {
        close()
        action()
    }
    DropdownMenu(expanded = open, onDismissRequest = close) {
        if (!dockOnly && shortcut == null) {
            DropdownMenuItem(text = { Text("Add to Home") }, onClick = { perform { actions.pin(app) } })
        }
        DropdownMenuItem(
            text = { Text(if (inDock) "Remove from dock" else "Add to dock") },
            onClick = { perform { actions.toggleDock(app) } }
        )
        if (shortcut != null) {
            if (canGroup) {
                DropdownMenuItem(text = { Text("Create folder") }, onClick = { perform { actions.group(shortcut) } })
            }
            DropdownMenuItem(text = { Text("Remove from Home") }, onClick = { perform { actions.remove(shortcut) } })
        }
        DropdownMenuItem(text = { Text("App info") }, onClick = { perform { actions.info(app) } })
    }
}
