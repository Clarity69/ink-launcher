# Shizuku: newProcess dipanggil lewat refleksi
-keep class rikka.shizuku.** { *; }
-keep class moe.shizuku.** { *; }

# Model JSON (kotlinx.serialization)
-keepattributes *Annotation*, InnerClasses
-keep,includedescriptorclasses class com.taro.inklauncher.**$$serializer { *; }
-keepclassmembers class com.taro.inklauncher.** {
    *** Companion;
}
-keepclasseswithmembers class com.taro.inklauncher.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# Aksesibilitas
-keep class com.taro.inklauncher.system.LockAccessibilityService { *; }
