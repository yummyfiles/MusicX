package com.yummyfiles.musicx.data

import kotlinx.serialization.Serializable

@Serializable
data class GeneralSettings(
    // playback settings
    val autoplayNext: Boolean = true,
    val jumpToNowPlaying: Boolean = false,
    val pauseOnDisconnect: Boolean = true,
    val gaplessPlayback: Boolean = true,
    val rememberPosition: Boolean = false,
    val fadeOnPlayPause: Boolean = true,

    // library settings
    val autoRefresh: Boolean = true,
    val excludeSmallFiles: Boolean = true,
    val preferEmbeddedArt: Boolean = true,
    val ignoreNoMedia: Boolean = false,

    // audio settings
    val eqEnabled: Boolean = false,
    val bassBoostEnabled: Boolean = false,
    val surroundSoundEnabled: Boolean = false,
    val normalizationEnabled: Boolean = true,
    val smartGainEnabled: Boolean = false,

    // lyrics settings
    val showLyricsInPlayer: Boolean = true,
    val syncLyrics: Boolean = true,
    val romanizedLyrics: Boolean = false,
    val biggerLyrics: Boolean = false,
    val centerLyrics: Boolean = true,

    // video settings
    val hardwareAcceleration: Boolean = true,
    val autoplayVideos: Boolean = false,
    val loopVideos: Boolean = true,
    val highQualityOnly: Boolean = false,
    val showSubtitles: Boolean = true,

    // UI vibes
    val terminalMode: Boolean = false,
)
