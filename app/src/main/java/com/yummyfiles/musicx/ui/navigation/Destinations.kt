package com.yummyfiles.musicx.ui.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

sealed interface Destination : NavKey {
    @Serializable
    data object Songs : Destination
    @Serializable
    data object Playlists : Destination
    @Serializable
    data object Import : Destination
    @Serializable
    data object Search : Destination
    @Serializable
    data object Settings : Destination
    @Serializable
    data object NowPlaying : Destination
    @Serializable
    data class EditMetadata(val songId: Long) : Destination
    @Serializable
    data class PlaylistDetail(val playlistId: Long) : Destination
    
    @Serializable
    data object About : Destination
}
