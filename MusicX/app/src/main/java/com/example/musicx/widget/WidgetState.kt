package com.example.musicx.widget

import kotlinx.serialization.Serializable

@Serializable
data class WidgetState(
    val songTitle: String = "",
    val songArtist: String = "",
    val albumArtPath: String = "",
    val isPlaying: Boolean = false,
    val hasSongs: Boolean = false,
    val shuffleMode: Boolean = false,
    val repeatMode: Int = 0,
    val playlists: List<PlaylistEntry> = emptyList(),
    val favorites: List<FavoriteEntry> = emptyList()
)

@Serializable
data class PlaylistEntry(
    val id: Long,
    val name: String,
    val songCount: Int
)

@Serializable
data class FavoriteEntry(
    val title: String,
    val artist: String,
    val uri: String
)
