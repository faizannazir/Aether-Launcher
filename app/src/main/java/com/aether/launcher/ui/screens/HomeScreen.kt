package com.aether.launcher.ui.screens

import android.appwidget.AppWidgetHost
import android.appwidget.AppWidgetManager
import android.text.format.DateFormat
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.IconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aether.launcher.data.model.AppItem
import com.aether.launcher.data.model.FolderItem
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
    isDarkTheme: Boolean,
    isBlurEnabled: Boolean,
    modifier: Modifier = Modifier,
    onSwipeDown: () -> Unit = {},
    timeProvider: () -> LocalDateTime = { LocalDateTime.now() }
) {
    val context = LocalContext.current
    var now by remember { mutableStateOf(timeProvider()) }
    val is24Hour = remember(context) { DateFormat.is24HourFormat(context) }

    LaunchedEffect(timeProvider) {
        while (true) {
            now = timeProvider()
            delay(ClockFormatter.calculateMillisToNextMinute(now))
        }
    }

    val primary = Color.White
    val secondary = Color.White.copy(alpha = 0.72f)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .pointerInput(onSwipeDown) {
                var distance = 0f
                detectVerticalDragGestures(
                    onDragStart = { distance = 0f },
                    onDragEnd = {
                        if (distance > 42.dp.toPx()) onSwipeDown()
                        distance = 0f
                    },
                    onDragCancel = { distance = 0f },
                    onVerticalDrag = { change, amount ->
                        distance += amount
                        change.consume()
                    }
                )
            },
        horizontalAlignment = Alignment.Start
    ) {
        Text(
            text = ClockFormatter.formatDate(now),
            color = secondary,
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(start = 24.dp)
        )
        Text(
            text = ClockFormatter.formatTime(now, is24Hour),
            color = primary,
            fontSize = 64.sp,
            lineHeight = 70.sp,
            fontWeight = FontWeight.Light,
            letterSpacing = (-3).sp,
            modifier = Modifier.padding(start = 20.dp, top = 2.dp)
        )
    }
}

