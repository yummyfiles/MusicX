package com.example.musicx.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "library_songs")
data class LibrarySong(
    @PrimaryKey val uri: String,
    val title: String,
    val artist: String,
    val duration: Long,
    val albumArtUri: String? = null,
    val lyrics: String? = null
)
