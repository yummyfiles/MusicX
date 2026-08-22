package com.yummyfiles.musicx.data

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import com.yummyfiles.musicx.model.Playlist
import com.yummyfiles.musicx.model.Song
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import android.app.PendingIntent
import android.os.Build
import android.provider.MediaStore.createDeleteRequest
import android.app.RecoverableSecurityException
import androidx.core.net.toUri

@Suppress("UNUSED_PARAMETER", "RedundantSuspendModifier")
class MusicRepository(private val context: Context) {
    private val database = MusicDatabase.getDatabase(context)
    private val playlistDao = database.playlistDao()
    private val favoriteDao = database.favoriteDao()
    private val lyricDao = database.lyricDao()

    private val json = Json { ignoreUnknownKeys = true }

    @Serializable
    private data class LrcLibResponse(
        val plainLyrics: String? = null,
        val syncedLyrics: String? = null,
    )

    suspend fun fetchLocalSongs(): List<Song> = withContext(Dispatchers.IO) {
        val songs = mutableListOf<Song>()
        val collection = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI

        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.ALBUM_ID,
            MediaStore.Audio.Media.DATA
        )

        val selection = "(${MediaStore.Audio.Media.IS_MUSIC} != 0) OR " +
                "(${MediaStore.Audio.Media.DATA} LIKE '%.webm') OR " +
                "(${MediaStore.Audio.Media.DATA} LIKE '%.mkv')"
        val sortOrder = "${MediaStore.Audio.Media.TITLE} ASC"

