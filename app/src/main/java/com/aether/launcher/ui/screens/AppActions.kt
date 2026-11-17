package com.aether.launcher.ui.screens

import com.aether.launcher.data.model.AppItem
import com.aether.launcher.data.model.HomeLayoutItem

internal data class AppActions(
    val launch: (AppItem) -> Unit,
    val startDrag: (AppItem, HomeLayoutItem.App?, Boolean) -> Unit,
    val remove: (HomeLayoutItem) -> Unit
)
