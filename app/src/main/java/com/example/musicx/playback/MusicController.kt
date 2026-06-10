package com.example.musicx.playback

import android.content.ComponentName
import android.content.Context
import android.util.Log
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class MusicController(context: Context) {

    private var controllerFuture: ListenableFuture<MediaController>? = null
    val mediaController: MutableState<MediaController?> = mutableStateOf(null)

    private val mainScope = MainScope()

    init {
        try {
            val sessionToken = SessionToken(context, ComponentName(context, PlaybackService::class.java))
            controllerFuture = MediaController.Builder(context, sessionToken).buildAsync()
            controllerFuture?.addListener({
                mainScope.launch {
                    try {
                        val controller = controllerFuture?.get()
                        if (controller != null) {
                            mediaController.value = controller
                            Log.d("MusicController", "MediaController connected successfully")
                        } else {
                            Log.e("MusicController", "Connected but controller is null")
                        }
                    } catch (e: Exception) {
                        Log.e("MusicController", "Failed to connect to MediaController", e)
                    }
                }
            }, MoreExecutors.directExecutor())
        } catch (e: Exception) {
            Log.e("MusicController", "Failed to initialize SessionToken or Builder", e)
        }
    }

    fun release() {
        controllerFuture?.let {
            MediaController.releaseFuture(it)
        }
        controllerFuture = null
        mediaController.value = null
        mainScope.cancel()
        Log.d("MusicController", "Released")
    }
}
