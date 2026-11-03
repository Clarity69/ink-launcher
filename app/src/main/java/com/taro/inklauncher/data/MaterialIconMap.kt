package com.taro.inklauncher.data

import androidx.annotation.DrawableRes
import com.taro.inklauncher.R

/**
 * Peta package name -> ikon Material Symbols Outlined (Apache 2.0).
 * Hanya ikon generik; tidak memakai logo merek.
 */
object MaterialIconMap {

    private val map: Map<String, Int> = buildMap {
        fun add(@DrawableRes res: Int, vararg pkgs: String) = pkgs.forEach { this[it] = res }
        add(R.drawable.ic_ms_call, "com.android.dialer", "com.google.android.dialer", "com.android.incallui", "com.samsung.android.dialer")
        add(R.drawable.ic_ms_contacts, "com.android.contacts", "com.google.android.contacts")
        add(R.drawable.ic_ms_sms, "com.android.mms", "com.google.android.apps.messaging", "com.android.messaging", "com.samsung.android.messaging")
        add(R.drawable.ic_ms_photo_camera, "com.android.camera", "com.android.camera2", "com.google.android.GoogleCamera", "org.codeaurora.snapcam", "com.sec.android.app.camera")
        add(R.drawable.ic_ms_photo_library, "com.miui.gallery", "com.google.android.apps.photos", "com.android.gallery3d", "com.sec.android.gallery3d", "com.google.android.apps.photosgo")
        add(R.drawable.ic_ms_settings, "com.android.settings")
        add(R.drawable.ic_ms_calendar_month, "com.google.android.calendar", "com.android.calendar", "com.xiaomi.calendar", "com.samsung.android.calendar")
        add(R.drawable.ic_ms_schedule, "com.android.deskclock", "com.google.android.deskclock", "com.sec.android.app.clockpackage")
        add(R.drawable.ic_ms_calculate, "com.miui.calculator", "com.google.android.calculator", "com.android.calculator2", "com.sec.android.app.popupcalculator")
        add(R.drawable.ic_ms_folder, "com.mi.android.globalFileexplorer", "com.android.fileexplorer", "com.google.android.apps.nbu.files", "com.android.documentsui", "com.google.android.documentsui", "com.sec.android.app.myfiles")
        add(R.drawable.ic_ms_language, "com.android.chrome", "com.mi.globalbrowser", "com.android.browser", "org.mozilla.firefox", "com.brave.browser", "com.microsoft.emmx", "com.opera.browser", "com.sec.android.app.sbrowser", "com.duckduckgo.mobile.android")
        add(R.drawable.ic_ms_mail, "com.google.android.gm", "com.android.email", "com.microsoft.office.outlook", "ch.protonmail.android")
        add(R.drawable.ic_ms_map, "com.google.android.apps.maps", "com.google.android.apps.mapslite", "net.osmand", "com.waze")
        add(R.drawable.ic_ms_music_note, "com.miui.player", "com.google.android.apps.youtube.music", "com.spotify.music", "com.android.music", "com.apple.android.music")
        add(R.drawable.ic_ms_edit_note, "com.miui.notes", "com.google.android.keep", "com.samsung.android.app.notes")
        add(R.drawable.ic_ms_partly_cloudy_day, "com.miui.weather2", "com.google.android.apps.weather", "com.sec.android.daemonapp")
        add(R.drawable.ic_ms_shop, "com.android.vending", "com.xiaomi.mipicks", "com.xiaomi.market")
        add(R.drawable.ic_ms_search, "com.google.android.googlequicksearchbox", "com.google.android.apps.searchlite")
        add(R.drawable.ic_ms_smart_display, "com.google.android.youtube", "com.miui.videoplayer", "com.miui.video", "org.videolan.vlc", "com.netflix.mediaclient")
        add(R.drawable.ic_ms_play_circle, "com.google.android.videos", "com.mxtech.videoplayer.ad")
        add(R.drawable.ic_ms_chat, "com.whatsapp", "com.whatsapp.w4b", "jp.naver.line.android", "com.facebook.orca")
        add(R.drawable.ic_ms_forum, "org.telegram.messenger", "org.thoughtcrime.securesms", "com.discord", "com.Slack")
        add(R.drawable.ic_ms_translate, "com.google.android.apps.translate")
        add(R.drawable.ic_ms_radio, "com.miui.fm", "com.android.fmradio")
        add(R.drawable.ic_ms_voicemail, "com.android.soundrecorder", "com.miui.soundrecorder", "com.google.android.apps.recorder")
        add(R.drawable.ic_ms_qr_code_scanner, "com.xiaomi.scanner")
        add(R.drawable.ic_ms_security, "com.miui.securitycenter", "com.miui.cleaner", "com.miui.cleanmaster")
        add(R.drawable.ic_ms_palette, "com.android.thememanager")
        add(R.drawable.ic_ms_fitness_center, "com.mi.health", "com.xiaomi.hm.health", "com.google.android.apps.fitness", "com.xiaomi.wearable")
        add(R.drawable.ic_ms_backup, "com.miui.backup", "com.miui.cloudbackup", "com.miui.cloudservice")
        add(R.drawable.ic_ms_screen_rotation, "com.miui.screenrecorder")
        add(R.drawable.ic_ms_flashlight_on, "com.miui.flashlight")
        add(R.drawable.ic_ms_menu_book, "com.google.android.apps.books", "com.amazon.kindle")
        add(R.drawable.ic_ms_podcasts, "com.google.android.apps.podcasts")
        add(R.drawable.ic_ms_wallet, "com.google.android.apps.walletnfcrel", "com.mipay.wallet.id")
        add(R.drawable.ic_ms_shopping_bag, "com.tokopedia.tkpd", "com.shopee.id", "com.lazada.android", "com.amazon.mShop.android.shopping")
        add(R.drawable.ic_ms_directions_car, "com.gojek.app", "com.grabtaxi.passenger", "com.ubercab")
        add(R.drawable.ic_ms_newsmode, "com.google.android.apps.magazines")
        add(R.drawable.ic_ms_storefront, "com.miui.compass", "com.android.stk")
        add(R.drawable.ic_ms_home, "com.google.android.apps.chromecast.app", "com.xiaomi.smarthome")
        add(R.drawable.ic_ms_mic, "com.google.android.apps.googleassistant")
        add(R.drawable.ic_ms_cloud, "com.google.android.apps.docs", "com.dropbox.android", "com.microsoft.skydrive")
        add(R.drawable.ic_ms_image_search, "com.google.ar.lens")
        add(R.drawable.ic_ms_sim_card, "com.android.stk2")
        add(R.drawable.ic_ms_person, "com.google.android.gms", "com.xiaomi.account")
        add(R.drawable.ic_ms_videocam, "com.google.android.apps.tachyon", "us.zoom.videomeetings", "com.google.android.apps.meetings")
        add(R.drawable.ic_ms_work, "com.microsoft.teams", "com.google.android.apps.dynamite")
        add(R.drawable.ic_ms_school, "com.google.android.apps.classroom", "com.duolingo")
        add(R.drawable.ic_ms_tune, "com.miui.misound", "com.miui.powerkeeper")
        add(R.drawable.ic_ms_download, "com.android.providers.downloads.ui")
        add(R.drawable.ic_ms_sticky_note_2, "com.microsoft.office.onenote")
        add(R.drawable.ic_ms_sports_esports, "com.xiaomi.glgm", "com.google.android.play.games")
    }

