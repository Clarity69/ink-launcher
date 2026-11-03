package com.taro.inklauncher.data

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File
import java.util.UUID

@Serializable
enum class ItemType { APP, WIDGET, CUSTOM }

@Serializable
data class HomeItem(
    val id: String = UUID.randomUUID().toString(),
    val type: ItemType,
    val appKey: String? = null,
    val appWidgetId: Int = -1,
    val customId: String? = null,
    val page: Int = 0,
    val x: Int = 0,
    val y: Int = 0,
    val w: Int = 1,
    val h: Int = 1,
)

const val DOCK_SLOTS = 5

@Serializable
data class HomeLayout(
    val items: List<HomeItem> = emptyList(),
    val dock: List<String?> = List(DOCK_SLOTS) { null },
    val initialized: Boolean = false,
) {
    fun dockSafe(): List<String?> =
        if (dock.size >= DOCK_SLOTS) dock.take(DOCK_SLOTS) else dock + List(DOCK_SLOTS - dock.size) { null }

    /** Sel kosong? (item `ignoreId` diabaikan) */
    fun isFree(page: Int, x: Int, y: Int, w: Int, h: Int, cols: Int, rows: Int, ignoreId: String? = null): Boolean {
        if (x < 0 || y < 0 || x + w > cols || y + h > rows) return false
        return items.none { it.id != ignoreId && it.page == page && overlaps(it, x, y, w, h) }
    }

    fun itemAt(page: Int, x: Int, y: Int): HomeItem? =
        items.firstOrNull { it.page == page && x >= it.x && x < it.x + it.w && y >= it.y && y < it.y + it.h }

    fun firstFree(w: Int, h: Int, cols: Int, rows: Int, pages: Int, startPage: Int = 0): Triple<Int, Int, Int>? {
        val order = (startPage until pages) + (0 until startPage)
        for (p in order) {
            for (y in 0..rows - h) for (x in 0..cols - w) {
                if (isFree(p, x, y, w, h, cols, rows)) return Triple(p, x, y)
            }
        }
        return null
    }

    private fun overlaps(a: HomeItem, x: Int, y: Int, w: Int, h: Int): Boolean =
        a.x < x + w && x < a.x + a.w && a.y < y + h && y < a.y + a.h
}

val LayoutJson = Json {
    ignoreUnknownKeys = true
    encodeDefaults = true
    prettyPrint = false
}

/** Menyimpan tata letak layar utama sebagai JSON di penyimpanan internal. */
class LayoutStore(context: Context, private val scope: CoroutineScope) {

    private val file = File(context.filesDir, "layout.json")
    private val mutex = Mutex()
    private var saveJob: Job? = null

    private val _layout = MutableStateFlow(load())
    val layout: StateFlow<HomeLayout> = _layout

    private fun load(): HomeLayout = runCatching {
        if (file.exists()) LayoutJson.decodeFromString(HomeLayout.serializer(), file.readText()) else HomeLayout()
    }.getOrDefault(HomeLayout())

    fun update(transform: (HomeLayout) -> HomeLayout) {
        val next = transform(_layout.value)
        if (next == _layout.value) return
        _layout.value = next
        scheduleSave()
    }

    fun replace(layout: HomeLayout) {
        _layout.value = layout
        scheduleSave()
    }

    private fun scheduleSave() {
        saveJob?.cancel()
        saveJob = scope.launch {
            delay(250)
            val snapshot = _layout.value
            withContext(Dispatchers.IO) {
                mutex.withLock {
                    runCatching {
                        val tmp = File(file.parentFile, "layout.json.tmp")
                        tmp.writeText(LayoutJson.encodeToString(HomeLayout.serializer(), snapshot))
                        if (!tmp.renameTo(file)) {
                            file.writeText(tmp.readText())
                            tmp.delete()
                        }
                    }
                }
            }
        }
    }
}
