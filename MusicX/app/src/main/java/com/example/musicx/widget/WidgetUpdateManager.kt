package com.example.musicx.widget

import android.content.ComponentName
import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.datastore.preferences.core.preferencesOf
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.update
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.example.musicx.data.MusicRepository
import com.example.musicx.data.local.entity.Playlist
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
                        ComponentName(context, "com.example.musicx", ".playback.PlaybackService")
                    )
                    val controllerFuture = MediaController.Builder(context, token).buildAsync()
                    val controller = controllerFuture.get()
                    try {
                        val item = controller.currentMediaItem
                        val meta = item?.mediaMetadata
                        currentTitle = meta?.title?.toString() ?: ""
                        currentArtist = meta?.artist?.toString() ?: ""
                        currentArtUri = item?.localConfiguration?.artworkUri?.toString() ?: ""
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

                val miniPrefs = preferencesOf(
                    MiniPlayerWidget.SONG_TITLE to currentTitle,
                    MiniPlayerWidget.SONG_ARTIST to currentArtist,
                    MiniPlayerWidget.ALBUM_ART_PATH to artContentUri,
                    MiniPlayerWidget.IS_PLAYING to isPlaying,
                    MiniPlayerWidget.HAS_SONGS to hasSongs
                )
                manager.getGlanceIds(MiniPlayerWidget::class.java).forEach { id ->
                    MiniPlayerWidget().update(context, id, miniPrefs)
                }

                val medPrefs = preferencesOf(
                    MediumPlayerWidget.SONG_TITLE to currentTitle,
                    MediumPlayerWidget.SONG_ARTIST to currentArtist,
                    MediumPlayerWidget.ALBUM_ART_PATH to artContentUri,
                    MediumPlayerWidget.IS_PLAYING to isPlaying,
                    MediumPlayerWidget.HAS_SONGS to hasSongs,
                    MediumPlayerWidget.SHUFFLE_MODE to shuffleMode,
                    MediumPlayerWidget.REPEAT_MODE to repeatMode.toLong()
                )
                manager.getGlanceIds(MediumPlayerWidget::class.java).forEach { id ->
                    MediumPlayerWidget().update(context, id, medPrefs)
                }

                val largePrefs = preferencesOf(
                    LargeAlbumWidget.SONG_TITLE to currentTitle,
                    LargeAlbumWidget.SONG_ARTIST to currentArtist,
                    LargeAlbumWidget.ALBUM_ART_PATH to artContentUri,
                    LargeAlbumWidget.IS_PLAYING to isPlaying,
                    LargeAlbumWidget.HAS_SONGS to hasSongs
                )
                manager.getGlanceIds(LargeAlbumWidget::class.java).forEach { id ->
                    LargeAlbumWidget().update(context, id, largePrefs)
                }

                val plPrefs = preferencesOf(
                    PlaylistLauncherWidget.PLAYLISTS_JSON to playlistsJson
                )
                manager.getGlanceIds(PlaylistLauncherWidget::class.java).forEach { id ->
                    PlaylistLauncherWidget().update(context, id, plPrefs)
                }

                val favPrefs = preferencesOf(
                    FavoritesWidget.FAVORITES_JSON to favoritesJson
                )
                manager.getGlanceIds(FavoritesWidget::class.java).forEach { id ->
                    FavoritesWidget().update(context, id, favPrefs)
                }

                val shufflePrefs = preferencesOf(
                    ShuffleWidget.HAS_SONGS to hasSongs
                )
                manager.getGlanceIds(ShuffleWidget::class.java).forEach { id ->
                    ShuffleWidget().update(context, id, shufflePrefs)
                }

                val qaPrefs = preferencesOf(
                    QuickActionsWidget.HAS_SONGS to hasSongs
                )
                manager.getGlanceIds(QuickActionsWidget::class.java).forEach { id ->
                    QuickActionsWidget().update(context, id, qaPrefs)
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
