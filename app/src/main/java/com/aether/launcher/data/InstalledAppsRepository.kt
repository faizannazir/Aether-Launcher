package com.aether.launcher.data

import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
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
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class InstalledAppsRepository(private val context: Context) {

    private val _installedApps = MutableStateFlow<List<AppItem>>(emptyList())
    val installedApps: StateFlow<List<AppItem>> = _installedApps.asStateFlow()

    private var packageReceiver: BroadcastReceiver? = null
    private var packageScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val iconRevisions = mutableMapOf<String, Int>()
    private val loadMutex = Mutex()

    suspend fun loadInstalledApps(changedPackage: String? = null) = withContext(Dispatchers.IO) {
        loadMutex.withLock {
            if (changedPackage != null) {
                iconRevisions[changedPackage] = (iconRevisions[changedPackage] ?: 0) + 1
            }

            val pm = context.packageManager
            val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
            val entries = pm.queryIntentActivities(intent, PackageManager.MATCH_ALL)
                .filter { it.activityInfo?.packageName != null && it.activityInfo.packageName != context.packageName }
                .distinctBy { it.activityInfo.packageName }
                .associateBy { it.activityInfo.packageName }
            val apps = entries.map { (packageName, info) ->
                val className = info.activityInfo.name
                val category = when {
                    listOf("youtube", "spotify", "music", "photos").any(packageName::contains) -> AppCategory.MEDIA
                    listOf("message", "talk", "mail", "chat", "social").any(packageName::contains) -> AppCategory.SOCIAL
                    listOf("doc", "keep", "note", "drive", "calendar").any(packageName::contains) -> AppCategory.PRODUCTIVITY
                    else -> AppCategory.UTILITIES
                }
                AppItem(packageName, className, info.loadLabel(pm).toString(), category, iconRevisions[packageName] ?: 0)
            }.sortedBy { it.label.lowercase() }

            _installedApps.value = apps
        }
    }

    fun registerPackageReceiver() {
        if (packageReceiver != null) return
        packageReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                packageScope.launch {
                    loadInstalledApps(intent?.data?.schemeSpecificPart)
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
            val intent = Intent.makeMainActivity(ComponentName(app.packageName, app.className))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
            context.startActivity(intent)
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}
