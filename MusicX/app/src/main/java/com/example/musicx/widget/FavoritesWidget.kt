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
import androidx.datastore.preferences.core.stringPreferencesKey

class FavoritesWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val prefs: Preferences? = getAppWidgetState(context, id)
        provideContent {
            val favoritesJson = prefs?.get(FAVORITES_JSON) ?: "[]"

            Column(
                modifier = GlanceModifier.fillMaxSize().background(0xFF000000.toInt()).padding(12)
            ) {
                Text(
                    text = "Favorites",
                    style = TextStyle(color = ColorProvider(0xFFFFFFFF.toInt()), fontWeight = FontWeight.Bold),
                    modifier = GlanceModifier.padding(bottom = 8)
                )
                Text(
                    text = "Use the app to manage favorites",
                    style = TextStyle(color = ColorProvider(0xFF888888.toInt()))
                )
            }
        }
    }

    companion object {
        val FAVORITES_JSON = stringPreferencesKey("fav_json")
    }
}

class FavoritesWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget = FavoritesWidget()
}
