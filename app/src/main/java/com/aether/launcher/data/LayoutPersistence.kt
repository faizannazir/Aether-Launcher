package com.aether.launcher.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.aether.launcher.data.model.HomeLayoutItem
import com.aether.launcher.data.model.PageData
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject

private val Context.layoutDataStore by preferencesDataStore(name = "aether_layout_preferences")

class LayoutPersistence(private val context: Context) {

    companion object {
        val LAYOUT_JSON = stringPreferencesKey("layout_json")
        val DOCK_PACKAGES_JSON = stringPreferencesKey("dock_packages_json")
    }

    val pagesFlow: Flow<List<PageData>> = context.layoutDataStore.data.map { prefs ->
        val rawJson = prefs[LAYOUT_JSON] ?: ""
        if (rawJson.isEmpty()) {
            emptyList()
        } else {
            deserializePages(rawJson)
        }
    }

    val dockPackagesFlow: Flow<List<String>> = context.layoutDataStore.data.map { prefs ->
        val rawJson = prefs[DOCK_PACKAGES_JSON] ?: ""
        if (rawJson.isEmpty()) {
            emptyList()
        } else {
            val jsonArray = JSONArray(rawJson)
            val list = mutableListOf<String>()
            for (i in 0 until jsonArray.length()) {
                list.add(jsonArray.getString(i))
            }
            list
        }
    }

    suspend fun savePages(pages: List<PageData>) {
        val jsonStr = serializePages(pages)
        context.layoutDataStore.edit { prefs ->
            prefs[LAYOUT_JSON] = jsonStr
        }
    }

    suspend fun saveDockPackages(packages: List<String>) {
        val jsonArray = JSONArray()
        packages.forEach { jsonArray.put(it) }
        context.layoutDataStore.edit { prefs ->
            prefs[DOCK_PACKAGES_JSON] = jsonArray.toString()
        }
    }

    fun serializePages(pages: List<PageData>): String {
        val pagesArray = JSONArray()
        pages.forEach { page ->
            val pageObj = JSONObject()
            pageObj.put("pageIndex", page.pageIndex)

            val itemsArray = JSONArray()
            page.items.forEach { item ->
                val itemObj = JSONObject()
                itemObj.put("id", item.id)
                itemObj.put("position", item.position)

                when (item) {
                    is HomeLayoutItem.App -> {
                        itemObj.put("type", "APP")
                        itemObj.put("packageName", item.packageName)
                        itemObj.put("className", item.className)
                    }
                    is HomeLayoutItem.Folder -> {
                        itemObj.put("type", "FOLDER")
                        itemObj.put("title", item.title)
                        val pkgsArray = JSONArray()
                        item.appPackages.forEach { pkgsArray.put(it) }
                        itemObj.put("appPackages", pkgsArray)
                    }
                    is HomeLayoutItem.ServiceWidget -> {
                        itemObj.put("type", "SERVICE_WIDGET")
                        itemObj.put("title", item.title)
                        itemObj.put("subtitle", item.subtitle)
                        itemObj.put("isFocusActive", item.isFocusActive)
                        itemObj.put("isMuted", item.isMuted)
                    }
                    is HomeLayoutItem.Widget -> {
                        itemObj.put("type", "WIDGET")
                        itemObj.put("appWidgetId", item.appWidgetId)
                        itemObj.put("providerPackage", item.providerPackage)
                        itemObj.put("providerClass", item.providerClass)
                        itemObj.put("spanX", item.spanX)
                        itemObj.put("spanY", item.spanY)
                    }
                }
                itemsArray.put(itemObj)
            }
            pageObj.put("items", itemsArray)
            pagesArray.put(pageObj)
        }
        return pagesArray.toString()
    }

    fun deserializePages(jsonStr: String): List<PageData> {
        val pages = mutableListOf<PageData>()
        try {
            val pagesArray = JSONArray(jsonStr)
            for (i in 0 until pagesArray.length()) {
                val pageObj = pagesArray.getJSONObject(i)
                val pageIndex = pageObj.getInt("pageIndex")

                val itemsArray = pageObj.getJSONArray("items")
                val items = mutableListOf<HomeLayoutItem>()

                for (j in 0 until itemsArray.length()) {
                    val itemObj = itemsArray.getJSONObject(j)
                    val id = itemObj.getString("id")
                    val position = itemObj.getInt("position")
                    val type = itemObj.getString("type")

                    when (type) {
                        "APP" -> {
                            items.add(
                                HomeLayoutItem.App(
                                    id = id,
                                    position = position,
                                    packageName = itemObj.getString("packageName"),
                                    className = itemObj.optString("className", "")
                                )
                            )
                        }
                        "FOLDER" -> {
                            val title = itemObj.getString("title")
                            val pkgsArray = itemObj.getJSONArray("appPackages")
                            val pkgs = mutableListOf<String>()
                            for (k in 0 until pkgsArray.length()) {
                                pkgs.add(pkgsArray.getString(k))
                            }
                            items.add(
                                HomeLayoutItem.Folder(
                                    id = id,
                                    position = position,
                                    title = title,
                                    appPackages = pkgs
                                )
                            )
                        }
                        "SERVICE_WIDGET" -> {
                            items.add(
                                HomeLayoutItem.ServiceWidget(
                                    id = id,
                                    position = position,
                                    title = itemObj.optString("title", "System Dashboard"),
                                    subtitle = itemObj.optString("subtitle", "Quick Controls & Settings"),
                                    isFocusActive = itemObj.optBoolean("isFocusActive", true),
                                    isMuted = itemObj.optBoolean("isMuted", false)
                                )
                            )
                        }
                        "WIDGET" -> {
                            items.add(
                                HomeLayoutItem.Widget(
                                    id = id,
                                    position = position,
                                    appWidgetId = itemObj.getInt("appWidgetId"),
                                    providerPackage = itemObj.getString("providerPackage"),
                                    providerClass = itemObj.getString("providerClass"),
                                    spanX = itemObj.optInt("spanX", 2),
                                    spanY = itemObj.optInt("spanY", 2)
                                )
                            )
                        }
                    }
                }
                pages.add(PageData(pageIndex = pageIndex, items = items))
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return pages
    }
}
