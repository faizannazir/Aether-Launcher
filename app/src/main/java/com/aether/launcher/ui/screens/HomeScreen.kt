package com.aether.launcher.ui.screens

import android.appwidget.AppWidgetHost
import android.appwidget.AppWidgetManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.text.format.DateFormat
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.aether.launcher.data.model.AppItem
import com.aether.launcher.data.model.FolderItem
import com.aether.launcher.data.model.HomeLayoutItem
import com.aether.launcher.glass.GlassLevel
import com.aether.launcher.glass.glassSurface
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
    fun formatTime(
        time: LocalDateTime,
        is24Hour: Boolean,
        locale: Locale = Locale.getDefault()
    ): String {
        val pattern = if (is24Hour) "HH:mm" else "h:mm"
        return time.format(DateTimeFormatter.ofPattern(pattern, locale))
    }

    fun formatDate(
        time: LocalDateTime,
        locale: Locale = Locale.getDefault()
    ): String {
        return time.format(DateTimeFormatter.ofPattern("EEEE, MMMM d", locale))
    }

    fun calculateMillisToNextMinute(time: LocalDateTime): Long {
        val seconds = time.second
        val millis = time.nano / 1_000_000
        return ((60 - seconds) * 1000L - millis).coerceAtLeast(100L)
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
    val lifecycleOwner = LocalLifecycleOwner.current

    var currentTime by remember { mutableStateOf(timeProvider()) }

    // Register broadcast receiver for system time & timezone changes and lifecycle observer for ON_RESUME
    DisposableEffect(context, lifecycleOwner) {
        val timeReceiver = object : BroadcastReceiver() {
            override fun onReceive(c: Context?, intent: Intent?) {
                currentTime = timeProvider()
            }
        }
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_TIME_TICK)
            addAction(Intent.ACTION_TIME_CHANGED)
            addAction(Intent.ACTION_TIMEZONE_CHANGED)
        }
        context.registerReceiver(timeReceiver, filter)

        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                currentTime = timeProvider()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)

        onDispose {
            try {
                context.unregisterReceiver(timeReceiver)
            } catch (e: Exception) {
                // Ignore if not registered
            }
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    // Schedule updates exactly at next minute boundary
    LaunchedEffect(Unit) {
        while (true) {
            val now = timeProvider()
            currentTime = now
            val delayMillis = ClockFormatter.calculateMillisToNextMinute(now)
            delay(delayMillis)
        }
    }

    val is24HourFormat = remember(context) { DateFormat.is24HourFormat(context) }

    val dateString = remember(currentTime.dayOfYear, currentTime.year) {
        ClockFormatter.formatDate(currentTime)
    }
    val timeString = remember(currentTime.minute, currentTime.hour, is24HourFormat) {
        ClockFormatter.formatTime(currentTime, is24HourFormat)
    }

    val textColor = if (isDarkTheme) Color.White else Color.White

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
        ) {
            Text(
                text = dateString,
                style = MaterialTheme.typography.titleSmall,
                color = textColor.copy(alpha = 0.95f)
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .glassSurface(
                        level = GlassLevel.SUBTLE,
                        isDarkTheme = isDarkTheme,
                        blurEnabled = isBlurEnabled,
                        shape = RoundedCornerShape(20.dp)
                    )
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.Schedule,
                    contentDescription = "System Clock",
                    tint = if (isDarkTheme) Color(0xFF00FBFF) else Color(0xFF0284C7),
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = "  Aether",
                    style = MaterialTheme.typography.labelSmall,
                    color = textColor
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = timeString,
            style = MaterialTheme.typography.displayLarge.copy(
                fontSize = 82.sp,
                fontWeight = FontWeight.Light,
                letterSpacing = (-2).sp
            ),
            color = textColor,
            modifier = Modifier.padding(horizontal = 24.dp)
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
    val backgroundBrush = if (state.isDarkTheme) {
        Brush.verticalGradient(
            colors = listOf(
                Color(0xFF0F141B),
                Color(0xFF1B2027),
                Color(0xFF0F141B)
            )
        )
    } else {
        Brush.verticalGradient(
            colors = listOf(
                Color(0xFF1E293B),
                Color(0xFF334155),
                Color(0xFF475569),
                Color(0xFF1E293B)
            )
        )
    }

    val activePage = state.pages.getOrNull(state.activePageIndex) ?: state.pages.firstOrNull()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundBrush)
            .statusBarsPadding()
            .navigationBarsPadding()
            .pointerInput(Unit) {
                detectTapGestures(
                    onLongPress = { onOpenEditMode() }
                )
            }
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // Real Device Time & Date Header
            GlanceClockHeader(
                isDarkTheme = state.isDarkTheme,
                isBlurEnabled = state.isBlurEnabled
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Search Capsule Bar
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .glassSurface(
                        level = GlassLevel.BALANCED,
                        isDarkTheme = state.isDarkTheme,
                        blurEnabled = state.isBlurEnabled,
                        shape = RoundedCornerShape(28.dp)
                    )
                    .clickable(onClick = onSearchClick)
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.Search,
                    contentDescription = "Search",
                    tint = Color.White.copy(alpha = 0.8f)
                )
                Text(
                    text = "  Search apps, contacts, web...",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.8f),
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Main Tactile Grid (4x5 positions: 0..19)
            val gridPositions = (0..19).toList()

            LazyVerticalGrid(
                columns = GridCells.Fixed(4),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .weight(1f),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                itemsIndexed(gridPositions) { _, position ->
                    val layoutItem = activePage?.items?.find { it.position == position }

                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(72.dp)
                            .pointerInput(position) {
                                detectTapGestures(
                                    onLongPress = {
                                        if (layoutItem != null) {
                                            onStartDrag(layoutItem, state.activePageIndex, position)
                                        }
                                    }
                                )
                            }
                    ) {
                        when (layoutItem) {
                            is HomeLayoutItem.App -> {
                                val app = state.installedApps.find { it.packageName == layoutItem.packageName }
                                if (app != null) {
                                    SquircleIcon(
                                        label = app.label,
                                        iconDrawable = app.icon,
                                        onClick = { onAppClick(app) },
                                        isDarkTheme = state.isDarkTheme,
                                        isBlurEnabled = state.isBlurEnabled
                                    )
                                }
                            }
                            is HomeLayoutItem.Folder -> {
                                SmartFolder2x2(
                                    folder = FolderItem(
                                        id = layoutItem.id,
                                        title = layoutItem.title,
                                        appPackages = layoutItem.appPackages
                                    ),
                                    installedApps = state.installedApps,
                                    isDarkTheme = state.isDarkTheme,
                                    isBlurEnabled = state.isBlurEnabled,
                                    onAppClick = onAppClick,
                                    onOpenFolderModal = {
                                        onFolderClick(
                                            FolderItem(
                                                id = layoutItem.id,
                                                title = layoutItem.title,
                                                appPackages = layoutItem.appPackages
                                            )
                                        )
                                    }
                                )
                            }
                            is HomeLayoutItem.ServiceWidget -> {
                                HarmonyServiceWidget(
                                    widgetItem = layoutItem,
                                    isDarkTheme = state.isDarkTheme,
                                    isBlurEnabled = state.isBlurEnabled,
                                    onToggleFocus = { onToggleServiceWidgetFocus(layoutItem.id) }
                                )
                            }
                            is HomeLayoutItem.Widget -> {
                                if (appWidgetManager != null && appWidgetHost != null) {
                                    WidgetHostContainer(
                                        widgetItem = layoutItem,
                                        appWidgetManager = appWidgetManager,
                                        appWidgetHost = appWidgetHost,
                                        isDarkTheme = state.isDarkTheme,
                                        isBlurEnabled = state.isBlurEnabled
                                    )
                                }
                            }
                            null -> {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .clickable {
                                            if (state.dragState.draggedItem != null) {
                                                onDropItem(position)
                                            }
                                        }
                                )
                            }
                        }
                    }
                }
            }

            // Page Indicator Dots
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 8.dp)
            ) {
                state.pages.forEachIndexed { idx, _ ->
                    val isActive = idx == state.activePageIndex
                    Box(
                        modifier = Modifier
                            .size(if (isActive) 8.dp else 6.dp)
                            .background(
                                color = if (isActive) Color(0xFF00FBFF) else Color.White.copy(alpha = 0.3f),
                                shape = RoundedCornerShape(4.dp)
                            )
                    )
                }
            }

            // Floating Dock
            FloatingDock(
                dockApps = state.dockApps,
                onAppClick = onAppClick,
                onAppDrawerClick = onAppDrawerClick,
                isDarkTheme = state.isDarkTheme,
                isBlurEnabled = state.isBlurEnabled,
                modifier = Modifier.padding(bottom = 12.dp)
            )
        }
    }
}
