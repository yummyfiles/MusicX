package com.yummyfiles.musicx.data

import kotlinx.serialization.Serializable

@Serializable
data class GeneralSettings(
    // Stuff related to playing music.
    val autoplayNext: Boolean = true,
    val jumpToNowPlaying: Boolean = false,
    val pauseOnDisconnect: Boolean = true,
    val gaplessPlayback: Boolean = true,
    val rememberPosition: Boolean = false,
    val fadeOnPlayPause: Boolean = true,

    // Settings for how the library works.
    val autoRefresh: Boolean = true,
    val excludeSmallFiles: Boolean = true,
    val preferEmbeddedArt: Boolean = true,
    val ignoreNoMedia: Boolean = false,

    // Tweak how the sound comes out.
    val eqEnabled: Boolean = false,
    val bassBoostEnabled: Boolean = false,
    val surroundSoundEnabled: Boolean = false,
    val normalizationEnabled: Boolean = true,
    val smartGainEnabled: Boolean = false,

    // How you want your lyrics to look.
    val showLyricsInPlayer: Boolean = true,
    val syncLyrics: Boolean = true,
    val romanizedLyrics: Boolean = false,
    val biggerLyrics: Boolean = false,
    val centerLyrics: Boolean = true,

    // For when you're watching videos.
    val hardwareAcceleration: Boolean = true,
    val autoplayVideos: Boolean = false,
    val loopVideos: Boolean = true,
    val highQualityOnly: Boolean = false,
    val showSubtitles: Boolean = true,

    // Changing up the look and feel.
    val terminalMode: Boolean = false,
)
