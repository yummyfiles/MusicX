package com.example.musicx.ui.theme

import android.content.Context
import android.content.ContextWrapper
import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

val LocalMusicXColors = compositionLocalOf<MusicXColors> {
    ThemeState().toComposeColors()
}

object MusicXTheme {
    val colors: MusicXColors
        @Composable
        @ReadOnlyComposable
        get() = LocalMusicXColors.current
}

@Composable
fun MusicXTheme(
    themeState: ThemeState = ThemeState(),
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val musicXColors = remember(themeState) { themeState.toComposeColors() }
    
    val colorScheme = darkColorScheme(
        primary = musicXColors.primaryAccent,
        secondary = musicXColors.secondaryAccent,
        background = musicXColors.primaryBackground,
        surface = musicXColors.surface,
        onPrimary = musicXColors.buttonText,
        onSecondary = musicXColors.primaryText,
        onBackground = musicXColors.primaryText,
        onSurface = musicXColors.primaryText,
        onSurfaceVariant = musicXColors.secondaryText,
        outline = musicXColors.outline,
        outlineVariant = musicXColors.outlineVariant,
        surfaceVariant = musicXColors.surfaceVariant,
        tertiary = musicXColors.mutedAccent
    )
    
    val view = LocalView.current
    if (!view.isInEditMode) {
        val activity = view.context.findActivity()
        if (activity != null) {
            @Suppress("DEPRECATION")
            SideEffect {
                val window = activity.window
                window.statusBarColor = musicXColors.topBar.toArgb()
                window.navigationBarColor = musicXColors.bottomBar.toArgb()
                
                val insetsController = WindowCompat.getInsetsController(window, view)
                insetsController.isAppearanceLightStatusBars = false
                insetsController.isAppearanceLightNavigationBars = false
            }
        }
    }

    CompositionLocalProvider(
        LocalMusicXColors provides musicXColors,
        androidx.compose.material3.LocalContentColor provides musicXColors.primaryText
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}

tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
