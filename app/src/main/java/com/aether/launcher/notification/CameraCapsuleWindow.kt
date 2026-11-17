package com.aether.launcher.notification

import android.animation.ValueAnimator
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.view.animation.DecelerateInterpolator
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import com.aether.launcher.MainActivity
import com.aether.launcher.data.model.LauncherOptions
import com.aether.launcher.data.model.LauncherPalette

internal class CameraCapsuleWindow(private val context: Context) {
    private val manager = context.getSystemService(WindowManager::class.java)
    private val handler = Handler(Looper.getMainLooper())
    private val density = context.resources.displayMetrics.density
    private fun dp(value: Int) = (value * density).toInt()

    private val root = FrameLayout(context).apply {
        clipToOutline = true
        elevation = dp(8).toFloat()
        contentDescription = "Camera capsule"
    }
    private val content = LinearLayout(context).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        setPadding(dp(14), dp(6), dp(14), dp(10))
        visibility = View.GONE
    }
    private val icon = ImageView(context)
    private val label = TextView(context).apply {
        setTextColor(Color.WHITE)
        textSize = 10f
        maxLines = 1
        ellipsize = android.text.TextUtils.TruncateAt.END
    }
    private val title = TextView(context).apply {
        setTextColor(Color.WHITE)
        textSize = 13f
        maxLines = 1
        ellipsize = android.text.TextUtils.TruncateAt.END
    }
    private val params = WindowManager.LayoutParams(
        dp(92), dp(36), WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
        PixelFormat.TRANSLUCENT
    ).apply {
        gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
        title = "Aether camera capsule"
        if (Build.VERSION.SDK_INT >= 28) {
            layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
        }
    }
    private var attached = false
    private var options = LauncherOptions()
    private var idleWidth = dp(92)
    private var idleHeight = dp(36)
    private var screenWidth = context.resources.displayMetrics.widthPixels
    private var animation: ValueAnimator? = null
    private var action: PendingIntent? = null
    private var expanded = false
    private val collapseTask = Runnable { collapse() }

    init {
        val text = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            addView(label)
            addView(title)
        }
        content.addView(icon, LinearLayout.LayoutParams(dp(28), dp(28)).apply { marginEnd = dp(10) })
        content.addView(text, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        root.addView(content, FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT).apply {
            topMargin = idleHeight
        })
        val bounds = if (Build.VERSION.SDK_INT >= 30) {
            val metrics = manager.currentWindowMetrics
            screenWidth = metrics.bounds.width()
            cameraCutoutBounds(metrics.windowInsets.displayCutout)
        } else null
        val geometry = cameraCapsuleGeometry(screenWidth, density, bounds)
        idleWidth = geometry.width
        idleHeight = geometry.height
        params.x = geometry.offsetX
        params.y = geometry.top
        params.width = idleWidth
        params.height = idleHeight
        (content.layoutParams as FrameLayout.LayoutParams).apply { topMargin = idleHeight }
        root.setOnClickListener {
            val pending = action
            if (pending != null) {
                try { pending.send() } catch (_: PendingIntent.CanceledException) { /* The source app withdrew the action. */ }
                collapse()
            } else {
                context.startActivity(Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            }
        }
        root.setOnLongClickListener { collapse(); true }
        updateOptions(options)
    }

    fun attach(): Boolean {
        if (attached) return true
        return try {
            manager.addView(root, params)
            attached = true
            root.requestApplyInsets()
            true
        } catch (_: SecurityException) {
            false
        } catch (_: WindowManager.BadTokenException) {
            false
        }
    }

    fun updateOptions(value: LauncherOptions) {
        options = value
        val accent = when (value.palette) {
            LauncherPalette.SAGE -> Color.rgb(168, 201, 176)
            LauncherPalette.ORCHID -> Color.rgb(216, 179, 226)
            LauncherPalette.WALLPAPER -> if (Build.VERSION.SDK_INT >= 31) context.getColor(android.R.color.system_accent1_200) else Color.LTGRAY
            LauncherPalette.GRAPHITE -> Color.LTGRAY
        }
        root.background = GradientDrawable().apply {
            cornerRadius = if (expanded) dp(24).toFloat() else idleHeight / 2f
            setColor(Color.rgb(7, 8, 10))
            setStroke(dp(1).coerceAtLeast(1), if (expanded) accent else Color.argb(110, 100, 102, 108))
        }
        if (!value.capsuleShowTitles) title.text = ""
    }

    fun showNotification(packageName: String, notificationTitle: String, pendingIntent: PendingIntent?) {
        action = pendingIntent
        val application = try { context.packageManager.getApplicationInfo(packageName, 0) } catch (_: android.content.pm.PackageManager.NameNotFoundException) { return }
        label.text = context.packageManager.getApplicationLabel(application)
        title.text = notificationTitle
        icon.setImageDrawable(context.packageManager.getApplicationIcon(application))
        content.visibility = View.VISIBLE
        root.contentDescription = "Notification from ${label.text}"
        expanded = true
        updateOptions(options)
        animateTo(minOf(dp(218), screenWidth - dp(24)), idleHeight + dp(54))
        handler.removeCallbacks(collapseTask)
        handler.postDelayed(collapseTask, 6000)
    }

    fun collapse() {
        handler.removeCallbacks(collapseTask)
        action = null
        expanded = false
        content.visibility = View.GONE
        label.text = ""
        title.text = ""
        icon.setImageDrawable(null)
        root.contentDescription = "Camera capsule"
        updateOptions(options)
        animateTo(idleWidth, idleHeight)
    }

    private fun animateTo(width: Int, height: Int) {
        animation?.cancel()
        val startWidth = params.width
        val startHeight = params.height
        animation = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 240
            interpolator = DecelerateInterpolator()
            addUpdateListener {
                val fraction = it.animatedFraction
                params.width = (startWidth + (width - startWidth) * fraction).toInt()
                params.height = (startHeight + (height - startHeight) * fraction).toInt()
                updateLayout()
            }
            start()
        }
    }

    private fun updateLayout() {
        if (attached) {
            try { manager.updateViewLayout(root, params) } catch (_: IllegalArgumentException) { attached = false }
        }
    }

    fun close() {
        handler.removeCallbacks(collapseTask)
        animation?.cancel()
        animation = null
        if (attached) {
            try { manager.removeViewImmediate(root) } catch (_: IllegalArgumentException) { /* Android already detached the window. */ }
        }
        attached = false
        action = null
        label.text = ""
        title.text = ""
        icon.setImageDrawable(null)
    }
}
