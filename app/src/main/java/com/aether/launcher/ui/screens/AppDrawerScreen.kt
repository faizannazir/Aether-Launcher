package com.aether.launcher.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.aether.launcher.data.AppSearchEngine
import com.aether.launcher.data.model.AppCategory
import com.aether.launcher.data.model.AppItem
import com.aether.launcher.ui.components.SquircleIcon
import com.aether.launcher.viewmodel.LauncherState
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.floor

@Composable
internal fun AppDrawerScreen(
    state: LauncherState,
    actions: AppActions,
    onCategorySelect: (AppCategory) -> Unit,
    onCloseDrawer: () -> Unit
) {
    var query by remember { mutableStateOf("") }
    val grid = rememberLazyGridState()
    val close by rememberUpdatedState(onCloseDrawer)
    val focus = LocalFocusManager.current
    val scope = rememberCoroutineScope()
    val search = remember { AppSearchEngine() }
    val foreground = MaterialTheme.colorScheme.onSurfaceVariant
    val category = if (state.options.showCategories) state.selectedCategory else AppCategory.ALL
    val apps = remember(state.installedApps, category, query) {
        val matching = if (category == AppCategory.ALL) state.installedApps
            else state.installedApps.filter { it.category == category }
        if (query.isBlank()) matching else search.search(query, matching)
    }
    val sections = remember(apps, query.isBlank()) {
        if (query.isNotBlank()) emptyMap()
        else apps.groupBy(::sectionFor).toSortedMap(compareBy<String> { it == "#" }.thenBy { it })
    }
    val jumpTargets = remember(sections) {
        var index = 0
        sections.mapValues { (_, entries) ->
            val target = index
            index += entries.size + 1
            target
        }
    }
    LaunchedEffect(query, category) { grid.scrollToItem(0) }

    Column(
        Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = if (state.options.glass) .94f else 1f))
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
        LauncherSearchBar(
            foreground, MaterialTheme.colorScheme.surface,
            Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
            value = query, onChange = { query = it }
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
                Text(
                    if (query.isBlank()) "No apps here" else "No matching apps",
                    color = foreground,
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        } else {
            Row(Modifier.weight(1f)) {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(state.options.drawerColumns),
                    state = grid,
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(start = 12.dp, end = 4.dp, bottom = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalArrangement = Arrangement.spacedBy(9.dp)
                ) {
                    if (sections.isEmpty()) {
                        items(apps, key = { it.packageName }) { AppCell(it, state, actions) }
                    } else {
                        sections.forEach { (letter, entries) ->
                            item(key = "section_$letter", span = { GridItemSpan(maxLineSpan) }) {
                                Text(letter, Modifier.padding(start = 8.dp, top = 7.dp),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            items(entries, key = { it.packageName }) { AppCell(it, state, actions) }
                        }
                    }
                }
                if (jumpTargets.size > 1) {
                    val letters = jumpTargets.keys.toList()
                    IndexRail(letters, Modifier.fillMaxHeight().width(26.dp).padding(vertical = 8.dp)) { letter ->
                        focus.clearFocus()
                        scope.launch { grid.scrollToItem(jumpTargets.getValue(letter)) }
                    }
                }
            }
        }
    }
}

private fun sectionFor(app: AppItem): String =
    app.label.trim().firstOrNull()?.takeIf { it.isLetter() }?.uppercase() ?: "#"

@Composable
private fun AppCell(app: AppItem, state: LauncherState, actions: AppActions) {
    SquircleIcon(
        app, onClick = { actions.launch(app) },
        onLongClick = { actions.startDrag(app, null, false) },
        size = 42.dp, isDarkTheme = state.isDarkTheme,
        labelColor = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.fillMaxWidth()
    )
}

@Composable
private fun IndexRail(letters: List<String>, modifier: Modifier, onJump: (String) -> Unit) {
    val jump by rememberUpdatedState(onJump)
    Column(
        modifier.semantics {
            customActions = letters.map { letter ->
                CustomAccessibilityAction("Jump to $letter") { jump(letter); true }
            }
        }.pointerInput(letters) {
            awaitEachGesture {
                val down = awaitFirstDown()
                var selected = -1
                fun select(y: Float) {
                    val index = floor(y / size.height * letters.size).toInt().coerceIn(0, letters.lastIndex)
                    if (index != selected) {
                        selected = index
                        jump(letters[index])
                    }
                }
                down.consume()
                select(down.position.y)
                while (true) {
                    val finger = awaitPointerEvent().changes.firstOrNull { it.id == down.id && it.pressed } ?: break
                    finger.consume()
                    select(finger.position.y)
                }
            }
        },
        verticalArrangement = Arrangement.Center
    ) {
        letters.forEach { letter ->
            Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Text(letter, textAlign = TextAlign.Center, style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
            }
        }
    }
}
