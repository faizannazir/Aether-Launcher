package com.aether.launcher.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.aether.launcher.data.AppSearchEngine
import com.aether.launcher.data.model.AppCategory
import com.aether.launcher.data.model.AppItem
import com.aether.launcher.ui.components.SquircleIcon
import com.aether.launcher.viewmodel.LauncherState
import kotlin.math.abs

@Composable
internal fun AppDrawerScreen(
    state: LauncherState,
    actions: AppActions,
    onCategorySelect: (AppCategory) -> Unit,
    visibility: State<Boolean>,
    onGoogleSearch: (String) -> Unit,
    onGemini: () -> Unit,
    onAddToHome: (AppItem) -> Unit,
    onCloseDrawer: () -> Unit
) {
    var query by remember { mutableStateOf("") }
    val grid = rememberLazyGridState()
    val focusManager = LocalFocusManager.current
    var searchFocused by remember { mutableStateOf(false) }
    LaunchedEffect(visibility) {
        snapshotFlow { visibility.value }.collect { visible ->
            if (!visible) {
                if (searchFocused) focusManager.clearFocus(force = true)
                query = ""
                if (grid.canScrollBackward) grid.scrollToItem(0)
            }
        }
    }
    val close by rememberUpdatedState(onCloseDrawer)
    val search = remember { AppSearchEngine() }
    val colors = MaterialTheme.colorScheme
    val foreground = colors.onSurface
    val glassOpacity = state.options.glassOpacity.coerceIn(0f, 1f)
    val backdrop = if (state.options.glass) {
        val surface = colors.surfaceVariant
        val glass = remember(surface, glassOpacity) {
            Brush.verticalGradient(
                0f to lerp(surface, Color.White, .06f).copy(alpha = .84f + .08f * glassOpacity),
                .25f to surface.copy(alpha = .90f + .06f * glassOpacity),
                1f to surface.copy(alpha = .94f + .05f * glassOpacity)
            )
        }
        Modifier.background(glass)
    } else Modifier.background(colors.surfaceVariant)
    val category = if (state.options.showCategories) state.selectedCategory else AppCategory.ALL
    val apps = remember(state.installedApps, category, query) {
        val matching = if (category == AppCategory.ALL) state.installedApps
            else state.installedApps.filter { it.category == category }
        if (query.isBlank()) matching else search.search(query, matching)
    }
    LaunchedEffect(query, category) {
        if (grid.canScrollBackward) grid.scrollToItem(0)
    }

    Column(
        Modifier.fillMaxSize().then(backdrop)
            .statusBarsPadding().navigationBarsPadding().imePadding()
            .semantics {
                customActions = listOf(CustomAccessibilityAction("Return to Home") { close(); true })
            }
            .pointerInput(grid) {
                val threshold = 48.dp.toPx()
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                    if (grid.canScrollBackward) return@awaitEachGesture
                    while (true) {
                        val event = awaitPointerEvent(PointerEventPass.Initial)
                        if (event.changes.count { it.pressed } > 1) break
                        val finger = event.changes.firstOrNull { it.id == down.id } ?: break
                        val travel = finger.position - down.position
                        if (travel.y < -viewConfiguration.touchSlop || abs(travel.x) > threshold) break
                        if (!finger.pressed) {
                            if (travel.y > threshold) close()
                            break
                        }
                    }
                }
            }
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(start = 20.dp, end = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("All apps", modifier = Modifier.weight(1f), color = foreground,
                style = MaterialTheme.typography.titleLarge)
            SearchProviderActions(onGoogleSearch = { onGoogleSearch(query) }, onGemini = onGemini)
        }
        LauncherSearchBar(
            foreground, if (state.options.glass) colors.surfaceContainerHigh.copy(alpha = .86f) else colors.surface,
            Modifier.padding(start = 16.dp, end = 16.dp, bottom = 8.dp)
                .onFocusChanged { searchFocused = it.hasFocus },
            value = query, onChange = { query = it },
            glass = state.options.glass,
            onSubmit = apps.firstOrNull()?.takeIf { query.isNotBlank() }?.let { app ->
                { actions.launch(app) }
            }
        )
        if (state.options.showCategories) {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(AppCategory.entries) { appCategory ->
                    FilterChip(category == appCategory, onClick = { onCategorySelect(appCategory) },
                        label = { Text(appCategory.displayName) })
                }
            }
        }
        if (apps.isEmpty()) {
            Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        if (query.isBlank()) "No apps here" else "No matching apps",
                        color = foreground, style = MaterialTheme.typography.bodyLarge
                    )
                    if (query.isNotBlank()) TextButton(onClick = { query = "" }) { Text("Clear search") }
                }
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(state.options.drawerColumns),
                state = grid,
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentPadding = PaddingValues(start = 14.dp, end = 14.dp, top = 10.dp, bottom = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(15.dp)
            ) {
                items(apps, key = { it.packageName }) { AppCell(it, state, actions, onAddToHome) }
            }
        }
    }
}

@Composable
private fun AppCell(app: AppItem, state: LauncherState, actions: AppActions, onAddToHome: (AppItem) -> Unit) {
    SquircleIcon(
        app, onClick = { actions.launch(app) },
        onLongClick = { actions.startDrag(app, null, false) },
        size = 46.dp, isDarkTheme = state.isDarkTheme,
        labelColor = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier.fillMaxWidth().semantics {
            customActions = listOf(CustomAccessibilityAction("Add to Home") {
                onAddToHome(app)
                true
            })
        }
    )
}
