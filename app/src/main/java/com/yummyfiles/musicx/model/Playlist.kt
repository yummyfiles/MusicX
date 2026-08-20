package com.yummyfiles.musicx.model

data class Playlist(
    val id: Long = 0,
    val name: String,
    val songUris: List<String> = emptyList(),
)
