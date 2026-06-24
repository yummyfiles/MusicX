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
        val songs = mutableListOf<Song>()
        
        // get all the metadata overrides and ignored songs at once
        // way faster than querying them one by one
        val overrides = try {
            metadataOverrideDao.getAllOverrides().firstOrNull()?.associateBy { it.songUri } ?: emptyMap()
        } catch (e: Exception) {
            android.util.Log.e("MusicRepository", "Failed to fetch overrides", e)
            emptyMap()
        }

        val ignoredUris = try {
            ignoredSongDao.getAllIgnoredUris().firstOrNull()?.toSet() ?: emptySet()
        } catch (e: Exception) {
            android.util.Log.e("MusicRepository", "Failed to fetch ignored URIs", e)
            emptySet()
        }

        // first get songs from our internal library (Room database)
        val librarySongs = try {
            librarySongDao.getAllSongs().firstOrNull() ?: emptyList()
        } catch (e: Exception) {
            android.util.Log.e("MusicRepository", "Failed to fetch library songs", e)
            emptyList()
        }
        
        val validLibrarySongs = mutableListOf<LibrarySong>()
        val brokenUris = mutableListOf<String>() // track broken files so we can clean them up

        librarySongs.forEach { libSong ->
            // skip google docs files, they dont work for some reason
            if (libSong.uri.contains("com.google.android.apps.docs")) {
                brokenUris.add(libSong.uri)
                return@forEach
            }

            if (ignoredUris.contains(libSong.uri)) return@forEach

            val uri = libSong.uri.toUri()
            
            // check if the file actually exists
            if (libSong.uri.startsWith("file://")) {
                val path = uri.path
                if (path == null || !File(path).exists()) {
                    brokenUris.add(libSong.uri)
                    return@forEach
                }
            }

            validLibrarySongs.add(libSong)
            val override = overrides[libSong.uri]
            val cleanTitle: String
            val cleanArtist: String
            if (override != null) {
                cleanTitle = override.customTitle ?: libSong.title
                cleanArtist = override.customArtist ?: libSong.artist
            } else {
                val parsed = MetadataCleaner.clean(
                    rawTitle = libSong.title,
                    rawArtist = libSong.artist
                )
                cleanTitle = parsed.title
                cleanArtist = parsed.artist
            }
            songs.add(
                Song(
                    id = libSong.uri.hashCode().toLong(),
                    title = cleanTitle,
                    artist = cleanArtist,
                    duration = libSong.duration,
                    mediaUri = uri,
                    albumArtUri = libSong.albumArtUri?.toUri(),
                    lyrics = override?.customLyrics ?: libSong.lyrics
                )
            )
        }

        if (brokenUris.isNotEmpty()) {
            librarySongDao.deleteSongsByUri(brokenUris)
        }

        val internalMusicUris = validLibrarySongs.asSequence().map { it.uri }.toSet()

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
                val displayNameColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DISPLAY_NAME)

                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idColumn)
                    val originalTitle = cursor.getString(titleColumn) ?: "Unknown"
                    val originalArtist = cursor.getString(artistColumn) ?: "Unknown"
                    val duration = cursor.getLong(durationColumn)
                    val albumId = cursor.getLong(albumIdColumn)

                    val contentUri = ContentUris.withAppendedId(
                        MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                        id
                    )
                    
                    val uriStr = contentUri.toString()
                    if (ignoredUris.contains(uriStr) || internalMusicUris.contains(uriStr)) continue

                    val override = overrides[uriStr]
                    val cleanTitle: String
                    val cleanArtist: String
                    if (override != null) {
                        cleanTitle = override.customTitle ?: originalTitle
                        cleanArtist = override.customArtist ?: originalArtist
                    } else {
                        val displayName = cursor.getString(displayNameColumn)
                        val parsed = MetadataCleaner.clean(
                            rawTitle = originalTitle,
                            rawArtist = originalArtist,
                            fileNameWithoutExtension = displayName?.substringBeforeLast(".")
                        )
                        cleanTitle = parsed.title
                        cleanArtist = parsed.artist
                    }

                    val albumArtUri = ContentUris.withAppendedId(
                        "content://media/external/audio/albumart".toUri(),
                        albumId
                    )

                    songs.add(Song(id, cleanTitle, cleanArtist, duration, contentUri, albumArtUri, override?.customLyrics))
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("MusicRepository", "Error querying MediaStore", e)
        }

        // Efficient de-duplication using HashSet (avoids creating intermediate lists)
        val seen = HashSet<String>(songs.size)
        val distinctSongs = songs.filter { seen.add("${it.title.lowercase().trim()}|${it.artist.lowercase().trim()}") }

        distinctSongs.sortedBy { it.title }
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
                
                    val cleanName = realName.substringBeforeLast(".")
                    val parsed = MetadataCleaner.clean(
                        rawTitle = title?.takeIf { !it.contains("encoded=") && !it.contains("acc=") },
                        rawArtist = artist,
                        fileNameWithoutExtension = cleanName
                    )
                    title = parsed.title
                    artist = parsed.artist

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
        val lyrics = lyricsFetcher.fetchLyrics(song.artist, song.title)
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
                    val lyrics = lyricsFetcher.fetchLyrics(song.artist, song.title)
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
