package com.example.musicx.data

import androidx.compose.runtime.Immutable
import kotlinx.serialization.Serializable

@Immutable
@Serializable
data class GeneralSettings(
    // Playback
    val autoplayNext: Boolean = true,
    val jumpToNowPlaying: Boolean = false,
    val pauseOnDisconnect: Boolean = true,
    val gaplessPlayback: Boolean = true,
    val rememberPosition: Boolean = false,
    val fadeOnPlayPause: Boolean = true,

    // Library
    val autoRefresh: Boolean = true,
    val excludeSmallFiles: Boolean = true,
    val preferEmbeddedArt: Boolean = true,
    val ignoreNoMedia: Boolean = false,

    // Audio
    val eqEnabled: Boolean = false,
    val bassBoostEnabled: Boolean = false,
    val surroundSoundEnabled: Boolean = false,
    val normalizationEnabled: Boolean = true,
    val smartGainEnabled: Boolean = false,

    // Lyrics
    val showLyricsInPlayer: Boolean = true,
    val syncLyrics: Boolean = true,
    val romanizedLyrics: Boolean = false,
    val biggerLyrics: Boolean = false,
    val centerLyrics: Boolean = true,

    // Video
    val hardwareAcceleration: Boolean = true,
    val autoplayVideos: Boolean = false,
    val loopVideos: Boolean = true,
    val highQualityOnly: Boolean = false,
    val showSubtitles: Boolean = true
)
