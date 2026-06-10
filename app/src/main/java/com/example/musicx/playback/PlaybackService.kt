package com.example.musicx.playback

import android.app.PendingIntent
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
import kotlinx.coroutines.launch

class PlaybackService : MediaSessionService() {

    private var mediaSession: MediaSession? = null
    private var bassBoost: BassBoost? = null
    private var virtualizer: Virtualizer? = null
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private lateinit var settingsRepository: SettingsRepository

    @OptIn(UnstableApi::class)
    override fun onCreate() {
        super.onCreate()
        settingsRepository = SettingsRepository(applicationContext)
        
        val player = ExoPlayer.Builder(this)
            .setAudioAttributes(AudioAttributes.DEFAULT, true)
            .setHandleAudioBecomingNoisy(true)
            .build()
            
        player.volume = 1.0f
        
        setupAudioEffects(player.audioSessionId)

        // Sync settings
        serviceScope.launch {
            settingsRepository.generalSettings.collect { settings ->
                applySettings(settings)
            }
        }

        val sessionActivityPendingIntent = packageManager
            ?.getLaunchIntentForPackage(packageName)
            ?.let { sessionIntent ->
                PendingIntent.getActivity(
                    this,
                    0,
                    sessionIntent,
                    PendingIntent.FLAG_IMMUTABLE
                )
            }

        val builder = MediaSession.Builder(this, player)
            .setId("musicx")
        
        if (sessionActivityPendingIntent != null) {
            builder.setSessionActivity(sessionActivityPendingIntent)
        }
        
        mediaSession = builder.build()
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
        } catch (e: Exception) {
            android.util.Log.e("PlaybackService", "Error applying audio settings", e)
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