@Composable
fun HomeScreen(
    state: LauncherState,
    appWidgetManager: AppWidgetManager?,
    appWidgetHost: AppWidgetHost?,
    onAppClick: (AppItem) -> Unit,
    onAppDrawerClick: () -> Unit,
    onSearchClick: () -> Unit,
    onFolderClick: (FolderItem) -> Unit,
    onOpenEditMode: () -> Unit,
    onStartDrag: (HomeLayoutItem, Int, Int) -> Unit,
    onDropItem: (Int) -> Unit,
    onToggleServiceWidgetFocus: (String) -> Unit = {},
    onSettingsClick: () -> Unit = {},
    onSwipeUp: () -> Unit = {},
    onPageSelected: (Int) -> Unit = {},
    onAddPage: () -> Unit = {}
) {
    val background = if (state.isDarkTheme) {
        Brush.verticalGradient(listOf(Color(0xFF101A31), Color(0xFF172B4D), Color(0xFF111827)))
    } else {
        Brush.verticalGradient(listOf(Color(0xFF5279A8), Color(0xFF7898B8), Color(0xFF344D70)))
    }
    val activePage = state.pages.getOrNull(state.activePageIndex) ?: state.pages.firstOrNull()
    val appByPackage = remember(state.installedApps) { state.installedApps.associateBy { it.packageName } }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(background)
            .statusBarsPadding()
            .navigationBarsPadding()
            .pointerInput(Unit) { detectTapGestures(onLongPress = { onOpenEditMode() }) }
            // Horizontal gestures switch desktops; vertical LazyGrid scrolling remains
            // available because this detector only wins horizontal touch slop.
            .pointerInput(state.pages.size, state.activePageIndex) {
                var horizontalDistance = 0f
                detectHorizontalDragGestures(
                    onHorizontalDrag = { change, dragAmount ->
                        horizontalDistance += dragAmount
                        change.consume()
                    },
                    onDragEnd = {
                        if (horizontalDistance < -56f && state.activePageIndex < state.pages.lastIndex) {
                            onPageSelected(state.activePageIndex + 1)
                        } else if (horizontalDistance > 56f && state.activePageIndex > 0) {
                            onPageSelected(state.activePageIndex - 1)
                        }
                        horizontalDistance = 0f
                    },
                    onDragCancel = { horizontalDistance = 0f }
                )
            }
            // Pinch-in is the Harmony-style entry gesture for the home editor.
            .pointerInput(onOpenEditMode) {
                var editorOpened = false
                detectTransformGestures { _, _, zoom, _ ->
                    if (zoom < 0.92f && !editorOpened) {
                        editorOpened = true
                        onOpenEditMode()
                    }
                }
            }
    ) {
        IconButton(onClick = onSettingsClick, modifier = Modifier.align(Alignment.TopEnd).padding(top = 2.dp, end = 12.dp)) {
            Icon(Icons.Rounded.Settings, contentDescription = "Launcher settings", tint = Color.White.copy(alpha = 0.9f))
        }

        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(18.dp))
            GlanceClockHeader(
                isDarkTheme = state.isDarkTheme,
                isBlurEnabled = state.isBlurEnabled,
                modifier = Modifier.padding(bottom = 16.dp),
                onSwipeDown = onSearchClick
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(Color.White.copy(alpha = if (state.isDarkTheme) 0.12f else 0.20f))
                    .clickable(onClick = onSearchClick)
                    .padding(horizontal = 16.dp, vertical = 13.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Rounded.Search, contentDescription = "Search apps", tint = Color.White.copy(alpha = 0.9f))
                Text(
                    text = "Search apps",
                    color = Color.White.copy(alpha = 0.78f),
                    fontSize = 14.sp,
                    modifier = Modifier.padding(start = 12.dp).weight(1f)
                )
                Text("⌕", color = Color.White.copy(alpha = 0.65f), fontSize = 18.sp)
            }

            Spacer(Modifier.height(14.dp))

            val specialItems = activePage?.items.orEmpty().filter {
                (it is HomeLayoutItem.Folder && it.id != "folder_social") ||
                    (it is HomeLayoutItem.ServiceWidget && it.id != "service_widget_schedule") ||
                    it is HomeLayoutItem.Widget
            }
            if (specialItems.isNotEmpty()) {
                Row(
                    modifier = Modifier.fillMaxWidth().height(142.dp).padding(horizontal = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    specialItems.take(2).forEach { item ->
                        Box(Modifier.weight(1f).fillMaxSize()) {
                            when (item) {
                                is HomeLayoutItem.Folder -> SmartFolder2x2(
                                    folder = FolderItem(item.id, item.title, item.appPackages),
                                    installedApps = state.installedApps,
                                    isDarkTheme = state.isDarkTheme,
                                    isBlurEnabled = state.isBlurEnabled,
                                    onAppClick = onAppClick,
                                    onOpenFolderModal = { onFolderClick(FolderItem(item.id, item.title, item.appPackages)) }
                                )
                                is HomeLayoutItem.ServiceWidget -> HarmonyServiceWidget(
                                    widgetItem = item,
                                    isDarkTheme = state.isDarkTheme,
                                    isBlurEnabled = state.isBlurEnabled,
                                    onToggleFocus = { onToggleServiceWidgetFocus(item.id) }
                                )
                                is HomeLayoutItem.Widget -> if (appWidgetManager != null && appWidgetHost != null) {
                                    WidgetHostContainer(
                                        widgetItem = item,
                                        appWidgetManager = appWidgetManager,
                                        appWidgetHost = appWidgetHost,
                                        isDarkTheme = state.isDarkTheme,
                                        isBlurEnabled = state.isBlurEnabled
                                    )
                                }
                                is HomeLayoutItem.App -> Unit
                            }
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))
            }

            // Real installed apps are shown immediately; saved home items retain their positions.
            val homeApps = remember(state.installedApps, activePage) {
                val saved = activePage?.items.orEmpty()
                    .filterIsInstance<HomeLayoutItem.App>()
                    .mapNotNull { appByPackage[it.packageName] }
                (saved.ifEmpty { if (activePage?.pageIndex == 0) state.installedApps.take(12) else emptyList() }).take(12)
            }

            LazyVerticalGrid(
                columns = GridCells.Fixed(state.gridColumns.substringBefore("x").toIntOrNull()?.coerceIn(3, 5) ?: 4),
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalArrangement = Arrangement.spacedBy(15.dp)
            ) {
                items(homeApps, key = { it.packageName }) { app ->
                    SquircleIcon(
                        label = app.label,
                        iconDrawable = app.icon,
                        onClick = { onAppClick(app) },
                        size = 46.dp,
                        isDarkTheme = state.isDarkTheme,
                        isBlurEnabled = state.isBlurEnabled,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(top = 4.dp, bottom = 12.dp)
            ) {
                repeat(maxOf(state.pages.size, 1)) { index ->
                    Box(
                        Modifier
                            .size(if (index == state.activePageIndex) 8.dp else 6.dp)
                            .clip(CircleShape)
                            .background(if (index == state.activePageIndex) Color.White else Color.White.copy(alpha = 0.38f))
                            .clickable { onPageSelected(index) }
                    )
                }
                Box(
                    modifier = Modifier
                        .size(22.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.18f))
                        .clickable { onAddPage() },
                    contentAlignment = Alignment.Center
                ) {
                    Text("+", color = Color.White, fontSize = 16.sp, lineHeight = 16.sp)
                }
            }

            // Keep the drawer gesture on the visible dock surface. A separate
            // invisible strip at the physical bottom is frequently covered by
            // Android's gesture-navigation area and therefore never receives touch.
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(76.dp)
                    .pointerInput(onSwipeUp) {
                        var upwardDistance = 0f
                        detectVerticalDragGestures(
                            onDragStart = { upwardDistance = 0f },
                            onDragEnd = {
                                if (upwardDistance <= -48.dp.toPx()) onSwipeUp()
                                upwardDistance = 0f
                            },
                            onDragCancel = { upwardDistance = 0f },
                            onVerticalDrag = { change, dragAmount ->
                                upwardDistance += dragAmount
                                change.consume()
                            }
                        )
                    },
                contentAlignment = Alignment.Center
            ) {
                FloatingDock(
                    dockApps = state.dockApps,
                    onAppClick = onAppClick,
                    onAppDrawerClick = onAppDrawerClick,
                    isDarkTheme = state.isDarkTheme,
                    isBlurEnabled = state.isBlurEnabled,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }
        }

    }
}
