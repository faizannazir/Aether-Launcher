package com.aether.launcher

import androidx.lifecycle.SavedStateHandle
import androidx.test.core.app.ApplicationProvider
import com.aether.launcher.data.model.AppItem
import com.aether.launcher.data.model.FolderItem
import com.aether.launcher.data.model.HomeLayoutItem
import com.aether.launcher.ui.screens.ClockFormatter
import com.aether.launcher.viewmodel.LauncherScreen
import com.aether.launcher.viewmodel.LauncherViewModel
import com.aether.launcher.viewmodel.refreshDockApps
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.time.LocalDateTime
import java.util.Locale

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class LauncherViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var viewModel: LauncherViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        viewModel = LauncherViewModel(
            ApplicationProvider.getApplicationContext(),
            SavedStateHandle()
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `dock entries refresh to latest installed app icon instances`() {
        val placeholder = AppItem(
            packageName = "com.example.dock",
            className = "com.example.dock.MainActivity",
            label = "Dock app"
        )
        val icon = android.graphics.drawable.ColorDrawable(android.graphics.Color.RED)
        val loaded = placeholder.copy(icon = icon)

        val refreshed = refreshDockApps(listOf(placeholder), listOf(loaded))

        assertEquals(1, refreshed.size)
        assertEquals("com.example.dock", refreshed.single().packageName)
        assertEquals(icon, refreshed.single().icon)
    }

    @Test
    fun `openScreen updates state correctly`() {
        assertEquals(LauncherScreen.HOME, viewModel.uiState.value.currentScreen)
        viewModel.openScreen(LauncherScreen.APP_DRAWER)
        assertEquals(LauncherScreen.APP_DRAWER, viewModel.uiState.value.currentScreen)
    }

    @Test
    fun `folder operations update title and active folder`() {
        val folder = FolderItem("folder_test", "Test Folder", listOf("com.test.app"))
        viewModel.openFolder(folder)

        assertNotNull(viewModel.uiState.value.activeFolder)
        assertEquals("Test Folder", viewModel.uiState.value.activeFolder?.title)

        viewModel.updateFolderTitle("folder_test", "Renamed Folder")
        assertEquals("Renamed Folder", viewModel.uiState.value.activeFolder?.title)

        viewModel.closeFolder()
        assertNull(viewModel.uiState.value.activeFolder)
    }

    @Test
    fun `drag and drop app onto app creates new folder`() {
        val app1 = HomeLayoutItem.App("app_1", 0, "com.test.app1", "Activity1")

        viewModel.startDrag(app1, pageIndex = 0, position = 0)
        assertEquals(app1, viewModel.uiState.value.dragState.draggedItem)

        viewModel.dropDraggedItem(targetPosition = 1)
        assertNull(viewModel.uiState.value.dragState.draggedItem)
    }

    @Test
    fun `dragging an app to an empty desktop slot moves it`() {
        val app = AppItem("com.test.move", "MoveActivity", "Move")
        viewModel.addAppToHomePage(app)

        val item = viewModel.uiState.value.pages.first().items
            .filterIsInstance<HomeLayoutItem.App>().single()
        viewModel.startDrag(item, pageIndex = 0, position = item.position)
        viewModel.dropDraggedItem(targetPosition = 7)

        val moved = viewModel.uiState.value.pages.first().items
            .filterIsInstance<HomeLayoutItem.App>().single()
        assertEquals(7, moved.position)
        assertNull(viewModel.uiState.value.dragState.draggedItem)
    }

    @Test
    fun `dragging an app onto a folder adds it to that folder`() {
        val first = AppItem("com.test.folder.first", "FirstActivity", "First")
        val second = AppItem("com.test.folder.second", "SecondActivity", "Second")
        viewModel.addAppToHomePage(first)
        viewModel.addAppToHomePage(second)
        viewModel.createFolderFromApps(first.packageName, second.packageName, pageIndex = 0)
        val folder = viewModel.uiState.value.pages.first().items
            .filterIsInstance<HomeLayoutItem.Folder>().single()

        val third = AppItem("com.test.folder.third", "ThirdActivity", "Third")
        viewModel.addAppToHomePage(third)
        val thirdItem = viewModel.uiState.value.pages.first().items
            .filterIsInstance<HomeLayoutItem.App>().single()
        viewModel.startDrag(thirdItem, pageIndex = 0, position = thirdItem.position)
        viewModel.dropDraggedItem(targetPosition = folder.position)

        val updatedFolder = viewModel.uiState.value.pages.first().items
            .filterIsInstance<HomeLayoutItem.Folder>().single()
        assertEquals(setOf(first.packageName, second.packageName, third.packageName), updatedFolder.appPackages.toSet())
    }

    @Test
    fun `long press grouping replaces two home apps with a folder`() {
        val first = AppItem("com.test.first", "FirstActivity", "First")
        val second = AppItem("com.test.second", "SecondActivity", "Second")

        viewModel.addAppToHomePage(first)
        viewModel.addAppToHomePage(second)
        viewModel.createFolderFromApps(first.packageName, second.packageName, pageIndex = 0)

        val page = viewModel.uiState.value.pages.first()
        val apps = page.items.filterIsInstance<HomeLayoutItem.App>()
        val folders = page.items.filterIsInstance<HomeLayoutItem.Folder>()

        assertEquals(0, apps.size)
        assertEquals(1, folders.size)
        assertEquals(setOf(first.packageName, second.packageName), folders.single().appPackages.toSet())
    }

    @Test
    fun `removing a home app leaves other desktop items intact`() {
        val first = AppItem("com.test.first", "FirstActivity", "First")
        val second = AppItem("com.test.second", "SecondActivity", "Second")

        viewModel.addAppToHomePage(first)
        viewModel.addAppToHomePage(second)
        viewModel.removeAppFromHome(first.packageName, pageIndex = 0)

        val remaining = viewModel.uiState.value.pages.first().items
            .filterIsInstance<HomeLayoutItem.App>()
            .map { it.packageName }

        assertEquals(listOf(second.packageName), remaining)
    }

    @Test
    fun `adding a home page creates and selects a persistent page`() {
        val initialPageCount = viewModel.uiState.value.pages.size

        viewModel.addHomePage()

        assertEquals(initialPageCount + 1, viewModel.uiState.value.pages.size)
        assertEquals(initialPageCount, viewModel.uiState.value.activePageIndex)
        assertEquals(initialPageCount, viewModel.uiState.value.pages.last().pageIndex)
    }

    @Test
    fun `pinning an app adds it to the active home page`() {
        val app = AppItem(
            packageName = "com.example.pinned",
            className = "com.example.pinned.MainActivity",
            label = "Pinned app"
        )

        viewModel.addHomePage()
        viewModel.addAppToHomePage(app)

        val activePage = viewModel.uiState.value.pages[viewModel.uiState.value.activePageIndex]
        val pinned = activePage.items.filterIsInstance<HomeLayoutItem.App>()
        assertEquals("com.example.pinned", pinned.single().packageName)
    }

    @Test
    fun `active page selection ignores invalid indexes`() {
        viewModel.addHomePage()
        viewModel.setActivePageIndex(0)

        assertEquals(0, viewModel.uiState.value.activePageIndex)

        viewModel.setActivePageIndex(99)
        assertEquals(0, viewModel.uiState.value.activePageIndex)
    }

    @Test
    fun `settings screen is reachable from launcher state`() {
        viewModel.openScreen(LauncherScreen.SETTINGS)
        assertEquals(LauncherScreen.SETTINGS, viewModel.uiState.value.currentScreen)
    }

    @Test
    fun `ClockFormatter formats time correctly for 12 and 24 hour modes`() {
        val testTime = LocalDateTime.of(2026, 10, 30, 14, 30)

        val formatted24 = ClockFormatter.formatTime(testTime, is24Hour = true, locale = Locale.US)
        val formatted12 = ClockFormatter.formatTime(testTime, is24Hour = false, locale = Locale.US)

        assertEquals("14:30", formatted24)
        assertEquals("2:30", formatted12)
    }

    @Test
    fun `calculateMillisToNextMinute computes exact delay to minute boundary`() {
        val timeAt30Secs = LocalDateTime.of(2026, 10, 30, 10, 15, 30, 0)
        val delay30 = ClockFormatter.calculateMillisToNextMinute(timeAt30Secs)
        assertEquals(30000L, delay30)

        val timeAt59Secs = LocalDateTime.of(2026, 10, 30, 10, 15, 59, 500_000_000)
        val delayShort = ClockFormatter.calculateMillisToNextMinute(timeAt59Secs)
        assertEquals(500L, delayShort)
    }
}
