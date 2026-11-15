package com.aether.launcher.data

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.aether.launcher.data.model.GestureAction
import com.aether.launcher.data.model.HomeBackground
import com.aether.launcher.data.model.HomeGesture
import com.aether.launcher.data.model.HomeGrid
import com.aether.launcher.data.model.LauncherOptions
import com.aether.launcher.data.model.LauncherTheme
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "aether_preferences")

class HomePreferences(private val context: Context) {
    private companion object {
        val THEME = stringPreferencesKey("theme")
        val LEGACY_DARK_THEME = booleanPreferencesKey("dark_theme")
        val BACKGROUND = stringPreferencesKey("home_background")
        val GLASS = booleanPreferencesKey("blur_enabled")
        val GLASS_OPACITY = floatPreferencesKey("blur_intensity")
        val GRID = stringPreferencesKey("grid_columns")
        val ICON_SIZE = intPreferencesKey("icon_size")
        val LABELS = booleanPreferencesKey("show_labels")
        val CLOCK = booleanPreferencesKey("show_clock")
        val SEARCH = booleanPreferencesKey("show_search")
        val DOCK = booleanPreferencesKey("show_dock")
        val WIDGET_SWIPE = booleanPreferencesKey("swipe_for_widgets")
        val DRAWER_COLUMNS = intPreferencesKey("drawer_columns")
        val CATEGORIES = booleanPreferencesKey("show_categories")
        val DOCK_PACKAGES = stringPreferencesKey("dock_packages")
        val GESTURES = HomeGesture.entries.associateWith { stringPreferencesKey("gesture_${it.name.lowercase()}") }
    }

    val options = context.dataStore.data.map(::readOptions).distinctUntilChanged()

    val dockPackages = context.dataStore.data.map { prefs ->
        prefs[DOCK_PACKAGES]?.split(",")?.filter(String::isNotEmpty)
    }.distinctUntilChanged()

    suspend fun updateOptions(transform: (LauncherOptions) -> LauncherOptions) {
        context.dataStore.edit { prefs ->
            val options = transform(readOptions(prefs))
            prefs[THEME] = options.theme.name
            prefs[BACKGROUND] = options.background.name
            prefs[GLASS] = options.glass
            prefs[GLASS_OPACITY] = options.glassOpacity
            prefs[GRID] = options.grid.title
            prefs[ICON_SIZE] = options.iconSize
            prefs[LABELS] = options.showLabels
            prefs[CLOCK] = options.showClock
            prefs[SEARCH] = options.showSearch
            prefs[DOCK] = options.showDock
            prefs[WIDGET_SWIPE] = options.swipeForWidgets
            prefs[DRAWER_COLUMNS] = options.drawerColumns
            prefs[CATEGORIES] = options.showCategories
            GESTURES.forEach { (gesture, key) -> prefs[key] = options.actionFor(gesture).name }
        }
    }

    suspend fun setDockPackages(packages: List<String>) {
        context.dataStore.edit { it[DOCK_PACKAGES] = packages.joinToString(",") }
    }

    private fun readOptions(prefs: Preferences): LauncherOptions {
        val theme = LauncherTheme.entries.firstOrNull { it.name == prefs[THEME] }
            ?: when (prefs[LEGACY_DARK_THEME]) {
                true -> LauncherTheme.DARK
                false -> LauncherTheme.LIGHT
                null -> LauncherTheme.SYSTEM
            }
        val gestures = GESTURES.mapValues { (gesture, key) ->
            GestureAction.entries.firstOrNull { it.name == prefs[key] } ?: gesture.defaultAction
        }

        return LauncherOptions(
            theme = theme,
            background = HomeBackground.entries.firstOrNull { it.name == prefs[BACKGROUND] } ?: HomeBackground.WALLPAPER,
            glass = prefs[GLASS] ?: true,
            glassOpacity = ((prefs[GLASS_OPACITY] ?: 0.75f).takeIf(Float::isFinite) ?: 0.75f).coerceIn(0f, 1f),
            grid = HomeGrid.entries.firstOrNull { it.title == prefs[GRID] } ?: HomeGrid.DEFAULT,
            iconSize = (prefs[ICON_SIZE] ?: 46).coerceIn(36, 56),
            showLabels = prefs[LABELS] ?: true,
            showClock = prefs[CLOCK] ?: true,
            showSearch = prefs[SEARCH] ?: true,
            showDock = prefs[DOCK] ?: true,
            swipeForWidgets = prefs[WIDGET_SWIPE] ?: true,
            drawerColumns = (prefs[DRAWER_COLUMNS] ?: 4).coerceIn(3, 5),
            showCategories = prefs[CATEGORIES] ?: false,
            gestures = gestures
        )
    }
}
