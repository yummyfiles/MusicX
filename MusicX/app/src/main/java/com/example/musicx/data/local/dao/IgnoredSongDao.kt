package com.example.musicx.data.local.dao

import androidx.room.*
import com.example.musicx.data.local.entity.IgnoredSong
import kotlinx.coroutines.flow.Flow

@Dao
interface IgnoredSongDao {
    @Query("SELECT uri FROM ignored_songs")
    fun getAllIgnoredUris(): Flow<List<String>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertIgnoredSongs(songs: List<IgnoredSong>)

    @Query("DELETE FROM ignored_songs WHERE uri IN (:uris)")
    suspend fun removeIgnoredSongs(uris: List<String>)
}
