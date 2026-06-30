package com.example.musicx.model

import android.net.Uri
import androidx.compose.runtime.Immutable

@Immutable
data class Song(
    val id: Long,
    val title: String,
    val artist: String,
    val duration: Long,
    val mediaUri: Uri,
    val albumArtUri: Uri? = null,
    val lyrics: String? = null,
    val isLiked: Boolean = false
)
