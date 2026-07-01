package com.example.musicx.widget

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.glance.action.ActionParameters
import androidx.media3.common.C
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.example.musicx.data.MusicRepository
import com.example.musicx.playback.PlaybackService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withContext

object WidgetActions {
    const val ACTION_PLAY_PAUSE = "play_pause"
    const val ACTION_NEXT = "next"
    const val ACTION_PREVIOUS = "previous"
    const val ACTION_SHUFFLE = "shuffle"
    const val ACTION_REPEAT = "repeat"
    const val ACTION_SHUFFLE_ALL = "shuffle_all"
    const val ACTION_PLAY_PLAYLIST = "play_playlist"
    const val ACTION_PLAY_SONG = "play_song"
    const val ACTION_RESUME = "resume"
    const val ACTION_OPEN_SEARCH = "open_search"
    const val ACTION_OPEN_IMPORT = "open_import"
    const val ACTION_OPEN_APP = "open_app"

    val ACTION_KEY = ActionParameters.Key<String>("command")
    val PLAYLIST_NAME_KEY = ActionParameters.Key<String>("playlist_name")
    val SONG_URI_KEY = ActionParameters.Key<String>("song_uri")

    private suspend fun connectAndRun(context: Context, block: (MediaController) -> Unit) {
        withContext(Dispatchers.IO) {
            try {
                val token = SessionToken(context, ComponentName(context, PlaybackService::class.java))
                val controllerFuture = MediaController.Builder(context, token).buildAsync()
                val controller = controllerFuture.get()
                try {
                    block(controller)
                } finally {
                    MediaController.releaseFuture(controllerFuture)
                }
            } catch (e: Exception) {
                Log.e("WidgetActions", "MediaSession command failed", e)
            }
        }
    }

    suspend fun handleAction(context: Context, actionParameters: ActionParameters) {
        when (val action = actionParameters[ACTION_KEY]) {
            ACTION_PLAY_PAUSE -> connectAndRun(context) { c ->
                if (c.isPlaying) c.pause() else c.play()
            }
            ACTION_NEXT -> connectAndRun(context) { it.seekToNext() }
            ACTION_PREVIOUS -> connectAndRun(context) { it.seekToPrevious() }
            ACTION_SHUFFLE -> connectAndRun(context) {
                it.shuffleModeEnabled = !it.shuffleModeEnabled
            }
            ACTION_REPEAT -> connectAndRun(context) {
                it.repeatMode = when (it.repeatMode) {
                    Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ALL
                    Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_ONE
                    else -> Player.REPEAT_MODE_OFF
                }
            }
            ACTION_SHUFFLE_ALL -> shuffleAll(context)
            ACTION_PLAY_PLAYLIST -> {
                val name = actionParameters[PLAYLIST_NAME_KEY] ?: ""
                playPlaylist(context, name)
            }
            ACTION_PLAY_SONG -> {
                val uri = actionParameters[SONG_URI_KEY] ?: ""
                playSongByUri(context, uri)
            }
            ACTION_RESUME -> connectAndRun(context) { c ->
                if (c.playbackState == Player.STATE_IDLE && c.mediaItemCount > 0) c.prepare()
                c.play()
            }
            ACTION_OPEN_SEARCH -> launchActivity(context, "Search")
            ACTION_OPEN_IMPORT -> launchActivity(context, "Import")
            ACTION_OPEN_APP -> launchActivity(context, null)
        }
    }

    private fun launchActivity(context: Context, tab: String?) {
        try {
            val intent = context.packageManager.getLaunchIntentForPackage("com.example.musicx")
                ?: return
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            if (tab != null) intent.putExtra("navigate_to", tab)
            context.startActivity(intent)
        } catch (e: Exception) {
            Log.e("WidgetActions", "Failed to launch activity", e)
        }
    }

    private suspend fun shuffleAll(context: Context) {
        withContext(Dispatchers.IO) {
            try {
                val repo = MusicRepository(context)
                val songs = repo.fetchLocalSongs()
                if (songs.isEmpty()) return@withContext
                val shuffled = songs.shuffled()
                connectAndRun(context) { controller ->
                    val items = shuffled.map { song ->
                        androidx.media3.common.MediaItem.Builder()
                            .setMediaId(song.id.toString())
                            .setUri(song.mediaUri)
                            .setMediaMetadata(
                                androidx.media3.common.MediaMetadata.Builder()
                                    .setTitle(song.title)
                                    .setArtist(song.artist)
                                    .build()
                            )
                            .build()
                    }
                    controller.setMediaItems(items, 0, C.TIME_UNSET)
                    controller.prepare()
                    controller.play()
                    controller.shuffleModeEnabled = true
                }
            } catch (e: Exception) {
                Log.e("WidgetActions", "Shuffle all failed", e)
            }
        }
    }

    private suspend fun playPlaylist(context: Context, nameOrId: String) {
        withContext(Dispatchers.IO) {
            try {
                val repo = MusicRepository(context)
                val playlists = repo.getAllPlaylists().firstOrNull() ?: return@withContext
                val playlist = playlists.find { it.name == nameOrId } ?: return@withContext
                val songs = repo.fetchLocalSongs()
                val filtered = songs.filter { it.mediaUri.toString() in playlist.songUris }
                if (filtered.isEmpty()) return@withContext
                connectAndRun(context) { controller ->
                    val items = filtered.map { song ->
                        androidx.media3.common.MediaItem.Builder()
                            .setMediaId(song.id.toString())
                            .setUri(song.mediaUri)
                            .setMediaMetadata(
                                androidx.media3.common.MediaMetadata.Builder()
                                    .setTitle(song.title)
                                    .setArtist(song.artist)
                                    .build()
                            )
                            .build()
                    }
                    controller.setMediaItems(items, 0, C.TIME_UNSET)
                    controller.prepare()
                    controller.play()
                }
            } catch (e: Exception) {
                Log.e("WidgetActions", "Play playlist failed", e)
            }
        }
    }

    private suspend fun playSongByUri(context: Context, songUri: String) {
        withContext(Dispatchers.IO) {
            try {
                val repo = MusicRepository(context)
                val songs = repo.fetchLocalSongs()
                val song = songs.find { it.mediaUri.toString() == songUri } ?: return@withContext
                connectAndRun(context) { controller ->
                    val item = androidx.media3.common.MediaItem.Builder()
                        .setMediaId(song.id.toString())
                        .setUri(song.mediaUri)
                        .setMediaMetadata(
                            androidx.media3.common.MediaMetadata.Builder()
                                .setTitle(song.title)
                                .setArtist(song.artist)
                                .build()
                        )
                        .build()
                    controller.setMediaItem(item)
                    controller.prepare()
                    controller.play()
                }
            } catch (e: Exception) {
                Log.e("WidgetActions", "Play song failed", e)
            }
        }
    }
}
