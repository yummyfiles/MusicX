package com.example.musicx.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "ignored_songs")
data class IgnoredSong(
    @PrimaryKey val uri: String
)
