package com.example.musicx.widget

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.action.actionParametersOf
import androidx.glance.action.actionRunCallback
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.state.currentState
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextOverflow
import androidx.glance.text.TextStyle
import androidx.glance.unit.Color
import androidx.glance.unit.dp
import androidx.glance.unit.sp

class MiniPlayerWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            val prefs = currentState<Preferences>()
            val title = prefs[SONG_TITLE] ?: ""
            val artist = prefs[SONG_ARTIST] ?: ""
            val artPath = prefs[ALBUM_ART_PATH] ?: ""
            val isPlaying = prefs[IS_PLAYING] ?: false
            val hasSongs = prefs[HAS_SONGS] ?: false

            Column(
                modifier = GlanceModifier.fillMaxSize().background(Color.Black).padding(8.dp),
                verticalAlignment = Alignment.Vertical.CenterVertically
            ) {
                if (!hasSongs) {
                    Text(
                        text = "No Music Found",
                        style = TextStyle(color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    )
                } else {
                    Row(
                        modifier = GlanceModifier.defaultWeight(),
                        verticalAlignment = Alignment.Vertical.CenterVertically
                    ) {
                        if (artPath.isNotBlank()) {
                            Image(
                                provider = ImageProvider(android.net.Uri.parse(artPath)),
                                contentDescription = "Album art",
                                modifier = GlanceModifier.size(48.dp)
                            )
                            Spacer(modifier = GlanceModifier.width(8.dp))
                        }

                        Column(
                            modifier = GlanceModifier.defaultWeight(),
                            verticalAlignment = Alignment.Vertical.CenterVertically
                        ) {
                            Text(
                                text = title.ifBlank { "Nothing Playing" },
                                style = TextStyle(color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (artist.isNotBlank()) {
                                Text(
                                    text = artist,
                                    style = TextStyle(color = Color(0xFFAAAAAA), fontSize = 10.sp),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        Spacer(modifier = GlanceModifier.width(4.dp))

                        Row(verticalAlignment = Alignment.Vertical.CenterVertically) {
                            Text(
                                text = "\u23EE",
                                style = TextStyle(color = Color.White, fontSize = 18.sp),
                                modifier = GlanceModifier.padding(4.dp).actionRunCallback<WidgetActionCallback>(
                                    actionParametersOf(WidgetActions.ACTION_KEY to WidgetActions.ACTION_PREVIOUS)
                                )
                            )
                            Text(
                                text = if (isPlaying) "\u23F8" else "\u25B6",
                                style = TextStyle(color = Color.White, fontSize = 20.sp),
                                modifier = GlanceModifier.padding(4.dp).actionRunCallback<WidgetActionCallback>(
                                    actionParametersOf(WidgetActions.ACTION_KEY to WidgetActions.ACTION_PLAY_PAUSE)
                                )
                            )
                            Text(
                                text = "\u23ED",
                                style = TextStyle(color = Color.White, fontSize = 18.sp),
                                modifier = GlanceModifier.padding(4.dp).actionRunCallback<WidgetActionCallback>(
                                    actionParametersOf(WidgetActions.ACTION_KEY to WidgetActions.ACTION_NEXT)
                                )
                            )
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
