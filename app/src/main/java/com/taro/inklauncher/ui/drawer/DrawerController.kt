package com.taro.inklauncher.ui.drawer

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.taro.inklauncher.LauncherViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * Laci yang mengikuti jari: [progress] 0 = tertutup, 1 = terbuka.
 * Dibaca di dalam lambda graphicsLayer/offset sehingga tidak memicu recompose per frame.
 */
@Stable
class DrawerController(
    private val scope: CoroutineScope,
    private val vm: LauncherViewModel,
) {
    val progress = Animatable(0f)
    var heightPx: Float = 1f

    private val springSpec = spring<Float>(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMediumLow)

    /** dy > 0 = jari bergerak ke bawah (menutup). */
    fun dragBy(dy: Float) {
        val next = (progress.value - dy / heightPx).coerceIn(0f, 1f)
        scope.launch { progress.snapTo(next) }
    }

    /** Lepas jari: tempel ke terbuka/tertutup memakai kecepatan (px/detik, + = ke bawah). */
    fun settle(velocityY: Float) {
        val open = when {
            velocityY < -FLING -> true
            velocityY > FLING -> false
            else -> progress.value > 0.5f
        }
        vm.drawerOpen.value = open
        animateTo(open, initialVelocity = -velocityY / heightPx)
    }

    fun animateTo(open: Boolean, initialVelocity: Float = 0f) {
        val target = if (open) 1f else 0f
        scope.launch { progress.animateTo(target, springSpec, initialVelocity) }
    }

    companion object {
        private const val FLING = 900f
    }
}

@Composable
fun rememberDrawerController(vm: LauncherViewModel): DrawerController {
    val scope = rememberCoroutineScope()
    val controller = remember(vm) { DrawerController(scope, vm) }
    val open by vm.drawerOpen.collectAsStateWithLifecycle()
    // Dibuka/ditutup dari tombol, back, atau home: animasikan bila belum menuju target itu.
    LaunchedEffect(open) {
        val target = if (open) 1f else 0f
        if (controller.progress.targetValue != target) controller.animateTo(open)
    }
    return controller
}
