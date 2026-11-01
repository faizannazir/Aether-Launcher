package com.aether.launcher.data

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
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

    // Cache activity icons by component; a launcher package may expose more than one activity.
    private val iconCache = LruCache<String, Drawable>(256)

    suspend fun loadInstalledApps() = withContext(Dispatchers.IO) {
        val pm = context.packageManager
        val intent = Intent(Intent.ACTION_MAIN, null).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }

        val resolveInfos = pm.queryIntentActivities(intent, PackageManager.MATCH_ALL)
        val appList = resolveInfos.asSequence().distinctBy { info ->
            info.activityInfo?.packageName
        }.mapNotNull { resolveInfo ->
            val activityInfo = resolveInfo.activityInfo ?: return@mapNotNull null
            val packageName = activityInfo.packageName
            val className = activityInfo.name
            val label = resolveInfo.loadLabel(pm).toString()
            val iconKey = "$packageName/$className"
            val icon = iconCache.get(iconKey) ?: resolveInfo.loadIcon(pm).also {
                iconCache.put(iconKey, it)
            }

            val category = when {
                packageName.contains("youtube") || packageName.contains("spotify") || packageName.contains("music") || packageName.contains("photos") -> AppCategory.MEDIA
                packageName.contains("message") || packageName.contains("talk") || packageName.contains("mail") || packageName.contains("chat") || packageName.contains("social") -> AppCategory.SOCIAL
                packageName.contains("doc") || packageName.contains("keep") || packageName.contains("note") || packageName.contains("drive") || packageName.contains("calendar") -> AppCategory.PRODUCTIVITY
                else -> AppCategory.UTILITIES
            }

            AppItem(
                packageName = packageName,
                className = className,
                label = label,
                icon = icon,
                category = category
            )
        }.sortedBy { it.label.lowercase() }.toList()

        _installedApps.value = appList
    }

    fun registerPackageReceiver() {
        if (packageReceiver != null) return
        packageReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                packageScope.launch {
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
