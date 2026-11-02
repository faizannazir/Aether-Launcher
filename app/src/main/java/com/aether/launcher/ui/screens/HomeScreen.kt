package com.aether.launcher.ui.screens

import android.app.WallpaperManager
import android.appwidget.AppWidgetHost
import android.appwidget.AppWidgetManager
import android.graphics.Bitmap
import android.text.format.DateFormat
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
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
import kotlin.math.abs

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

    Column(
        modifier = modifier
            .fillMaxWidth()
            .pointerInput(onSwipeDown) {
                awaitEachGesture {
                    val down = awaitFirstDown(
                        requireUnconsumed = false,
                        pass = PointerEventPass.Initial
                    )
                    var totalY = 0f
                    var triggered = false
                    do {
                        val event = awaitPointerEvent(PointerEventPass.Initial)
                        val change = event.changes.firstOrNull { it.id == down.id } ?: break
                        if (change.pressed) {
                            totalY += change.position.y - change.previousPosition.y
                            if (!triggered && totalY >= 48.dp.toPx()) {
                                change.consume()
                                triggered = true
                                onSwipeDown()
                            }
                        }
                    } while (event.changes.any { it.pressed })
                }
            },
        horizontalAlignment = Alignment.Start
    ) {
        Text(
            text = ClockFormatter.formatDate(now).uppercase(locale = Locale.getDefault()),
            color = Color.White.copy(alpha = 0.74f),
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 1.1.sp,
            modifier = Modifier.padding(start = 4.dp)
        )
        Text(
            text = ClockFormatter.formatTime(now, is24Hour),
            color = Color.White,
            fontSize = 48.sp,
            lineHeight = 52.sp,
            fontWeight = FontWeight.Light,
            letterSpacing = (-2.5).sp,
            modifier = Modifier.padding(top = 2.dp)
        )
    }
}

