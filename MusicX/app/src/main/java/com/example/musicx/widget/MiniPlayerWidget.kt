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
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.padding
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey

class MiniPlayerWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val prefs: Preferences? = getAppWidgetState(context, id)
        provideContent {
            val title = prefs?.get(SONG_TITLE) ?: ""
            val artist = prefs?.get(SONG_ARTIST) ?: ""
            val artPath = prefs?.get(ALBUM_ART_PATH) ?: ""
            val isPlaying = prefs?.get(IS_PLAYING) ?: false
            val hasSongs = prefs?.get(HAS_SONGS) ?: false

            Column(
                modifier = GlanceModifier.fillMaxSize().background(0xFF000000.toInt()).padding(8),
                verticalAlignment = Alignment.Vertical.CenterVertically
            ) {
                if (!hasSongs) {
                    Text(
                        text = "No Music Found",
                        style = TextStyle(color = ColorProvider(0xFFFFFFFF.toInt()), fontWeight = FontWeight.Bold)
                    )
                } else {
                    Row(
                        modifier = GlanceModifier.defaultWeight(),
                        verticalAlignment = Alignment.Vertical.CenterVertically
                    ) {
                        Column(
                            modifier = GlanceModifier.defaultWeight(),
                            verticalAlignment = Alignment.Vertical.CenterVertically
                        ) {
                            Text(
                                text = title.ifBlank { "Nothing Playing" },
                                style = TextStyle(color = ColorProvider(0xFFFFFFFF.toInt()), fontWeight = FontWeight.Bold),
                                maxLines = 1
                            )
                            if (artist.isNotBlank()) {
                                Text(
                                    text = artist,
                                    style = TextStyle(color = ColorProvider(0xFFAAAAAA.toInt())),
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    companion object {
        val SONG_TITLE = stringPreferencesKey("mini_song_title")
        val SONG_ARTIST = stringPreferencesKey("mini_song_artist")
        val ALBUM_ART_PATH = stringPreferencesKey("mini_album_art")
        val IS_PLAYING = booleanPreferencesKey("mini_is_playing")
        val HAS_SONGS = booleanPreferencesKey("mini_has_songs")
    }
}

class MiniPlayerWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget = MiniPlayerWidget()
}
