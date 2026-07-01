package com.example.musicx.widget

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
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

class MediumPlayerWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            val prefs = currentState<Preferences>()
            val title = prefs[SONG_TITLE] ?: ""
            val artist = prefs[SONG_ARTIST] ?: ""
            val artPath = prefs[ALBUM_ART_PATH] ?: ""
            val isPlaying = prefs[IS_PLAYING] ?: false
            val hasSongs = prefs[HAS_SONGS] ?: false
            val shuffleOn = prefs[SHUFFLE_MODE] ?: false
            val repeatMode = prefs[REPEAT_MODE] ?: 0

            Column(
                modifier = GlanceModifier.fillMaxSize().background(Color.Black).padding(12.dp),
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
                                modifier = GlanceModifier.size(72.dp)
                            )
                            Spacer(modifier = GlanceModifier.width(12.dp))
                        }

                        Column(
                            modifier = GlanceModifier.defaultWeight(),
                            verticalAlignment = Alignment.Vertical.CenterVertically
                        ) {
                            Text(
                                text = title.ifBlank { "Nothing Playing" },
                                style = TextStyle(color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (artist.isNotBlank()) {
                                Text(
                                    text = artist,
                                    style = TextStyle(color = Color(0xFFAAAAAA), fontSize = 12.sp),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }

                    Spacer(modifier = GlanceModifier.height(8.dp))

                    Row(
                        modifier = GlanceModifier.fillMaxSize(),
                        horizontalAlignment = Alignment.Horizontal.CenterHorizontally,
                        verticalAlignment = Alignment.Vertical.CenterVertically
                    ) {
                        Text(
                            text = if (shuffleOn) "\uD83D\uDD00" else "\uD83D\uDD02",
                            style = TextStyle(color = if (shuffleOn) Color.White else Color(0xFF666666), fontSize = 16.sp),
                            modifier = GlanceModifier.padding(8.dp).actionRunCallback<WidgetActionCallback>(
                                actionParametersOf(WidgetActions.ACTION_KEY to WidgetActions.ACTION_SHUFFLE)
                            )
                        )
                        Text(
                            text = "\u23EE",
                            style = TextStyle(color = Color.White, fontSize = 22.sp),
                            modifier = GlanceModifier.padding(8.dp).actionRunCallback<WidgetActionCallback>(
                                actionParametersOf(WidgetActions.ACTION_KEY to WidgetActions.ACTION_PREVIOUS)
                            )
                        )
                        Text(
                            text = if (isPlaying) "\u23F8" else "\u25B6",
                            style = TextStyle(color = Color.White, fontSize = 26.sp),
                            modifier = GlanceModifier.padding(8.dp).actionRunCallback<WidgetActionCallback>(
                                actionParametersOf(WidgetActions.ACTION_KEY to WidgetActions.ACTION_PLAY_PAUSE)
                            )
                        )
                        Text(
                            text = "\u23ED",
                            style = TextStyle(color = Color.White, fontSize = 22.sp),
                            modifier = GlanceModifier.padding(8.dp).actionRunCallback<WidgetActionCallback>(
                                actionParametersOf(WidgetActions.ACTION_KEY to WidgetActions.ACTION_NEXT)
                            )
                        )
                        Text(
                            text = when (repeatMode) { 1 -> "\uD83D\uDD01"; 2 -> "\uD83D\uDD02"; else -> "\uD83D\uDD04" },
                            style = TextStyle(color = if (repeatMode > 0) Color.White else Color(0xFF666666), fontSize = 16.sp),
                            modifier = GlanceModifier.padding(8.dp).actionRunCallback<WidgetActionCallback>(
                                actionParametersOf(WidgetActions.ACTION_KEY to WidgetActions.ACTION_REPEAT)
                            )
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
