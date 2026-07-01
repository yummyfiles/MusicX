package com.example.musicx.widget

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.action.ActionParameters
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
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextOverflow
import androidx.glance.text.TextStyle
import androidx.glance.unit.Color
import androidx.glance.unit.dp
import androidx.glance.unit.sp
import kotlinx.serialization.json.Json
import kotlinx.serialization.decodeFromString

class PlaylistLauncherWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            val prefs = currentState<Preferences>()
            val playlistsJson = prefs[PLAYLISTS_JSON] ?: "[]"
            val playlists: List<PlaylistEntry> = try {
                Json.decodeFromString(playlistsJson)
            } catch (_: Exception) { emptyList() }

            Column(
                modifier = GlanceModifier.fillMaxSize().background(Color.Black).padding(12.dp)
            ) {
                Text(
                    text = "Playlists",
                    style = TextStyle(color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold),
                    modifier = GlanceModifier.padding(bottom = 8.dp)
                )

                if (playlists.isEmpty()) {
                    Text(
                        text = "No playlists yet",
                        style = TextStyle(color = Color(0xFF888888), fontSize = 12.sp)
                    )
                } else {
                    playlists.take(6).forEach { playlist ->
                        Row(
                            modifier = GlanceModifier.fillMaxSize().padding(vertical = 4.dp)
                                .actionRunCallback<WidgetActionCallback>(
                                    actionParametersOf(
                                        WidgetActions.ACTION_KEY to WidgetActions.ACTION_PLAY_PLAYLIST,
                                        ActionParameters.Key<String>("playlist_name") to playlist.name
                                    )
                                ),
                            verticalAlignment = Alignment.Vertical.CenterVertically
                        ) {
                            Text(
                                text = playlist.name,
                                style = TextStyle(color = Color.White, fontSize = 12.sp),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = GlanceModifier.defaultWeight()
                            )
                            Spacer(modifier = GlanceModifier.width(4.dp))
                            Text(
                                text = "${playlist.songCount} songs",
                                style = TextStyle(color = Color(0xFF888888), fontSize = 10.sp)
                            )
                            Spacer(modifier = GlanceModifier.width(4.dp))
                            Text(
                                text = "\u25B6",
                                style = TextStyle(color = Color.White, fontSize = 12.sp)
                            )
                        }
                    }
                }
            }
        }
    }

    companion object {
        val PLAYLISTS_JSON = stringPreferencesKey("playlists_json")
    }
}

class PlaylistLauncherWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget = PlaylistLauncherWidget()
}
