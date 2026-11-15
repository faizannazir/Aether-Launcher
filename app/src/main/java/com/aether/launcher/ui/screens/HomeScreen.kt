package com.aether.launcher.ui.screens

import android.appwidget.AppWidgetHost
import android.appwidget.AppWidgetManager
import android.text.format.DateFormat
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.repeatOnLifecycle
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aether.launcher.data.model.AppItem
import com.aether.launcher.data.model.FolderItem
import com.aether.launcher.data.model.GestureAction
import com.aether.launcher.data.model.HomeBackground
import com.aether.launcher.data.model.HomeGesture
import com.aether.launcher.data.model.HomeLayoutItem
import com.aether.launcher.ui.components.FloatingDock
import com.aether.launcher.ui.components.HarmonyServiceWidget
import com.aether.launcher.ui.components.SmartFolder2x2
import com.aether.launcher.ui.components.SquircleIcon
import com.aether.launcher.ui.components.WidgetHostContainer
import com.aether.launcher.viewmodel.LauncherState
import kotlinx.coroutines.delay
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

object ClockFormatter {
    fun formatTime(time: LocalDateTime, is24Hour: Boolean, locale: Locale = Locale.getDefault()): String {
        return time.format(DateTimeFormatter.ofPattern(if (is24Hour) "HH:mm" else "h:mm", locale))
    }

    fun formatDate(time: LocalDateTime, locale: Locale = Locale.getDefault()): String {
        return time.format(DateTimeFormatter.ofPattern("EEEE, d MMMM", locale))
    }

    fun calculateMillisToNextMinute(time: LocalDateTime): Long {
        return ((60 - time.second) * 1000L - time.nano / 1_000_000L).coerceAtLeast(100L)
    }
}

