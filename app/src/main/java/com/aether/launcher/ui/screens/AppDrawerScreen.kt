package com.aether.launcher.ui.screens

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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import com.aether.launcher.data.model.AppCategory
import com.aether.launcher.data.model.AppItem
import com.aether.launcher.glass.GlassLevel
import com.aether.launcher.glass.glassSurface
import com.aether.launcher.ui.components.SquircleIcon

@Composable
fun AppDrawerScreen(
    state: com.aether.launcher.viewmodel.LauncherState,
    onAppClick: (AppItem) -> Unit,
    onPinApp: (AppItem) -> Unit = {},
    onCategorySelect: (AppCategory) -> Unit,
    onCloseDrawer: () -> Unit,
    onSettingsClick: () -> Unit = {}
) {
    var drawerSearchQuery by remember { mutableStateOf("") }
    val textColor = if (state.isDarkTheme) Color.White else Color(0xFF191C1E)
    val containerBg = if (state.isDarkTheme) Color(0xF2121316) else Color(0xF2F0F4F8)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(containerBg)
            .statusBarsPadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            // Header Row
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "App Drawer",
                    style = MaterialTheme.typography.headlineMedium,
                    color = textColor,
                    modifier = Modifier.weight(1f)
                )

                IconButton(onClick = onSettingsClick) {
                    Icon(Icons.Rounded.Settings, contentDescription = "Launcher settings", tint = textColor)
                }
                IconButton(onClick = onCloseDrawer) {
                    Icon(Icons.Rounded.Close, contentDescription = "Close", tint = textColor)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Inline Drawer Search Field
            OutlinedTextField(
                value = drawerSearchQuery,
                onValueChange = { drawerSearchQuery = it },
                placeholder = { Text("Search apps...", color = textColor.copy(alpha = 0.5f)) },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Rounded.Search,
                        contentDescription = "Search",
                        tint = textColor.copy(alpha = 0.7f)
                    )
                },
                trailingIcon = {
                    if (drawerSearchQuery.isNotEmpty()) {
                        Icon(
                            imageVector = Icons.Rounded.Close,
                            contentDescription = "Clear search",
                            tint = textColor.copy(alpha = 0.7f),
                            modifier = Modifier.clickable { drawerSearchQuery = "" }
                        )
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = textColor.copy(alpha = 0.05f),
                    unfocusedContainerColor = textColor.copy(alpha = 0.05f),
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = Color.Transparent,
                    focusedTextColor = textColor,
                    unfocusedTextColor = textColor
                ),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Category Filter Chips
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(AppCategory.entries.toTypedArray()) { category ->
                    val isSelected = state.selectedCategory == category
                    Box(
                        modifier = Modifier
                            .clickable { onCategorySelect(category) }
                            .glassSurface(
                                level = if (isSelected) GlassLevel.BALANCED else GlassLevel.SUBTLE,
                            isDarkTheme = state.isDarkTheme,
                                blurEnabled = state.isBlurEnabled,
                                shape = RoundedCornerShape(20.dp)
                            )
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = category.displayName,
                            style = MaterialTheme.typography.labelMedium,
                            color = if (isSelected) MaterialTheme.colorScheme.primary else textColor.copy(alpha = 0.7f)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Filtered App List
            val categoryApps = if (state.selectedCategory == AppCategory.ALL) {
                state.installedApps
            } else {
                state.installedApps.filter { it.category == state.selectedCategory }
            }

            val displayedApps = if (drawerSearchQuery.isBlank()) {
                categoryApps
            } else {
                categoryApps.filter { it.label.contains(drawerSearchQuery, ignoreCase = true) }
            }

            if (displayedApps.isEmpty()) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    Text(
                        text = if (state.installedApps.isEmpty()) "Loading applications..." else "No applications found",
                        style = MaterialTheme.typography.bodyLarge,
                        color = textColor.copy(alpha = 0.6f)
                    )
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(state.gridColumns.substringBefore("x").toIntOrNull()?.coerceIn(3, 5) ?: 4),
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(displayedApps, key = { it.packageName }) { app ->
                        SquircleIcon(
                            label = app.label,
                            iconDrawable = app.icon,
                            onClick = { onAppClick(app) },
                            onLongClick = { onPinApp(app) },
                            size = 44.dp,
                            isDarkTheme = state.isDarkTheme,
                            isBlurEnabled = state.isBlurEnabled
                        )
                    }
                }
            }
        }
    }
}
