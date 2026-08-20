# R8 Optimization Rules for MusicX
# Aggressive optimization with full R8

# Keep serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt

-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}

-keep,includedescriptorclasses class com.yummyfiles.musicx.**$$serializer { *; }
-keepclassmembers class com.yummyfiles.musicx.** {
    *** Companion;
}
-keepclasseswithmembers class com.yummyfiles.musicx.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# Keep Room entities
-keep class com.yummyfiles.musicx.data.local.entity.** { *; }

# Keep data models
-keep class com.yummyfiles.musicx.model.** { *; }

# Keep Media3 components
-keep class androidx.media3.** { *; }

# Keep Coil
-keep class coil.** { *; }

# Keep Moshi
-keep class com.squareup.moshi.** { *; }

# Keep Compose
-dontwarn androidx.compose.**

# Remove logging in release
-assumenosideeffects class android.util.Log {
    public static *** d(...);
    public static *** v(...);
    public static *** i(...);
}