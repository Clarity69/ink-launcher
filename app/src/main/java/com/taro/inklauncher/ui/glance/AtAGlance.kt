package com.taro.inklauncher.ui.glance

import android.text.format.DateFormat
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.taro.inklauncher.LauncherViewModel
import com.taro.inklauncher.system.LaunchHelper
import com.taro.inklauncher.ui.common.MsIcon
import com.taro.inklauncher.ui.theme.Figtree
import com.taro.inklauncher.ui.theme.LocalInk
import java.text.SimpleDateFormat
import java.util.Date

/** "At a Glance" versi e-ink: tanggal besar + satu baris info (kalender/cuaca). */
@Composable
fun AtAGlance(vm: LauncherViewModel, modifier: Modifier = Modifier) {
    val ink = LocalInk.current
    val context = LocalContext.current
    val now by vm.now.collectAsStateWithLifecycle()
    val line by vm.glanceLine.collectAsStateWithLifecycle()
    val locale = LocalConfiguration.current.locales[0]
    val minute = now / 60_000L
    val date = remember(minute, locale) {
        val pattern = DateFormat.getBestDateTimePattern(locale, "EEEEdMMM")
        SimpleDateFormat(pattern, locale).format(Date(now))
    }
    Column(modifier.padding(horizontal = 12.dp, vertical = 12.dp)) {
        Text(
            text = date,
            color = ink.fg,
            fontFamily = Figtree,
            fontSize = 30.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
            ) { LaunchHelper.calendar(context) },
        )
        line?.let { l ->
            Row(Modifier.padding(top = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                MsIcon(l.icon, null, size = 18.dp)
                Spacer(Modifier.width(8.dp))
                Text(
                    text = l.text,
                    color = ink.fg,
                    fontFamily = Figtree,
                    fontSize = 16.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}
