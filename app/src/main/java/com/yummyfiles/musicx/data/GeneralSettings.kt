package com.yummyfiles.musicx.data

import kotlinx.serialization.Serializable

@Serializable
data class GeneralSettings(
    // bruh playback stuff lol
    val autoplayNext: Boolean = true,
    val jumpToNowPlaying: Boolean = false,
    val pauseOnDisconnect: Boolean = true,
    val gaplessPlayback: Boolean = true,
    val rememberPosition: Boolean = false,
    val fadeOnPlayPause: Boolean = true,

    // library vibes fr
    val autoRefresh: Boolean = true,
    val excludeSmallFiles: Boolean = true,
    val preferEmbeddedArt: Boolean = true,
    val ignoreNoMedia: Boolean = false,

    // audio lowkey important
    val eqEnabled: Boolean = false,
    val bassBoostEnabled: Boolean = false,
    val surroundSoundEnabled: Boolean = false,
    val normalizationEnabled: Boolean = true,
    val smartGainEnabled: Boolean = false,

    // lyrics for the soul tbh
    val showLyricsInPlayer: Boolean = true,
    val syncLyrics: Boolean = true,
    val romanizedLyrics: Boolean = false,
    val biggerLyrics: Boolean = false,
    val centerLyrics: Boolean = true,

    // video vibes main character energy
    val hardwareAcceleration: Boolean = true,
    val autoplayVideos: Boolean = false,
    val loopVideos: Boolean = true,
    val highQualityOnly: Boolean = false,
    val showSubtitles: Boolean = true,
)
