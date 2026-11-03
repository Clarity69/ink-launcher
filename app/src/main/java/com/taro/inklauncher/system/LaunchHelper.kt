// Intent Search/voice/Lens diadaptasi dari Lawnchair (GPL-3.0):
// lawnchair/src/app/lawnchair/qsb/LawnQsbLayout.kt & qsb/providers/Google.kt
package com.taro.inklauncher.system

import android.app.SearchManager
import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.AlarmClock
import android.provider.CalendarContract
import android.provider.Settings
import android.speech.RecognizerIntent

object LaunchHelper {

    const val GOOGLE_APP = "com.google.android.googlequicksearchbox"
    const val LENS_APP = "com.google.ar.lens"

    /** Coba intent satu per satu; berhenti di yang pertama berhasil. */
    fun tryStart(context: Context, intents: List<Intent?>): Boolean {
        for (intent in intents) {
            if (intent == null) continue
            try {
                context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                return true
            } catch (e: ActivityNotFoundException) {
            } catch (e: SecurityException) {
            } catch (e: Exception) {
            }
        }
        return false
    }

    fun googleSearch(context: Context): Boolean = tryStart(
        context,
        listOf(
            Intent(Intent.ACTION_MAIN).setComponent(
                ComponentName(GOOGLE_APP, "com.google.android.googlequicksearchbox.SearchActivity"),
            ),
            Intent("android.search.action.GLOBAL_SEARCH").setPackage(GOOGLE_APP),
            Intent(Intent.ACTION_WEB_SEARCH).putExtra(SearchManager.QUERY, ""),
            Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/")),
        ),
    )

    fun voiceSearch(context: Context): Boolean = tryStart(
        context,
        listOf(
            Intent(RecognizerIntent.ACTION_WEB_SEARCH).setPackage(GOOGLE_APP),
            Intent(Intent.ACTION_VOICE_COMMAND).setPackage(GOOGLE_APP),
            Intent(Intent.ACTION_VOICE_COMMAND),
            Intent(RecognizerIntent.ACTION_WEB_SEARCH),
        ),
    ) || googleSearch(context)

    fun lens(context: Context): Boolean = tryStart(
        context,
        listOf(
            Intent.makeMainActivity(
                ComponentName(LENS_APP, "com.google.vr.apps.ornament.app.lens.LensLauncherActivity"),
            ),
            context.packageManager.getLaunchIntentForPackage(LENS_APP),
            Intent(Intent.ACTION_MAIN).setComponent(
                ComponentName(GOOGLE_APP, "com.google.android.apps.search.lens.LensExportedActivity"),
            ),
            Intent(Intent.ACTION_VIEW, Uri.parse("googleapp://lens")).setPackage(GOOGLE_APP),
        ),
    ) || googleSearch(context)

    fun clock(context: Context): Boolean = tryStart(
        context,
        listOf(
            Intent(AlarmClock.ACTION_SHOW_ALARMS),
            context.packageManager.getLaunchIntentForPackage("com.android.deskclock"),
            context.packageManager.getLaunchIntentForPackage("com.google.android.deskclock"),
        ),
    )

    fun calendar(context: Context): Boolean = tryStart(
        context,
        listOf(
            Intent(Intent.ACTION_VIEW).setData(
                CalendarContract.CONTENT_URI.buildUpon().appendPath("time")
                    .appendPath(System.currentTimeMillis().toString()).build(),
            ),
            Intent.makeMainSelectorActivity(Intent.ACTION_MAIN, Intent.CATEGORY_APP_CALENDAR),
        ),
    )

    fun openPackage(context: Context, pkg: String): Boolean =
        tryStart(context, listOf(context.packageManager.getLaunchIntentForPackage(pkg)))

    fun uninstall(context: Context, pkg: String): Boolean = tryStart(
        context,
        listOf(Intent(Intent.ACTION_DELETE, Uri.fromParts("package", pkg, null))),
    )

    fun wallpaper(context: Context): Boolean = tryStart(
        context,
        listOf(
            Intent.createChooser(Intent(Intent.ACTION_SET_WALLPAPER), null),
            Intent(Intent.ACTION_SET_WALLPAPER),
        ),
    )

    fun homeSettings(context: Context): Boolean = tryStart(
        context,
        listOf(
            Intent(Settings.ACTION_HOME_SETTINGS),
            Intent(Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS),
            Intent(Settings.ACTION_SETTINGS),
        ),
    )

    fun url(context: Context, url: String): Boolean =
        tryStart(context, listOf(Intent(Intent.ACTION_VIEW, Uri.parse(url))))
}
