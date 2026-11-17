package com.aether.launcher.notification

import android.graphics.RectF
import android.os.Build
import android.view.DisplayCutout
import kotlin.math.roundToInt

internal data class CameraCapsuleGeometry(val width: Int, val height: Int, val offsetX: Int, val top: Int)

internal fun cameraCutoutBounds(cutout: DisplayCutout?): RectF? {
    if (cutout == null) return null
    if (Build.VERSION.SDK_INT >= 31) {
        val path = cutout.cutoutPath
        if (path != null) {
            val bounds = RectF()
            path.computeBounds(bounds, true)
            if (!bounds.isEmpty) return bounds
        }
    }
    return cutout.boundingRects.firstOrNull { it.top == 0 && it.width() > 0 }?.let(::RectF)
}

internal fun cameraCapsuleGeometry(screenWidth: Int, density: Float, camera: RectF?): CameraCapsuleGeometry {
    val width = maxOf(64 * density, (camera?.width() ?: 0f) + 16 * density).roundToInt().coerceAtMost(screenWidth)
    val height = maxOf(24 * density, (camera?.height() ?: 0f) + 6 * density).roundToInt()
    val centerX = (camera?.centerX() ?: screenWidth / 2f).coerceIn(width / 2f, screenWidth - width / 2f)
    val centerY = camera?.centerY() ?: height / 2f
    return CameraCapsuleGeometry(
        width,
        height,
        (centerX - screenWidth / 2f).roundToInt(),
        (centerY - height / 2f).roundToInt().coerceAtLeast(0)
    )
}
