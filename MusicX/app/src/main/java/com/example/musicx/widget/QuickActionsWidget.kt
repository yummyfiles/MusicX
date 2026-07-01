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
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey

class QuickActionsWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val prefs: Preferences? = getAppWidgetState(context, id)
        provideContent {
            val hasSongs = prefs?.get(HAS_SONGS) ?: false

            Row(
                modifier = GlanceModifier.fillMaxSize().background(0xFF000000.toInt()).padding(8),
                horizontalAlignment = Alignment.Horizontal.CenterHorizontally,
                verticalAlignment = Alignment.Vertical.CenterVertically
            ) {
                if (hasSongs) {
                    Column(
                        modifier = GlanceModifier.defaultWeight(),
                        horizontalAlignment = Alignment.Horizontal.CenterHorizontally
                    ) {
                        Text(text = "\u25B6", style = TextStyle(color = ColorProvider(0xFFFFFFFF.toInt())))
                        Text(text = "Resume", style = TextStyle(color = ColorProvider(0xFFFFFFFF.toInt())))
                    }
                }

                Spacer(modifier = GlanceModifier.width(8))

                Column(
                    modifier = GlanceModifier.defaultWeight(),
                    horizontalAlignment = Alignment.Horizontal.CenterHorizontally
                ) {
                    Text(text = "\uD83D\uDD0D", style = TextStyle(color = ColorProvider(0xFFFFFFFF.toInt())))
                    Text(text = "Search", style = TextStyle(color = ColorProvider(0xFFFFFFFF.toInt())))
                }

                Spacer(modifier = GlanceModifier.width(8))

                Column(
                    modifier = GlanceModifier.defaultWeight(),
                    horizontalAlignment = Alignment.Horizontal.CenterHorizontally
                ) {
                    Text(text = "\u2B07", style = TextStyle(color = ColorProvider(0xFFFFFFFF.toInt())))
                    Text(text = "Import", style = TextStyle(color = ColorProvider(0xFFFFFFFF.toInt())))
                }
            }
        }
    }

    companion object {
        val HAS_SONGS = booleanPreferencesKey("qa_has_songs")
    }
}

class QuickActionsWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget = QuickActionsWidget()
}
