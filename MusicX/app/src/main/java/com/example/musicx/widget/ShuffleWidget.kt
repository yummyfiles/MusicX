package com.example.musicx.widget

import android.content.Context
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.state.getAppWidgetState
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.padding
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey

class ShuffleWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val prefs: Preferences? = getAppWidgetState(context, id)
        provideContent {
            val hasSongs = prefs?.get(HAS_SONGS) ?: false

            Column(
                modifier = GlanceModifier.fillMaxSize().background(0xFF000000.toInt()).padding(8),
                horizontalAlignment = Alignment.Horizontal.CenterHorizontally,
                verticalAlignment = Alignment.Vertical.CenterVertically
            ) {
                Text(
                    text = if (hasSongs) "\uD83D\uDD00" else "\u274C",
                    style = TextStyle(color = ColorProvider(0xFFFFFFFF.toInt())),
                    modifier = GlanceModifier.padding(bottom = 4)
                )
                Text(
                    text = if (hasSongs) "Shuffle All" else "No Music",
                    style = TextStyle(color = ColorProvider(0xFFFFFFFF.toInt()), fontWeight = FontWeight.Bold)
                )
            }
        }
    }

    companion object {
        val HAS_SONGS = booleanPreferencesKey("shuffle_has_songs")
    }
}

class ShuffleWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget = ShuffleWidget()
}
