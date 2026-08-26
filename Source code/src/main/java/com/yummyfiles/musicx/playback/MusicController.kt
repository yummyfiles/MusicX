package com.yummyfiles.musicx.playback

import android.content.ComponentName
import android.content.Context
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors

class MusicController(context: Context) {
    val mediaController: MutableState<MediaController?> = mutableStateOf(null)
    private var controllerFuture: ListenableFuture<MediaController>? = null

    init {
        val sessionToken = SessionToken(context, ComponentName(context, PlaybackService::class.java))
        controllerFuture = MediaController.Builder(context, sessionToken).buildAsync()
        controllerFuture?.addListener(
            {
                mediaController.value = controllerFuture?.get()
            },
            MoreExecutors.directExecutor(),
        )
    }

    fun release() {
        controllerFuture?.let {
            MediaController.releaseFuture(it)
        }
    }
}
