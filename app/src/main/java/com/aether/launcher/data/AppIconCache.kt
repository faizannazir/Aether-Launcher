package com.aether.launcher.data

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Rect
import android.graphics.drawable.AdaptiveIconDrawable
import androidx.collection.LruCache
import androidx.core.graphics.drawable.toBitmap
import com.aether.launcher.data.model.AppItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext

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

            val bitmap = if (drawable is AdaptiveIconDrawable) {
                val image = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
                val canvas = Canvas(image)
                // Adaptive icon layers include a 25% safe margin beyond the visible viewport.
                val inset = size / 4
                val bounds = Rect(-inset, -inset, size + inset, size + inset)
                listOfNotNull(drawable.background, drawable.foreground).forEach { layer ->
                    layer.bounds = bounds
                    layer.draw(canvas)
                }
                image
            } else {
                drawable.toBitmap(size, size, Bitmap.Config.ARGB_8888)
            }
            icons.put(key, bitmap)
            bitmap
        }
    }
}
