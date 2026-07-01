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
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey

class MediumPlayerWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val prefs: Preferences? = getAppWidgetState(context, id)
        provideContent {
            val title = prefs?.get(SONG_TITLE) ?: ""
            val artist = prefs?.get(SONG_ARTIST) ?: ""
            val isPlaying = prefs?.get(IS_PLAYING) ?: false
            val hasSongs = prefs?.get(HAS_SONGS) ?: false
            val shuffleOn = prefs?.get(SHUFFLE_MODE) ?: false
            val repeatMode = prefs?.get(REPEAT_MODE) ?: 0L

            Column(
                modifier = GlanceModifier.fillMaxSize().background(0xFF000000.toInt()).padding(12),
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

                    Spacer(modifier = GlanceModifier.height(8))

                    Row(
                        modifier = GlanceModifier.fillMaxSize(),
                        horizontalAlignment = Alignment.Horizontal.CenterHorizontally,
                        verticalAlignment = Alignment.Vertical.CenterVertically
                    ) {
                        Text(
                            text = if (shuffleOn) "\uD83D\uDD00" else "\uD83D\uDD02",
                            style = TextStyle(color = if (shuffleOn) ColorProvider(0xFFFFFFFF.toInt()) else ColorProvider(0xFF666666.toInt())),
                            modifier = GlanceModifier.padding(8)
                        )
                        Text(
                            text = "\u23EE",
                            style = TextStyle(color = ColorProvider(0xFFFFFFFF.toInt())),
                            modifier = GlanceModifier.padding(8)
                        )
                        Text(
                            text = if (isPlaying) "\u23F8" else "\u25B6",
                            style = TextStyle(color = ColorProvider(0xFFFFFFFF.toInt())),
                            modifier = GlanceModifier.padding(8)
                        )
                        Text(
                            text = "\u23ED",
                            style = TextStyle(color = ColorProvider(0xFFFFFFFF.toInt())),
                            modifier = GlanceModifier.padding(8)
                        )
                        Text(
                            text = when (repeatMode.toInt()) { 1 -> "\uD83D\uDD01"; 2 -> "\uD83D\uDD02"; else -> "\uD83D\uDD04" },
                            style = TextStyle(color = if (repeatMode > 0) ColorProvider(0xFFFFFFFF.toInt()) else ColorProvider(0xFF666666.toInt())),
                            modifier = GlanceModifier.padding(8)
                        )
                    }
                }
            }
        }
    }

    companion object {
        val SONG_TITLE = stringPreferencesKey("med_song_title")
        val SONG_ARTIST = stringPreferencesKey("med_song_artist")
        val ALBUM_ART_PATH = stringPreferencesKey("med_album_art")
        val IS_PLAYING = booleanPreferencesKey("med_is_playing")
        val HAS_SONGS = booleanPreferencesKey("med_has_songs")
        val SHUFFLE_MODE = booleanPreferencesKey("med_shuffle")
        val REPEAT_MODE = longPreferencesKey("med_repeat")
    }
}

class MediumPlayerWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget = MediumPlayerWidget()
}
