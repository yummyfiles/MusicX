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

# Keep Room components
-keep class com.yummyfiles.musicx.data.** { *; }
-keep @androidx.room.Entity class *
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Dao interface *

# Keep data models
-keep class com.yummyfiles.musicx.model.** { *; }

# Keep Media3 components
-keep class androidx.media3.** { *; }
-keep interface androidx.media3.** { *; }
-dontwarn androidx.media3.**
-keep class com.yummyfiles.musicx.playback.** { *; }
-keep class com.google.android.exoplayer2.** { *; }
-dontwarn com.google.android.exoplayer2.**

# Keep Coil
-keep class coil.** { *; }
-keep interface coil.** { *; }
-dontwarn coil.**
-keep class com.google.common.util.concurrent.ListenableFuture { *; }

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