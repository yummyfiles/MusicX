package com.example.musicx

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import android.util.Log
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.disk.DiskCache
import coil.memory.MemoryCache
import kotlin.system.exitProcess

// main app class - this is where it all starts fr
class MusicXApplication : Application(), ImageLoaderFactory {

    override fun onCreate() {
        super.onCreate()

        // ok so this basically catches crashes so the app doesnt just explode
        // learned this the hard way lol
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            Log.e("MusicX_Crash", "CRITICAL CRASH in thread ${thread.name}", throwable)
            exitProcess(1)
        }

        createNotificationChannel()

        Log.d("MusicX", "Application created") // finally it works omg
    }

    // need this for the notification thingy to work on newer android versions
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "musicx_playback",
                "Music Playback",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Controls music playback"
                setShowBadge(false)
                enableLights(true)
                lightColor = 0xFFFFFFFF.toInt()
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    // coil is the image loading thing - this sets up caching so images load faster
    // without this its gonna be slow af loading album art
    override fun newImageLoader(): ImageLoader {
        return ImageLoader.Builder(this)
            .memoryCache {
                MemoryCache.Builder(this)
                    .maxSizePercent(0.25) // 25% of memory for images, should be good enough
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(cacheDir.resolve("coil_cache"))
                    .maxSizeBytes(50 * 1024 * 1024) // 50mb cache, more than enough
                    .build()
            }
            .crossfade(true) // makes image transitions smooth instead of jarring
            .build()
    }
}