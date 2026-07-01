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

-keep,includedescriptorclasses class com.example.musicx.**$$serializer { *; }
-keepclassmembers class com.example.musicx.** {
    *** Companion;
}
-keepclasseswithmembers class com.example.musicx.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# Keep Room entities
-keep class com.example.musicx.data.local.entity.** { *; }

# Keep data models
-keep class com.example.musicx.model.** { *; }

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

# Keep all GlanceAppWidget subclasses and their provideGlance method
-keep class com.example.musicx.widget.** extends androidx.glance.appwidget.GlanceAppWidget {
    *;
}
-keep class com.example.musicx.widget.** extends androidx.glance.appwidget.GlanceAppWidgetReceiver {
    *;
}
-keep class com.example.musicx.widget.WidgetActionCallback { *; }
-keep class com.example.musicx.widget.WidgetUpdateManager { *; }
-keep class com.example.musicx.widget.WidgetUpdateReceiver { *; }
-keep class com.example.musicx.widget.WidgetActions { *; }
-keep class com.example.musicx.widget.WidgetState { *; }

# Keep Glance framework classes
-keep class androidx.glance.** { *; }
-keep class androidx.glance.appwidget.** { *; }