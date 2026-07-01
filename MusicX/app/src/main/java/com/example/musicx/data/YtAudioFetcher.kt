package com.example.musicx.data

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.net.ConnectException
import java.net.UnknownHostException
import java.util.concurrent.TimeUnit

class YtAudioFetcher {
    companion object {
        private val sharedClient = OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(120, TimeUnit.SECONDS)
            .writeTimeout(120, TimeUnit.SECONDS)
            .followRedirects(true)
            .build()

        private val pipedInstances = listOf(
            "https://pipedapi.kavin.rocks",
            "https://pipedapi.syncpundit.io",
            "https://pipedapi.pfcd.me"
        )
    }

    private val client get() = sharedClient

    suspend fun downloadAudio(youtubeUrl: String, destFile: File): Unit = withContext(Dispatchers.IO) {
        try {
            val videoId = extractVideoId(youtubeUrl)
                ?: throw Exception("Could not extract video ID from URL")

            var lastError: Exception? = null

            for (instance in pipedInstances) {
                try {
                    Log.d("YtAudioFetcher", "trying piped instance $instance for video $videoId")

                    val request = Request.Builder()
                        .url("$instance/streams/$videoId")
                        .header("Accept", "application/json")
                        .build()

                    val response = client.newCall(request).execute()
                    if (!response.isSuccessful) {
                        Log.w("YtAudioFetcher", "$instance returned ${response.code}")
                        continue
                    }

                    val body = response.body?.string() ?: continue
                    val json = JSONObject(body)

                    val audioStreams = json.optJSONArray("audioStreams")
                    if (audioStreams == null || audioStreams.length() == 0) {
                        throw Exception("No audio streams found for this video")
                    }

                    val bestStream = pickBestAudioStream(audioStreams)
                    val downloadUrl = bestStream.optString("url")
                        ?: throw Exception("No download URL in audio stream")

                    val title = json.optString("title", "Unknown Title")
                    Log.d("YtAudioFetcher", "downloading audio for: $title")

                    val dlRequest = Request.Builder()
                        .url(downloadUrl)
                        .header("Accept", "audio/*,*/*")
                        .build()

                    val dlResponse = client.newCall(dlRequest).execute()
                    if (!dlResponse.isSuccessful) {
                        throw Exception("Download failed with status ${dlResponse.code}")
                    }

                    dlResponse.body?.byteStream()?.use { input ->
                        FileOutputStream(destFile).use { output ->
                            input.copyTo(output)
                        }
                    } ?: throw Exception("Empty download response body")

                    return@withContext
                } catch (e: Exception) {
                    lastError = e
                    Log.w("YtAudioFetcher", "instance $instance failed: ${e.message}")
                }
            }

            throw lastError ?: Exception("All Piped instances failed")
        } catch (e: ConnectException) {
            throw Exception("Can't reach the download server — check your internet connection")
        } catch (e: UnknownHostException) {
            throw Exception("Can't resolve the download server address — check your internet connection")
        }
    }

    private fun extractVideoId(url: String): String? {
        val patterns = listOf(
            Regex("""(?:youtube\.com/watch\?.*v=|youtu\.be/|youtube\.com/embed/|youtube\.com/shorts/)([a-zA-Z0-9_-]{11})"""),
            Regex("""^([a-zA-Z0-9_-]{11})$""")
        )
        for (pattern in patterns) {
            pattern.find(url)?.groupValues?.get(1)?.let { return it }
        }
        return null
    }

    private fun pickBestAudioStream(streams: JSONArray): JSONObject {
        var best = streams.getJSONObject(0)
        var bestBitrate = best.optInt("bitrate", 0)

        for (i in 1 until streams.length()) {
            val s = streams.getJSONObject(i)
            val bitrate = s.optInt("bitrate", 0)
            if (bitrate > bestBitrate) {
                best = s
                bestBitrate = bitrate
            }
        }

        return best
    }
}
