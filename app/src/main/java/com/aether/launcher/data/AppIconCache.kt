package com.aether.launcher.data

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Rect
import android.graphics.drawable.AdaptiveIconDrawable
import android.graphics.drawable.Drawable
import androidx.collection.LruCache
import com.aether.launcher.data.model.AppItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt

internal class AppIconCache(private val context: Context) {
    private data class Key(val packageName: String, val className: String, val revision: Int, val size: Int)

    private val icons = object : LruCache<Key, Bitmap>(6 * 1024 * 1024) {
        override fun sizeOf(key: Key, value: Bitmap): Int = value.allocationByteCount
    }
    private val workers = Semaphore(4)

    private fun key(app: AppItem, size: Int) = Key(app.packageName, app.className, app.iconRevision, size)
    fun cached(app: AppItem, size: Int): Bitmap? = icons.get(key(app, size))

    suspend fun load(app: AppItem, size: Int): Bitmap? = withContext(Dispatchers.IO) {
        workers.withPermit {
            val key = key(app, size)
            icons.get(key)?.let { return@withPermit it }

            val drawable = try {
                context.packageManager.getActivityIcon(ComponentName(app.packageName, app.className))
            } catch (_: PackageManager.NameNotFoundException) {
                return@withPermit null
            }

            val bitmap = renderAppIcon(drawable, size)
            icons.put(key, bitmap)
            bitmap
        }
    }
}

internal fun renderAppIcon(drawable: Drawable, size: Int): Bitmap {
    val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    val intrinsicWidth = drawable.intrinsicWidth
    val intrinsicHeight = drawable.intrinsicHeight
    val bounds = if (drawable is AdaptiveIconDrawable || intrinsicWidth <= 0 || intrinsicHeight <= 0) {
        Rect(0, 0, size, size)
    } else {
        val scale = size.toFloat() / maxOf(intrinsicWidth, intrinsicHeight)
        val width = (intrinsicWidth * scale).roundToInt().coerceIn(1, size)
        val height = (intrinsicHeight * scale).roundToInt().coerceIn(1, size)
        val left = (size - width) / 2
        val top = (size - height) / 2
        Rect(left, top, left + width, top + height)
    }
    val previousBounds = drawable.copyBounds()
    try {
        drawable.bounds = bounds
        drawable.draw(Canvas(bitmap))
    } finally {
        drawable.bounds = previousBounds
    }
    return bitmap
}
