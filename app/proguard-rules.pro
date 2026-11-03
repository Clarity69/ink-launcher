# Model JSON (kotlinx.serialization)
-keepattributes *Annotation*, InnerClasses
-keep,includedescriptorclasses class com.taro.inklauncher.**$$serializer { *; }
-keepclassmembers class com.taro.inklauncher.** {
    *** Companion;
}
-keepclasseswithmembers class com.taro.inklauncher.** {
    kotlinx.serialization.KSerializer serializer(...);
}

