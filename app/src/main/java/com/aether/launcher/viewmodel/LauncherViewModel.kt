package com.aether.launcher.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.aether.launcher.data.AppSearchEngine
import com.aether.launcher.data.HomePreferences
import com.aether.launcher.data.InstalledAppsRepository
import com.aether.launcher.data.LayoutPersistence
import com.aether.launcher.data.model.AppCategory
import com.aether.launcher.data.model.AppItem
import com.aether.launcher.data.model.FolderItem
import com.aether.launcher.data.model.HomeLayoutItem
import com.aether.launcher.data.model.PageData
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class LauncherViewModel(
    application: Application,
    private val savedStateHandle: SavedStateHandle
) : AndroidViewModel(application) {

    private val repository = InstalledAppsRepository(application)
    private val preferences = HomePreferences(application)
    private val layoutPersistence = LayoutPersistence(application)
    private val searchEngine = AppSearchEngine()

    private val _uiState = MutableStateFlow(LauncherState())
    val uiState: StateFlow<LauncherState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            repository.loadInstalledApps()
        }

        viewModelScope.launch {
            repository.installedApps.collectLatest { apps ->
                _uiState.update { current ->
                    val defaultDock = apps.take(4)
                    val defaultPageItems = mutableListOf<HomeLayoutItem>()
                    if (apps.isNotEmpty()) {
                        defaultPageItems += HomeLayoutItem.ServiceWidget(
                            id = "service_dashboard",
                            position = 0,
                            title = "Quick controls",
                            subtitle = "Focus and system settings"
                        )

                        if (apps.size >= 4) {
                            defaultPageItems += HomeLayoutItem.Folder(
                                id = "folder_everyday",
                                position = 1,
                                title = "Everyday",
                                appPackages = apps.take(4).map { it.packageName }
                            )
                        }

                        val desktopApps = if (apps.size >= 4) apps.drop(4).take(8) else apps.take(8)
                        val firstAppPosition = if (apps.size >= 4) 2 else 1
                        defaultPageItems += desktopApps.mapIndexed { index, app ->
                            HomeLayoutItem.App(
                                id = "app_${app.packageName}",
                                position = firstAppPosition + index,
                                packageName = app.packageName,
                                className = app.className
                            )
                        }
                    }

                    val firstPage = current.pages.firstOrNull()
                    val existingPageApps = firstPage?.items
                        ?.filterIsInstance<HomeLayoutItem.App>()
                        ?.sortedBy { it.position }
                        .orEmpty()
                    val expectedDefaultApps = apps.take(12)
                    val isUntouchedDefaultLayout = firstPage != null &&
                        firstPage.items.size == expectedDefaultApps.size &&
                        existingPageApps.size == expectedDefaultApps.size &&
                        existingPageApps.indices.all { index ->
                            existingPageApps[index].position == index &&
                                existingPageApps[index].packageName == expectedDefaultApps[index].packageName
                        }
                    val shouldApplyDefaultDesktop = firstPage?.items.isNullOrEmpty() || isUntouchedDefaultLayout
                    val updatedPages = if (shouldApplyDefaultDesktop) {
                        val pages = current.pages.toMutableList()
                        if (pages.isEmpty()) {
                            pages += PageData(0, defaultPageItems)
                        } else {
                            pages[0] = PageData(pages[0].pageIndex, defaultPageItems)
                        }
                        pages
                    } else {
                        current.pages
                    }

                    current.copy(
                        installedApps = apps,
                        dockApps = refreshDockApps(current.dockApps, apps).ifEmpty { defaultDock },
                        pages = updatedPages
                    )
                }
            }
        }

        viewModelScope.launch {
            layoutPersistence.pagesFlow.collectLatest { savedPages ->
                if (savedPages.isNotEmpty()) {
                    _uiState.update { it.copy(pages = savedPages) }
                }
            }
        }

        viewModelScope.launch {
            preferences.darkTheme.collectLatest { dark ->
                _uiState.update { it.copy(isDarkTheme = dark) }
            }
        }

        viewModelScope.launch {
            preferences.blurEnabled.collectLatest { blur ->
                _uiState.update { it.copy(isBlurEnabled = blur) }
            }
        }

        viewModelScope.launch {
            preferences.blurIntensity.collectLatest { intensity ->
                _uiState.update { it.copy(blurIntensity = intensity) }
            }
        }

        viewModelScope.launch {
            preferences.gridColumns.collectLatest { grid ->
                _uiState.update { it.copy(gridColumns = grid) }
            }
        }
    }

    fun openScreen(screen: LauncherScreen) {
        _uiState.update { it.copy(currentScreen = screen) }
    }

    fun setActivePageIndex(index: Int) {
        _uiState.update { current ->
            if (index in current.pages.indices) current.copy(activePageIndex = index) else current
        }
    }

    fun addAppToHomePage(app: AppItem) {
        _uiState.update { current ->
            val pages = current.pages.toMutableList()
            val pageIndex = current.activePageIndex.coerceIn(0, pages.lastIndex)
            val page = pages[pageIndex]
            if (page.items.filterIsInstance<HomeLayoutItem.App>().any { it.packageName == app.packageName }) {
                return@update current
            }

            val occupied = page.items.mapTo(mutableSetOf()) { it.position }
            val freePosition = (0 until 20).firstOrNull { it !in occupied }
            if (freePosition != null) {
                pages[pageIndex] = page.copy(
                    items = page.items + HomeLayoutItem.App(
                        id = "app_${app.packageName}",
                        position = freePosition,
                        packageName = app.packageName,
                        className = app.className
                    )
                )
                current.copy(pages = pages)
            } else {
                val newPageIndex = (pages.maxOfOrNull { it.pageIndex } ?: -1) + 1
                pages.add(
                    PageData(
                        pageIndex = newPageIndex,
                        items = listOf(HomeLayoutItem.App(
                            id = "app_${app.packageName}",
                            position = 0,
                            packageName = app.packageName,
                            className = app.className
                        ))
                    )
                )
                current.copy(pages = pages, activePageIndex = pages.lastIndex)
            }
        }
        saveCurrentLayout()
    }

    fun addHomePage() {
        _uiState.update { current ->
            val nextIndex = (current.pages.maxOfOrNull { it.pageIndex } ?: -1) + 1
            current.copy(
                pages = current.pages + PageData(pageIndex = nextIndex),
                activePageIndex = current.pages.size
            )
        }
        saveCurrentLayout()
    }

    fun onSearchQueryChanged(query: String) {
        val results = searchEngine.search(query, _uiState.value.installedApps)
        _uiState.update { it.copy(searchQuery = query, searchResults = results) }
    }

    fun onCategorySelected(category: AppCategory) {
        _uiState.update { it.copy(selectedCategory = category) }
    }

    fun openFolder(folder: FolderItem) {
        _uiState.update { it.copy(activeFolder = folder) }
    }

    fun closeFolder() {
        _uiState.update { it.copy(activeFolder = null) }
    }

    fun updateFolderTitle(folderId: String, newTitle: String) {
        _uiState.update { current ->
            val updatedPages = current.pages.map { page ->
                val newItems = page.items.map { item ->
                    if (item is HomeLayoutItem.Folder && item.id == folderId) {
                        item.copy(title = newTitle)
                    } else item
                }
                page.copy(items = newItems)
            }
            current.copy(
                pages = updatedPages,
                activeFolder = current.activeFolder?.takeIf { it.id == folderId }?.copy(title = newTitle) ?: current.activeFolder
            )
        }
        saveCurrentLayout()
    }

    fun toggleServiceWidgetFocus(widgetId: String) {
        _uiState.update { current ->
            val updatedPages = current.pages.map { page ->
                val updatedItems = page.items.map { item ->
                    if (item is HomeLayoutItem.ServiceWidget && item.id == widgetId) {
                        item.copy(isFocusActive = !item.isFocusActive)
                    } else item
                }
                page.copy(items = updatedItems)
            }
            current.copy(pages = updatedPages)
        }
        saveCurrentLayout()
    }

    fun createFolderFromApps(firstPackage: String, secondPackage: String, pageIndex: Int) {
        if (firstPackage == secondPackage) return
        _uiState.update { current ->
            val pagePosition = current.pages.indexOfFirst { it.pageIndex == pageIndex }
            if (pagePosition < 0) return@update current
            val page = current.pages[pagePosition]
            val apps = page.items.filterIsInstance<HomeLayoutItem.App>()
            val first = apps.firstOrNull { it.packageName == firstPackage } ?: return@update current
            val second = apps.firstOrNull { it.packageName == secondPackage } ?: return@update current
            val folder = HomeLayoutItem.Folder(
                id = "folder_${System.currentTimeMillis()}",
                position = minOf(first.position, second.position),
                title = "New folder",
                appPackages = listOf(first.packageName, second.packageName)
            )
            val updatedPages = current.pages.toMutableList()
            updatedPages[pagePosition] = page.copy(
                items = page.items.filterNot { it.id == first.id || it.id == second.id } + folder
            )
            current.copy(pages = updatedPages)
        }
        saveCurrentLayout()
    }

    fun removeAppFromHome(packageName: String, pageIndex: Int) {
        _uiState.update { current ->
            val pagePosition = current.pages.indexOfFirst { it.pageIndex == pageIndex }
            if (pagePosition < 0) return@update current
            val page = current.pages[pagePosition]
            val updatedPages = current.pages.toMutableList()
            updatedPages[pagePosition] = page.copy(
                items = page.items.filterNot {
                    it is HomeLayoutItem.App && it.packageName == packageName
                }
            )
            current.copy(pages = updatedPages)
        }
        saveCurrentLayout()
    }

    fun removeAppFromFolder(folderId: String, packageName: String) {
        _uiState.update { current ->
            val updatedPages = current.pages.map { page ->
                val folder = page.items.filterIsInstance<HomeLayoutItem.Folder>()
                    .firstOrNull { it.id == folderId } ?: return@map page
                val remaining = folder.appPackages.filterNot { it == packageName }
                val replacement: List<HomeLayoutItem> = when (remaining.size) {
                    0 -> emptyList()
                    1 -> {
                        val app = current.installedApps.firstOrNull { it.packageName == remaining.single() }
                        if (app == null) emptyList() else listOf(
                            HomeLayoutItem.App(
                                id = "app_${app.packageName}",
                                position = folder.position,
                                packageName = app.packageName,
                                className = app.className
                            )
                        )
                    }
                    else -> listOf(folder.copy(appPackages = remaining))
                }
                page.copy(items = page.items.filterNot { it.id == folderId } + replacement)
            }
            val updatedActiveFolder = current.activeFolder?.let { active ->
                if (active.id != folderId) active else {
                    val remaining = active.appPackages.filterNot { it == packageName }
                    if (remaining.isEmpty()) null else active.copy(appPackages = remaining)
                }
            }
            current.copy(pages = updatedPages, activeFolder = updatedActiveFolder)
        }
        saveCurrentLayout()
    }

    fun ungroupFolder(folderId: String, pageIndex: Int) {
        _uiState.update { current ->
            val pagePosition = current.pages.indexOfFirst { it.pageIndex == pageIndex }
            if (pagePosition < 0) return@update current
            val page = current.pages[pagePosition]
            val folder = page.items.filterIsInstance<HomeLayoutItem.Folder>()
                .firstOrNull { it.id == folderId } ?: return@update current
            val existingPackages = page.items.filterIsInstance<HomeLayoutItem.App>()
                .mapTo(mutableSetOf()) { it.packageName }
            val expandedApps = folder.appPackages
                .distinct()
                .filterNot { it in existingPackages }
                .mapNotNull { packageName ->
                    current.installedApps.firstOrNull { it.packageName == packageName }
                }
            val occupiedPositions = page.items
                .filterNot { it.id == folder.id }
                .mapTo(mutableSetOf()) { it.position }
            val freePositions = (0 until 20)
                .filterNot { it in occupiedPositions }
                .sortedBy { position -> kotlin.math.abs(position - folder.position) }
            val replacement = expandedApps.zip(freePositions).map { (app, position) ->
                HomeLayoutItem.App(
                    id = "app_${app.packageName}",
                    position = position,
                    packageName = app.packageName,
                    className = app.className
                )
            }
            val updatedPages = current.pages.toMutableList()
            updatedPages[pagePosition] = page.copy(
                items = page.items.filterNot { it.id == folderId } + replacement
            )
            current.copy(
                pages = updatedPages,
                activeFolder = current.activeFolder?.takeIf { it.id != folderId }
            )
        }
        saveCurrentLayout()
    }

    fun startDrag(item: HomeLayoutItem, pageIndex: Int, position: Int) {
        _uiState.update { current ->
            val pagePosition = current.pages.indexOfFirst { it.pageIndex == pageIndex }
            val updatedPages = if (pagePosition >= 0 && current.pages[pagePosition].items.isEmpty()) {
                val seededItems = current.installedApps.take(12).mapIndexed { index, app ->
                    HomeLayoutItem.App(
                        id = "app_${app.packageName}",
                        position = index,
                        packageName = app.packageName,
                        className = app.className
                    )
                }
                current.pages.toMutableList().also { pages ->
                    pages[pagePosition] = pages[pagePosition].copy(items = seededItems)
                }
            } else current.pages
            current.copy(
                pages = updatedPages,
                dragState = DragState(
                    draggedItem = item,
                    sourcePage = pageIndex,
                    sourcePosition = position,
                    currentHoverPosition = position
                )
            )
        }
    }

    fun updateHoverPosition(position: Int) {
        _uiState.update { current ->
            current.copy(dragState = current.dragState.copy(currentHoverPosition = position))
        }
    }

    fun dropDraggedItem(targetPosition: Int) {
        val current = _uiState.value
        val drag = current.dragState
        val draggedItem = drag.draggedItem ?: return
        val pagePosition = current.pages.indexOfFirst { it.pageIndex == drag.sourcePage }
        if (pagePosition < 0) {
            cancelDrag()
            return
        }

        val page = current.pages[pagePosition]
        val target = page.items.firstOrNull { it.position == targetPosition }
        val newItems = page.items.toMutableList()

        when {
            target == null -> {
                newItems.remove(draggedItem)
                val moved = when (draggedItem) {
                    is HomeLayoutItem.App -> draggedItem.copy(position = targetPosition)
                    is HomeLayoutItem.Folder -> draggedItem.copy(position = targetPosition)
                    is HomeLayoutItem.ServiceWidget -> draggedItem.copy(position = targetPosition)
                    is HomeLayoutItem.Widget -> draggedItem.copy(position = targetPosition)
                }
                newItems.add(moved)
            }
            target.id == draggedItem.id -> Unit
            target is HomeLayoutItem.App && draggedItem is HomeLayoutItem.App -> {
                newItems.remove(draggedItem)
                newItems.remove(target)
                newItems.add(
                    HomeLayoutItem.Folder(
                        id = "folder_${System.currentTimeMillis()}",
                        position = targetPosition,
                        title = "New folder",
                        appPackages = listOf(target.packageName, draggedItem.packageName).distinct()
                    )
                )
            }
            target is HomeLayoutItem.Folder && draggedItem is HomeLayoutItem.App -> {
                newItems.remove(draggedItem)
                newItems[newItems.indexOf(target)] = target.copy(
                    appPackages = (target.appPackages + draggedItem.packageName).distinct()
                )
            }
            else -> {
                _uiState.update { it.copy(dragState = DragState()) }
                return
            }
        }

        val updatedPages = current.pages.toMutableList()
        updatedPages[pagePosition] = page.copy(items = newItems)
        _uiState.update { it.copy(pages = updatedPages, dragState = DragState()) }
        saveCurrentLayout()
    }

    fun cancelDrag() {
        _uiState.update { it.copy(dragState = DragState()) }
    }

    fun addWidgetToPage(widget: HomeLayoutItem.Widget, pageIndex: Int) {
        _uiState.update { current ->
            val page = current.pages.getOrNull(pageIndex) ?: PageData(pageIndex)
            val newItems = page.items.toMutableList()
            newItems.add(widget)
            val updatedPages = current.pages.toMutableList()
            if (pageIndex < updatedPages.size) {
                updatedPages[pageIndex] = page.copy(items = newItems)
            } else {
                updatedPages.add(page.copy(items = newItems))
            }
            current.copy(pages = updatedPages)
        }
        saveCurrentLayout()
    }

    fun removeLayoutItem(item: HomeLayoutItem, pageIndex: Int) {
        _uiState.update { current ->
            val page = current.pages.getOrNull(pageIndex) ?: return@update current
            val newItems = page.items.filterNot { it.id == item.id }
            val updatedPages = current.pages.toMutableList()
            updatedPages[pageIndex] = page.copy(items = newItems)
            current.copy(pages = updatedPages)
        }
        saveCurrentLayout()
    }

    fun toggleCustomizationSheet(open: Boolean) {
        _uiState.update { it.copy(isCustomizationSheetOpen = open) }
    }

    fun toggleWidgetSheet(open: Boolean) {
        _uiState.update { it.copy(isWidgetSheetOpen = open) }
    }

    fun setDarkTheme(enabled: Boolean) {
        viewModelScope.launch { preferences.setDarkTheme(enabled) }
    }

    fun setBlurEnabled(enabled: Boolean) {
        viewModelScope.launch { preferences.setBlurEnabled(enabled) }
    }

    fun setBlurIntensity(intensity: Float) {
        viewModelScope.launch { preferences.setBlurIntensity(intensity) }
    }

    fun setGridColumns(grid: String) {
        viewModelScope.launch { preferences.setGridColumns(grid) }
    }


    fun registerPackageReceiver() {
        repository.registerPackageReceiver()
    }

    fun unregisterPackageReceiver() {
        repository.unregisterPackageReceiver()
    }

    fun launchApp(app: AppItem) {
        repository.launchApp(app)
    }

    private fun saveCurrentLayout() {
        viewModelScope.launch {
            layoutPersistence.savePages(_uiState.value.pages)
        }
    }
}
