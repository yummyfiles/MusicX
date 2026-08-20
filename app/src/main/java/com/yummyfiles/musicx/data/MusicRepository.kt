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
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Suppress("UNUSED_PARAMETER", "RedundantSuspendModifier")
class MusicRepository(private val context: Context) {
    private val database = MusicDatabase.getDatabase(context)
    private val playlistDao = database.playlistDao()

    suspend fun fetchLocalSongs(): List<Song> = withContext(Dispatchers.IO) {
        val songs = mutableListOf<Song>()
        val collection = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI

        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.ALBUM_ID
        )

        val selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0"
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

            while (cursor.moveToNext()) {
                val id = cursor.getLong(idColumn)
                val title = cursor.getString(titleColumn) ?: "Unknown"
                val artist = cursor.getString(artistColumn) ?: "Unknown"
                val duration = cursor.getLong(durationColumn)
                val albumId = cursor.getLong(albumIdColumn)

                val contentUri: Uri = ContentUris.withAppendedId(
                    MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                    id
                )
                
                val albumArtUri = ContentUris.withAppendedId(
                    Uri.parse("content://media/external/audio/albumart"),
                    albumId
                )

                songs.add(
                    Song(
                        id = id,
                        title = title,
                        artist = artist,
                        duration = duration,
                        mediaUri = contentUri,
                        albumArtUri = albumArtUri
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
    }

    suspend fun deleteSongs(uris: List<String>) = withContext(Dispatchers.IO) {
        uris.forEach { uriString ->
            try {
                val uri = Uri.parse(uriString)
                context.contentResolver.delete(uri, null, null)
            } catch (e: Exception) {
                android.util.Log.e("MusicRepository", "Failed to delete song: $uriString", e)
            }
        }
    }
    suspend fun autoFetchLyrics(song: Song): String? = null
    suspend fun syncAllLyrics() { /* No-op */ }
}
