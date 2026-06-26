package com.example.musicx.data

import android.content.ContentUris
import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.MediaStore
import android.provider.OpenableColumns
import androidx.core.net.toUri
import androidx.room.Room
import com.example.musicx.data.local.MusicDatabase
import com.example.musicx.data.local.entity.IgnoredSong
import com.example.musicx.data.local.entity.LibrarySong
import com.example.musicx.data.local.entity.MetadataOverride
import com.example.musicx.data.local.entity.Playlist
import com.example.musicx.model.Song
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.security.MessageDigest

// this is the main data class that handles all the music loading and saving
// its basically the brain of the app when it comes to data stuff
class MusicRepository(private val context: Context) {

    companion object {
        @Volatile
        private var instance: MusicDatabase? = null

        // singleton pattern - only want one database instance or things get messy
        private fun getDatabase(context: Context): MusicDatabase {
            return instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    MusicDatabase::class.java,
                    "music_db"
                ).fallbackToDestructiveMigration(dropAllTables = true).build().also { instance = it }
            }
        }
    }

    private val db = getDatabase(context)
    private val playlistDao = db.playlistDao()
    private val metadataOverrideDao = db.metadataOverrideDao()
    private val librarySongDao = db.librarySongDao()
    private val ignoredSongDao = db.ignoredSongDao()
    private val lyricsFetcher = LyricsFetcher() // this guy fetches lyrics from the internet

    // update the Genius API key used by the lyrics fetcher
    // called when user changes their API key in settings
    fun updateGeniusApiKey(key: String) {
        lyricsFetcher.geniusApiKey = key
    }

    // loads all the songs from the library and mediastore
    // this was painful to write ngl, so many edge cases
    suspend fun fetchLocalSongs(): List<Song> = withContext(Dispatchers.IO) {
        // parallelize Room DB queries for speed
        val (overrides, ignoredUris, librarySongs) = try {
            val overridesDef = async { metadataOverrideDao.getAllOverridesList() }
            val ignoredDef = async { ignoredSongDao.getAllIgnoredUrisList() }
            val libraryDef = async { librarySongDao.getAllSongsList() }
            Triple(
                overridesDef.await().associateBy { it.songUri },
                ignoredDef.await().toSet(),
                libraryDef.await()
            )
        } catch (e: Exception) {
            android.util.Log.e("MusicRepository", "Failed to fetch library data", e)
            Triple(emptyMap(), emptySet(), emptyList())
        }

        val songs = ArrayList<Song>(librarySongs.size + 64)
        val validLibraryUris = HashSet<String>(librarySongs.size)
        val brokenUris = ArrayList<String>()

        librarySongs.forEach { libSong ->
            if (libSong.uri.contains("com.google.android.apps.docs")) {
                brokenUris.add(libSong.uri)
                return@forEach
            }
            if (ignoredUris.contains(libSong.uri)) return@forEach

            val uri = libSong.uri.toUri()
            if (libSong.uri.startsWith("file://")) {
                val path = uri.path
                if (path == null || !File(path).exists()) {
                    brokenUris.add(libSong.uri)
                    return@forEach
                }
            }

            validLibraryUris.add(libSong.uri)
            val override = overrides[libSong.uri]
            if (override != null) {
                songs.add(Song(
                    id = libSong.uri.hashCode().toLong(),
                    title = override.customTitle ?: libSong.title,
                    artist = override.customArtist ?: libSong.artist,
                    duration = libSong.duration,
                    mediaUri = uri,
                    albumArtUri = libSong.albumArtUri?.toUri(),
                    lyrics = override.customLyrics ?: libSong.lyrics
                ))
            } else {
                // keep the raw title because youtube metadata is cursed
                // restore old titles if the last build cooked them
                val recovered = recoverDisplayTitleFromPath(libSong.uri)
                songs.add(Song(
                    id = libSong.uri.hashCode().toLong(),
                    title = recovered ?: libSong.title,
                    artist = libSong.artist,
                    duration = libSong.duration,
                    mediaUri = uri,
                    albumArtUri = libSong.albumArtUri?.toUri(),
                    lyrics = libSong.lyrics
                ))
            }
        }

        if (brokenUris.isNotEmpty()) {
            librarySongDao.deleteSongsByUri(brokenUris)
        }

        // 2. Fetch from MediaStore (skip tracks shorter than 5 seconds)
        val collection = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.ALBUM_ID,
            MediaStore.Audio.Media.DISPLAY_NAME
        )
        val selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0 AND ${MediaStore.Audio.Media.DURATION} > 5000"
        val sortOrder = "${MediaStore.Audio.Media.TITLE} ASC"

        try {
            context.contentResolver.query(
                collection, projection, selection, null, sortOrder
            )?.use { cursor ->
                val idCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
                val titleCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
                val artistCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
                val durCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
                val albumCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)
                val dispCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DISPLAY_NAME)

                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idCol)
                    val uriStr = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id).toString()
                    if (ignoredUris.contains(uriStr) || validLibraryUris.contains(uriStr)) continue

                    val originalTitle = cursor.getString(titleCol) ?: "Unknown"
                    val originalArtist = cursor.getString(artistCol) ?: "Unknown"
                    val duration = cursor.getLong(durCol)
                    val albumId = cursor.getLong(albumCol)

                    val override = overrides[uriStr]
                    if (override != null) {
                        songs.add(Song(id, override.customTitle ?: originalTitle, override.customArtist ?: originalArtist, duration,
                            ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id),
                            ContentUris.withAppendedId("content://media/external/audio/albumart".toUri(), albumId),
                            override.customLyrics))
                    } else {
                        // keep the raw title, no spacer crimes this time
                        songs.add(Song(id, originalTitle, originalArtist, duration,
                            ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id),
                            ContentUris.withAppendedId("content://media/external/audio/albumart".toUri(), albumId),
                            null))
                    }
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("MusicRepository", "Error querying MediaStore", e)
        }

        // dedup and sort
        val seen = HashSet<String>(songs.size)
        songs.filter { seen.add("${it.title.lowercase()}|${it.artist.lowercase()}") }
            .sortedBy { it.title }
    }

    suspend fun importSongs(uris: List<Uri>) = withContext(Dispatchers.IO) {
        val storageDir = File(context.filesDir, "music")
        if (!storageDir.exists()) storageDir.mkdirs()
        val artDir = File(context.filesDir, "art")
        if (!artDir.exists()) artDir.mkdirs()

        // Pre-compute existing hashes once (avoids re-reading files for each import)
        val existingHashes = try {
            librarySongDao.getAllSongs().firstOrNull()?.mapNotNull { libSong ->
                try {
                    calculateHash(libSong.uri.toUri())
                } catch (e: Exception) {
                    null
                }
            }?.toSet() ?: emptySet()
        } catch (e: Exception) {
            emptySet()
        }
        val existingUris = try {
            librarySongDao.getAllUris().toSet()
        } catch (e: Exception) {
            emptySet()
        }

        val librarySongs = uris.mapNotNull { uri ->
            try {
                val uriStr = uri.toString()
                if (existingUris.contains(uriStr)) return@mapNotNull null

                // Get real filename using ContentResolver
                var realName = "Unknown Song"
                context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                    val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (nameIndex != -1 && cursor.moveToFirst()) {
                        realName = cursor.getString(nameIndex)
                    }
                }

                // Quick file size check before full hash
                var fileSize = 0L
                context.contentResolver.openInputStream(uri)?.use { input ->
                    val buffer = ByteArray(8192)
                    var bytesRead: Int
                    val digest = MessageDigest.getInstance("MD5")
                    while (input.read(buffer).also { bytesRead = it } != -1) {
                        digest.update(buffer, 0, bytesRead)
                        fileSize += bytesRead
                    }
                    val hash = digest.digest().joinToString("") { "%02x".format(it) }
                    if (existingHashes.contains(hash)) {
                        android.util.Log.d("MusicRepository", "Skipping duplicate import for $realName")
                        return@mapNotNull null
                    }
                }

                val fileName = "music_${System.currentTimeMillis()}_${realName.replace(" ", "_")}"
                val destFile = File(storageDir, fileName)
                
                context.contentResolver.openInputStream(uri)?.use { input ->
                    FileOutputStream(destFile).use { output ->
                        input.copyTo(output)
                    }
                }

                if (!destFile.exists() || destFile.length() == 0L) return@mapNotNull null

                val internalUri = Uri.fromFile(destFile)
                val retriever = MediaMetadataRetriever()
                try {
                    retriever.setDataSource(context, internalUri)
                
                    var title = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE)
                    var artist = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST)
                        ?: retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUMARTIST)
                    val duration = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLong() ?: 0L
                
                    // keep the raw title, dont clean it for display
                    // only fall back to filename if metadata is completely empty
                    if (title?.contains("encoded=") == true || title?.contains("acc=") == true) {
                        title = null
                    }
                    if (title.isNullOrBlank()) title = realName.substringBeforeLast(".")
                    if (artist.isNullOrBlank()) artist = "Unknown Artist"

                    val artworkBytes = retriever.embeddedPicture
                    var internalArtUri: String? = null
                    if (artworkBytes != null) {
                        val artFile = File(artDir, "art_${destFile.nameWithoutExtension}.jpg")
                        FileOutputStream(artFile).use { it.write(artworkBytes) }
                        internalArtUri = Uri.fromFile(artFile).toString()
                    }

                    LibrarySong(
                        uri = internalUri.toString(),
                        title = title!!,
                        artist = artist!!,
                        duration = duration,
                        albumArtUri = internalArtUri,
                        lyrics = null
                    )
                } finally {
                    try { retriever.release() } catch (_: Exception) {}
                }
            } catch (e: Exception) {
                android.util.Log.e("MusicRepository", "Failed to import: $uri", e)
                null
            }
        }
        
        if (librarySongs.isNotEmpty()) {
            librarySongDao.insertSongs(librarySongs)
            ignoredSongDao.removeIgnoredSongs(librarySongs.map { it.uri })
        }
    }

    // restore old titles if the last build cooked them
    // internal files are named music_TIMESTAMP_original_name
    // we strip the prefix and recover the real display title
    private fun recoverDisplayTitleFromPath(uriStr: String): String? {
        if (!uriStr.startsWith("file://")) return null
        val path = Uri.parse(uriStr).path ?: return null
        val fileName = java.io.File(path).name
        val internalPrefix = Regex("""^music_\d+_""")
        val match = internalPrefix.find(fileName) ?: return null
        val restored = fileName.substring(match.value.length)
            .substringBeforeLast(".")  // remove extension
            .replace("_", " ")         // underscores to spaces
            .trim()
        // only use if it has special characters that suggest a real formatted title
        // special characters are part of the title, not permission to start surgery
        if (restored.any { it in "\"\"“”|[]():\\-" }) {
            return restored
        }
        // if no special chars but different from stored title, still use it
        return restored
    }

    private fun calculateHash(uri: Uri): String {
        return try {
            val digest = MessageDigest.getInstance("MD5")
            context.contentResolver.openInputStream(uri)?.use { input ->
                val buffer = ByteArray(8192)
                var bytesRead: Int
                while (input.read(buffer).also { bytesRead = it } != -1) {
                    digest.update(buffer, 0, bytesRead)
                }
            }
            digest.digest().joinToString("") { "%02x".format(it) }
        } catch (e: Exception) {
            uri.toString()
        }
    }

    // Playlist management
    fun getAllPlaylists(): Flow<List<Playlist>> = playlistDao.getAllPlaylists()
        .catch { e ->
            android.util.Log.e("MusicRepository", "Failed to load playlists", e)
            emit(emptyList())
        }
    
    suspend fun createPlaylist(name: String) = withContext(Dispatchers.IO) {
        playlistDao.insertPlaylist(Playlist(name = name))
    }
    
    suspend fun deletePlaylist(playlist: Playlist) = withContext(Dispatchers.IO) {
        playlistDao.deletePlaylist(playlist)
    }

    suspend fun updatePlaylist(playlist: Playlist) = withContext(Dispatchers.IO) {
        playlistDao.updatePlaylist(playlist)
    }

    // Metadata management
    suspend fun updateMetadata(uri: String, title: String?, artist: String?, lyrics: String? = null) = withContext(Dispatchers.IO) {
        metadataOverrideDao.insertOverride(MetadataOverride(uri, title, artist, lyrics))
    }

    suspend fun deleteSongs(uris: List<String>) = withContext(Dispatchers.IO) {
        if (uris.isEmpty()) return@withContext
        try {
            uris.forEach { uriStr ->
                if (uriStr.startsWith("file://")) {
                    try {
                        val file = File(Uri.parse(uriStr).path ?: "")
                        if (file.exists()) file.delete()
                    } catch (e: Exception) {
                        android.util.Log.w("MusicRepository", "Failed to delete file for $uriStr", e)
                    }
                }
            }
            librarySongDao.deleteSongsByUri(uris)
            metadataOverrideDao.deleteOverridesByUri(uris)
            ignoredSongDao.insertIgnoredSongs(uris.map { IgnoredSong(it) })
        } catch (e: Exception) {
            android.util.Log.e("MusicRepository", "Failed to delete songs", e)
            throw e
        }
    }
    
    // automatically fetch lyrics for a song - tries multiple sources
    suspend fun autoFetchLyrics(song: Song): String? = withContext(Dispatchers.IO) {
        // clean the title for lookup only, display stays raw
        val lookup = MetadataCleaner.cleanForLyricsLookup(song.title, song.artist)
        val lyrics = lyricsFetcher.fetchLyrics(lookup.artist, lookup.title)
        if (lyrics != null) {
            updateMetadata(song.mediaUri.toString(), song.title, song.artist, lyrics)
            librarySongDao.updateLyrics(song.mediaUri.toString(), lyrics)
        }
        lyrics
    }

    // syncs lyrics for songs that dont have them yet
    // runs in background with up to 3 concurrent requests for speed
    // originally did them one by one but it was SO slow
    suspend fun syncAllLyrics() = withContext(Dispatchers.IO) {
        val missing = librarySongDao.getSongsMissingLyrics().take(20) // max 20 at a time
        if (missing.isEmpty()) return@withContext
        
        android.util.Log.d("MusicRepository", "Starting background lyrics sync for ${missing.size} songs")
        
        // semaphore limits to 3 concurrent requests so we dont get rate limited
        val semaphore = kotlinx.coroutines.sync.Semaphore(3)
        missing.map { song ->
            async {
                semaphore.acquire()
                try {
                    // clean for lookup, keep the raw title visible
                    val lookup = MetadataCleaner.cleanForLyricsLookup(song.title, song.artist)
                    val lyrics = lyricsFetcher.fetchLyrics(lookup.artist, lookup.title)
                    if (lyrics != null) {
                        librarySongDao.updateLyrics(song.uri, lyrics)
                        android.util.Log.d("MusicRepository", "Synced lyrics for: ${song.title}")
                    }
                } catch (e: Exception) {
                    android.util.Log.w("MusicRepository", "Failed to sync lyrics for ${song.title}: ${e.message}")
                } finally {
                    semaphore.release()
                }
            }
        }.awaitAll()
        android.util.Log.d("MusicRepository", "Finished background lyrics sync")
    }
}
