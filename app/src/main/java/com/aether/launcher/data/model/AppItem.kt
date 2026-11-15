package com.aether.launcher.data.model

data class AppItem(
    val packageName: String,
    val className: String,
    val label: String,
    val category: AppCategory = AppCategory.ALL,
    val iconRevision: Int = 0
)

enum class AppCategory(val displayName: String) {
    ALL("All"),
    SOCIAL("Social"),
    PRODUCTIVITY("Productivity"),
    MEDIA("Media"),
    UTILITIES("Utilities")
}
