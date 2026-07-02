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
            "https://pipedapi.pfcd.me",
            "https://pipedapi.r4fo.com",
            "https://pipedapi.sneaky.email",
            "https://pipedapi.adminforge.de",
            "https://pipedapi.lunar.icu"
        )

        private val invidiousInstances = listOf(
            "https://invidious.snopyta.org",
            "https://yewtu.be",
            "https://inv.bp.projectsegfau.lt",
            "https://invidious.private.coffee"
        )

        private fun mimeTypeToExtension(mimeType: String): String {
            return when {
                mimeType.contains("mp4") || mimeType.contains("m4a") -> "m4a"
                mimeType.contains("webm") -> "webm"
                mimeType.contains("opus") -> "opus"
                mimeType.contains("ogg") -> "ogg"
                mimeType.contains("mp3") -> "mp3"
                mimeType.contains("aac") -> "aac"
                mimeType.contains("wav") -> "wav"
                mimeType.contains("flac") -> "flac"
                else -> "m4a"
            }
        }
    }

    private val client get() = sharedClient

    suspend fun downloadAudio(youtubeUrl: String, destDir: File): File = withContext(Dispatchers.IO) {
        try {
            val videoId = extractVideoId(youtubeUrl)
                ?: throw Exception("Could not extract video ID from URL")

            var lastError: Exception? = null

            // Tier 1: Piped API
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
                    val mimeType = bestStream.optString("mimeType", "audio/mp4")
                    val ext = mimeTypeToExtension(mimeType)

                    val title = json.optString("title", "Unknown Title")
                    Log.d("YtAudioFetcher", "downloading $ext audio for: $title")

                    val destFile = File(destDir, "yt_download_${System.currentTimeMillis()}.$ext")
                    downloadToFile(downloadUrl, destFile)
                    return@withContext destFile
                } catch (e: Exception) {
                    lastError = e
                    Log.w("YtAudioFetcher", "piped instance $instance failed: ${e.message}")
                }
            }

            // Tier 2: Invidious API
            for (instance in invidiousInstances) {
                try {
                    Log.d("YtAudioFetcher", "trying invidious instance $instance for video $videoId")

                    val request = Request.Builder()
                        .url("$instance/api/v1/videos/$videoId")
                        .header("Accept", "application/json")
                        .build()

                    val response = client.newCall(request).execute()
                    if (!response.isSuccessful) {
                        Log.w("YtAudioFetcher", "$instance returned ${response.code}")
                        continue
                    }

                    val body = response.body?.string() ?: continue
                    val json = JSONObject(body)

                    val formats = json.optJSONArray("adaptiveFormats")
                    if (formats == null || formats.length() == 0) {
                        throw Exception("No adaptive formats found for this video")
                    }

                    val audioStream = pickBestAudioStreamInvidious(formats)
                    val downloadUrl = audioStream.optString("url")
                        ?: throw Exception("No download URL in audio stream")
                    val type = audioStream.optString("type", "audio/mp4")
                    val ext = mimeTypeToExtension(type)

                    val title = json.optString("title", "Unknown Title")
                    Log.d("YtAudioFetcher", "downloading $ext audio from invidious for: $title")

                    val destFile = File(destDir, "yt_download_${System.currentTimeMillis()}.$ext")
                    downloadToFile(downloadUrl, destFile)
                    return@withContext destFile
                } catch (e: Exception) {
                    lastError = e
                    Log.w("YtAudioFetcher", "invidious instance $instance failed: ${e.message}")
                }
            }

            throw lastError ?: Exception("All download servers failed")
        } catch (e: ConnectException) {
            throw Exception("Can't reach the download server — check your internet connection")
        } catch (e: UnknownHostException) {
            throw Exception("Can't resolve the download server address — check your internet connection")
        }
    }

    private fun downloadToFile(url: String, destFile: File) {
        val dlRequest = Request.Builder()
            .url(url)
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

    private fun pickBestAudioStreamInvidious(formats: JSONArray): JSONObject {
        var best: JSONObject? = null
        var bestBitrate = 0

        for (i in 0 until formats.length()) {
            val s = formats.getJSONObject(i)
            val type = s.optString("type", "")
            if (!type.contains("audio")) continue
            val bitrate = s.optInt("bitrate", 0)
            if (best == null || bitrate > bestBitrate) {
                best = s
                bestBitrate = bitrate
            }
        }

        return best ?: throw Exception("No audio streams found")
    }
}
