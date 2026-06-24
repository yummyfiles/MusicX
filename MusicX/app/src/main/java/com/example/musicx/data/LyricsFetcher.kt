package com.example.musicx.data

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.net.URLEncoder

// this class fetches lyrics from multiple sources on the internet
// tried a bunch of APIs and these are the ones that actually work reliably
class LyricsFetcher(var geniusApiKey: String = "") {
    companion object {
        // shared http client - no point making a new one every time
        private val sharedClient = OkHttpClient.Builder()
            .connectTimeout(15, java.util.concurrent.TimeUnit.SECONDS)
            .readTimeout(15, java.util.concurrent.TimeUnit.SECONDS)
            .build()

        // pre-compiled regex patterns for parsing HTML and cleaning up text
        // learned the hard way that compiling regex every time is slow af
        private val containerPattern = Regex("data-lyrics-container=\"true\">(.*?)</div>")
        private val tagStripPattern = Regex("<.*?>")
        private val oldLyricsPattern = Regex("class=\"lyrics\">(.*?)</div>", RegexOption.DOT_MATCHES_ALL)
        private val artistSplitPattern = Regex("(?i)ft\\.|feat\\.|featuring|&|,|and")
        private val parenBracketPattern = Regex("(?i)\\(.*?\\)|\\[.*?]")
        private val videoInfoPattern = Regex("(?i)official.*?video|music.*?video|remastered|live|lyric.*?video")
    }

    private val client get() = sharedClient
    // need a real browser user agent or some sites block us
    private val USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/119.0.0.0 Safari/537.36"

    // tries multiple sources to find lyrics - LRCLIB first for synced lyrics
    // falls back to genius and lyrics.ovh if that fails
    suspend fun fetchLyrics(artist: String, title: String): String? = withContext(Dispatchers.IO) {
        val cleanArtist = cleanArtistName(artist)
        val cleanTitle = cleanTrackTitle(title)

        // 1. Try LRCLIB first - has synced lyrics which is what we want
        var lyrics = tryLrcLib(cleanArtist, cleanTitle)
        if (lyrics != null) return@withContext lyrics

        // 2. Try Genius - best coverage but usually just plain text
        //    uses official Genius API when api key is configured
        lyrics = tryGeniusScrape(cleanArtist, cleanTitle)
        if (lyrics != null) return@withContext lyrics

        // 3. Try Lyrics.ovh - backup option
        lyrics = tryLyricsOvh(cleanArtist, cleanTitle)
        if (lyrics != null) return@withContext lyrics

        // 4. Try broader search on LRCLIB with just the title
        lyrics = tryLrcLib("", cleanTitle)
        if (lyrics != null) return@withContext lyrics

        null // no luck finding lyrics anywhere :(
    }

