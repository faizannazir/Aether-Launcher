package com.aether.launcher.ui.screens

import android.appwidget.AppWidgetHost
import android.appwidget.AppWidgetManager
import android.text.format.DateFormat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.foundation.layout.weight
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Widgets
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
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
        modifier = modifier.fillMaxWidth(),
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
            fontSize = 76.sp,
            lineHeight = 82.sp,
            fontWeight = FontWeight.Light,
            letterSpacing = (-3).sp,
            modifier = Modifier.padding(start = 20.dp, top = 2.dp)
        )
        Text(
            text = "Make today yours",
            color = secondary,
            fontSize = 13.sp,
            modifier = Modifier.padding(start = 26.dp, top = 2.dp)
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
    onToggleServiceWidgetFocus: (String) -> Unit = {}
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
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(22.dp))
            GlanceClockHeader(
                isDarkTheme = state.isDarkTheme,
                isBlurEnabled = state.isBlurEnabled,
                modifier = Modifier.padding(bottom = 20.dp)
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
                it is HomeLayoutItem.Folder || it is HomeLayoutItem.ServiceWidget || it is HomeLayoutItem.Widget
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
                (saved + state.installedApps.filterNot { app -> saved.any { it.packageName == app.packageName } }).take(16)
            }

            LazyVerticalGrid(
                columns = GridCells.Fixed(4),
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
                        size = 54.dp,
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
                            .size(if (index == state.activePageIndex) 7.dp else 5.dp)
                            .clip(CircleShape)
                            .background(if (index == state.activePageIndex) Color.White else Color.White.copy(alpha = 0.38f))
                    )
                }
            }

            FloatingDock(
                dockApps = state.dockApps,
                onAppClick = onAppClick,
                onAppDrawerClick = onAppDrawerClick,
                isDarkTheme = state.isDarkTheme,
                isBlurEnabled = state.isBlurEnabled,
                modifier = Modifier.padding(bottom = 10.dp)
            )
        }

        // Keep real Android widgets and Harmony smart tiles available in edit layouts.
        val specialItems = activePage?.items.orEmpty().filter {
            it is HomeLayoutItem.Folder || it is HomeLayoutItem.ServiceWidget || it is HomeLayoutItem.Widget
        }
        if (specialItems.isNotEmpty()) {
            // These items remain accessible from the app drawer/customization flow without
            // forcing oversized demo cards into the primary launcher grid.
        }
    }
}
