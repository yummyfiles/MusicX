package com.example.musicx.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "metadata_overrides")
data class MetadataOverride(
    @PrimaryKey val songUri: String,
    val customTitle: String?,
    val customArtist: String?,
    val customLyrics: String? = null
)
