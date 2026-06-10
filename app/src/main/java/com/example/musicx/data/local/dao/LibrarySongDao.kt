package com.example.musicx.data.local.dao

import androidx.room.*
import com.example.musicx.data.local.entity.LibrarySong
import kotlinx.coroutines.flow.Flow

@Dao
interface LibrarySongDao {
    @Query("SELECT * FROM library_songs")
    fun getAllSongs(): Flow<List<LibrarySong>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSongs(songs: List<LibrarySong>)

    @Query("DELETE FROM library_songs")
    suspend fun clearAll()

    @Query("SELECT uri FROM library_songs")
    suspend fun getAllUris(): List<String>

    @Query("SELECT * FROM library_songs WHERE lyrics IS NULL OR lyrics = ''")
    suspend fun getSongsMissingLyrics(): List<LibrarySong>

    @Query("UPDATE library_songs SET lyrics = :lyrics WHERE uri = :uri")
    suspend fun updateLyrics(uri: String, lyrics: String)

    @Query("DELETE FROM library_songs WHERE uri IN (:uris)")
    suspend fun deleteSongsByUri(uris: List<String>)
}
