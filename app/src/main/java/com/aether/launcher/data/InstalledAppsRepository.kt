package com.aether.launcher.data

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.core.graphics.drawable.toBitmap
import java.io.File
import java.security.MessageDigest
import androidx.collection.LruCache
import com.aether.launcher.data.model.AppCategory
import com.aether.launcher.data.model.AppItem
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.cancel
import kotlinx.coroutines.withContext

class InstalledAppsRepository(private val context: Context) {

    private val _installedApps = MutableStateFlow<List<AppItem>>(emptyList())
    val installedApps: StateFlow<List<AppItem>> = _installedApps.asStateFlow()

    private var packageReceiver: BroadcastReceiver? = null
    private var packageScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    // Decoded icons stay off the main thread and the memory cache is bounded by bytes.
    private val iconCache = object : LruCache<String, Bitmap>(32 * 1024) {
        override fun sizeOf(key: String, value: Bitmap): Int =
            (value.byteCount / 1024).coerceAtLeast(1)
    }
    private val iconDirectory = File(context.cacheDir, "launcher-icons")
    private val maxDiskIconCacheBytes = 32L * 1024L * 1024L

    private fun iconKey(packageName: String, className: String, lastUpdateTime: Long): String =
        "$packageName/$className/$lastUpdateTime"

    private fun iconFile(key: String): File {
        val digest = MessageDigest.getInstance("SHA-256")
            .digest(key.toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it) }
        return File(iconDirectory, "$digest.png")
    }

    private fun loadIconBitmap(key: String, packageName: String, className: String): Bitmap? {
        iconCache.get(key)?.let { return it }
        val diskFile = iconFile(key)
        if (diskFile.isFile) {
            BitmapFactory.decodeFile(diskFile.absolutePath)?.let { cached ->
                iconCache.put(key, cached)
                diskFile.setLastModified(System.currentTimeMillis())
                return cached
            }
        }

        val drawable = runCatching {
            context.packageManager.getActivityIcon(android.content.ComponentName(packageName, className))
        }.getOrNull() ?: return null
        val bitmap = runCatching {
            drawable.toBitmap(width = 192, height = 192, config = Bitmap.Config.ARGB_8888)
        }.getOrNull() ?: return null

        iconCache.put(key, bitmap)
        runCatching {
            iconDirectory.mkdirs()
            val temporary = File(diskFile.parentFile, "${diskFile.name}.tmp")
            temporary.outputStream().use { output ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, output)
            }
            if (!temporary.renameTo(diskFile)) {
                temporary.copyTo(diskFile, overwrite = true)
                temporary.delete()
            }
            trimDiskIconCache()
        }
        return bitmap
    }

    private fun trimDiskIconCache() {
        val files = iconDirectory.listFiles()?.filter { it.isFile && it.extension == "png" } ?: return
        var totalBytes = files.sumOf { it.length() }
        if (totalBytes <= maxDiskIconCacheBytes) return
        for (file in files.sortedBy { it.lastModified() }) {
            if (totalBytes <= maxDiskIconCacheBytes) break
            val size = file.length()
            if (file.delete()) totalBytes -= size
        }
    }

    private fun invalidateIconCache() {
        iconCache.evictAll()
        iconDirectory.listFiles()?.forEach { it.delete() }
    }

    suspend fun loadInstalledApps() = withContext(Dispatchers.IO) {
        val pm = context.packageManager
        val intent = Intent(Intent.ACTION_MAIN, null).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }

        // Publish labels and package identities first. Loading every launcher icon
        // synchronously makes the first home-screen render wait on hundreds of assets.
        val appsWithoutIcons = pm.queryIntentActivities(intent, PackageManager.MATCH_ALL)
            .asSequence()
            .distinctBy { info -> info.activityInfo?.packageName }
            .mapNotNull { resolveInfo ->
                val activityInfo = resolveInfo.activityInfo ?: return@mapNotNull null
                val packageName = activityInfo.packageName
                val category = when {
                    packageName.contains("youtube") || packageName.contains("spotify") ||
                        packageName.contains("music") || packageName.contains("photos") -> AppCategory.MEDIA
                    packageName.contains("message") || packageName.contains("talk") ||
                        packageName.contains("mail") || packageName.contains("chat") ||
                        packageName.contains("social") -> AppCategory.SOCIAL
                    packageName.contains("doc") || packageName.contains("keep") ||
                        packageName.contains("note") || packageName.contains("drive") ||
                        packageName.contains("calendar") -> AppCategory.PRODUCTIVITY
                    else -> AppCategory.UTILITIES
                }

                AppItem(
                    packageName = packageName,
                    className = activityInfo.name,
                    label = resolveInfo.loadLabel(pm).toString(),
                    category = category
                )
            }
            .sortedBy { it.label.lowercase() }
            .toList()

        _installedApps.value = appsWithoutIcons

        // Resolve icons in small batches and publish incrementally. The home screen
        // becomes interactive while remaining icons continue loading in the background.
        appsWithoutIcons.chunked(16).forEach { batch ->
            val resolvedIcons = batch.associate { app ->
                val updateTime = runCatching {
                    pm.getPackageInfo(app.packageName, 0).lastUpdateTime
                }.getOrDefault(0L)
                val key = iconKey(app.packageName, app.className, updateTime)
                app.packageName to loadIconBitmap(key, app.packageName, app.className)
            }

            val current = _installedApps.value
            _installedApps.value = current.map { app ->
                val resolved = resolvedIcons[app.packageName]
                if (resolved != null) app.copy(iconBitmap = resolved) else app
            }
        }
    }

    fun registerPackageReceiver() {
        if (packageReceiver != null) return
        packageReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                packageScope.launch {
                    invalidateIconCache()
                    loadInstalledApps()
                }
            }
        }
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_PACKAGE_ADDED)
            addAction(Intent.ACTION_PACKAGE_REMOVED)
            addAction(Intent.ACTION_PACKAGE_CHANGED)
            addDataScheme("package")
        }
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            context.registerReceiver(packageReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            @Suppress("DEPRECATION")
            context.registerReceiver(packageReceiver, filter)
        }
    }

    fun unregisterPackageReceiver() {
        packageReceiver?.let {
            try {
                context.unregisterReceiver(it)
            } catch (e: Exception) {
                // Ignore if not registered
            }
            packageReceiver = null
        }
        packageScope.coroutineContext.cancel()
        packageScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    }

    fun launchApp(app: AppItem): Boolean {
        return try {
            val intent = context.packageManager.getLaunchIntentForPackage(app.packageName)
            if (intent != null) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
                true
            } else {
                false
            }
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}
