// Tata letak bilah pencarian terinspirasi Lawnchair (GPL-3.0): lawnchair/src/app/lawnchair/qsb/LawnQsbUi.kt
package com.taro.inklauncher.ui.search

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.taro.inklauncher.R
import com.taro.inklauncher.system.LaunchHelper
import com.taro.inklauncher.ui.common.MsIcon
import com.taro.inklauncher.ui.theme.Figtree
import com.taro.inklauncher.ui.theme.LocalInk

/** Bilah pencarian kapsul ala Pixel: "G" di kiri, mic & Lens di kanan. */
@Composable
fun PixelSearchBar(modifier: Modifier = Modifier) {
    val ink = LocalInk.current
    val context = LocalContext.current
    Row(
        modifier
            .fillMaxWidth()
            .height(52.dp)
            .clip(CircleShape)
            .border(1.5.dp, ink.fg, CircleShape)
            .clickable { LaunchHelper.googleSearch(context) }
            .padding(start = 20.dp, end = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "G",
            color = ink.fg,
            fontFamily = Figtree,
            fontWeight = FontWeight.Medium,
            fontSize = 24.sp,
        )
        Spacer(Modifier.weight(1f))
        RoundAction(R.drawable.ic_ms_mic, stringResource(R.string.cd_voice_search)) {
            LaunchHelper.voiceSearch(context)
        }
        Spacer(Modifier.width(2.dp))
        RoundAction(R.drawable.ic_ms_center_focus_weak, stringResource(R.string.cd_lens)) {
            LaunchHelper.lens(context)
        }
    }
}

@Composable
private fun RoundAction(icon: Int, cd: String, onClick: () -> Unit) {
    Box(
        Modifier
            .size(40.dp)
            .clip(CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        MsIcon(icon, cd, size = 22.dp)
    }
}
