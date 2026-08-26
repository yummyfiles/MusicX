package com.yummyfiles.musicx.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "lyrics")
data class LyricEntity(
    @PrimaryKey val songId: Long,
    val lyrics: String
)