    @DrawableRes
    fun forPackage(packageName: String): Int? = map[packageName]
}

/** Daftar ikon Material Symbols bawaan untuk editor widget kustom. */
object MaterialSymbols {
    val all: Map<String, Int> = linkedMapOf(
        "account_balance" to R.drawable.ic_ms_account_balance,
        "add" to R.drawable.ic_ms_add,
        "alarm" to R.drawable.ic_ms_alarm,
        "apps" to R.drawable.ic_ms_apps,
        "arrow_back" to R.drawable.ic_ms_arrow_back,
        "arrow_downward" to R.drawable.ic_ms_arrow_downward,
        "arrow_upward" to R.drawable.ic_ms_arrow_upward,
        "backup" to R.drawable.ic_ms_backup,
        "battery_charging_full" to R.drawable.ic_ms_battery_charging_full,
        "battery_full" to R.drawable.ic_ms_battery_full,
        "bluetooth" to R.drawable.ic_ms_bluetooth,
        "bolt" to R.drawable.ic_ms_bolt,
        "book" to R.drawable.ic_ms_book,
        "calculate" to R.drawable.ic_ms_calculate,
        "calendar_month" to R.drawable.ic_ms_calendar_month,
        "call" to R.drawable.ic_ms_call,
        "cast" to R.drawable.ic_ms_cast,
        "cell_tower" to R.drawable.ic_ms_cell_tower,
        "center_focus_weak" to R.drawable.ic_ms_center_focus_weak,
        "chat" to R.drawable.ic_ms_chat,
        "check" to R.drawable.ic_ms_check,
        "circle" to R.drawable.ic_ms_circle,
        "close" to R.drawable.ic_ms_close,
        "cloud" to R.drawable.ic_ms_cloud,
        "contacts" to R.drawable.ic_ms_contacts,
        "content_copy" to R.drawable.ic_ms_content_copy,
        "crop_square" to R.drawable.ic_ms_crop_square,
        "dark_mode" to R.drawable.ic_ms_dark_mode,
        "dashboard_customize" to R.drawable.ic_ms_dashboard_customize,
        "delete" to R.drawable.ic_ms_delete,
        "directions_car" to R.drawable.ic_ms_directions_car,
        "download" to R.drawable.ic_ms_download,
        "drag_indicator" to R.drawable.ic_ms_drag_indicator,
        "edit_note" to R.drawable.ic_ms_edit_note,
        "edit" to R.drawable.ic_ms_edit,
        "event" to R.drawable.ic_ms_event,
        "favorite" to R.drawable.ic_ms_favorite,
        "fitness_center" to R.drawable.ic_ms_fitness_center,
        "flashlight_on" to R.drawable.ic_ms_flashlight_on,
        "foggy" to R.drawable.ic_ms_foggy,
        "folder" to R.drawable.ic_ms_folder,
        "forum" to R.drawable.ic_ms_forum,
        "gesture" to R.drawable.ic_ms_gesture,
        "grid_view" to R.drawable.ic_ms_grid_view,
        "headphones" to R.drawable.ic_ms_headphones,
        "health_and_safety" to R.drawable.ic_ms_health_and_safety,
        "history" to R.drawable.ic_ms_history,
        "home" to R.drawable.ic_ms_home,
        "horizontal_rule" to R.drawable.ic_ms_horizontal_rule,
        "image_search" to R.drawable.ic_ms_image_search,
        "image" to R.drawable.ic_ms_image,
        "info" to R.drawable.ic_ms_info,
        "language" to R.drawable.ic_ms_language,
        "layers" to R.drawable.ic_ms_layers,
        "light_mode" to R.drawable.ic_ms_light_mode,
        "location_on" to R.drawable.ic_ms_location_on,
        "lock" to R.drawable.ic_ms_lock,
        "mail" to R.drawable.ic_ms_mail,
        "map" to R.drawable.ic_ms_map,
        "menu_book" to R.drawable.ic_ms_menu_book,
        "mic" to R.drawable.ic_ms_mic,
        "more_vert" to R.drawable.ic_ms_more_vert,
        "music_note" to R.drawable.ic_ms_music_note,
        "newsmode" to R.drawable.ic_ms_newsmode,
        "notes" to R.drawable.ic_ms_notes,
        "open_with" to R.drawable.ic_ms_open_with,
        "palette" to R.drawable.ic_ms_palette,
        "partly_cloudy_day" to R.drawable.ic_ms_partly_cloudy_day,
        "person" to R.drawable.ic_ms_person,
        "phone_android" to R.drawable.ic_ms_phone_android,
        "phone_in_talk" to R.drawable.ic_ms_phone_in_talk,
        "photo_camera" to R.drawable.ic_ms_photo_camera,
        "photo_library" to R.drawable.ic_ms_photo_library,
        "play_circle" to R.drawable.ic_ms_play_circle,
        "podcasts" to R.drawable.ic_ms_podcasts,
        "public" to R.drawable.ic_ms_public,
        "qr_code_scanner" to R.drawable.ic_ms_qr_code_scanner,
        "radio" to R.drawable.ic_ms_radio,
        "rainy" to R.drawable.ic_ms_rainy,
        "refresh" to R.drawable.ic_ms_refresh,
        "remove" to R.drawable.ic_ms_remove,
        "schedule" to R.drawable.ic_ms_schedule,
        "school" to R.drawable.ic_ms_school,
        "screen_rotation" to R.drawable.ic_ms_screen_rotation,
        "search" to R.drawable.ic_ms_search,
        "security" to R.drawable.ic_ms_security,
        "settings_backup_restore" to R.drawable.ic_ms_settings_backup_restore,
        "settings" to R.drawable.ic_ms_settings,
        "share" to R.drawable.ic_ms_share,
        "shopping_bag" to R.drawable.ic_ms_shopping_bag,
        "shopping_cart" to R.drawable.ic_ms_shopping_cart,
        "shop" to R.drawable.ic_ms_shop,
        "sim_card" to R.drawable.ic_ms_sim_card,
        "smart_display" to R.drawable.ic_ms_smart_display,
        "sms" to R.drawable.ic_ms_sms,
        "sports_esports" to R.drawable.ic_ms_sports_esports,
        "star" to R.drawable.ic_ms_star,
        "sticky_note_2" to R.drawable.ic_ms_sticky_note_2,
        "storefront" to R.drawable.ic_ms_storefront,
        "sunny" to R.drawable.ic_ms_sunny,
        "swipe_up" to R.drawable.ic_ms_swipe_up,
        "text_fields" to R.drawable.ic_ms_text_fields,
        "thunderstorm" to R.drawable.ic_ms_thunderstorm,
        "touch_app" to R.drawable.ic_ms_touch_app,
        "translate" to R.drawable.ic_ms_translate,
        "tune" to R.drawable.ic_ms_tune,
        "upload" to R.drawable.ic_ms_upload,
        "videocam" to R.drawable.ic_ms_videocam,
        "visibility_off" to R.drawable.ic_ms_visibility_off,
        "visibility" to R.drawable.ic_ms_visibility,
        "voicemail" to R.drawable.ic_ms_voicemail,
        "wallet" to R.drawable.ic_ms_wallet,
        "wallpaper" to R.drawable.ic_ms_wallpaper,
        "weather_snowy" to R.drawable.ic_ms_weather_snowy,
        "widgets" to R.drawable.ic_ms_widgets,
        "wifi" to R.drawable.ic_ms_wifi,
        "work" to R.drawable.ic_ms_work,
    )

    @DrawableRes
    fun res(name: String): Int = all[name] ?: R.drawable.ic_ms_star
}
