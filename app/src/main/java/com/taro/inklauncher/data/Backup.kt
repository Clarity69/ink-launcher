package com.taro.inklauncher.data

import com.taro.inklauncher.widget.custom.CustomWidget
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/** Cadangan tata letak + widget kustom + pengaturan (widget sistem tidak ikut). */
@Serializable
data class Backup(
    val app: String = "InkLauncher",
    val version: Int = 1,
    val layout: HomeLayout,
    val customWidgets: List<CustomWidget> = emptyList(),
    val prefs: Map<String, String> = emptyMap(),
)

val BackupJson = Json {
    ignoreUnknownKeys = true
    encodeDefaults = true
    prettyPrint = true
    coerceInputValues = true
}
