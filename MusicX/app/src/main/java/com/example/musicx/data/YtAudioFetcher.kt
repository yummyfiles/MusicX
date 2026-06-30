package com.example.musicx.data

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

class YtAudioFetcher {
    companion object {
        private val sharedClient = OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(120, TimeUnit.SECONDS)
            .writeTimeout(120, TimeUnit.SECONDS)
            .build()
    }

    private val client get() = sharedClient

    suspend fun requestToken(apiBaseUrl: String, youtubeUrl: String): String = withContext(Dispatchers.IO) {
        val encodedUrl = URLEncoder.encode(youtubeUrl, "UTF-8")
        val requestUrl = "${apiBaseUrl.trimEnd('/')}/?url=$encodedUrl"
        Log.d("YtAudioFetcher", "requesting token from $requestUrl")
        val request = Request.Builder().url(requestUrl).build()
        val response = client.newCall(request).execute()
        if (!response.isSuccessful) {
            throw Exception("API returned ${response.code}: ${response.body?.string()}")
        }
        val body = response.body?.string() ?: throw Exception("Empty response from API")
        val json = JSONObject(body)
        json.getString("token")
    }

    suspend fun downloadAudio(apiBaseUrl: String, token: String, destFile: File): Unit = withContext(Dispatchers.IO) {
        val encodedToken = URLEncoder.encode(token, "UTF-8")
        val downloadUrl = "${apiBaseUrl.trimEnd('/')}/download?token=$encodedToken"
        Log.d("YtAudioFetcher", "downloading from $downloadUrl")
        val request = Request.Builder().url(downloadUrl).build()
        val response = client.newCall(request).execute()
        if (!response.isSuccessful) {
            if (response.code == 404) throw Exception("Token expired or invalid, try again")
            throw Exception("Download returned ${response.code}: ${response.body?.string()}")
        }
        response.body?.byteStream()?.use { input ->
            FileOutputStream(destFile).use { output ->
                input.copyTo(output)
            }
        } ?: throw Exception("Empty response body from API")
    }
}
