package com.example.musicx.playback

import android.app.Notification
import android.content.Context
import android.os.Build
import android.os.Bundle
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.CommandButton
import androidx.media3.session.DefaultMediaNotificationProvider
import androidx.media3.session.MediaNotification
import androidx.media3.session.MediaSession
import com.google.common.collect.ImmutableList

// wraps DefaultMediaNotificationProvider to apply our theme colors to the media notification
// we cant subclass it directly because createNotification is final, so we implement the interface
@OptIn(UnstableApi::class)
class MusicXNotificationProvider(private val context: Context) : MediaNotification.Provider {

    private val delegate = DefaultMediaNotificationProvider.Builder(context).build()
    private var accentColor = 0xFF000000.toInt()

    fun updateColors(accent: Long, text: Long) {
        accentColor = accent.toInt()
    }

    override fun createNotification(
        mediaSession: MediaSession,
        mediaButtons: ImmutableList<CommandButton>,
        actionFactory: MediaNotification.ActionFactory,
        onNotificationChangedCallback: MediaNotification.Provider.Callback
    ): MediaNotification {
        val result = delegate.createNotification(
            mediaSession, mediaButtons, actionFactory, onNotificationChangedCallback
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try {
                val builder = Notification.Builder.recoverBuilder(context, result.notification)
                builder.setColor(accentColor)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    builder.setColorized(true)
                }
                return MediaNotification(result.notificationId, builder.build())
            } catch (e: Exception) {
                // if recovery fails just return the default notification
            }
        }

        return result
    }

    override fun handleCustomCommand(
        session: MediaSession,
        command: String,
        extras: Bundle
    ): Boolean {
        return delegate.handleCustomCommand(session, command, extras)
    }

    override fun getNotificationChannelInfo(): MediaNotification.Provider.NotificationChannelInfo {
        return delegate.notificationChannelInfo
    }
}
