package com.aether.launcher.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.aether.launcher.data.model.AppItem
import com.aether.launcher.ui.components.SquircleIcon

@Composable
fun SearchOverlay(
    state: com.aether.launcher.viewmodel.LauncherState,
    onQueryChange: (String) -> Unit,
    onAppClick: (AppItem) -> Unit,
    onClose: () -> Unit,
    onGoogleSearch: (String) -> Unit,
    onGemini: () -> Unit
) {
    val textColor = if (state.isDarkTheme) Color.White else Color(0xFF191C1E)

    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { focus.requestFocus() }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            LauncherSearchBar(
                foreground = textColor,
                background = MaterialTheme.colorScheme.surfaceVariant,
                glass = state.options.glass,
                modifier = Modifier.focusRequester(focus),
                value = state.searchQuery,
                onChange = onQueryChange,
                onClose = onClose,
                onSubmit = state.searchResults.firstOrNull()?.let { app -> { onAppClick(app) } }
            )

            SearchProviderActions(onGoogleSearch = { onGoogleSearch(state.searchQuery) }, onGemini = onGemini)
            Spacer(modifier = Modifier.height(8.dp))

            // Search Results
            if (state.searchResults.isNotEmpty()) {
                Text(
                    text = "TOP MATCHES",
                    style = MaterialTheme.typography.labelSmall,
                    color = textColor.copy(alpha = 0.6f)
                )

                Spacer(modifier = Modifier.height(8.dp))

                LazyColumn(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(state.searchResults) { app ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onAppClick(app) }
                                .padding(vertical = 8.dp)
                        ) {
                            SquircleIcon(
                                app = app,
                                onClick = { onAppClick(app) },
                                showLabel = false,
                                isDarkTheme = state.isDarkTheme,
                                size = 44.dp
                            )
                            Spacer(modifier = Modifier.padding(start = 12.dp))
                            Column {
                                Text(
                                    text = app.label,
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = textColor
                                )
                                Text(
                                    text = app.packageName,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = textColor.copy(alpha = 0.5f)
                                )
                            }
                        }
                    }
                }
            } else if (state.searchQuery.isNotEmpty()) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp)
                ) {
                    Text(
                        text = "No matching apps found.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = textColor.copy(alpha = 0.6f)
                    )
                }
            }
        }
    }
}
