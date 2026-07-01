package com.example.musicx.widget

import android.content.ComponentName
import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.state.PreferencesGlanceStateDefinition
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.example.musicx.data.MusicRepository
import com.example.musicx.playback.PlaybackService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

object WidgetUpdateManager {

    private val json = Json { ignoreUnknownKeys = true }

    suspend fun updateAllWidgets(context: Context) {
        withContext(Dispatchers.IO) {
            try {
                val repo = MusicRepository(context)
                val songs = repo.fetchLocalSongs()
                val hasSongs = songs.isNotEmpty()

                val likedSongs = songs.filter { it.isLiked }

                val playlistFlow = repo.getAllPlaylists()
                val playlists = playlistFlow.firstOrNull() ?: emptyList()

                var currentTitle = ""
                var currentArtist = ""
                var currentArtUri = ""
                var isPlaying = false
                var shuffleMode = false
                var repeatMode = 0

                try {
                    val token = SessionToken(
                        context,
                        ComponentName(context, PlaybackService::class.java)
                    )
                    val controllerFuture = MediaController.Builder(context, token).buildAsync()
                    val controller = controllerFuture.get()
                    try {
                        val item = controller.currentMediaItem
                        val meta = item?.mediaMetadata
                        currentTitle = meta?.title?.toString() ?: ""
                        currentArtist = meta?.artist?.toString() ?: ""
                        currentArtUri = item?.mediaMetadata?.artworkUri?.toString() ?: ""
                        isPlaying = controller.isPlaying
                        shuffleMode = controller.shuffleModeEnabled
                        repeatMode = controller.repeatMode
                    } finally {
                        MediaController.releaseFuture(controllerFuture)
                    }
                } catch (_: Exception) {}

                val artContentUri = toWidgetAccessibleUri(context, currentArtUri)

                val playlistEntries = playlists.map { pl ->
                    PlaylistEntry(id = pl.id, name = pl.name, songCount = pl.songUris.size)
                }
                val favoriteEntries = likedSongs.map { song ->
                    FavoriteEntry(title = song.title, artist = song.artist, uri = song.mediaUri.toString())
                }

                val playlistsJson = json.encodeToString(playlistEntries)
                val favoritesJson = json.encodeToString(favoriteEntries)

                val manager = GlanceAppWidgetManager(context)

                manager.getGlanceIds(MiniPlayerWidget::class.java).forEach { id ->
                    updateAppWidgetState(context, id) { prefs ->
                        prefs[MiniPlayerWidget.SONG_TITLE] = currentTitle
                        prefs[MiniPlayerWidget.SONG_ARTIST] = currentArtist
                        prefs[MiniPlayerWidget.ALBUM_ART_PATH] = artContentUri
                        prefs[MiniPlayerWidget.IS_PLAYING] = isPlaying
                        prefs[MiniPlayerWidget.HAS_SONGS] = hasSongs
                    }
                }

                manager.getGlanceIds(MediumPlayerWidget::class.java).forEach { id ->
                    updateAppWidgetState(context, id) { prefs ->
                        prefs[MediumPlayerWidget.SONG_TITLE] = currentTitle
                        prefs[MediumPlayerWidget.SONG_ARTIST] = currentArtist
                        prefs[MediumPlayerWidget.ALBUM_ART_PATH] = artContentUri
                        prefs[MediumPlayerWidget.IS_PLAYING] = isPlaying
                        prefs[MediumPlayerWidget.HAS_SONGS] = hasSongs
                        prefs[MediumPlayerWidget.SHUFFLE_MODE] = shuffleMode
                        prefs[MediumPlayerWidget.REPEAT_MODE] = repeatMode.toLong()
                    }
                }

                manager.getGlanceIds(LargeAlbumWidget::class.java).forEach { id ->
                    updateAppWidgetState(context, id) { prefs ->
                        prefs[LargeAlbumWidget.SONG_TITLE] = currentTitle
                        prefs[LargeAlbumWidget.SONG_ARTIST] = currentArtist
                        prefs[LargeAlbumWidget.ALBUM_ART_PATH] = artContentUri
                        prefs[LargeAlbumWidget.IS_PLAYING] = isPlaying
                        prefs[LargeAlbumWidget.HAS_SONGS] = hasSongs
                    }
                }

                manager.getGlanceIds(FavoritesWidget::class.java).forEach { id ->
                    updateAppWidgetState(context, id) { prefs ->
                        prefs[FavoritesWidget.FAVORITES_JSON] = favoritesJson
                    }
                }

                manager.getGlanceIds(ShuffleWidget::class.java).forEach { id ->
                    updateAppWidgetState(context, id) { prefs ->
                        prefs[ShuffleWidget.HAS_SONGS] = hasSongs
                    }
                }

                manager.getGlanceIds(QuickActionsWidget::class.java).forEach { id ->
                    updateAppWidgetState(context, id) { prefs ->
                        prefs[QuickActionsWidget.HAS_SONGS] = hasSongs
                    }
                }

            } catch (e: Exception) {
                android.util.Log.e("WidgetUpdateManager", "Failed to update widgets", e)
            }
        }
    }

    private fun toWidgetAccessibleUri(context: Context, uriStr: String): String {
        if (uriStr.isBlank()) return ""
        return try {
            val uri = Uri.parse(uriStr)
            if (uri.scheme == "file") {
                val file = File(uri.path ?: return uriStr)
                if (file.exists()) {
                    FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
                        .toString()
                } else uriStr
            } else uriStr
        } catch (_: Exception) { uriStr }
    }
}
