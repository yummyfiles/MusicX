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
import androidx.glance.layout.Box
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

class FavoritesWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            val prefs = currentState<Preferences>()
            val favoritesJson = prefs[FAVORITES_JSON] ?: "[]"
            val favorites: List<FavoriteEntry> = try {
                Json.decodeFromString(favoritesJson)
            } catch (_: Exception) { emptyList() }

            Column(
                modifier = GlanceModifier.fillMaxSize().background(Color.Black).padding(12.dp)
            ) {
                Text(
                    text = "Favorites",
                    style = TextStyle(color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold),
                    modifier = GlanceModifier.padding(bottom = 8.dp)
                )

                if (favorites.isEmpty()) {
                    Text(
                        text = "No favorites yet",
                        style = TextStyle(color = Color(0xFF888888), fontSize = 12.sp)
                    )
                } else {
                    favorites.take(8).forEach { song ->
                        Row(
                            modifier = GlanceModifier.fillMaxSize().padding(vertical = 4.dp)
                                .actionRunCallback<WidgetActionCallback>(
                                    actionParametersOf(
                                        WidgetActions.ACTION_KEY to WidgetActions.ACTION_PLAY_SONG,
                                        ActionParameters.Key<String>("song_uri") to song.uri
                                    )
                                ),
                            verticalAlignment = Alignment.Vertical.CenterVertically
                        ) {
                            Text(
                                text = "\u2665",
                                style = TextStyle(color = Color.White, fontSize = 10.sp),
                                modifier = GlanceModifier.padding(end = 4.dp)
                            )
                            Column(modifier = GlanceModifier.defaultWeight()) {
                                Text(
                                    text = song.title,
                                    style = TextStyle(color = Color.White, fontSize = 12.sp),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = song.artist,
                                    style = TextStyle(color = Color(0xFF888888), fontSize = 10.sp),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    companion object {
        val FAVORITES_JSON = stringPreferencesKey("favorites_json")
    }
}

class FavoritesWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget = FavoritesWidget()
}
