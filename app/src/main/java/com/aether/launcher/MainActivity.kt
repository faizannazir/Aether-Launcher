package com.aether.launcher

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProviderInfo
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.aether.launcher.data.widget.WidgetHostManager
import com.aether.launcher.ui.screens.AppDrawerScreen
import com.aether.launcher.ui.screens.CustomizationSheet
import com.aether.launcher.ui.screens.FolderModal
import com.aether.launcher.ui.screens.HomeScreen
import com.aether.launcher.ui.screens.SearchOverlay
import com.aether.launcher.ui.screens.WidgetPickerSheet
import com.aether.launcher.ui.theme.AetherLauncherTheme
import com.aether.launcher.viewmodel.LauncherScreen
import com.aether.launcher.viewmodel.LauncherViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: LauncherViewModel by viewModels()
    private lateinit var widgetHostManager: WidgetHostManager

    private var pendingWidgetInfo: AppWidgetProviderInfo? = null

    private val bindWidgetLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val appWidgetId = result.data?.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, -1) ?: -1
            if (appWidgetId != -1 && pendingWidgetInfo != null) {
                configureOrAddWidget(appWidgetId, pendingWidgetInfo!!)
            }
        }
    }

    private val configWidgetLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val appWidgetId = result.data?.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, -1) ?: -1
            if (appWidgetId != -1 && pendingWidgetInfo != null) {
                val widgetItem = widgetHostManager.createWidgetLayoutItem(
                    position = 0,
                    appWidgetId = appWidgetId,
                    info = pendingWidgetInfo!!
                )
                viewModel.addWidgetToPage(widgetItem, viewModel.uiState.value.activePageIndex)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        widgetHostManager = WidgetHostManager(applicationContext)

        setContent {
            val state by viewModel.uiState.collectAsState()

            AetherLauncherTheme(darkTheme = state.isDarkTheme) {
                Box(modifier = Modifier.fillMaxSize()) {
                    HomeScreen(
                        state = state,
                        appWidgetManager = widgetHostManager.appWidgetManager,
                        appWidgetHost = widgetHostManager.appWidgetHost,
                        onAppClick = { viewModel.launchApp(it) },
                        onAppDrawerClick = { viewModel.openScreen(LauncherScreen.APP_DRAWER) },
                        onSearchClick = { viewModel.openScreen(LauncherScreen.SEARCH_OVERLAY) },
                        onFolderClick = { viewModel.openFolder(it) },
                        onOpenEditMode = { viewModel.toggleCustomizationSheet(true) },
                        onStartDrag = { item, page, pos -> viewModel.startDrag(item, page, pos) },
                        onDropItem = { pos -> viewModel.dropDraggedItem(pos) },
                        onToggleServiceWidgetFocus = { id -> viewModel.toggleServiceWidgetFocus(id) }
                    )

                    if (state.currentScreen == LauncherScreen.APP_DRAWER) {
                        AppDrawerScreen(
                            state = state,
                            onAppClick = {
                                viewModel.launchApp(it)
                                viewModel.openScreen(LauncherScreen.HOME)
                            },
                            onCategorySelect = { viewModel.onCategorySelected(it) },
                            onCloseDrawer = { viewModel.openScreen(LauncherScreen.HOME) }
                        )
                    }

                    if (state.currentScreen == LauncherScreen.SEARCH_OVERLAY) {
                        SearchOverlay(
                            state = state,
                            onQueryChange = { viewModel.onSearchQueryChanged(it) },
                            onAppClick = {
                                viewModel.launchApp(it)
                                viewModel.openScreen(LauncherScreen.HOME)
                            },
                            onClose = { viewModel.openScreen(LauncherScreen.HOME) }
                        )
                    }

                    state.activeFolder?.let { folder ->
                        FolderModal(
                            folder = folder,
                            installedApps = state.installedApps,
                            isDarkTheme = state.isDarkTheme,
                            isBlurEnabled = state.isBlurEnabled,
                            onAppClick = { viewModel.launchApp(it) },
                            onTitleChange = { viewModel.updateFolderTitle(folder.id, it) },
                            onDismiss = { viewModel.closeFolder() }
                        )
                    }

                    if (state.isCustomizationSheetOpen) {
                        Box(
                            contentAlignment = Alignment.BottomCenter,
                            modifier = Modifier.fillMaxSize()
                        ) {
                            CustomizationSheet(
                                state = state,
                                onDarkThemeToggle = { viewModel.setDarkTheme(it) },
                                onBlurToggle = { viewModel.setBlurEnabled(it) },
                                onBlurIntensityChange = { viewModel.setBlurIntensity(it) },
                                onClose = { viewModel.toggleCustomizationSheet(false) }
                            )
                        }
                    }

                    if (state.isWidgetSheetOpen) {
                        Box(
                            contentAlignment = Alignment.BottomCenter,
                            modifier = Modifier.fillMaxSize()
                        ) {
                            WidgetPickerSheet(
                                providers = widgetHostManager.getInstalledProviders(),
                                isDarkTheme = state.isDarkTheme,
                                isBlurEnabled = state.isBlurEnabled,
                                onProviderSelect = { provider ->
                                    viewModel.toggleWidgetSheet(false)
                                    requestAddWidget(provider)
                                },
                                onClose = { viewModel.toggleWidgetSheet(false) }
                            )
                        }
                    }
                }
            }
        }
    }

    fun requestDefaultHomeRole() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            val roleManager = getSystemService(android.app.role.RoleManager::class.java)
            if (roleManager != null && roleManager.isRoleAvailable(android.app.role.RoleManager.ROLE_HOME)) {
                if (!roleManager.isRoleHeld(android.app.role.RoleManager.ROLE_HOME)) {
                    val intent = roleManager.createRequestRoleIntent(android.app.role.RoleManager.ROLE_HOME)
                    startActivity(intent)
                    return
                }
            }
        }
        try {
            val intent = Intent(android.provider.Settings.ACTION_HOME_SETTINGS)
            startActivity(intent)
        } catch (e: Exception) {
            val intent = Intent(android.provider.Settings.ACTION_SETTINGS)
            startActivity(intent)
        }
    }

    override fun onStart() {
        super.onStart()
        widgetHostManager.startListening()
        viewModel.registerPackageReceiver()
    }

    override fun onStop() {
        super.onStop()
        widgetHostManager.stopListening()
        viewModel.unregisterPackageReceiver()
    }

    private fun requestAddWidget(info: AppWidgetProviderInfo) {
        pendingWidgetInfo = info
        val appWidgetId = widgetHostManager.allocateAppWidgetId()
        val canBind = widgetHostManager.appWidgetManager.bindAppWidgetIdIfAllowed(appWidgetId, info.provider)

        if (canBind) {
            configureOrAddWidget(appWidgetId, info)
        } else {
            val intent = Intent(AppWidgetManager.ACTION_APPWIDGET_BIND).apply {
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_PROVIDER, info.provider)
            }
            bindWidgetLauncher.launch(intent)
        }
    }

    private fun configureOrAddWidget(appWidgetId: Int, info: AppWidgetProviderInfo) {
        if (info.configure != null) {
            val intent = Intent(AppWidgetManager.ACTION_APPWIDGET_CONFIGURE).apply {
                component = info.configure
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
            }
            configWidgetLauncher.launch(intent)
        } else {
            val widgetItem = widgetHostManager.createWidgetLayoutItem(
                position = 0,
                appWidgetId = appWidgetId,
                info = info
            )
            viewModel.addWidgetToPage(widgetItem, viewModel.uiState.value.activePageIndex)
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        if (viewModel.uiState.value.currentScreen != LauncherScreen.HOME) {
            viewModel.openScreen(LauncherScreen.HOME)
        } else if (viewModel.uiState.value.activeFolder != null) {
            viewModel.closeFolder()
        } else {
            super.onBackPressed()
        }
    }
}
