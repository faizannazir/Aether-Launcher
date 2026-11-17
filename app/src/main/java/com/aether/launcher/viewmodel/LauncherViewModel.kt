package com.aether.launcher.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.aether.launcher.data.AppIconCache
import com.aether.launcher.data.AppSearchEngine
import com.aether.launcher.data.HomePreferences
import com.aether.launcher.data.InstalledAppsRepository
import com.aether.launcher.data.LayoutPersistence
import com.aether.launcher.data.model.AppCategory
import com.aether.launcher.data.model.AppItem
import com.aether.launcher.data.model.FolderItem
import com.aether.launcher.data.model.HomeLayoutItem
import com.aether.launcher.data.model.LauncherOptions
import com.aether.launcher.data.model.PageData
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class LauncherViewModel(
    application: Application,
    private val savedStateHandle: SavedStateHandle
) : AndroidViewModel(application) {

    internal val appIcons = AppIconCache(application)
    private val repository = InstalledAppsRepository(application)
    private val preferences = HomePreferences(application)
    private val layoutPersistence = LayoutPersistence(application)
    private val searchEngine = AppSearchEngine()

    private val _uiState = MutableStateFlow(LauncherState())
    val uiState: StateFlow<LauncherState> = _uiState.asStateFlow()

    private var hasRestoredLayout = false
    private var dockPackages: List<String>? = null

    init {
        viewModelScope.launch {
            val savedPages = layoutPersistence.pagesFlow.first()
            hasRestoredLayout = savedPages.isNotEmpty()
            _uiState.update { current ->
                current.copy(
                    pages = savedPages.ifEmpty { current.pages },
                    isLayoutReady = true
                )
            }
            repository.registerPackageReceiver()
            repository.loadInstalledApps()
        }

        viewModelScope.launch {
            repository.installedApps.collect { apps ->
                _uiState.update { current ->
                    val seedHome = current.installedApps.isEmpty() && apps.isNotEmpty() &&
                        current.pages == listOf(PageData(0)) &&
                        !hasRestoredLayout
                    current.copy(
                        installedApps = apps,
                        dockApps = resolveDock(apps),
                        pages = if (seedHome) starterPages(apps) else current.pages,
                        searchResults = searchEngine.search(current.searchQuery, apps)
                    )
                }
                if (apps.isNotEmpty()) hasRestoredLayout = true
            }
        }

        viewModelScope.launch {
            preferences.options.collect { options -> _uiState.update { it.copy(options = options) } }
        }
        viewModelScope.launch {
            preferences.dockPackages.collect { packages ->
                dockPackages = packages
                _uiState.update { it.copy(dockApps = resolveDock(it.installedApps)) }
            }
        }
    }

    private fun homeShortcut(position: Int, app: AppItem) = HomeLayoutItem.App(
        id = "app_${app.packageName}",
        position = position,
        packageName = app.packageName,
        className = app.className
    )

    private fun resolveDock(apps: List<AppItem>): List<AppItem> {
        val packages = dockPackages ?: return chooseApps(apps, dockDefaults, 4)
        val byPackage = apps.associateBy { it.packageName }
        return packages.mapNotNull(byPackage::get)
    }

    private fun starterPages(apps: List<AppItem>): List<PageData> {
        val dock = resolveDock(apps).mapTo(mutableSetOf()) { it.packageName }
        val available = apps.filterNot { it.packageName in dock }
        val byPackage = available.associateBy { it.packageName }
        val tools = toolDefaults.mapNotNull(byPackage::get)
        val toolPackages = tools.mapTo(mutableSetOf()) { it.packageName }
        val mainApps = homeDefaults.mapNotNull(byPackage::get).filterNot { it.packageName in toolPackages }.take(8)
        val homeItems = buildList {
            add(HomeLayoutItem.ServiceWidget("android_controls_0", 0, "Android controls", ""))
            if (tools.isNotEmpty()) {
                add(HomeLayoutItem.Folder("folder_tools", 1, "Tools", tools.map { it.packageName }))
            }
            addAll(mainApps.mapIndexed { index, app -> homeShortcut(index + 2, app) })
        }
        val used = mainApps.mapTo(toolPackages) { it.packageName }
        val secondPage = chooseApps(available.filterNot { it.packageName in used }, secondPageDefaults, 12)
        return buildList {
            add(PageData(0, homeItems))
            if (secondPage.isNotEmpty()) add(PageData(1, secondPage.mapIndexed(::homeShortcut)))
        }
    }

    private fun chooseApps(apps: List<AppItem>, preferred: List<String>, count: Int): List<AppItem> {
        val byPackage = apps.associateBy { it.packageName }
        return (preferred.mapNotNull(byPackage::get) + apps).distinctBy { it.packageName }.take(count)
    }

    private companion object {
        val dockDefaults = listOf(
            "com.google.android.dialer", "com.android.dialer", "com.google.android.apps.messaging",
            "com.android.messaging", "com.android.chrome", "com.google.android.GoogleCamera", "com.android.camera2"
        )
        val toolDefaults = listOf(
            "com.google.android.calendar", "com.google.android.deskclock",
            "com.google.android.calculator", "com.google.android.apps.nbu.files"
        )
        val homeDefaults = listOf(
            "com.google.android.apps.photos", "com.google.android.apps.maps", "com.google.android.gm",
            "com.android.vending", "com.google.android.youtube", "com.google.android.contacts",
            "com.google.android.keep", "com.google.android.apps.drive"
        )
        val secondPageDefaults = listOf(
            "com.whatsapp", "com.google.android.apps.youtube.music", "com.spotify.music",
            "com.google.android.apps.docs", "com.google.android.apps.walletnfcrel", "com.android.settings"
        )
    }

    fun applyStarterLayout(): List<Int> {
        val current = _uiState.value
        if (current.installedApps.isEmpty()) return emptyList()
        dockPackages = null
        val dock = resolveDock(current.installedApps)
        _uiState.value = current.copy(
            pages = starterPages(current.installedApps),
            activePageIndex = 0,
            dockApps = dock,
            activeFolder = null
        )
        val packages = dock.map { it.packageName }
        dockPackages = packages
        viewModelScope.launch { preferences.setDockPackages(packages) }
        saveCurrentLayout()
        return current.pages.flatMap { it.items }.filterIsInstance<HomeLayoutItem.Widget>().map { it.appWidgetId }
    }

    fun placeAppOnHome(
        app: AppItem,
        targetPageId: Int,
        targetIndex: Int,
        sourceItemId: String? = null,
        sourcePageId: Int = targetPageId,
        fromDock: Boolean = false
    ): Boolean {
        val current = _uiState.value
        var destination = current.pages.indexOfFirst { it.pageIndex == targetPageId }
        if (destination < 0) return false
        val pages = current.pages.toMutableList()
        val target = pages[destination]
        val alreadyOnTarget = target.items.any {
            it.id == sourceItemId || it is HomeLayoutItem.App && it.packageName == app.packageName
        }
        if (target.items.size >= current.options.grid.capacity && !alreadyOnTarget) {
            destination = pages.indexOfFirst {
                it.pageIndex != sourcePageId && it.items.size < current.options.grid.capacity
            }
            if (destination < 0) {
                pages.add(PageData((pages.maxOfOrNull { it.pageIndex } ?: -1) + 1))
                destination = pages.lastIndex
            }
        }
        val sourceIndex = pages.indexOfFirst { it.pageIndex == sourcePageId }
        val oldItems = pages[destination].items.sortedBy { it.position }
        val existing = oldItems.filterIsInstance<HomeLayoutItem.App>().firstOrNull { it.packageName == app.packageName }
        val insertion = targetIndex.coerceIn(0, oldItems.size)
        val precedingRemoved = oldItems.take(insertion).count {
            it.id == sourceItemId || it.id == existing?.id
        }
        if (sourceItemId != null && sourceIndex >= 0) {
            pages[sourceIndex] = pages[sourceIndex].copy(items = pages[sourceIndex].items.filterNot { it.id == sourceItemId })
        }
        val items = pages[destination].items.sortedBy { it.position }
            .filterNot { it is HomeLayoutItem.App && it.packageName == app.packageName }.toMutableList()
        items.add((insertion - precedingRemoved).coerceIn(0, items.size), homeShortcut(0, app))
        pages[destination] = pages[destination].copy(items = items.mapIndexed { index, item ->
            when (item) {
                is HomeLayoutItem.App -> item.copy(position = index)
                is HomeLayoutItem.Folder -> item.copy(position = index)
                is HomeLayoutItem.ServiceWidget -> item.copy(position = index)
                is HomeLayoutItem.Widget -> item.copy(position = index)
            }
        })
        _uiState.value = current.copy(pages = pages, activePageIndex = destination)
        if (fromDock) removeDockApp(app)
        saveCurrentLayout()
        return true
    }

    fun groupAppOnHome(
        app: AppItem,
        pageId: Int,
        targetId: String,
        sourceItemId: String? = null,
        sourcePageId: Int = pageId,
        fromDock: Boolean = false
    ): Boolean {
        val current = _uiState.value
        val pageIndex = current.pages.indexOfFirst { it.pageIndex == pageId }
        val page = current.pages.getOrNull(pageIndex) ?: return false
        val target = page.items.firstOrNull { it.id == targetId } ?: return false
        if (targetId == sourceItemId) return false
        val folder = when (target) {
            is HomeLayoutItem.App -> {
                if (target.packageName == app.packageName) return false
                HomeLayoutItem.Folder(
                    "folder_${java.util.UUID.randomUUID()}",
                    target.position, "New folder", listOf(target.packageName, app.packageName)
                )
            }
            is HomeLayoutItem.Folder -> target.copy(appPackages = (target.appPackages + app.packageName).distinct())
            else -> return false
        }
        val pages = current.pages.map { sourcePage ->
            val items = sourcePage.items.filterNot {
                (sourcePage.pageIndex == sourcePageId && it.id == sourceItemId) ||
                    (sourcePage.pageIndex == pageId && it is HomeLayoutItem.App && it.packageName == app.packageName)
            }.map { if (sourcePage.pageIndex == pageId && it.id == targetId) folder else it }
            sourcePage.copy(items = items)
        }
        _uiState.value = current.copy(pages = pages, activePageIndex = pageIndex)
        if (fromDock) removeDockApp(app)
        saveCurrentLayout()
        return true
    }

    fun addDockApp(app: AppItem) {
        val packages = dockPackages ?: _uiState.value.dockApps.map { it.packageName }
        setDockPackages((packages - app.packageName + app.packageName).takeLast(4))
    }

    fun removeDockApp(app: AppItem) {
        val packages = dockPackages ?: _uiState.value.dockApps.map { it.packageName }
        setDockPackages(packages - app.packageName)
    }

    private fun setDockPackages(packages: List<String>) {
        dockPackages = packages
        _uiState.update { it.copy(dockApps = resolveDock(it.installedApps)) }
        viewModelScope.launch { preferences.setDockPackages(packages) }
    }

    fun toggleDockApp(app: AppItem) {
        if (_uiState.value.dockApps.any { it.packageName == app.packageName }) removeDockApp(app)
        else addDockApp(app)
    }

    fun openScreen(screen: LauncherScreen) {
        _uiState.update {
            it.copy(
                currentScreen = screen,
                activeFolder = null,
                isCustomizationSheetOpen = false,
                isWidgetSheetOpen = false,
                searchQuery = "",
                searchResults = emptyList(),
                selectedCategory = AppCategory.ALL
            )
        }
    }

    fun setActivePageIndex(index: Int) {
        _uiState.update { current ->
            if (index in current.pages.indices) current.copy(activePageIndex = index) else current
        }
    }

    fun addAppToHomePage(app: AppItem): Boolean {
        val previous = _uiState.value.pages
        _uiState.update { current ->
            val pages = current.pages.toMutableList()
            val pageIndex = current.activePageIndex.coerceIn(0, pages.lastIndex)
            val page = pages[pageIndex]
            if (page.items.filterIsInstance<HomeLayoutItem.App>().any { it.packageName == app.packageName }) {
                return@update current
            }

            val occupied = page.items.mapTo(mutableSetOf()) { it.position }
            val capacity = current.options.grid.capacity
            val freePosition = (0 until capacity).firstOrNull { it !in occupied }
                ?.takeIf { page.items.size < capacity }
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
        if (_uiState.value.pages == previous) return false
        saveCurrentLayout()
        return true
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

    fun removeEmptyHomePage() {
        val current = _uiState.value
        val index = current.activePageIndex
        if (current.pages.size <= 1 || current.pages.getOrNull(index)?.items?.isNotEmpty() != false) return
        val pages = current.pages.toMutableList().apply { removeAt(index) }
        _uiState.value = current.copy(pages = pages, activePageIndex = index.coerceAtMost(pages.lastIndex))
        saveCurrentLayout()
    }

    fun createFolder(first: HomeLayoutItem.App, second: HomeLayoutItem.App) {
        val current = _uiState.value
        val page = current.pages[current.activePageIndex]
        if (first == second || first !in page.items || second !in page.items) return
        val folder = HomeLayoutItem.Folder(
            id = "folder_${java.util.UUID.randomUUID()}",
            position = minOf(first.position, second.position),
            title = "New folder",
            appPackages = listOf(first.packageName, second.packageName)
        )
        val pages = current.pages.toMutableList()
        pages[current.activePageIndex] = page.copy(items = page.items - first - second + folder)
        _uiState.value = current.copy(pages = pages)
        saveCurrentLayout()
    }

    fun ungroupFolder(folderId: String) {
        val current = _uiState.value
        val page = current.pages[current.activePageIndex]
        val folder = page.items.filterIsInstance<HomeLayoutItem.Folder>().firstOrNull { it.id == folderId } ?: return
        val apps = current.installedApps.associateBy { it.packageName }
        val items = page.items.filterNot { it.id == folderId }.toMutableList()
        val existing = items.filterIsInstance<HomeLayoutItem.App>().mapTo(mutableSetOf()) { it.packageName }
        val occupied = items.mapTo(mutableSetOf()) { it.position }
        folder.appPackages.distinct().filterNot(existing::contains).forEach { packageName ->
            val app = apps[packageName] ?: return@forEach
            val position = generateSequence(folder.position) { it + 1 }.first { occupied.add(it) }
            items.add(homeShortcut(position, app))
        }
        val pages = current.pages.toMutableList()
        pages[current.activePageIndex] = page.copy(items = items)
        _uiState.value = current.copy(pages = pages, activeFolder = null)
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

    fun addAndroidControls() {
        val current = _uiState.value
        val page = current.pages[current.activePageIndex]
        if (page.items.any { it is HomeLayoutItem.ServiceWidget }) return
        val position = (page.items.maxOfOrNull { it.position } ?: -1) + 1
        val item = HomeLayoutItem.ServiceWidget(
            id = "android_controls_${page.pageIndex}",
            position = position,
            title = "Android controls",
            subtitle = ""
        )
        val pages = current.pages.toMutableList()
        pages[current.activePageIndex] = page.copy(items = page.items + item)
        _uiState.value = current.copy(pages = pages)
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
            val occupied = newItems.mapTo(mutableSetOf()) { it.position }
            val position = generateSequence(0) { it + 1 }.first { it !in occupied }
            newItems.add(widget.copy(position = position))
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

    fun updateOptions(transform: (LauncherOptions) -> LauncherOptions) {
        viewModelScope.launch { preferences.updateOptions(transform) }
    }

    override fun onCleared() {
        repository.unregisterPackageReceiver()
        super.onCleared()
    }

    fun launchApp(app: AppItem): Boolean = repository.launchApp(app)

    private fun saveCurrentLayout() {
        viewModelScope.launch {
            layoutPersistence.savePages(_uiState.value.pages)
        }
    }
}
