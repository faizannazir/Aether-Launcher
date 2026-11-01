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
                    val defaultDock = apps.take(5)
                    val defaultPageItems = mutableListOf<HomeLayoutItem>()

                    // HarmonyOS 2x2 Service Widget at position 0
                    defaultPageItems.add(
                        HomeLayoutItem.ServiceWidget(
                            id = "service_widget_schedule",
                            position = 0,
                            title = "System Dashboard",
                            subtitle = "Quick Controls & Settings",
                            isFocusActive = true
                        )
                    )

                    // HarmonyOS 2x2 Smart Folder at position 1
                    defaultPageItems.add(
                        HomeLayoutItem.Folder(
                            id = "folder_social",
                            position = 1,
                            title = "Social & Media",
                            appPackages = apps.take(4).map { it.packageName }
                        )
                    )

                    // Remaining apps
                    apps.drop(4).take(8).forEachIndexed { index, app ->
                        defaultPageItems.add(
                            HomeLayoutItem.App(
                                id = "app_${app.packageName}",
                                position = index + 2,
                                packageName = app.packageName,
                                className = app.className
                            )
                        )
                    }

                    current.copy(
                        installedApps = apps,
                        dockApps = if (current.dockApps.isEmpty()) defaultDock else current.dockApps,
                        pages = if (current.pages.firstOrNull()?.items.isNullOrEmpty()) {
                            listOf(PageData(0, defaultPageItems))
                        } else current.pages
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
        _uiState.update { it.copy(activePageIndex = index) }
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

    fun startDrag(item: HomeLayoutItem, pageIndex: Int, position: Int) {
        _uiState.update {
            it.copy(
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

        val page = current.pages.getOrNull(drag.sourcePage) ?: return
        val existingAtTarget = page.items.find { it.position == targetPosition }

        val newItems = page.items.toMutableList()

        if (existingAtTarget != null && existingAtTarget is HomeLayoutItem.App && draggedItem is HomeLayoutItem.App && existingAtTarget.id != draggedItem.id) {
            newItems.remove(draggedItem)
            newItems.remove(existingAtTarget)
            val newFolder = HomeLayoutItem.Folder(
                id = "folder_${System.currentTimeMillis()}",
                position = targetPosition,
                title = "New Folder",
                appPackages = listOf(existingAtTarget.packageName, draggedItem.packageName)
            )
            newItems.add(newFolder)
        } else {
            newItems.remove(draggedItem)
            val moved = when (draggedItem) {
                is HomeLayoutItem.App -> draggedItem.copy(position = targetPosition)
                is HomeLayoutItem.Folder -> draggedItem.copy(position = targetPosition)
                is HomeLayoutItem.ServiceWidget -> draggedItem.copy(position = targetPosition)
                is HomeLayoutItem.Widget -> draggedItem.copy(position = targetPosition)
            }
            newItems.add(moved)
        }

        val updatedPages = current.pages.mapIndexed { idx, p ->
            if (idx == drag.sourcePage) p.copy(items = newItems) else p
        }

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
