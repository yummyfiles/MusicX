package com.example.musicx.ui.navigation

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
    data object AppearanceSettings : Destination
    @Serializable
    data object AudioSettings : Destination
    @Serializable
    data object PlaybackSettings : Destination
    @Serializable
    data object LibrarySettings : Destination
    @Serializable
    data object LyricsSettings : Destination
    @Serializable
    data object VideoSettings : Destination
}
