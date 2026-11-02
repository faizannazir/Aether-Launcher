package com.aether.launcher.viewmodel

import com.aether.launcher.data.model.AppCategory
import com.aether.launcher.data.model.AppItem
import com.aether.launcher.data.model.FolderItem
import com.aether.launcher.data.model.HomeLayoutItem
import com.aether.launcher.data.model.PageData

enum class LauncherScreen {
    HOME,
    APP_DRAWER,
    SEARCH_OVERLAY,
    EDIT_MODE,
    SETTINGS
}

data class DragState(
    val draggedItem: HomeLayoutItem? = null,
    val sourcePage: Int = 0,
    val sourcePosition: Int = -1,
    val currentHoverPosition: Int = -1,
    val targetFolderId: String? = null
)

data class LauncherState(
    val currentScreen: LauncherScreen = LauncherScreen.HOME,
    val installedApps: List<AppItem> = emptyList(),
    val pages: List<PageData> = listOf(PageData(0, emptyList())),
    val activePageIndex: Int = 0,
    val dockApps: List<AppItem> = emptyList(),
    val folders: List<FolderItem> = emptyList(),
    val activeFolder: FolderItem? = null,
    val dragState: DragState = DragState(),
    val isCustomizationSheetOpen: Boolean = false,
    val isWidgetSheetOpen: Boolean = false,
    val searchQuery: String = "",
    val searchResults: List<AppItem> = emptyList(),
    val selectedCategory: AppCategory = AppCategory.ALL,
    val isDarkTheme: Boolean = false,
    val isBlurEnabled: Boolean = true,
    val blurIntensity: Float = 0.75f,
    val gridColumns: String = "4x5",
    val iconSizeDp: Float = 52f,
    val showIconLabels: Boolean = true,
    val dockIconSizeDp: Float = 48f
)

/** Rebind dock entries to the latest installed-app objects so asynchronously loaded icons
 * and package metadata replace the initial icon-less placeholders. Removed apps disappear. */
internal fun refreshDockApps(
    dockApps: List<AppItem>,
    installedApps: List<AppItem>
): List<AppItem> {
    val installedByPackage = installedApps.associateBy(AppItem::packageName)
    return if (dockApps.isEmpty()) {
        installedApps.take(4)
    } else {
        dockApps.mapNotNull { dockApp -> installedByPackage[dockApp.packageName] }
    }
}
