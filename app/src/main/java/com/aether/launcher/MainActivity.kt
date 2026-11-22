package com.aether.launcher

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.ComponentName
import android.app.Activity
import android.app.SearchManager
import android.app.role.RoleManager
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProviderInfo
import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.tween
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.produceState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowInsetsControllerCompat
import androidx.core.app.NotificationManagerCompat
import com.aether.launcher.notification.CameraCapsuleService
import com.aether.launcher.ui.screens.CameraCapsulePreview
import com.aether.launcher.data.model.AppItem
import com.aether.launcher.data.model.GestureAction
import com.aether.launcher.data.model.HomeBackground
import com.aether.launcher.data.model.HomeGesture
import com.aether.launcher.data.model.HomeLayoutItem
import com.aether.launcher.data.model.LauncherTheme
import com.aether.launcher.data.widget.WidgetHostManager
import com.aether.launcher.ui.components.LocalAppIcons
import com.aether.launcher.ui.screens.AndroidShortcut
import com.aether.launcher.ui.screens.AppActions
import com.aether.launcher.ui.screens.AppDragSource
import com.aether.launcher.ui.screens.AppDropTarget
import com.aether.launcher.ui.screens.rememberAppDrag
import com.aether.launcher.ui.screens.AppDrawerScreen
import com.aether.launcher.ui.screens.CustomizationSheet
import com.aether.launcher.ui.screens.FolderModal
import com.aether.launcher.ui.screens.HomeScreen
import com.aether.launcher.ui.screens.LauncherSettingsScreen
import com.aether.launcher.ui.screens.SearchOverlay
import com.aether.launcher.ui.screens.WidgetPickerSheet
import com.aether.launcher.ui.theme.AetherLauncherTheme
import com.aether.launcher.viewmodel.LauncherScreen
import com.aether.launcher.viewmodel.LauncherViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {

    private val viewModel: LauncherViewModel by viewModels()
    private lateinit var widgetHostManager: WidgetHostManager
    private var pendingWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID
    private var pendingWidgetPage = 0
    private var isDefaultHome by mutableStateOf(false)
    private var overlayAllowed by mutableStateOf(false)
    private var notificationAccess by mutableStateOf(false)
    private var cameraSetup by mutableStateOf(false)

    private val notificationPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) {
        if (it) postCapsuleTest()
    }

    private val homeRoleLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        isDefaultHome = isDefaultHomeApp()
    }
    private val bindWidgetLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val info = widgetHostManager.appWidgetManager.getAppWidgetInfo(pendingWidgetId)
        if (result.resultCode == Activity.RESULT_OK && info != null) {
            configureOrAddWidget(info)
        } else {
            cancelPendingWidget()
        }
    }
    private val configWidgetLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            addPendingWidget()
        } else {
            cancelPendingWidget()
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT)
        )
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT >= 28) {
            window.attributes = window.attributes.apply {
                layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
            }
        }
        widgetHostManager = WidgetHostManager(applicationContext)
        pendingWidgetId = savedInstanceState?.getInt("widgetId") ?: AppWidgetManager.INVALID_APPWIDGET_ID
        pendingWidgetPage = savedInstanceState?.getInt("widgetPage") ?: 0

        setContent {
            val rawState by viewModel.uiState.collectAsState()
            val overlayAttached by CameraCapsuleService.attached.collectAsState()
            val darkTheme = when (rawState.options.theme) {
                LauncherTheme.SYSTEM -> isSystemInDarkTheme()
                LauncherTheme.LIGHT -> false
                LauncherTheme.DARK -> true
            }
            val state = rawState.copy(isDarkTheme = darkTheme)
            val screen = state.currentScreen
            val onHome = screen == LauncherScreen.HOME
            val showWallpaper = state.options.background == HomeBackground.WALLPAPER &&
                (onHome || state.options.glass && screen == LauncherScreen.APP_DRAWER)
            val activePage = state.pages[state.activePageIndex]
            var editAnchor by remember { mutableStateOf<Offset?>(null) }
            var widgetSourcePackage by rememberSaveable { mutableStateOf<String?>(null) }
            val providers by produceState(emptyList<AppWidgetProviderInfo>(), state.installedApps, state.isWidgetSheetOpen) {
                if (state.installedApps.isNotEmpty() || state.isWidgetSheetOpen) {
                    value = withContext(Dispatchers.IO) { widgetHostManager.getInstalledProviders() }
                }
            }
            val widgetPackages = remember(providers) { providers.mapTo(mutableSetOf()) { it.provider.packageName } }

            LaunchedEffect(showWallpaper, onHome, darkTheme) {
                if (showWallpaper) {
                    window.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
                    window.addFlags(WindowManager.LayoutParams.FLAG_SHOW_WALLPAPER)
                    if (Build.VERSION.SDK_INT >= 34) {
                        window.attributes = window.attributes.apply { setWallpaperTouchEventsEnabled(false) }
                    }
                } else {
                    window.clearFlags(WindowManager.LayoutParams.FLAG_SHOW_WALLPAPER)
                    window.setBackgroundDrawable(ColorDrawable(if (darkTheme) 0xFF101419.toInt() else 0xFFF4F6F8.toInt()))
                }
                WindowInsetsControllerCompat(window, window.decorView).apply {
                    isAppearanceLightStatusBars = !onHome && !darkTheme
                    isAppearanceLightNavigationBars = !onHome && !darkTheme
                }
            }

            val appDrag = rememberAppDrag(
                icons = viewModel.appIcons,
                onStart = { viewModel.openScreen(LauncherScreen.HOME) },
                onDrop = { source, target ->
                    when (target) {
                        is AppDropTarget.Home -> viewModel.placeAppOnHome(
                            source.app, target.pageId, target.index,
                            source.homeItem?.id, source.pageId, source.fromDock
                        )
                        is AppDropTarget.Group -> viewModel.groupAppOnHome(
                            source.app, target.pageId, target.itemId,
                            source.homeItem?.id, source.pageId, source.fromDock
                        )
                        AppDropTarget.Dock -> {
                            viewModel.addDockApp(source.app)
                            source.homeItem?.let { item ->
                                val page = viewModel.uiState.value.pages.indexOfFirst { it.pageIndex == source.pageId }
                                if (page >= 0) viewModel.removeLayoutItem(item, page)
                            }
                        }
                        AppDropTarget.Remove -> {
                            if (source.fromDock) viewModel.removeDockApp(source.app)
                            source.homeItem?.let { item ->
                                val page = viewModel.uiState.value.pages.indexOfFirst { it.pageIndex == source.pageId }
                                if (page >= 0) viewModel.removeLayoutItem(item, page)
                            }
                        }
                        AppDropTarget.Info -> openNative(
                            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", source.app.packageName, null))
                        )
                    }
                }
            )

            appDrag.fallback = AppDropTarget.Home(activePage.pageIndex, Int.MAX_VALUE)

            LaunchedEffect(appDrag.active, appDrag.edge) {
                val edge = appDrag.edge
                if (appDrag.active && edge != 0) {
                    kotlinx.coroutines.delay(550)
                    while (appDrag.active && appDrag.edge == edge) {
                        viewModel.setActivePageIndex(viewModel.uiState.value.activePageIndex + edge)
                        kotlinx.coroutines.delay(850)
                    }
                }
            }

            val actions = AppActions(
                launch = ::launchApp,
                startDrag = { app, item, fromDock ->
                    appDrag.start(AppDragSource(app, item, activePage.pageIndex, fromDock))
                },
                remove = { item ->
                    viewModel.removeLayoutItem(item, state.activePageIndex)
                    if (item is HomeLayoutItem.Widget) widgetHostManager.deleteAppWidgetId(item.appWidgetId)
                }
            )

            BackHandler(enabled = !onHome || state.activeFolder != null || state.activePageIndex != 0) {
                when {
                    state.activeFolder != null -> viewModel.closeFolder()
                    !onHome -> viewModel.openScreen(LauncherScreen.HOME)
                    else -> viewModel.setActivePageIndex(0)
                }
            }

            CompositionLocalProvider(LocalAppIcons provides viewModel.appIcons) {
                AetherLauncherTheme(darkTheme, state.options.palette) {
                    Box(Modifier.fillMaxSize()) {
                    AnimatedContent(
                        targetState = screen,
                        modifier = Modifier.fillMaxSize(),
                        transitionSpec = {
                            when {
                                targetState == LauncherScreen.APP_DRAWER && initialState == LauncherScreen.HOME ->
                                    (slideInVertically(tween(170)) { it / 8 } + fadeIn(tween(140)))
                                        .togetherWith(fadeOut(tween(100)))
                                initialState == LauncherScreen.APP_DRAWER && targetState == LauncherScreen.HOME ->
                                    fadeIn(tween(130)).togetherWith(
                                        slideOutVertically(tween(160)) { it / 8 } + fadeOut(tween(110))
                                    )
                                else -> fadeIn(tween(120)).togetherWith(fadeOut(tween(90)))
                            }
                        },
                        label = "Launcher screen"
                    ) { displayedScreen ->
                    when (displayedScreen) {
                        LauncherScreen.HOME, LauncherScreen.EDIT_MODE -> HomeScreen(
                            state, widgetHostManager.appWidgetManager, widgetHostManager.appWidgetHost, actions, appDrag,
                            onOpenDrawer = { viewModel.openScreen(LauncherScreen.APP_DRAWER) },
                            onOpenSearch = { viewModel.openScreen(LauncherScreen.SEARCH_OVERLAY) },
                            onOpenEditor = { editAnchor = null; viewModel.toggleCustomizationSheet(true) },
                            onEditAt = { position -> editAnchor = position; viewModel.toggleCustomizationSheet(true) },
                            onOpenFolder = viewModel::openFolder,
                            onAndroidShortcut = ::openAndroidShortcut,
                            widgetPackages = widgetPackages,
                            onAppWidgets = { app ->
                                widgetSourcePackage = app.packageName
                                viewModel.toggleWidgetSheet(true)
                            },
                            onGesture = ::performGesture,
                            onPageSelected = viewModel::setActivePageIndex
                        )
                        LauncherScreen.SETTINGS -> LauncherSettingsScreen(
                            options = state.options,
                            overlayAllowed = overlayAllowed,
                            notificationAccess = notificationAccess,
                            startOnAndroidTab = cameraSetup,
                            onOverlayPermission = {
                                openNative(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName")))
                            },
                            onNotificationAccess = {
                                val intent = if (Build.VERSION.SDK_INT >= 30) {
                                    Intent(Settings.ACTION_NOTIFICATION_LISTENER_DETAIL_SETTINGS).putExtra(
                                        Settings.EXTRA_NOTIFICATION_LISTENER_COMPONENT_NAME,
                                        ComponentName(this@MainActivity, CameraCapsuleService::class.java).flattenToString()
                                    )
                                } else Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
                                if (!openNative(intent, showError = false)) openNative(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
                            },
                            onTestNotification = {
                                if (Build.VERSION.SDK_INT >= 33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                                    notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                } else postCapsuleTest()
                            },
                            isDefaultLauncher = isDefaultHome,
                            onBack = { viewModel.openScreen(LauncherScreen.HOME) },
                            onSetDefaultLauncher = ::requestDefaultHomeRole,
                            onAndroidShortcut = ::openAndroidShortcut,
                            onStarterLayout = {
                                viewModel.applyStarterLayout().forEach(widgetHostManager::deleteAppWidgetId)
                                viewModel.openScreen(LauncherScreen.HOME)
                            },
                            onUpdate = viewModel::updateOptions
                        )
                        LauncherScreen.APP_DRAWER -> AppDrawerScreen(
                            state, actions, viewModel::onCategorySelected,
                            onGoogleSearch = ::openGoogleSearch,
                            onGemini = ::openGemini,
                            onAddToHome = { app ->
                                viewModel.addAppToHomePage(app)
                                viewModel.openScreen(LauncherScreen.HOME)
                            },
                            onCloseDrawer = { viewModel.openScreen(LauncherScreen.HOME) }
                        )
                        LauncherScreen.SEARCH_OVERLAY -> SearchOverlay(
                            state, viewModel::onSearchQueryChanged, ::launchApp,
                            onGoogleSearch = ::openGoogleSearch,
                            onGemini = ::openGemini,
                            onClose = { viewModel.openScreen(LauncherScreen.HOME) }
                        )
                    }

                    }

                    state.activeFolder?.let { folder ->
                        FolderModal(
                            folder = folder,
                            installedApps = state.installedApps,
                            isDarkTheme = darkTheme,
                            isBlurEnabled = state.isBlurEnabled,
                            onAppClick = ::launchApp,
                            onTitleChange = { viewModel.updateFolderTitle(folder.id, it) },
                            onUngroup = { viewModel.ungroupFolder(folder.id) },
                            onDismiss = viewModel::closeFolder
                        )
                    }

                    if (state.isCustomizationSheetOpen) {
                        CustomizationSheet(
                            anchor = editAnchor,
                            canRemovePage = state.pages.size > 1 && activePage.items.isEmpty(),
                            onAddPage = {
                                viewModel.addHomePage()
                                viewModel.toggleCustomizationSheet(false)
                            },
                            onRemovePage = {
                                viewModel.removeEmptyHomePage()
                                viewModel.toggleCustomizationSheet(false)
                            },
                            onWidgets = {
                                widgetSourcePackage = null
                                viewModel.toggleCustomizationSheet(false)
                                viewModel.toggleWidgetSheet(true)
                            },
                            onAddControls = {
                                viewModel.addAndroidControls()
                                viewModel.toggleCustomizationSheet(false)
                            },
                            onWallpaper = {
                                viewModel.toggleCustomizationSheet(false)
                                openAndroidShortcut(AndroidShortcut.WALLPAPER)
                            },
                            onSettings = { viewModel.openScreen(LauncherScreen.SETTINGS) },
                            onApps = { viewModel.openScreen(LauncherScreen.APP_DRAWER) },
                            onClose = { viewModel.toggleCustomizationSheet(false) }
                        )
                    }

                    if (state.isWidgetSheetOpen) {
                        val source = widgetSourcePackage
                        val visibleProviders = remember(providers, source) {
                            if (source == null) providers else providers.filter { it.provider.packageName == source }
                        }
                        val appName = state.installedApps.firstOrNull { it.packageName == source }?.label
                        val close = { viewModel.toggleWidgetSheet(false) }
                        ModalBottomSheet(onDismissRequest = close) {
                            WidgetPickerSheet(visibleProviders, appName, onProviderSelect = {
                                close()
                                requestAddWidget(it)
                            }, onClose = close)
                        }
                    }
                    if (state.options.showCapsule && !overlayAttached) {
                        CameraCapsulePreview(
                            onSetup = {
                                cameraSetup = true
                                viewModel.openScreen(LauncherScreen.SETTINGS)
                            },
                            modifier = Modifier.align(Alignment.TopCenter)
                        )
                    }
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        if (intent.hasCategory(Intent.CATEGORY_HOME)) {
            viewModel.openScreen(LauncherScreen.HOME)
            viewModel.setActivePageIndex(0)
        }
    }

    private fun performGesture(gesture: HomeGesture) {
        when (viewModel.uiState.value.options.actionFor(gesture)) {
            GestureAction.NONE -> Unit
            GestureAction.DRAWER -> viewModel.openScreen(LauncherScreen.APP_DRAWER)
            GestureAction.SEARCH -> viewModel.openScreen(LauncherScreen.SEARCH_OVERLAY)
            GestureAction.EDIT -> viewModel.toggleCustomizationSheet(true)
            GestureAction.SETTINGS -> viewModel.openScreen(LauncherScreen.SETTINGS)
            GestureAction.INTERNET -> openAndroidShortcut(AndroidShortcut.INTERNET)
            GestureAction.ANDROID_SETTINGS -> openAndroidShortcut(AndroidShortcut.SETTINGS)
            GestureAction.NAVIGATION -> openAndroidShortcut(AndroidShortcut.NAVIGATION)
            GestureAction.GOOGLE -> openGoogleSearch("")
            GestureAction.GEMINI -> openGemini()
        }
    }

    private fun openGoogleSearch(query: String) {
        val trimmed = query.trim()
        val native = if (trimmed.isEmpty()) {
            packageManager.getLaunchIntentForPackage("com.google.android.googlequicksearchbox")
        } else {
            Intent(Intent.ACTION_WEB_SEARCH)
                .setPackage("com.google.android.googlequicksearchbox")
                .putExtra(SearchManager.QUERY, trimmed)
        }

        val web = Uri.parse("https://www.google.com/").buildUpon()
        if (trimmed.isNotEmpty()) web.path("search").appendQueryParameter("q", trimmed)
        if ((native != null && openNative(native, showError = false)) ||
            openNative(Intent(Intent.ACTION_VIEW, web.build()))) {
            viewModel.openScreen(LauncherScreen.HOME)
        }
    }

    private fun openGemini() {
        val native = packageManager.getLaunchIntentForPackage("com.google.android.apps.bard")
        if ((native != null && openNative(native, showError = false)) ||
            openNative(Intent(Intent.ACTION_VIEW, Uri.parse("https://gemini.google.com/")))) {
            viewModel.openScreen(LauncherScreen.HOME)
        }
    }

    private fun launchApp(app: AppItem) {
        if (viewModel.launchApp(app)) {
            viewModel.openScreen(LauncherScreen.HOME)
        } else {
            toast("Couldn't open ${app.label}")
        }
    }

    private fun openAndroidShortcut(shortcut: AndroidShortcut) {
        val intent = when (shortcut) {
            AndroidShortcut.SETTINGS -> Intent(Settings.ACTION_SETTINGS)
            AndroidShortcut.INTERNET -> if (Build.VERSION.SDK_INT >= 29) {
                Intent(Settings.Panel.ACTION_INTERNET_CONNECTIVITY)
            } else Intent(Settings.ACTION_WIFI_SETTINGS)
            AndroidShortcut.BLUETOOTH -> Intent(Settings.ACTION_BLUETOOTH_SETTINGS)
            AndroidShortcut.NAVIGATION -> Intent("com.android.settings.NAVIGATION_MODE_SETTINGS").setPackage("com.android.settings")
            AndroidShortcut.DISPLAY -> Intent(Settings.ACTION_DISPLAY_SETTINGS)
            AndroidShortcut.SOUND -> Intent(Settings.ACTION_SOUND_SETTINGS)
            AndroidShortcut.NOTIFICATIONS -> Intent("android.settings.NOTIFICATION_SETTINGS")
            AndroidShortcut.ACCESSIBILITY -> Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
            AndroidShortcut.WALLPAPER -> {
                val wallpaper = Intent(Intent.ACTION_SET_WALLPAPER)
                if (openNative(Intent(wallpaper).setPackage("com.google.android.apps.wallpaper"), showError = false)) return
                Intent.createChooser(wallpaper, "Wallpaper & style")
            }
        }
        if (!openNative(intent, showError = false)) {
            if (shortcut == AndroidShortcut.NAVIGATION) toast("In Settings, search for Navigation mode")
            openNative(Intent(Settings.ACTION_SETTINGS))
        }
    }

    private fun openNative(intent: Intent, showError: Boolean = true): Boolean {
        return try {
            startActivity(intent)
            true
        } catch (_: ActivityNotFoundException) {
            if (showError) toast("This Android control is unavailable")
            false
        } catch (_: SecurityException) {
            if (showError) toast("This Android control is unavailable")
            false
        }
    }

    private fun requestDefaultHomeRole() {
        if (Build.VERSION.SDK_INT >= 29 && !isDefaultHome) {
            val manager = getSystemService(RoleManager::class.java)
            if (manager?.isRoleAvailable(RoleManager.ROLE_HOME) == true) {
                homeRoleLauncher.launch(manager.createRequestRoleIntent(RoleManager.ROLE_HOME))
                return
            }
        }
        openNative(Intent(Settings.ACTION_HOME_SETTINGS))
    }

    override fun onResume() {
        super.onResume()
        isDefaultHome = isDefaultHomeApp()
        overlayAllowed = Settings.canDrawOverlays(this)
        notificationAccess = packageName in NotificationManagerCompat.getEnabledListenerPackages(this)
        if (notificationAccess) CameraCapsuleService.refresh(this)
    }

    private fun postCapsuleTest() {
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(NotificationChannel("capsule_preview", "Camera capsule preview", NotificationManager.IMPORTANCE_DEFAULT))
        val open = PendingIntent.getActivity(this, 0, Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        manager.notify(1107, Notification.Builder(this, "capsule_preview")
            .setSmallIcon(com.aether.launcher.R.drawable.ic_aether_launcher)
            .setContentTitle("Camera capsule")
            .setContentText("Incoming notifications expand around your camera.")
            .setContentIntent(open)
            .setAutoCancel(true)
            .build())
    }

    private fun isDefaultHomeApp(): Boolean {
        if (Build.VERSION.SDK_INT >= 29) {
            return getSystemService(RoleManager::class.java)?.isRoleHeld(RoleManager.ROLE_HOME) == true
        }
        return packageManager.resolveActivity(
            Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME), PackageManager.MATCH_DEFAULT_ONLY
        )?.activityInfo?.packageName == packageName
    }

    override fun onStart() {
        super.onStart()
        widgetHostManager.startListening()
    }

    override fun onStop() {
        widgetHostManager.stopListening()
        super.onStop()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putInt("widgetId", pendingWidgetId)
        outState.putInt("widgetPage", pendingWidgetPage)
        super.onSaveInstanceState(outState)
    }

    private fun requestAddWidget(info: AppWidgetProviderInfo) {
        cancelPendingWidget()
        pendingWidgetPage = viewModel.uiState.value.activePageIndex
        pendingWidgetId = widgetHostManager.allocateAppWidgetId()
        if (widgetHostManager.appWidgetManager.bindAppWidgetIdIfAllowed(pendingWidgetId, info.provider)) {
            configureOrAddWidget(info)
        } else {
            val intent = Intent(AppWidgetManager.ACTION_APPWIDGET_BIND)
                .putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, pendingWidgetId)
                .putExtra(AppWidgetManager.EXTRA_APPWIDGET_PROVIDER, info.provider)
            try {
                bindWidgetLauncher.launch(intent)
            } catch (_: ActivityNotFoundException) {
                cancelPendingWidget()
                toast("Widget binding is unavailable")
            }
        }
    }

    private fun configureOrAddWidget(info: AppWidgetProviderInfo) {
        val configuration = info.configure
        if (configuration == null) {
            addPendingWidget()
        } else {
            val intent = Intent(AppWidgetManager.ACTION_APPWIDGET_CONFIGURE).setComponent(configuration)
                .putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, pendingWidgetId)
            try {
                configWidgetLauncher.launch(intent)
            } catch (_: ActivityNotFoundException) {
                cancelPendingWidget()
                toast("Widget configuration is unavailable")
            } catch (_: SecurityException) {
                cancelPendingWidget()
                toast("Widget configuration is unavailable")
            }
        }
    }

    private fun addPendingWidget() {
        val info = widgetHostManager.appWidgetManager.getAppWidgetInfo(pendingWidgetId)
        if (info == null) {
            cancelPendingWidget()
            return
        }
        val item = widgetHostManager.createWidgetLayoutItem(0, pendingWidgetId, info)
        viewModel.addWidgetToPage(item, pendingWidgetPage)
        pendingWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID
    }

    private fun cancelPendingWidget() {
        if (pendingWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
            widgetHostManager.deleteAppWidgetId(pendingWidgetId)
            pendingWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID
        }
    }

    private fun toast(message: String) = Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
}
