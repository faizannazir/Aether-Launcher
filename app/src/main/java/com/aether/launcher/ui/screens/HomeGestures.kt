package com.aether.launcher.ui.screens

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.unit.dp
import androidx.compose.ui.input.pointer.pointerInput
import com.aether.launcher.data.model.HomeGesture
import kotlin.math.abs

internal fun Modifier.homeSwipes(
    enabled: Boolean,
    onGesture: (HomeGesture) -> Unit,
    onPageStep: (Int) -> Unit
): Modifier = if (!enabled) this else pointerInput(Unit) {
    val swipeThreshold = 32.dp.toPx()
    awaitEachGesture {
        val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Final)
        var axis: SwipeAxis? = null
        var pinchStart: Float? = null
        while (true) {
            val event = awaitPointerEvent(PointerEventPass.Final)
            val pointer = event.changes.firstOrNull { it.id == down.id } ?: break
            val secondary = event.changes.firstOrNull { it.id != down.id && it.pressed }
            if (secondary != null) {
                if (!pointer.pressed || axis != null || event.changes.count { it.pressed } > 2) break
                val distance = (pointer.position - secondary.position).getDistance()
                val start = pinchStart
                if (start == null) {
                    pinchStart = distance
                } else if (start > swipeThreshold && distance < start * 0.72f) {
                    pointer.consume()
                    secondary.consume()
                    onGesture(HomeGesture.PINCH)
                    break
                }
                continue
            }
            if (pinchStart != null) break
            val travel = pointer.position - down.position
            val x = abs(travel.x)
            val y = abs(travel.y)
            if (!pointer.pressed) {
                when (axis) {
                    null -> Unit
                    SwipeAxis.HORIZONTAL -> if (x >= swipeThreshold) onPageStep(if (travel.x < 0) 1 else -1)
                    SwipeAxis.VERTICAL -> if (y >= swipeThreshold) {
                        onGesture(if (travel.y < 0) HomeGesture.SWIPE_UP else HomeGesture.SWIPE_DOWN)
                    }
                }
                break
            }
            if (pointer.isConsumed) break
            if (axis == null && maxOf(x, y) >= viewConfiguration.touchSlop) {
                axis = if (x > y) SwipeAxis.HORIZONTAL else SwipeAxis.VERTICAL
            }
            if (axis != null) pointer.consume()
        }
    }
}

private enum class SwipeAxis { HORIZONTAL, VERTICAL }

internal fun Modifier.appWidgetSwipe(onOpen: (() -> Unit)?): Modifier =
    if (onOpen == null) this else pointerInput(onOpen) {
        val minimum = 32.dp.toPx()
        awaitEachGesture {
            val down = awaitFirstDown(requireUnconsumed = false)
            var recognized = false
            while (true) {
                val event = awaitPointerEvent()
                val finger = event.changes.firstOrNull { it.id == down.id } ?: break
                if (event.changes.any { it.id != finger.id && it.pressed }) break
                val travel = finger.position - down.position
                if (!finger.pressed) {
                    if (recognized && -travel.y >= minimum) onOpen()
                    break
                }
                if (finger.isConsumed) break
                if (!recognized && maxOf(abs(travel.x), abs(travel.y)) >= viewConfiguration.touchSlop) {
                    if (-travel.y <= abs(travel.x)) break
                    recognized = true
                }
                if (recognized) finger.consume()
            }
        }
    }
