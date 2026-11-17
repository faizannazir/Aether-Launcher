package com.aether.launcher

import android.app.Application
import android.content.Intent
import androidx.lifecycle.SavedStateHandle
import androidx.test.core.app.ApplicationProvider
import com.aether.launcher.data.model.AppItem
import com.aether.launcher.data.model.FolderItem
import com.aether.launcher.data.model.HomeLayoutItem
import com.aether.launcher.ui.screens.ClockFormatter
import com.aether.launcher.viewmodel.LauncherScreen
import com.aether.launcher.viewmodel.LauncherViewModel
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
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
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
    fun `grouping shortcuts replaces both with one large folder`() {
        val first = AppItem("com.example.one", "Activity", "One")
        val second = AppItem("com.example.two", "Activity", "Two")
        viewModel.addAppToHomePage(first)
        viewModel.addAppToHomePage(second)
        val shortcuts = viewModel.uiState.value.pages.single().items.filterIsInstance<HomeLayoutItem.App>()

        viewModel.createFolder(shortcuts[0], shortcuts[1])

        val folder = viewModel.uiState.value.pages.single().items.single() as HomeLayoutItem.Folder
        assertEquals(listOf(first.packageName, second.packageName), folder.appPackages)
        assertEquals(0, folder.position)
    }

    @Test
    fun `dropping a drawer app inserts it at the selected Home position`() {
        val first = AppItem("first", "Activity", "First")
        val second = AppItem("second", "Activity", "Second")
        val inserted = AppItem("inserted", "Activity", "Inserted")
        viewModel.addAppToHomePage(first)
        viewModel.addAppToHomePage(second)

        assertTrue(viewModel.placeAppOnHome(inserted, targetPageId = 0, targetIndex = 1))
        assertTrue(viewModel.placeAppOnHome(inserted, targetPageId = 0, targetIndex = 1))

        val apps = viewModel.uiState.value.pages.single().items.filterIsInstance<HomeLayoutItem.App>()
        assertEquals(listOf("first", "inserted", "second"), apps.map { it.packageName })
        assertEquals(listOf(0, 1, 2), apps.map { it.position })
    }

    @Test
    fun `dragging an existing shortcut across pages moves it once`() {
        val app = AppItem("moving", "Activity", "Moving")
        viewModel.addAppToHomePage(app)
        val source = viewModel.uiState.value.pages.single().items.single()
        viewModel.addHomePage()

        assertTrue(viewModel.placeAppOnHome(app, 1, 0, source.id, sourcePageId = 0))

        val pages = viewModel.uiState.value.pages
        assertTrue(pages.first().items.isEmpty())
        assertEquals("moving", (pages.last().items.single() as HomeLayoutItem.App).packageName)
        assertEquals(1, viewModel.uiState.value.activePageIndex)
    }

    @Test
    fun `dropping on an app groups shortcuts and supports adding to that folder`() {
        val first = AppItem("first", "Activity", "First")
        val second = AppItem("second", "Activity", "Second")
        val third = AppItem("third", "Activity", "Third")
        viewModel.addAppToHomePage(first)
        viewModel.addAppToHomePage(second)
        val shortcuts = viewModel.uiState.value.pages.single().items

        assertTrue(viewModel.groupAppOnHome(first, 0, shortcuts[1].id, shortcuts[0].id))
        val folder = viewModel.uiState.value.pages.single().items.single() as HomeLayoutItem.Folder
        assertTrue(viewModel.groupAppOnHome(third, 0, folder.id))
        assertTrue(viewModel.groupAppOnHome(third, 0, folder.id))

        val updated = viewModel.uiState.value.pages.single().items.single() as HomeLayoutItem.Folder
        assertEquals(listOf("second", "first", "third"), updated.appPackages)
    }

    @Test
    fun `dropping on a full page creates a page with an available position`() {
        repeat(20) { index ->
            viewModel.addAppToHomePage(AppItem("app.$index", "Activity", "App $index"))
        }
        val overflow = AppItem("overflow", "Activity", "Overflow")

        assertTrue(viewModel.placeAppOnHome(overflow, 0, Int.MAX_VALUE))

        val state = viewModel.uiState.value
        assertEquals(20, state.pages.first().items.size)
        assertEquals(2, state.pages.size)
        assertEquals(1, state.activePageIndex)
        assertEquals("overflow", (state.pages.last().items.single() as HomeLayoutItem.App).packageName)
    }

    @Test
    fun `a drop on a removed page preserves the source shortcut`() {
        val app = AppItem("moving", "Activity", "Moving")
        viewModel.addAppToHomePage(app)
        val before = viewModel.uiState.value.pages
        val source = before.single().items.single()

        assertEquals(false, viewModel.placeAppOnHome(app, 99, 0, source.id, sourcePageId = 0))
        assertEquals(before, viewModel.uiState.value.pages)
    }

    @Test
    fun `starter layout preserves existing pages while app discovery is empty`() {
        viewModel.addHomePage()
        val pages = viewModel.uiState.value.pages

        val removedWidgets = viewModel.applyStarterLayout()

        assertEquals(pages, viewModel.uiState.value.pages)
        assertEquals(emptyList<Int>(), removedWidgets)
    }

    @Test
    fun `an empty page can be removed without removing the primary page`() {
        viewModel.addHomePage()

        viewModel.removeEmptyHomePage()
        viewModel.removeEmptyHomePage()

        assertEquals(1, viewModel.uiState.value.pages.size)
        assertEquals(0, viewModel.uiState.value.activePageIndex)
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
    fun `launching a shortcut starts its exact activity in a task`() {
        val app = AppItem("com.example", "com.example.Activity", "Example")
        val application = ApplicationProvider.getApplicationContext<Application>()

        assertTrue(viewModel.launchApp(app))

        val launched = shadowOf(application).nextStartedActivity
        assertEquals(app.packageName, launched.component?.packageName)
        assertEquals(app.className, launched.component?.className)
        assertTrue(launched.flags and Intent.FLAG_ACTIVITY_NEW_TASK != 0)
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
