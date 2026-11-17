package com.aether.launcher.ui.screens

import android.content.ClipData
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Point
import android.graphics.RectF
import android.view.DragEvent
import android.view.HapticFeedbackConstants
import android.view.View
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalView
import com.aether.launcher.data.AppIconCache
import com.aether.launcher.data.model.AppItem
import com.aether.launcher.data.model.HomeLayoutItem

internal data class AppDragSource(
    val app: AppItem,
    val homeItem: HomeLayoutItem.App?,
    val pageId: Int,
    val fromDock: Boolean
)

internal sealed interface AppDropTarget {
    data class Home(val pageId: Int, val index: Int, val groupItemId: String? = null) : AppDropTarget
    data class Group(val pageId: Int, val itemId: String) : AppDropTarget
    data object Dock : AppDropTarget
    data object Remove : AppDropTarget
    data object Info : AppDropTarget
}

@Stable
internal class LauncherAppDrag(private val view: View, private val icons: AppIconCache) {
    var source by mutableStateOf<AppDragSource?>(null)
        private set
    var position by mutableStateOf(Offset.Unspecified)
        private set
    var target by mutableStateOf<AppDropTarget?>(null)
        private set

    var groupReady by mutableStateOf(false)
        private set

    val edge: Int get() = when {
        !active || position == Offset.Unspecified -> 0
        position.x < 24 * view.resources.displayMetrics.density -> -1
        position.x > view.width - 24 * view.resources.displayMetrics.density -> 1
        else -> 0
    }

    fun armGrouping() {
        val destination = target as? AppDropTarget.Home
        groupReady = destination?.groupItemId != null && destination.groupItemId != source?.homeItem?.id
    }

    var fallback: AppDropTarget.Home? = null
    var placedPackage by mutableStateOf<String?>(null)
        private set
    var placedPageId by mutableStateOf<Int?>(null)
        private set

    fun acknowledgePlacement() {
        placedPackage = null
        placedPageId = null
    }

    private val targets = mutableMapOf<AppDropTarget, Rect>()
    val active: Boolean get() = source != null

    fun start(source: AppDragSource) {
        val pixels = (48 * view.resources.displayMetrics.density).toInt().coerceIn(48, 160)
        val bitmap = icons.cached(source.app, pixels)
        view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
        view.startDragAndDrop(
            ClipData.newPlainText("Aether app", source.app.packageName),
            AppShadow(bitmap, source.app.label, pixels),
            source,
            0
        )
    }

    fun register(target: AppDropTarget, bounds: Rect) {
        targets[target] = bounds
        updateTarget()
    }

    fun unregister(target: AppDropTarget) {
        targets.remove(target)
        updateTarget()
    }

    fun receive(event: DragEvent, onStart: () -> Unit, onDrop: (AppDragSource, AppDropTarget) -> Unit): Boolean {
        val payload = event.localState as? AppDragSource ?: return false
        when (event.action) {
            DragEvent.ACTION_DRAG_STARTED -> {
                source = payload
                acknowledgePlacement()
                position = Offset.Unspecified
                target = null
                onStart()
            }
            DragEvent.ACTION_DRAG_LOCATION, DragEvent.ACTION_DRAG_ENTERED -> {
                position = Offset(event.x, event.y)
                updateTarget()
            }
            DragEvent.ACTION_DRAG_EXITED -> target = null
            DragEvent.ACTION_DROP -> {
                position = Offset(event.x, event.y)
                updateTarget()
                val destination = target ?: fallback ?: return false
                val grouped = if (groupReady && destination is AppDropTarget.Home && destination.groupItemId != null) {
                    AppDropTarget.Group(destination.pageId, destination.groupItemId)
                } else destination
                onDrop(payload, grouped)
                if (grouped is AppDropTarget.Home) {
                    placedPackage = payload.app.packageName
                    placedPageId = grouped.pageId
                }
            }
            DragEvent.ACTION_DRAG_ENDED -> {
                source = null
                target = null
                position = Offset.Unspecified
            }
        }
        return true
    }

    private fun updateTarget() {
        val nextTarget = if (position == Offset.Unspecified) null else {
            targets.entries.filter { it.value.contains(position) }.minByOrNull { it.value.width * it.value.height }?.key
        }
        if (target != nextTarget) groupReady = false
        target = nextTarget
    }
}

@Composable
internal fun rememberAppDrag(
    icons: AppIconCache,
    onStart: () -> Unit,
    onDrop: (AppDragSource, AppDropTarget) -> Unit
): LauncherAppDrag {
    val view = LocalView.current
    val controller = remember(view, icons) { LauncherAppDrag(view, icons) }
    val start by rememberUpdatedState(onStart)
    val drop by rememberUpdatedState(onDrop)
    LaunchedEffect(controller.active, controller.target) {
        if (controller.active && controller.target is AppDropTarget.Home) {
            kotlinx.coroutines.delay(700)
            controller.armGrouping()
        }
    }
    DisposableEffect(view, controller) {
        view.setOnDragListener { _, event -> controller.receive(event, { start() }, { source, target -> drop(source, target) }) }
        onDispose { view.setOnDragListener(null) }
    }
    return controller
}

@Composable
internal fun Modifier.appDropTarget(controller: LauncherAppDrag, target: AppDropTarget): Modifier {
    DisposableEffect(controller, target) {
        onDispose { controller.unregister(target) }
    }
    return onGloballyPositioned { controller.register(target, it.boundsInRoot()) }
}

private class AppShadow(
    private val bitmap: Bitmap?,
    private val label: String,
    private val iconSize: Int
) : View.DragShadowBuilder() {
    private val shadowSize = (iconSize * 1.18f).toInt()
    override fun onProvideShadowMetrics(size: Point, touch: Point) {
        size.set(shadowSize, shadowSize)
        touch.set(shadowSize / 2, shadowSize / 2)
    }

    override fun onDrawShadow(canvas: Canvas) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
        val bounds = RectF(0f, 0f, shadowSize.toFloat(), shadowSize.toFloat())
        val radius = shadowSize * .27f
        canvas.clipPath(Path().apply { addRoundRect(bounds, radius, radius, Path.Direction.CW) })
        val image = bitmap
        if (image != null) {
            canvas.drawBitmap(image, null, bounds, paint)
        } else {
            paint.color = android.graphics.Color.DKGRAY
            canvas.drawRoundRect(bounds, radius, radius, paint)
            paint.color = android.graphics.Color.WHITE
            paint.textSize = shadowSize * .55f
            paint.textAlign = Paint.Align.CENTER
            canvas.drawText(label.take(1), shadowSize / 2f, shadowSize * .7f, paint)
        }
    }
}
