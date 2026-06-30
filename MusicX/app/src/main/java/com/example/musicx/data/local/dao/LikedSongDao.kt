package com.example.musicx.data.local.dao

import androidx.room.*
import com.example.musicx.data.local.entity.LikedSong

@Dao
interface LikedSongDao {
    @Query("SELECT uri FROM liked_songs")
    suspend fun getAllLikedUris(): List<String>

    @Query("SELECT COUNT(*) FROM liked_songs WHERE uri = :uri")
    suspend fun isLiked(uri: String): Boolean

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(likedSong: LikedSong)

    @Query("DELETE FROM liked_songs WHERE uri = :uri")
    suspend fun delete(uri: String)
}
