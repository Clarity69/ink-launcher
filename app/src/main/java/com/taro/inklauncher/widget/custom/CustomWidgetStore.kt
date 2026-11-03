package com.taro.inklauncher.widget.custom

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File

/** Widget kustom disimpan sebagai file JSON di penyimpanan internal (filesDir/custom_widgets). */
class CustomWidgetStore(context: Context, private val scope: CoroutineScope) {

    private val dir = File(context.filesDir, "custom_widgets").apply { mkdirs() }
    private val mutex = Mutex()

    private val _widgets = MutableStateFlow<List<CustomWidget>>(emptyList())
    val widgets: StateFlow<List<CustomWidget>> = _widgets

    init {
        scope.launch {
            val list = withContext(Dispatchers.IO) { loadAll() }
            _widgets.value = (list + _widgets.value).distinctBy { it.id }.sortedBy { it.name.lowercase() }
        }
    }

    private fun loadAll(): List<CustomWidget> =
        (dir.listFiles { f -> f.extension == "json" } ?: emptyArray())
            .mapNotNull { f -> runCatching { WidgetJson.decodeFromString(CustomWidget.serializer(), f.readText()) }.getOrNull() }
            .sortedBy { it.name.lowercase() }

    fun get(id: String): CustomWidget? = _widgets.value.firstOrNull { it.id == id }

    fun save(widget: CustomWidget) {
        _widgets.value = (_widgets.value.filterNot { it.id == widget.id } + widget).sortedBy { it.name.lowercase() }
        scope.launch(Dispatchers.IO) {
            mutex.withLock {
                runCatching { File(dir, "${widget.id}.json").writeText(toJson(widget)) }
            }
        }
    }

    fun delete(id: String) {
        _widgets.value = _widgets.value.filterNot { it.id == id }
        scope.launch(Dispatchers.IO) {
            mutex.withLock { runCatching { File(dir, "$id.json").delete() } }
        }
    }

    fun duplicate(id: String, suffix: String): CustomWidget? {
        val src = get(id) ?: return null
        val copy = src.copy(id = newId(), name = "${src.name} $suffix", layers = src.layers.map { it.copy(id = newId()) })
        save(copy)
        return copy
    }

    fun toJson(widget: CustomWidget): String = WidgetJson.encodeToString(CustomWidget.serializer(), widget)

    /** Impor .inkw: selalu diberi id baru agar tidak menimpa widget yang ada. */
    fun importJson(text: String): CustomWidget {
        val parsed = WidgetJson.decodeFromString(CustomWidget.serializer(), text)
        val w = parsed.copy(
            id = newId(),
            w = parsed.w.coerceIn(1, 8),
            h = parsed.h.coerceIn(1, 8),
            layers = parsed.layers.map { it.copy(id = newId()) },
        )
        save(w)
        return w
    }

    fun replaceAll(list: List<CustomWidget>) {
        _widgets.value.forEach { File(dir, "${it.id}.json").delete() }
        _widgets.value = emptyList()
        list.forEach { save(it) }
    }
}
