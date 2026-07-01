package com.example.musicx.data

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.net.ConnectException
import java.net.URLEncoder
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
    }

    private val client get() = sharedClient

    suspend fun downloadAudio(
        youtubeUrl: String,
        destFile: File,
        instanceUrl: String = "https://api.cobalt.tools",
        apiKey: String = ""
    ): Unit = withContext(Dispatchers.IO) {
        try {
            Log.d("YtAudioFetcher", "requesting audio from $instanceUrl for: $youtubeUrl")

            val jsonBody = JSONObject().apply {
                put("url", youtubeUrl)
                put("downloadMode", "audio")
                put("audioFormat", "mp3")
                put("filenameStyle", "pretty")
                put("audioBitrate", "128")
            }
            val body = jsonBody.toString().toRequestBody("application/json".toMediaType())

            val requestBuilder = Request.Builder()
                .url("$instanceUrl/")
                .header("Accept", "application/json")
                .post(body)
            if (apiKey.isNotBlank()) {
                requestBuilder.header("Authorization", "Api-Key $apiKey")
            }
            val request = requestBuilder.build()

            val response = client.newCall(request).execute()
            if (!response.isSuccessful) {
                val errorBody = response.body?.string() ?: "no body"
                throw Exception("cobalt API returned ${response.code}: $errorBody")
            }

            val responseBody = response.body?.string() ?: throw Exception("Empty response from cobalt")
            val json = JSONObject(responseBody)
            val status = json.optString("status")

            when (status) {
                "tunnel", "redirect" -> {
                    val downloadUrl = json.optString("url")
                        ?: throw Exception("No download URL in response")
                    Log.d("YtAudioFetcher", "cobalt returned $status url: $downloadUrl")

                    val dlRequest = Request.Builder()
                        .url(downloadUrl)
                        .header("Accept", "audio/mpeg,audio/*,*/*")
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
                "error" -> {
                    val errorCode = json.optJSONObject("error")?.optString("code") ?: "unknown"
                    throw Exception("cobalt error: $errorCode")
                }
                "picker" -> {
                    throw Exception("Multiple formats available - not supported yet")
                }
                else -> {
                    throw Exception("Unexpected cobalt response status: $status")
                }
            }
        } catch (e: ConnectException) {
            throw Exception("Can't reach the download server — check your internet connection")
        } catch (e: UnknownHostException) {
            throw Exception("Can't resolve the download server address — check your internet connection")
        }
    }
}