@Composable
fun GlanceClockHeader(
    modifier: Modifier = Modifier,
    timeProvider: () -> LocalDateTime = { LocalDateTime.now() }
) {
    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    var now by remember { mutableStateOf(timeProvider()) }
    val is24Hour = remember(context) { DateFormat.is24HourFormat(context) }

    LaunchedEffect(lifecycle) {
        lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            while (true) {
                now = timeProvider()
                delay(ClockFormatter.calculateMillisToNextMinute(now))
            }
        }
    }

    val primary = Color.White
    val secondary = Color.White.copy(alpha = 0.72f)

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.Start
    ) {
        Text(
            text = ClockFormatter.formatDate(now),
            color = secondary,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(start = 22.dp)
        )
        Text(
            text = ClockFormatter.formatTime(now, is24Hour),
            color = primary,
            fontSize = 52.sp,
            lineHeight = 56.sp,
            fontWeight = FontWeight.Light,
            letterSpacing = (-2).sp,
            modifier = Modifier.padding(start = 20.dp, top = 2.dp)
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun HomeScreen(
    state: LauncherState,
    appWidgetManager: AppWidgetManager,
    appWidgetHost: AppWidgetHost,
    actions: AppActions,
    onOpenDrawer: () -> Unit,
    onOpenSearch: () -> Unit,
    onOpenEditor: () -> Unit,
    onOpenFolder: (FolderItem) -> Unit,
    onAndroidShortcut: (AndroidShortcut) -> Unit,
    widgetPackages: Set<String>,
    onAppWidgets: (AppItem) -> Unit,
    onGesture: (HomeGesture) -> Unit,
    onPageSelected: (Int) -> Unit
) {
    val options = state.options
    val currentGesture by rememberUpdatedState(onGesture)
    val currentAppWidgets by rememberUpdatedState(onAppWidgets)
    val isRtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    val pageStep by rememberUpdatedState<(Int) -> Unit> { step ->
        onPageSelected(state.activePageIndex + if (isRtl) -step else step)
    }
    val gradient = remember(state.isDarkTheme) {
        Brush.verticalGradient(
            if (state.isDarkTheme) listOf(Color(0xFF171714), Color(0xFF302C25), Color(0xFF1E1C19))
            else listOf(Color(0xFF3A342A), Color(0xFF655740), Color(0xFF302A22))
        )
    }
    val backdrop = when (options.background) {
        HomeBackground.WALLPAPER -> Modifier.background(Color.Black.copy(alpha = 0.22f))
        HomeBackground.GRADIENT -> Modifier.background(gradient)
        HomeBackground.SOLID -> Modifier.background(Color(0xFF272624))
    }
    val activePage = state.pages.getOrNull(state.activePageIndex)
    val appByPackage = remember(state.installedApps) { state.installedApps.associateBy { it.packageName } }
    val visibleItems = remember(activePage, appByPackage) {
        activePage?.items.orEmpty().filter { it !is HomeLayoutItem.App || it.packageName in appByPackage }
            .sortedBy { it.position }
    }
    val canGroup = remember(visibleItems) { visibleItems.count { it is HomeLayoutItem.App } > 1 }
    val gesturesEnabled = state.isLayoutReady && !state.isCustomizationSheetOpen && !state.isWidgetSheetOpen && state.activeFolder == null
    val doubleTap = options.actionFor(HomeGesture.DOUBLE_TAP) != GestureAction.NONE
    val longPress = options.actionFor(HomeGesture.LONG_PRESS) != GestureAction.NONE
    val glassAlpha = 0.12f + options.glassOpacity * 0.28f

    Box(
        Modifier.fillMaxSize().then(backdrop)
            .statusBarsPadding().navigationBarsPadding()
            .semantics {
                customActions = listOf(
                    CustomAccessibilityAction("Open all apps") { onOpenDrawer(); true },
                    CustomAccessibilityAction("Edit Home") { onOpenEditor(); true }
                )
            }
            .homeSwipes(gesturesEnabled, { currentGesture(it) }, { pageStep(it) })
            .then(if (gesturesEnabled && (doubleTap || longPress)) {
                Modifier.pointerInput(doubleTap, longPress) {
                    detectTapGestures(
                        onDoubleTap = if (doubleTap) { _ -> currentGesture(HomeGesture.DOUBLE_TAP) } else null,
                        onLongPress = if (longPress) { _ -> currentGesture(HomeGesture.LONG_PRESS) } else null
                    )
                }
            } else Modifier)
    ) {
        if (!state.isLayoutReady) return@Box
        Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
            if (options.showClock) GlanceClockHeader(Modifier.padding(top = 18.dp, bottom = 20.dp))
            else Spacer(Modifier.height(14.dp))
            key(activePage?.pageIndex) {
                val gridState = rememberLazyGridState()
                LazyVerticalGrid(
                    state = gridState,
                    userScrollEnabled = gridState.canScrollBackward || gridState.canScrollForward,
                    columns = GridCells.Fixed(options.grid.columns),
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 2.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(visibleItems, key = { it.id }, span = { item ->
                        GridItemSpan(when (item) {
                            is HomeLayoutItem.App -> 1
                            is HomeLayoutItem.Folder -> 2
                            is HomeLayoutItem.Widget -> item.spanX.coerceIn(2, maxLineSpan)
                            is HomeLayoutItem.ServiceWidget -> if (maxLineSpan == 4) 2 else maxLineSpan
                        })
                    }) { item ->
                        when (item) {
                            is HomeLayoutItem.App -> {
                                val app = appByPackage.getValue(item.packageName)
                                var menuOpen by remember { mutableStateOf(false) }
                                val offersWidgets = options.swipeForWidgets && app.packageName in widgetPackages
                                val openWidgets = remember(app, offersWidgets, gesturesEnabled) {
                                    if (offersWidgets && gesturesEnabled) ({ currentAppWidgets(app) }) else null
                                }
                                val widgetGesture = Modifier.appWidgetSwipe(openWidgets)
                                    .then(if (openWidgets == null) Modifier else Modifier.semantics {
                                        customActions = listOf(CustomAccessibilityAction("Show widgets") {
                                            openWidgets()
                                            true
                                        })
                                    })
                                Box(Modifier.fillMaxWidth().then(widgetGesture), contentAlignment = Alignment.Center) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        SquircleIcon(
                                            app = app,
                                            onClick = { actions.launch(app) },
                                            onLongClick = { menuOpen = true },
                                            showLabel = options.showLabels,
                                            size = options.iconSize.dp.coerceAtMost(if (options.grid.columns == 5) 50.dp else 56.dp),
                                            isDarkTheme = state.isDarkTheme,
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                        if (offersWidgets) Box(
                                            Modifier.padding(top = 2.dp).size(10.dp, 2.dp).clip(CircleShape)
                                                .background(Color.White.copy(alpha = 0.58f))
                                        )
                                    }
                                    AppMenu(menuOpen, { menuOpen = false }, app, actions,
                                        state.dockApps.any { it.packageName == app.packageName }, item, canGroup)
                                }
                            }
                            is HomeLayoutItem.Folder -> {
                                val folder = remember(item) { FolderItem(item.id, item.title, item.appPackages) }
                                Box(Modifier.height(120.dp)) {
                                    SmartFolder2x2(folder, state.installedApps, state.isDarkTheme, options.glass,
                                        actions.launch, { onOpenFolder(folder) })
                                }
                            }
                            is HomeLayoutItem.ServiceWidget -> Box(Modifier.height(120.dp)) {
                                HarmonyServiceWidget(item, options.glass, onAndroidShortcut,
                                    onRemove = { actions.remove(item) })
                            }
                            is HomeLayoutItem.Widget -> Box(Modifier.height((item.spanY.coerceIn(1, 4) * 62).dp)) {
                                WidgetHostContainer(item, appWidgetManager, appWidgetHost, state.isDarkTheme, options.glass)
                                RemoveTile { actions.remove(item) }
                            }
                        }
                    }
                }
            }

            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                state.pages.indices.forEach { index ->
                    Box(
                        Modifier.size(26.dp).combinedClickable(
                            onClick = { onPageSelected(index) },
                            onLongClick = onOpenEditor,
                            onLongClickLabel = "Edit Home"
                        ).semantics { contentDescription = "Home page ${index + 1} of ${state.pages.size}" },
                        contentAlignment = Alignment.Center
                    ) {
                        Box(Modifier.size(if (index == state.activePageIndex) 7.dp else 4.dp).clip(CircleShape)
                            .background(Color.White.copy(alpha = if (index == state.activePageIndex) 1f else 0.56f)))
                    }
                }
            }
            if (options.showSearch) {
                LauncherSearchBar(
                    foreground = Color.White,
                    background = if (options.glass) Color.Black.copy(alpha = glassAlpha) else Color(0xFF242321),
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                    onOpen = onOpenSearch
                )
            }

            if (options.showDock) {
                FloatingDock(
                    dockApps = state.dockApps,
                    actions = actions,
                    isDarkTheme = state.isDarkTheme,
                    isBlurEnabled = options.glass,
                    glassOpacity = options.glassOpacity,
                    iconSize = options.iconSize.dp.coerceAtMost(46.dp),
                    modifier = Modifier.padding(bottom = 12.dp)
                )
            }
        }
    }
}

@Composable
private fun BoxScope.RemoveTile(onRemove: () -> Unit) {
    var confirm by remember { mutableStateOf(false) }
    IconButton(onClick = { confirm = true }, modifier = Modifier.align(Alignment.TopEnd)) {
        Icon(Icons.Rounded.Close, "Remove widget", tint = Color.White,
            modifier = Modifier.size(24.dp).background(Color.Black.copy(alpha = 0.65f), CircleShape).padding(3.dp))
    }
    if (confirm) androidx.compose.material3.AlertDialog(
        onDismissRequest = { confirm = false },
        title = { Text("Remove widget?") },
        confirmButton = { androidx.compose.material3.TextButton(onClick = onRemove) { Text("Remove") } },
        dismissButton = { androidx.compose.material3.TextButton(onClick = { confirm = false }) { Text("Cancel") } }
    )
}
