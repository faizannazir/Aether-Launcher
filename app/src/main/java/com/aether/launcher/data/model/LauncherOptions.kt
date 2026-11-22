package com.aether.launcher.data.model

enum class LauncherTheme(val title: String) {
    SYSTEM("System"), LIGHT("Light"), DARK("Dark")
}

enum class LauncherPalette(val title: String) {
    WALLPAPER("Wallpaper colors"), GRAPHITE("Graphite"), SAGE("Sage"), ORCHID("Orchid")
}

enum class HomeBackground(val title: String) {
    WALLPAPER("Wallpaper"), GRADIENT("Gradient"), SOLID("Solid")
}

enum class HomeGrid(val title: String, val columns: Int, val rows: Int) {
    COMFORTABLE("3x4", 3, 4), WIDE("3x5", 3, 5), DEFAULT("4x5", 4, 5),
    COMPACT("5x5", 5, 5), DENSE("5x6", 5, 6);

    val capacity: Int get() = columns * rows
}

enum class HomeGesture(val title: String, val defaultAction: GestureAction) {
    SWIPE_UP("Swipe up", GestureAction.DRAWER),
    SWIPE_DOWN("Swipe down", GestureAction.SEARCH),
    DOUBLE_TAP("Double tap empty space", GestureAction.NONE),
    PINCH("Pinch in", GestureAction.EDIT),
    LONG_PRESS("Hold empty space", GestureAction.EDIT)
}

enum class GestureAction(val title: String) {
    NONE("No action"), DRAWER("App drawer"), SEARCH("Search apps"),
    EDIT("Edit home"), SETTINGS("Launcher settings"), INTERNET("Internet controls"),
    ANDROID_SETTINGS("Android settings"), NAVIGATION("System navigation"),
    GOOGLE("Google search"), GEMINI("Open Gemini")
}

data class LauncherOptions(
    val theme: LauncherTheme = LauncherTheme.SYSTEM,
    val palette: LauncherPalette = LauncherPalette.WALLPAPER,
    val showCapsule: Boolean = true,
    val capsuleShowTitles: Boolean = true,
    val background: HomeBackground = HomeBackground.WALLPAPER,
    val glass: Boolean = true,
    val glassOpacity: Float = 0.75f,
    val grid: HomeGrid = HomeGrid.DEFAULT,
    val iconSize: Int = 46,
    val showLabels: Boolean = true,
    val showClock: Boolean = true,
    val showSearch: Boolean = true,
    val showDock: Boolean = true,
    val swipeForWidgets: Boolean = true,
    val drawerColumns: Int = 4,
    val showCategories: Boolean = false,
    val gestures: Map<HomeGesture, GestureAction> = emptyMap()
) {
    fun actionFor(gesture: HomeGesture): GestureAction = gestures[gesture] ?: gesture.defaultAction
}
