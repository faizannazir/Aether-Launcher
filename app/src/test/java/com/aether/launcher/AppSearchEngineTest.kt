package com.aether.launcher

import com.aether.launcher.data.AppSearchEngine
import com.aether.launcher.data.model.AppCategory
import com.aether.launcher.data.model.AppItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class AppSearchEngineTest {

    private lateinit var searchEngine: AppSearchEngine
    private lateinit var testApps: List<AppItem>

    @Before
    fun setUp() {
        searchEngine = AppSearchEngine()
        testApps = listOf(
            AppItem("com.spotify.music", "MainActivity", "Spotify", AppCategory.MEDIA),
            AppItem("com.google.android.youtube", "MainActivity", "YouTube", AppCategory.MEDIA),
            AppItem("com.google.android.apps.maps", "MainActivity", "Google Maps", AppCategory.PRODUCTIVITY),
            AppItem("com.aether.settings", "SettingsActivity", "Settings", AppCategory.UTILITIES)
        )
    }

    @Test
    fun `search returns matching apps starting with query first`() {
        val results = searchEngine.search("spo", testApps)
        assertEquals(1, results.size)
        assertEquals("Spotify", results[0].label)
    }

    @Test
    fun `search returns multiple substring matches sorted correctly`() {
        val results = searchEngine.search("s", testApps)
        assertTrue(results.isNotEmpty())
        assertEquals("Settings", results[0].label)
        assertEquals("Spotify", results[1].label)
    }

    @Test
    fun `search with empty query returns empty list`() {
        val results = searchEngine.search("   ", testApps)
        assertTrue(results.isEmpty())
    }
}
