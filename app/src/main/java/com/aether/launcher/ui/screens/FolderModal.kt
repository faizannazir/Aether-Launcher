package com.aether.launcher.ui.screens

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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.aether.launcher.data.model.AppItem
import com.aether.launcher.data.model.FolderItem
import com.aether.launcher.glass.GlassLevel
import com.aether.launcher.glass.glassSurface
import com.aether.launcher.ui.components.SquircleIcon

@Composable
fun FolderModal(
    folder: FolderItem,
    installedApps: List<AppItem>,
    isDarkTheme: Boolean,
    isBlurEnabled: Boolean,
    onAppClick: (AppItem) -> Unit,
    onTitleChange: (String) -> Unit,
    onRemoveApp: (String) -> Unit = {},
    onUngroup: () -> Unit = {},
    onDismiss: () -> Unit
) {
    val textColor = if (isDarkTheme) Color.White else Color(0xFF191C1E)
    var isEditingTitle by remember { mutableStateOf(false) }
    var folderTitle by remember(folder.id, folder.title) { mutableStateOf(folder.title) }

    val folderApps = installedApps.filter { app ->
        folder.appPackages.contains(app.packageName)
    }

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .fillMaxSize()
            .clickable(onClick = onDismiss)
            .padding(24.dp)
    ) {
        Box(
            modifier = Modifier
                .clickable(enabled = false) {}
                .fillMaxWidth()
                .glassSurface(
                    level = GlassLevel.STRONG,
                    isDarkTheme = isDarkTheme,
                    blurEnabled = isBlurEnabled,
                    shape = RoundedCornerShape(32.dp)
                )
                .padding(24.dp)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (isEditingTitle) {
                        OutlinedTextField(
                            value = folderTitle,
                            onValueChange = {
                                folderTitle = it
                                onTitleChange(it)
                            },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    } else {
                        Text(
                            text = folder.title,
                            style = MaterialTheme.typography.titleSmall,
                            color = textColor,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Icon(
                        imageVector = Icons.Rounded.Edit,
                        contentDescription = "Edit Title",
                        tint = textColor.copy(alpha = 0.7f),
                        modifier = Modifier
                            .clickable { isEditingTitle = !isEditingTitle }
                            .padding(8.dp)
                    )

                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = "Close",
                        tint = textColor,
                        modifier = Modifier
                            .clickable(onClick = onDismiss)
                            .padding(8.dp)
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(240.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(folderApps, key = { it.packageName }) { app ->
                        var menuExpanded by remember(app.packageName) { mutableStateOf(false) }
                        Box {
                            SquircleIcon(
                                label = app.label,
                                iconDrawable = app.icon,
                                onClick = {
                                    onAppClick(app)
                                    onDismiss()
                                },
                                onLongClick = { menuExpanded = true },
                                isDarkTheme = isDarkTheme,
                                isBlurEnabled = isBlurEnabled
                            )
                            DropdownMenu(
                                expanded = menuExpanded,
                                onDismissRequest = { menuExpanded = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Remove from folder") },
                                    onClick = {
                                        menuExpanded = false
                                        onRemoveApp(app.packageName)
                                    }
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = onUngroup,
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        Text("Ungroup")
                    }
                    Button(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        Text("Done")
                    }
                }
            }
        }
    }
}
