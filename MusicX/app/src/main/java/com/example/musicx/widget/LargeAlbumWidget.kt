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
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextOverflow
import androidx.glance.text.TextStyle
import androidx.glance.unit.Color
import androidx.glance.unit.dp
import androidx.glance.unit.sp

class LargeAlbumWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            val prefs = currentState<Preferences>()
            val title = prefs[SONG_TITLE] ?: ""
            val artist = prefs[SONG_ARTIST] ?: ""
            val artPath = prefs[ALBUM_ART_PATH] ?: ""
            val isPlaying = prefs[IS_PLAYING] ?: false
            val hasSongs = prefs[HAS_SONGS] ?: false

            Column(
                modifier = GlanceModifier.fillMaxSize().background(Color.Black).padding(16.dp),
                horizontalAlignment = Alignment.Horizontal.CenterHorizontally,
                verticalAlignment = Alignment.Vertical.CenterVertically
            ) {
                if (!hasSongs) {
                    Text(
                        text = "No Music Found",
                        style = TextStyle(color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    )
                } else {
                    Image(
                        provider = ImageProvider(android.net.Uri.parse(artPath.ifBlank { "" })),
                        contentDescription = "Album art",
                        modifier = GlanceModifier.size(160.dp).defaultWeight()
                    )

                    Spacer(modifier = GlanceModifier.height(12.dp))

                    Text(
                        text = title.ifBlank { "Nothing Playing" },
                        style = TextStyle(color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = GlanceModifier.fillMaxSize()
                    )

                    if (artist.isNotBlank()) {
                        Text(
                            text = artist,
                            style = TextStyle(color = Color(0xFFAAAAAA), fontSize = 14.sp),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = GlanceModifier.fillMaxSize()
                        )
                    }

                    Spacer(modifier = GlanceModifier.height(12.dp))

                    Row(
                        horizontalAlignment = Alignment.Horizontal.CenterHorizontally,
                        verticalAlignment = Alignment.Vertical.CenterVertically
                    ) {
                        Text(
                            text = "\u23EE",
                            style = TextStyle(color = Color.White, fontSize = 24.sp),
                            modifier = GlanceModifier.padding(12.dp).actionRunCallback<WidgetActionCallback>(
                                actionParametersOf(WidgetActions.ACTION_KEY to WidgetActions.ACTION_PREVIOUS)
                            )
                        )
                        Text(
                            text = if (isPlaying) "\u23F8" else "\u25B6",
                            style = TextStyle(color = Color.White, fontSize = 32.sp),
                            modifier = GlanceModifier.padding(12.dp).actionRunCallback<WidgetActionCallback>(
                                actionParametersOf(WidgetActions.ACTION_KEY to WidgetActions.ACTION_PLAY_PAUSE)
                            )
                        )
                        Text(
                            text = "\u23ED",
                            style = TextStyle(color = Color.White, fontSize = 24.sp),
                            modifier = GlanceModifier.padding(12.dp).actionRunCallback<WidgetActionCallback>(
                                actionParametersOf(WidgetActions.ACTION_KEY to WidgetActions.ACTION_NEXT)
                            )
                        )
                    }
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
