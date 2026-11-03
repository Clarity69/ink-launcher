package com.taro.inklauncher.ui.home

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import com.taro.inklauncher.DropTarget
import com.taro.inklauncher.MoveSource
import com.taro.inklauncher.data.AppEntry

data class DragPayload(val source: MoveSource, val entry: AppEntry)

/** Status drag & drop ikon (koordinat root). */
@Stable
class DragController {
    var payload by mutableStateOf<DragPayload?>(null)
        private set
    var pointer by mutableStateOf(Offset.Zero)
    var rootOrigin = Offset.Zero

    val gridBounds = HashMap<Int, Rect>()
    var currentPage = 0
    var cols = 4
    var rows = 5
    var dockBounds = Rect.Zero
    var dockCount = 4

    fun start(p: DragPayload) {
        payload = p
    }

    fun end() {
        payload = null
    }

    fun target(): DropTarget? {
        val pt = pointer
        if (dockCount > 0 && dockBounds.contains(pt)) {
            val slot = dockBounds.width / dockCount
            val i = ((pt.x - dockBounds.left) / slot).toInt().coerceIn(0, dockCount - 1)
            return DropTarget.Dock(i)
        }
        val g = gridBounds[currentPage] ?: return null
        if (!g.contains(pt)) return null
        val x = ((pt.x - g.left) / (g.width / cols)).toInt().coerceIn(0, cols - 1)
        val y = ((pt.y - g.top) / (g.height / rows)).toInt().coerceIn(0, rows - 1)
        return DropTarget.Cell(currentPage, x, y)
    }
}