    private fun tryLrcLib(artist: String, title: String): String? {
        try {
            val query = URLEncoder.encode("$title $artist".trim(), "UTF-8")
            val url = "https://lrclib.net/api/search?q=$query"
            
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "MusicX/1.0")
                .build()
            val response = client.newCall(request).execute()
            
            if (response.isSuccessful) {
                val jsonArray = response.body?.string()?.let { JSONArray(it) }
                if (jsonArray != null && jsonArray.length() > 0) {
                    // Preferred: find first result that HAS synced lyrics
                    for (i in 0 until jsonArray.length()) {
                        val res = jsonArray.getJSONObject(i)
                        val synced = res.optString("syncedLyrics")
                        if (synced.isNotBlank()) return synced
                    }
                    // Fallback: just return the plain lyrics of the first result
                    val firstResult = jsonArray.getJSONObject(0)
                    return firstResult.optString("plainLyrics").ifBlank { null }
                }
            }
        } catch (e: Exception) {
            Log.w("LyricsFetcher", "LRCLIB failed for $title: ${e.message}")
        }
        return null
    }

    private fun tryGeniusScrape(artist: String, title: String): String? {
        try {
            var bestUrl: String? = null

            // If we have a Genius API key, use the official API
            if (geniusApiKey.isNotBlank()) {
                bestUrl = searchOfficialGeniusApi(artist, title)
            }

            // Fall back to the public multi-search endpoint if no key or official search failed
            if (bestUrl == null) {
                bestUrl = searchPublicGeniusApi(artist, title)
            }

            if (bestUrl != null) {
                return scrapeGeniusUrl(bestUrl)
            }
        } catch (e: Exception) {
            Log.w("LyricsFetcher", "Genius search failed for $title: ${e.message}")
        }
        return null
    }

    // uses the official Genius API with an access token
    // https://docs.genius.com/
    private fun searchOfficialGeniusApi(artist: String, title: String): String? {
        try {
            val query = URLEncoder.encode("$title $artist", "UTF-8")
            val url = "https://api.genius.com/search?q=$query&access_token=$geniusApiKey"

            val request = Request.Builder()
                .url(url)
                .header("User-Agent", USER_AGENT)
                .build()
            val response = client.newCall(request).execute()

            if (response.isSuccessful) {
                val json = JSONObject(response.body?.string() ?: "")
                val hits = json.getJSONObject("response").getJSONArray("hits")
                if (hits.length() > 0) {
                    return hits.getJSONObject(0)
                        .getJSONObject("result")
                        .getString("url")
                }
            }
        } catch (e: Exception) {
            Log.w("LyricsFetcher", "Official Genius API failed: ${e.message}")
        }
        return null
    }

    // fallback: uses the public genius.com search endpoint (no API key needed)
    private fun searchPublicGeniusApi(artist: String, title: String): String? {
        try {
            val query = URLEncoder.encode("$artist $title", "UTF-8")
            val searchUrl = "https://genius.com/api/search/multi?q=$query"
            
            val request = Request.Builder()
                .url(searchUrl)
                .header("User-Agent", USER_AGENT)
                .build()
            val response = client.newCall(request).execute()
            
            if (response.isSuccessful) {
                val json = JSONObject(response.body?.string() ?: "")
                val sections = json.getJSONObject("response").getJSONArray("sections")
                
                // First pass: look for top hit
                for (i in 0 until sections.length()) {
                    val section = sections.getJSONObject(i)
                    if (section.getString("type") == "top_hit") {
                        val hits = section.getJSONArray("hits")
                        if (hits.length() > 0) {
                            val hit = hits.getJSONObject(0)
                            if (hit.getString("type") == "song") {
                                return hit.getJSONObject("result").getString("url")
                            }
                        }
                    }
                }
                
                // Second pass: look for any song if top hit wasn't a song
                for (i in 0 until sections.length()) {
                    val section = sections.getJSONObject(i)
                    if (section.getString("type") == "song") {
                        val hits = section.getJSONArray("hits")
                        if (hits.length() > 0) {
                            return hits.getJSONObject(0).getJSONObject("result").getString("url")
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.w("LyricsFetcher", "Public Genius search failed for $title: ${e.message}")
        }
        return null
    }

    private fun scrapeGeniusUrl(url: String): String? {
        try {
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", USER_AGENT)
                .build()
            val response = client.newCall(request).execute()
            if (response.isSuccessful) {
                val html = response.body?.string() ?: ""
                
                // Genius structure: look for data-lyrics-container divs
                val containers = containerPattern.findAll(html)
                
                if (containers.any()) {
                    return containers.map { it.groupValues[1] }
                        .joinToString("\n")
                        .replace("<br/>", "\n")
                        .replace(tagStripPattern, "") // Strip tags
                        .replace("&quot;", "\"")
                        .replace("&amp;", "&")
                        .replace("&#x27;", "'")
                        .trim()
                }
                
                // Fallback for older Genius layout or different structure
                val oldMatch = oldLyricsPattern.find(html)
                if (oldMatch != null) {
                    return oldMatch.groupValues[1]
                        .replace(tagStripPattern, "")
                        .trim()
                }
            }
        } catch (e: Exception) {
            Log.w("LyricsFetcher", "Scraping Genius URL failed: ${e.message}")
        }
        return null
    }

    private fun tryLyricsOvh(artist: String, title: String): String? {
        if (artist.isBlank() || artist.contains("Unknown", ignoreCase = true)) return null
        try {
            val encodedArtist = URLEncoder.encode(artist, "UTF-8")
            val encodedTitle = URLEncoder.encode(title, "UTF-8")
            val url = "https://api.lyrics.ovh/v1/$encodedArtist/$encodedTitle"
            
            val request = Request.Builder().url(url).build()
            val response = client.newCall(request).execute()
            
            if (response.isSuccessful) {
                val json = JSONObject(response.body?.string() ?: "")
                val rawLyrics = json.optString("lyrics")
                if (rawLyrics.isNotBlank()) {
                    return rawLyrics.substringAfter("\n\n").trim()
                }
            }
        } catch (e: Exception) {
            Log.w("LyricsFetcher", "Lyrics.ovh failed for $title: ${e.message}")
        }
        return null
    }

    private fun cleanArtistName(artist: String): String {
        return artist.split(artistSplitPattern).first().trim()
    }

    private fun cleanTrackTitle(title: String): String {
        return title.replace(parenBracketPattern, "")
            .replace(videoInfoPattern, "")
            .trim()
    }
}
