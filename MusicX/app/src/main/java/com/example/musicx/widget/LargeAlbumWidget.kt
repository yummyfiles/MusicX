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
import androidx.datastore.preferences.core.stringPreferencesKey

class LargeAlbumWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val prefs: Preferences? = getAppWidgetState(context, id)
        provideContent {
            val title = prefs?.get(SONG_TITLE) ?: ""
            val artist = prefs?.get(SONG_ARTIST) ?: ""
            val isPlaying = prefs?.get(IS_PLAYING) ?: false
            val hasSongs = prefs?.get(HAS_SONGS) ?: false

            Column(
                modifier = GlanceModifier.fillMaxSize().background(0xFF000000.toInt()).padding(12),
                horizontalAlignment = Alignment.Horizontal.CenterHorizontally,
                verticalAlignment = Alignment.Vertical.CenterVertically
            ) {
                if (!hasSongs) {
                    Text(
                        text = "No Music Found",
                        style = TextStyle(color = ColorProvider(0xFFFFFFFF.toInt()), fontWeight = FontWeight.Bold)
                    )
                } else {
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
                    Text(
                        text = if (isPlaying) "\u25B6 Playing" else "\u23F8 Paused",
                        style = TextStyle(color = ColorProvider(0xFF1DB954.toInt())),
                        modifier = GlanceModifier.padding(4)
                    )
                }
            }
        }
    }

    companion object {
        val SONG_TITLE = stringPreferencesKey("large_song_title")
        val SONG_ARTIST = stringPreferencesKey("large_song_artist")
        val ALBUM_ART_PATH = stringPreferencesKey("large_album_art")
        val IS_PLAYING = booleanPreferencesKey("large_is_playing")
        val HAS_SONGS = booleanPreferencesKey("large_has_songs")
    }
}

class LargeAlbumWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget = LargeAlbumWidget()
}
