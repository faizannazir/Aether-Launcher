package com.aether.launcher.notification

import android.app.KeyguardManager
import android.app.Notification
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import android.provider.Settings
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import androidx.core.content.ContextCompat
import com.aether.launcher.data.HomePreferences
import com.aether.launcher.data.model.LauncherOptions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class CameraCapsuleService : NotificationListenerService() {
    companion object {
        private var instance: CameraCapsuleService? = null
        private val attachedState = MutableStateFlow(false)
        val attached = attachedState.asStateFlow()

        fun refresh(context: Context) {
            val service = instance
            if (service != null) Handler(Looper.getMainLooper()).post { service.refreshWindow() }
            else requestRebind(ComponentName(context, CameraCapsuleService::class.java))
        }
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var options: LauncherOptions? = null
    private var connected = false
    private var capsule: CameraCapsuleWindow? = null
    private var notificationKey: String? = null
    private val screenReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == Intent.ACTION_SCREEN_OFF) {
                notificationKey = null
                removeWindow()
            } else {
                refreshWindow()
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        ContextCompat.registerReceiver(
            this, screenReceiver,
            IntentFilter().apply {
                addAction(Intent.ACTION_SCREEN_OFF)
                addAction(Intent.ACTION_USER_PRESENT)
            },
            ContextCompat.RECEIVER_NOT_EXPORTED
        )
        scope.launch {
            HomePreferences(this@CameraCapsuleService).options.collect {
                options = it
                refreshWindow()
            }
        }
    }

    override fun onListenerConnected() {
        super.onListenerConnected()
        connected = true
        refreshWindow()
    }

    override fun onListenerDisconnected() {
        connected = false
        removeWindow()
        super.onListenerDisconnected()
    }

    private fun unlocked(): Boolean =
        getSystemService(PowerManager::class.java).isInteractive &&
            !getSystemService(KeyguardManager::class.java).isKeyguardLocked

    private fun refreshWindow() {
        val current = options ?: return
        if (!connected || !current.showCapsule || !Settings.canDrawOverlays(this) || !unlocked()) {
            removeWindow()
            return
        }
        val window = capsule ?: CameraCapsuleWindow(this).also { capsule = it }
        attachedState.value = window.attach()
        window.updateOptions(current)
    }

    override fun onNotificationPosted(notification: StatusBarNotification, rankingMap: RankingMap) {
        val current = options ?: return
        if (!current.showCapsule || !unlocked()) return
        if (currentInterruptionFilter == INTERRUPTION_FILTER_NONE || currentInterruptionFilter == INTERRUPTION_FILTER_ALARMS) return
        if (notification.notification.flags and Notification.FLAG_GROUP_SUMMARY != 0) return
        val ranking = Ranking()
        if (!rankingMap.getRanking(notification.key, ranking)) return
        if (!ranking.matchesInterruptionFilter() || ranking.importance < NotificationManager.IMPORTANCE_DEFAULT) return
        val suppressedPeek = NotificationManager.Policy.SUPPRESSED_EFFECT_PEEK or NotificationManager.Policy.SUPPRESSED_EFFECT_SCREEN_ON
        if (ranking.suppressedVisualEffects and suppressedPeek != 0) return
        refreshWindow()
        val window = capsule ?: return
        notificationKey = notification.key
        val title = if (current.capsuleShowTitles) {
            notification.notification.extras.getCharSequence(Notification.EXTRA_TITLE)?.toString().orEmpty()
        } else ""
        window.showNotification(notification.packageName, title, notification.notification.contentIntent)
    }

    override fun onNotificationRemoved(notification: StatusBarNotification) {
        if (notification.key == notificationKey) {
            notificationKey = null
            capsule?.collapse()
        }
    }

    override fun onConfigurationChanged(newConfig: android.content.res.Configuration) {
        super.onConfigurationChanged(newConfig)
        removeWindow()
        refreshWindow()
    }

    private fun removeWindow() {
        capsule?.close()
        capsule = null
        attachedState.value = false
    }

    override fun onDestroy() {
        removeWindow()
        unregisterReceiver(screenReceiver)
        scope.cancel()
        if (instance === this) instance = null
        super.onDestroy()
    }
}
