# --- kotlinx.serialization (model JSON layout, widget kustom, cadangan) ---
-keepattributes *Annotation*, InnerClasses, Signature
-dontnote kotlinx.serialization.**
-keepclassmembers class kotlinx.serialization.json.** { *** Companion; }
-keep,includedescriptorclasses class com.taro.inklauncher.**$$serializer { *; }
-keepclassmembers class com.taro.inklauncher.** {
    *** Companion;
}
-keepclasseswithmembers class com.taro.inklauncher.** {
    kotlinx.serialization.KSerializer serializer(...);
}
-keepclassmembers @kotlinx.serialization.Serializable class com.taro.inklauncher.** {
    <fields>;
}
-keepclassmembers enum com.taro.inklauncher.** { *; }

# --- AppWidgetHost: view dibuat lewat AppWidgetHost.onCreateView ---
-keep class * extends android.appwidget.AppWidgetHostView { *; }
-keep class com.taro.inklauncher.widget.InkWidgetHostView { *; }

# --- StatusBarManager.expandNotificationsPanel dipanggil lewat refleksi (kelas sistem, tidak di-obfuscate) ---

# Compose & AndroidX membawa aturan consumer sendiri di AAR-nya.