@Composable
private fun WallpaperBackdrop(
    modifier: Modifier = Modifier,
    isDarkTheme: Boolean
) {
    val context = LocalContext.current
    val wallpaper = remember(context) {
        runCatching {
            WallpaperManager.getInstance(context).drawable?.toBitmap(
                width = 720,
                height = 1600,
                config = Bitmap.Config.ARGB_8888
            )?.asImageBitmap()
        }.getOrNull()
    }

    Box(modifier = modifier) {
        if (wallpaper != null) {
            Image(
                bitmap = wallpaper,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(
                        Brush.linearGradient(
                            listOf(Color(0xFF24334B), Color(0xFF52647C), Color(0xFF252E43))
                        )
                    )
            )
        }
        Box(
            Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        if (isDarkTheme) {
                            listOf(Color.Black.copy(alpha = 0.34f), Color(0xFF07111F).copy(alpha = 0.48f))
                        } else {
                            listOf(Color(0xFF101A2A).copy(alpha = 0.28f), Color(0xFF101A2A).copy(alpha = 0.42f))
                        }
                    )
                )
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
    onAddPage: () -> Unit = {},
    onRemoveApp: (String, Int) -> Unit = { _, _ -> },
    onCreateFolder: (String, String, Int) -> Unit = { _, _, _ -> }
) {
    val activePage = state.pages.getOrNull(state.activePageIndex) ?: state.pages.firstOrNull()
    var appSelectedForFolder by remember { mutableStateOf<String?>(null) }
    var draggingPackage by remember { mutableStateOf<String?>(null) }
    var dragVisualOffset by remember { mutableStateOf(Offset.Zero) }
    val gridCellCenters = remember(activePage?.pageIndex) { mutableStateMapOf<Int, Offset>() }
    val appByPackage = remember(state.installedApps) {
        state.installedApps.associateBy { it.packageName }
    }
    val pageApps = activePage?.items
        .orEmpty()
        .filterIsInstance<HomeLayoutItem.App>()
        .sortedBy { it.position }
    val appsByGridPosition = remember(pageApps, state.installedApps, activePage?.pageIndex) {
        if (pageApps.isEmpty() && activePage?.pageIndex == 0) {
            state.installedApps.take(12).mapIndexed { index, app -> index to app }.toMap()
        } else {
            pageApps.mapNotNull { item ->
                appByPackage[item.packageName]?.let { item.position to it }
            }.toMap()
        }
    }
    val homeApps = remember(appsByGridPosition) { appsByGridPosition.values.toList() }
    val specialItems = activePage?.items.orEmpty()
        .filter { it !is HomeLayoutItem.App }
        .sortedBy { it.position }
        .take(2)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF182235))
    ) {
        WallpaperBackdrop(Modifier.fillMaxSize(), state.isDarkTheme)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp)
                .pointerInput(onSwipeUp, onPageSelected, state.pages.size, state.activePageIndex, draggingPackage) {
                    val horizontalThreshold = 56.dp.toPx()
                    val upwardThreshold = 64.dp.toPx()
                    val bottomGestureZone = 176.dp.toPx()

                    awaitEachGesture {
                        val down = awaitFirstDown(
                            requireUnconsumed = false,
                            pass = PointerEventPass.Initial
                        )
                        val startedNearDock = down.position.y >= size.height - bottomGestureZone
                        var totalX = 0f
                        var totalY = 0f
                        var handled = false

                        do {
                            val event = awaitPointerEvent(PointerEventPass.Initial)
                            val change = event.changes.firstOrNull { it.id == down.id } ?: break
                            if (change.pressed) {
                                val delta = change.position - change.previousPosition
                                totalX += delta.x
                                totalY += delta.y

                                if (!handled && draggingPackage == null &&
                                    state.pages.size > 1 &&
                                    abs(totalX) >= horizontalThreshold &&
                                    abs(totalX) > abs(totalY) * 1.2f
                                ) {
                                    change.consume()
                                    handled = true
                                    val nextPage = if (totalX < 0f) {
                                        (state.activePageIndex + 1).coerceAtMost(state.pages.lastIndex)
                                    } else {
                                        (state.activePageIndex - 1).coerceAtLeast(0)
                                    }
                                    if (nextPage != state.activePageIndex) onPageSelected(nextPage)
                                } else if (!handled && draggingPackage == null &&
                                    startedNearDock &&
                                    totalY <= -upwardThreshold &&
                                    abs(totalY) > abs(totalX) * 1.2f
                                ) {
                                    change.consume()
                                    handled = true
                                    onSwipeUp()
                                }
                            }
                        } while (event.changes.any { it.pressed })
                    }
                }
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
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                verticalAlignment = Alignment.Top
            ) {
                GlanceClockHeader(
                    isDarkTheme = state.isDarkTheme,
                    isBlurEnabled = state.isBlurEnabled,
                    modifier = Modifier
                        .weight(1f)
                        .padding(top = 4.dp),
                    onSwipeDown = onSearchClick
                )
                IconButton(
                    onClick = onSearchClick,
                    modifier = Modifier
                        .padding(top = 2.dp)
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.16f))
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Search,
                        contentDescription = "Search apps",
                        tint = Color.White.copy(alpha = 0.96f)
                    )
                }
                IconButton(
                    onClick = onSettingsClick,
                    modifier = Modifier
                        .padding(top = 2.dp)
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.16f))
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Settings,
                        contentDescription = "Launcher settings",
                        tint = Color.White.copy(alpha = 0.96f)
                    )
                }
            }

            Spacer(Modifier.height(10.dp))

            appSelectedForFolder?.let { selectedPackage ->
                val selectedLabel = state.installedApps.firstOrNull { it.packageName == selectedPackage }?.label
                    ?: selectedPackage
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.White.copy(alpha = 0.18f))
                        .clickable { appSelectedForFolder = null }
                        .padding(horizontal = 12.dp, vertical = 9.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Choose another app to group with $selectedLabel · Tap to cancel",
                        color = Color.White,
                        fontSize = 12.sp,
                        modifier = Modifier.weight(1f)
                    )
                }
                Spacer(Modifier.height(8.dp))
            }

            if (specialItems.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(152.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    specialItems.forEach { item ->
                        Box(Modifier.weight(1f).fillMaxSize()) {
                            when (item) {
                                is HomeLayoutItem.Folder -> SmartFolder2x2(
                                    folder = FolderItem(item.id, item.title, item.appPackages),
                                    installedApps = state.installedApps,
                                    isDarkTheme = state.isDarkTheme,
                                    isBlurEnabled = state.isBlurEnabled,
                                    onAppClick = onAppClick,
                                    onOpenFolderModal = {
                                        onFolderClick(FolderItem(item.id, item.title, item.appPackages))
                                    }
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
                Spacer(Modifier.height(14.dp))
            }

            Spacer(Modifier.height(4.dp))

            if (homeApps.isNotEmpty()) {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(
                        state.gridColumns.substringBefore("x").toIntOrNull()?.coerceIn(3, 5) ?: 4
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentPadding = PaddingValues(bottom = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(20, key = { "home-slot-$it" }) { position ->
                        val app = appsByGridPosition[position]
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(76.dp)
                                .onGloballyPositioned { coordinates ->
                                    gridCellCenters[position] = coordinates.localToRoot(
                                        Offset(coordinates.size.width / 2f, coordinates.size.height / 2f)
                                    )
                                }
                        ) {
                            if (app != null) {
                                val isDragging = draggingPackage == app.packageName
                                var iconCoordinates by remember(app.packageName) { mutableStateOf<LayoutCoordinates?>(null) }
                                SquircleIcon(
                                    label = app.label,
                                    iconDrawable = app.icon,
                                    onClick = { if (draggingPackage == null) onAppClick(app) },
                                    size = state.iconSizeDp.dp,
                                    isDarkTheme = state.isDarkTheme,
                                    isBlurEnabled = state.isBlurEnabled,
                                    labelColor = if (state.isDarkTheme) Color.White else Color(0xFF17212B),
                                    showLabel = state.showIconLabels,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .onGloballyPositioned { iconCoordinates = it }
                                        .pointerInput(app.packageName, activePage?.pageIndex) {
                                            var targetPosition = position
                                            detectDragGesturesAfterLongPress(
                                                onDragStart = {
                                                    draggingPackage = app.packageName
                                                    dragVisualOffset = Offset.Zero
                                                    onStartDrag(
                                                        HomeLayoutItem.App(
                                                            id = "app_${app.packageName}",
                                                            position = position,
                                                            packageName = app.packageName,
                                                            className = app.className
                                                        ),
                                                        activePage?.pageIndex ?: state.activePageIndex,
                                                        position
                                                    )
                                                },
                                                onDrag = { change, dragAmount ->
                                                    change.consume()
                                                    dragVisualOffset += dragAmount
                                                    val pointerInRoot = iconCoordinates?.localToRoot(change.position)
                                                    if (pointerInRoot != null) {
                                                        targetPosition = gridCellCenters.minByOrNull { (_, center) ->
                                                            (center - pointerInRoot).getDistance()
                                                        }?.key ?: position
                                                    }
                                                },
                                                onDragEnd = {
                                                    draggingPackage = null
                                                    dragVisualOffset = Offset.Zero
                                                    onDropItem(targetPosition)
                                                },
                                                onDragCancel = {
                                                    draggingPackage = null
                                                    dragVisualOffset = Offset.Zero
                                                    onDropItem(position)
                                                }
                                            )
                                        }
                                        .graphicsLayer {
                                            if (isDragging) {
                                                translationX = dragVisualOffset.x
                                                translationY = dragVisualOffset.y
                                                scaleX = 1.08f
                                                scaleY = 1.08f
                                                alpha = 0.88f
                                            }
                                        }
                                )
                            }
                        }
                    }
                }
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "This page is ready for your apps",
                            color = Color.White.copy(alpha = 0.92f),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "Open the app drawer and hold an app to add it here.",
                            color = Color.White.copy(alpha = 0.68f),
                            fontSize = 12.sp,
                            modifier = Modifier.padding(top = 6.dp)
                        )
                    }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp, bottom = 12.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                repeat(maxOf(state.pages.size, 1)) { index ->
                    Box(
                        Modifier
                            .padding(horizontal = 4.dp)
                            .size(if (index == state.activePageIndex) 8.dp else 6.dp)
                            .clip(CircleShape)
                            .background(
                                if (index == state.activePageIndex) Color.White
                                else Color.White.copy(alpha = 0.42f)
                            )
                            .clickable { onPageSelected(index) }
                    )
                }
                Box(
                    modifier = Modifier
                        .padding(start = 8.dp)
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.16f))
                        .border(1.dp, Color.White.copy(alpha = 0.22f), CircleShape)
                        .clickable(onClick = onAddPage),
                    contentAlignment = Alignment.Center
                ) {
                    Text("+", color = Color.White, fontSize = 17.sp, lineHeight = 18.sp)
                }
            }

            FloatingDock(
                dockApps = state.dockApps,
                onAppClick = onAppClick,
                onAppDrawerClick = onAppDrawerClick,
                isDarkTheme = state.isDarkTheme,
                isBlurEnabled = state.isBlurEnabled,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp)
            )
        }
    }
}
