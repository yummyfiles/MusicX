package com.example.musicx.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "liked_songs")
data class LikedSong(
    @PrimaryKey val uri: String
)
