package com.example.musicx.playback

import android.app.PendingIntent
import android.content.SharedPreferences
import android.media.audiofx.BassBoost
import android.media.audiofx.Virtualizer
import androidx.annotation.OptIn
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.example.musicx.data.GeneralSettings
import com.example.musicx.data.SettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class PlaybackService : MediaSessionService() {

    private var mediaSession: MediaSession? = null
    private var bassBoost: BassBoost? = null
    private var virtualizer: Virtualizer? = null
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private lateinit var settingsRepository: SettingsRepository
    private lateinit var prefs: SharedPreferences
    private var currentSettings: GeneralSettings = GeneralSettings()

    @OptIn(UnstableApi::class)
    override fun onCreate() {
        super.onCreate()
        settingsRepository = SettingsRepository(applicationContext)
        prefs = getSharedPreferences("playback_prefs", MODE_PRIVATE)

        val provider = MusicXNotificationProvider(applicationContext)
        setMediaNotificationProvider(provider)

        serviceScope.launch(Dispatchers.Main) {
            settingsRepository.themeState
                .flowOn(Dispatchers.IO)
                .collect { theme ->
                    provider.updateColors(theme.notificationBackground, theme.notificationText)
                }
        }

        val player = ExoPlayer.Builder(this)
            .setAudioAttributes(AudioAttributes.DEFAULT, true)
            .setHandleAudioBecomingNoisy(true)
            .build()

        player.volume = 1.0f
        player.addListener(playerListener)

        setupAudioEffects(player.audioSessionId)

        serviceScope.launch(Dispatchers.IO) {
            settingsRepository.generalSettings.collect { settings ->
                currentSettings = settings
                withContext(Dispatchers.Main) { applySettings(settings) }
            }
        }

        val sessionActivityPendingIntent = packageManager
            ?.getLaunchIntentForPackage(packageName)
            ?.let { sessionIntent ->
                PendingIntent.getActivity(
                    this, 0, sessionIntent, PendingIntent.FLAG_IMMUTABLE
                )
            }

        val builder = MediaSession.Builder(this, player)
            .setId("musicx")

        if (sessionActivityPendingIntent != null) {
            builder.setSessionActivity(sessionActivityPendingIntent)
        }

        mediaSession = builder.build()
    }

    private val playerListener = object : Player.Listener {
        override fun onPlaybackStateChanged(playbackState: Int) {
            if (playbackState == Player.STATE_ENDED) {
                val player = mediaSession?.player ?: return
                val settings = currentSettings

                if (!settings.autoplayNext) {
                    player.pause()
                    player.seekTo(0)
                } else if (!settings.loopVideos && player.repeatMode == Player.REPEAT_MODE_ALL) {
                    player.repeatMode = Player.REPEAT_MODE_OFF
                }
            }
        }

        override fun onIsPlayingChanged(isPlaying: Boolean) {
            val player = mediaSession?.player ?: return
            val settings = currentSettings

            if (settings.rememberPosition && !isPlaying) {
                val item = player.currentMediaItem ?: return
                prefs.edit()
                    .putLong("pos_${item.mediaId}", player.currentPosition)
                    .apply()
            }

            if (settings.fadeOnPlayPause) {
                if (isPlaying) {
                    rampVolume(if (settings.normalizationEnabled) 0.85f else 1.0f, 150)
                }
            }
        }

        override fun onPositionDiscontinuity(
            oldPosition: Player.PositionInfo,
            newPosition: Player.PositionInfo,
            reason: Int
        ) {
            if (reason == Player.DISCONTINUITY_REASON_AUTO_TRANSITION) {
                val settings = currentSettings
                if (settings.rememberPosition) {
                    val savedPos = prefs.getLong("pos_${newPosition.mediaItem?.mediaId ?: ""}", 0L)
                    if (savedPos > 0) {
                        mediaSession?.player?.seekTo(savedPos)
                    }
                }
            }
        }
    }

    override fun onTaskRemoved(rootIntent: android.content.Intent?) {
        val player = mediaSession?.player ?: return
        if (!player.playWhenReady) {
            player.pause()
            stopSelf()
        }
        super.onTaskRemoved(rootIntent)
    }

    private fun setupAudioEffects(sessionId: Int) {
        try {
            bassBoost = BassBoost(0, sessionId)
            @Suppress("DEPRECATION")
            virtualizer = Virtualizer(0, sessionId)
        } catch (e: Exception) {
            android.util.Log.e("PlaybackService", "Failed to setup effects", e)
        }
    }

    private fun applySettings(settings: GeneralSettings) {
        try {
            bassBoost?.let {
                it.enabled = settings.eqEnabled && settings.bassBoostEnabled
                @Suppress("DEPRECATION")
                it.setStrength(if (it.enabled) 1000.toShort() else 0.toShort())
            }
            virtualizer?.let {
                it.enabled = settings.eqEnabled && settings.surroundSoundEnabled
                @Suppress("DEPRECATION")
                it.setStrength(if (it.enabled) 1000.toShort() else 0.toShort())
            }

            val player = mediaSession?.player ?: return

            if (settings.autoplayNext) {
                player.repeatMode = Player.REPEAT_MODE_ALL
            }

            player.volume = if (settings.normalizationEnabled) 0.85f else 1.0f
        } catch (e: Exception) {
            android.util.Log.e("PlaybackService", "Error applying settings", e)
        }
    }

    private fun rampVolume(target: Float, durationMs: Long = 150) {
        serviceScope.launch {
            val player = mediaSession?.player ?: return@launch
            val startVolume = player.volume
            val steps = (durationMs / 20).toInt().coerceAtLeast(1)
            for (i in 1..steps) {
                player.volume = startVolume + (target - startVolume) * (i.toFloat() / steps)
                kotlinx.coroutines.delay(20)
            }
            player.volume = target
        }
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? = mediaSession

    override fun onDestroy() {
        mediaSession?.run {
            player.release()
            release()
        }
        bassBoost?.release()
        virtualizer?.release()
        serviceScope.cancel()
        mediaSession = null
        super.onDestroy()
    }
}