        context.contentResolver.query(
            collection,
            projection,
            selection,
            null,
            sortOrder
        )?.use { cursor ->
            val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
            val titleColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
            val artistColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
            val durationColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
            val albumIdColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)
            val dataColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)

            while (cursor.moveToNext()) {
                val id = cursor.getLong(idColumn)
                val title = cursor.getString(titleColumn) ?: "Unknown"
                val artist = cursor.getString(artistColumn) ?: "Unknown"
                val duration = cursor.getLong(durationColumn)
                val albumId = cursor.getLong(albumIdColumn)
                val path = cursor.getString(dataColumn)

                // Try to load lyrics from database first
                var lyrics = lyricDao.getLyricsForSong(id)

                // If not in database, check for local .lrc file
                if (lyrics == null && (path != null)) {
                    try {
                        val lrcFile = java.io.File(path.substringBeforeLast(".") + ".lrc")
                        if (lrcFile.exists()) {
                            lyrics = lrcFile.readText()
                            // Cache it in database
                            lyricDao.insertLyrics(LyricEntity(id, lyrics))
                        }
                    } catch (e: Exception) {
                        android.util.Log.e("MusicRepository", "Failed to read local lrc for $path", e)
                    }
                }

                val contentUri: Uri = ContentUris.withAppendedId(
                    MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                    id
                )
                
                val albumArtUri = ContentUris.withAppendedId(
                    "content://media/external/audio/albumart".toUri(),
                    albumId
                )

                songs.add(
                    Song(
                        id = id,
                        title = title,
                        artist = artist,
                        duration = duration,
                        mediaUri = contentUri,
                        albumArtUri = albumArtUri,
                        lyrics = lyrics,
                        path = path
                    )
                )
            }
        }
        songs
    }
    suspend fun importSongs(uris: List<Uri>) {
        // MediaStore usually picks up new files automatically, 
        // but we can trigger a scan or handle specific imports here if needed.
        // For now, we rely on the system scanner and fetchLocalSongs()
    }

    fun getAllPlaylists(): Flow<List<Playlist>> {
        return playlistDao.getAllPlaylists().map { entities ->
            entities.map { entity ->
                Playlist(
                    id = entity.id,
                    name = entity.name,
                    songUris = try {
                        Json.decodeFromString(entity.songUrisJson)
                    } catch (e: Exception) {
                        emptyList()
                    }
                )
            }
        }
    }

    suspend fun createPlaylist(name: String) = withContext(Dispatchers.IO) {
        playlistDao.insertPlaylist(
            PlaylistEntity(
                name = name,
                songUrisJson = Json.encodeToString(emptyList<String>())
            )
        )
    }

    suspend fun deletePlaylist(playlist: Playlist) = withContext(Dispatchers.IO) {
        playlistDao.deletePlaylist(
            PlaylistEntity(
                id = playlist.id,
                name = playlist.name,
                songUrisJson = Json.encodeToString(playlist.songUris)
            )
        )
    }

    suspend fun updatePlaylist(playlist: Playlist) = withContext(Dispatchers.IO) {
        playlistDao.updatePlaylist(
            PlaylistEntity(
                id = playlist.id,
                name = playlist.name,
                songUrisJson = Json.encodeToString(playlist.songUris)
            )
        )
    }

    suspend fun updateMetadata(uri: String, title: String?, artist: String?, lyrics: String? = null) {
        // Metadata editing for local files often requires specific tag libraries (like JAudioTagger)
        // or updating MediaStore (limited support). For now, we'll log it.
        android.util.Log.d("MusicRepository", "Update metadata for $uri: $title, $artist")
        if (lyrics != null) {
            uri.substringAfterLast("/").toLongOrNull()?.let { songId ->
                lyricDao.insertLyrics(LyricEntity(songId, lyrics))
            }
        }
    }

    suspend fun deleteSongs(uris: List<String>): PendingIntent? = withContext(Dispatchers.IO) {
        val uriList = uris.map { it.toUri() }
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            return@withContext createDeleteRequest(context.contentResolver, uriList)
        } else {
            uriList.forEach { uri ->
                try {
                    context.contentResolver.delete(uri, null, null)
                } catch (e: Exception) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && e is RecoverableSecurityException) {
                        return@withContext e.userAction.actionIntent
                    }
                    android.util.Log.e("MusicRepository", "Failed to delete song: $uri", e)
                }
            }
        }
        null
    }

    fun getFavoriteIds(): Flow<List<Long>> = favoriteDao.getAllFavoriteIds()

    suspend fun toggleFavorite(songId: Long, isFavorite: Boolean) = withContext(Dispatchers.IO) {
        if (isFavorite) {
            favoriteDao.insertFavorite(FavoriteEntity(songId))
        } else {
            favoriteDao.deleteFavorite(FavoriteEntity(songId))
        }
    }

    suspend fun autoFetchLyrics(song: Song): String? = withContext(Dispatchers.IO) {
        try {
            val cleanArtist = cleanForQuery(song.artist)
            val cleanTitle = cleanForQuery(song.title)
            
            val query = "artist_name=${URLEncoder.encode(cleanArtist, "UTF-8")}" +
                    "&track_name=${URLEncoder.encode(cleanTitle, "UTF-8")}" +
                    "&duration=${song.duration / 1000}"
            val url = URL("https://lrclib.net/api/get?$query")
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.connectTimeout = 5000
            connection.readTimeout = 5000

            if (connection.responseCode == 200) {
                val responseText = connection.inputStream.bufferedReader().readText()
                val response = json.decodeFromString<LrcLibResponse>(responseText)
                response.syncedLyrics ?: response.plainLyrics
            } else {
                null
            }
        } catch (e: Exception) {
            android.util.Log.e("MusicRepository", "Error fetching lyrics from LRCLIB", e)
            null
        }
    }

    private fun cleanForQuery(text: String): String {
        return text.replace(Regex("(?i)\\(official[^)]*\\)|\\[official[^]]*]"), "")
            .replace(Regex("(?i)ft\\.|feat\\.|featuring.*"), "")
            .filter { it !in setOf('|', '\\', '(', ')', '"', '[', ']') }
            .trim()
    }
    suspend fun syncAllLyrics() { /* No-op */ }
}
