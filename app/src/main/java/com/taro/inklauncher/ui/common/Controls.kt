package com.taro.inklauncher.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.taro.inklauncher.R
import com.taro.inklauncher.ui.theme.Figtree
import com.taro.inklauncher.ui.theme.LocalInk
import kotlin.math.roundToInt

@Composable
fun InkChip(label: String, selected: Boolean, onClick: () -> Unit) {
    val ink = LocalInk.current
    val shape = RoundedCornerShape(50)
    Box(
        Modifier
            .clip(shape)
            .then(if (selected) Modifier.background(ink.fg, shape) else Modifier)
            .border(1.dp, ink.fg, shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 7.dp),
    ) {
        Text(label, color = if (selected) ink.bg else ink.fg, fontFamily = Figtree, fontSize = 14.sp)
    }
}

@Composable
fun ChipRow(content: @Composable RowScope.() -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
        content = content,
    )
}

@Composable
fun LabeledSlider(
    label: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    onChange: (Float) -> Unit,
    format: (Float) -> String = { it.roundToInt().toString() },
) {
    val ink = LocalInk.current
    Column(Modifier.padding(horizontal = 16.dp, vertical = 2.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(label, color = ink.fg, fontFamily = Figtree, fontSize = 14.sp, modifier = Modifier.weight(1f))
            Text(format(value), color = ink.muted, fontFamily = Figtree, fontSize = 14.sp)
        }
        Slider(value = value.coerceIn(range.start, range.endInclusive), onValueChange = onChange, valueRange = range)
    }
}

@Composable
fun Stepper(label: String, value: Int, min: Int, max: Int, onChange: (Int) -> Unit) {
    val ink = LocalInk.current
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, color = ink.fg, fontFamily = Figtree, fontSize = 15.sp, modifier = Modifier.weight(1f))
        IconAction(R.drawable.ic_ms_remove, null) { if (value > min) onChange(value - 1) }
        Text(value.toString(), color = ink.fg, fontFamily = Figtree, fontSize = 16.sp, modifier = Modifier.padding(horizontal = 8.dp))
        IconAction(R.drawable.ic_ms_add, null) { if (value < max) onChange(value + 1) }
    }
}

@Composable
fun InkTextField(
    value: String,
    onChange: (String) -> Unit,
    label: String,
    singleLine: Boolean = true,
    modifier: Modifier = Modifier,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label) },
        singleLine = singleLine,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
    )
}
