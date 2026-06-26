package com.example.musicx.data

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

// lrclib has the songs, we just need to ask better
// docs: https://lrclib.net/docs
data class LyricsResult(
    val lyrics: String,
    val synced: Boolean,
    val matchedTrack: String?,
    val matchedArtist: String?
)

// build a few guesses because downloaded titles are cursed
data class LookupCandidate(
    val title: String,
    val artist: String,
    val album: String? = null
)

class LyricsFetcher {
    companion object {
        private val sharedClient = OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS)
            .build()

        // regex for finding quoted text and splitting artists
        private val quotedRegex = Regex("""[""]([^""]+)[""]""")
        private val bracketParenRegex = Regex("""\[[^\]]*\]|\([^)]*\)""")
        private val artistSplitRegex = Regex("""(?i)\s+(?:feat\.|ft\.|featuring|&|,|/|and)\s+""")
        private val multiSpaceRegex = Regex("\\s+")
        private val junkRegex = Regex(
            """(?i)\b(?:official\s+(?:music\s+)?video|official\s+lyric\s+video|""" +
            """lyric\s+video|lyrics?|visualizer|official\s+audio|audio|AMV|remastered|live)\b"""
        )
    }

    private val client get() = sharedClient
    private val USER_AGENT = "MusicX/1.4.2 (https://github.com/yummyfiles/MusicX)"

    // main entry point - generates candidates and tries lrclib endpoints
    suspend fun fetchLyrics(
        title: String,
        artist: String,
        album: String? = null,
        durationMs: Long? = null
    ): LyricsResult? = withContext(Dispatchers.IO) {
        val candidates = generateCandidates(title, artist)
        val durationSec = durationMs?.let { (it / 1000).toInt() }

        Log.d("LyricsFetcher", "looking up: \"$title\" by \"$artist\" " +
                "album=$album duration=${durationSec}s " +
                "candidates=${candidates.size}")

        // step 1: try get-cached with each candidate (no external hits)
        for (cand in candidates) {
            val result = tryGetCached(cand.title, cand.artist, album, durationSec)
            if (result != null) {
                val score = scoreResult(result, cand, durationSec)
                if (score >= 50) {
                    Log.d("LyricsFetcher", "get-cached matched: \"${result.trackName}\" " +
                            "by \"${result.artistName}\" score=$score synced=${result.syncedLyrics?.isNotBlank()}")
                    return@withContext buildResult(result)
                }
                Log.d("LyricsFetcher", "get-cached low score=$score for \"${result.trackName}\" - skipping")
            }
        }

        // step 2: try get with each candidate (may hit external sources)
        for (cand in candidates) {
            val result = tryGet(cand.title, cand.artist, album, durationSec)
            if (result != null) {
                val score = scoreResult(result, cand, durationSec)
                if (score >= 50) {
                    Log.d("LyricsFetcher", "get matched: \"${result.trackName}\" " +
                            "by \"${result.artistName}\" score=$score")
                    return@withContext buildResult(result)
                }
                Log.d("LyricsFetcher", "get low score=$score for \"${result.trackName}\" - skipping")
            }
        }

        // step 3: build search queries and search
        val searchQueries = buildSearchQueries(candidates)
        var bestResult: LyricsResponse? = null
        var bestScore = 0

        for (query in searchQueries) {
            val results = trySearch(query)
            if (results.isNotEmpty()) {
                Log.d("LyricsFetcher", "search \"$query\" returned ${results.size} results")
                for (r in results) {
                    // score against each candidate, take the best
                    for (cand in candidates) {
                        val s = scoreResult(r, cand, durationSec)
                        if (s > bestScore) {
                            bestScore = s
                            bestResult = r
                        }
                    }
                }
            }
        }

        if (bestResult != null && bestScore >= 40) {
            Log.d("LyricsFetcher", "best search result: \"${bestResult.trackName}\" " +
                    "by \"${bestResult.artistName}\" score=$bestScore " +
                    "synced=${bestResult.syncedLyrics?.isNotBlank()}")
            return@withContext buildResult(bestResult)
        }

        Log.d("LyricsFetcher", "no confident lyrics found (best score=$bestScore)")
        null
    }

    // generate multiple lookup candidates from messy titles
    private fun generateCandidates(title: String, artist: String): List<LookupCandidate> {
        val candidates = mutableListOf<LookupCandidate>()
        val rawTitle = title.trim()
        val rawArtist = artist.trim()

        // candidate 1: raw title + raw artist
        candidates.add(LookupCandidate(rawTitle, rawArtist))

        // extract quoted content if present
        val quoted = quotedRegex.find(rawTitle)
        val quotedText = quoted?.groupValues?.get(1)?.trim()

        // check for pipe - usually means "title | artist [tag]"
        val pipeIndex = rawTitle.indexOf("|")
        val beforePipe = if (pipeIndex >= 0) rawTitle.substring(0, pipeIndex).trim() else null
        val afterPipe = if (pipeIndex >= 0) {
            // strip brackets from artist part
            rawTitle.substring(pipeIndex + 1).trim()
                .replace(bracketParenRegex, "")
                .trim()
        } else null

        // candidate 2: quoted title + pipe artist if available
        if (quotedText != null && afterPipe != null && afterPipe.isNotBlank()) {
            candidates.add(LookupCandidate(quotedText, afterPipe))
        }

        // candidate 3: quoted title + raw artist
        if (quotedText != null) {
            candidates.add(LookupCandidate(quotedText, rawArtist))
        }

        // candidate 4: before pipe + pipe artist
        if (beforePipe != null && afterPipe != null && afterPipe.isNotBlank()) {
            candidates.add(LookupCandidate(beforePipe, afterPipe))
        }

        // candidate 5: dash split - "Artist - Title" pattern
        if (rawTitle.contains(" - ") && rawArtist.isNotBlank()) {
            val parts = rawTitle.split(" - ", limit = 2)
            if (parts.size == 2) {
                candidates.add(LookupCandidate(parts[1].trim(), parts[0].trim()))
            }
        }

        // candidate 6: before pipe + raw artist
        if (beforePipe != null && rawArtist.isNotBlank()) {
            candidates.add(LookupCandidate(beforePipe, rawArtist))
        }

        // candidate 7: raw title with brackets/parens stripped, plus raw artist
        val strippedTitle = rawTitle.replace(bracketParenRegex, "").trim()
        if (strippedTitle != rawTitle) {
            candidates.add(LookupCandidate(strippedTitle, rawArtist))

            // candidate 8: stripped title + pipe artist
            if (afterPipe != null && afterPipe.isNotBlank()) {
                candidates.add(LookupCandidate(strippedTitle, afterPipe))
            }
        }

        // candidate 9: clean junk from title
        val cleanTitle = cleanJunk(rawTitle)
        if (cleanTitle != rawTitle && cleanTitle.isNotBlank()) {
            candidates.add(LookupCandidate(cleanTitle, rawArtist))
        }

        // split artist on feat/ft/etc and generate more
        val artistParts = rawArtist.split(artistSplitRegex).map { it.trim() }.filter { it.isNotBlank() }
        if (artistParts.size > 1) {
            for (part in artistParts) {
                if (part != rawArtist) {
                    if (quotedText != null) candidates.add(LookupCandidate(quotedText, part))
                    candidates.add(LookupCandidate(strippedTitle, part))
                }
            }
        }

        // remove duplicates (same title+artist combo)
        return candidates.distinctBy { "${it.title.lowercase()}|${it.artist.lowercase()}" }
    }

    // build search query strings from candidates
    private fun buildSearchQueries(candidates: List<LookupCandidate>): List<String> {
        val queries = mutableListOf<String>()
        for (c in candidates) {
            val q = "${c.title} ${c.artist}".trim()
            if (q.isNotBlank()) queries.add(q)
            // also try artist-first ordering
            val rev = "${c.artist} ${c.title}".trim()
            if (rev.isNotBlank() && rev != q) queries.add(rev)
        }
        return queries.distinct()
    }

    // /api/get-cached - only checks internal database
    private fun tryGetCached(
        title: String, artist: String, album: String?, durationSec: Int?
    ): LyricsResponse? {
        return try {
            val url = buildGetUrl("https://lrclib.net/api/get-cached", title, artist, album, durationSec)
            val request = Request.Builder().url(url).header("User-Agent", USER_AGENT).build()
            val response = client.newCall(request).execute()
            if (response.isSuccessful) {
                response.body?.string()?.let { parseLyricsResponse(it) }
            } else null
        } catch (e: Exception) {
            Log.w("LyricsFetcher", "get-cached failed for \"$title\": ${e.message}")
            null
        }
    }

    // /api/get - may hit external sources if not in db
    private fun tryGet(
        title: String, artist: String, album: String?, durationSec: Int?
    ): LyricsResponse? {
        return try {
            val url = buildGetUrl("https://lrclib.net/api/get", title, artist, album, durationSec)
            val request = Request.Builder().url(url).header("User-Agent", USER_AGENT).build()
            val response = client.newCall(request).execute()
            if (response.isSuccessful) {
                response.body?.string()?.let { parseLyricsResponse(it) }
            } else null
        } catch (e: Exception) {
            Log.w("LyricsFetcher", "get failed for \"$title\": ${e.message}")
            null
        }
    }

    // /api/search - search by query
    private fun trySearch(query: String): List<LyricsResponse> {
        return try {
            val encoded = URLEncoder.encode(query, "UTF-8")
            val url = "https://lrclib.net/api/search?q=$encoded"
            val request = Request.Builder().url(url).header("User-Agent", USER_AGENT).build()
            val response = client.newCall(request).execute()
            if (response.isSuccessful) {
                val body = response.body?.string() ?: return emptyList()
                val arr = JSONArray(body)
                (0 until arr.length()).mapNotNull { i ->
                    parseLyricsResponse(arr.getJSONObject(i).toString())
                }
            } else emptyList()
        } catch (e: Exception) {
            Log.w("LyricsFetcher", "search failed for \"$query\": ${e.message}")
            emptyList()
        }
    }

    private fun buildGetUrl(
        base: String, title: String, artist: String, album: String?, durationSec: Int?
    ): String {
        val params = mutableListOf(
            "track_name=${URLEncoder.encode(title, "UTF-8")}",
            "artist_name=${URLEncoder.encode(artist, "UTF-8")}"
        )
        if (!album.isNullOrBlank()) {
            params.add("album_name=${URLEncoder.encode(album, "UTF-8")}")
        }
        if (durationSec != null && durationSec > 0) {
            params.add("duration=$durationSec")
        }
        return "$base?${params.joinToString("&")}"
    }

    private fun parseLyricsResponse(json: String): LyricsResponse? {
        return try {
            val obj = JSONObject(json)
            LyricsResponse(
                id = obj.optLong("id"),
                trackName = obj.optString("trackName", ""),
                artistName = obj.optString("artistName", ""),
                albumName = obj.optString("albumName"),
                duration = obj.optInt("duration"),
                instrumental = obj.optBoolean("instrumental", false),
                plainLyrics = obj.optString("plainLyrics"),
                syncedLyrics = obj.optString("syncedLyrics")
            )
        } catch (e: Exception) {
            Log.w("LyricsFetcher", "failed to parse response: ${e.message}")
            null
        }
    }

    // score how well an lrclib result matches what we're looking for
    private fun scoreResult(
        result: LyricsResponse, candidate: LookupCandidate, durationSec: Int?
    ): Int {
        // instrumental = reject
        if (result.instrumental) return -100

        // dont grab random lyrics just because they showed up first
        val nTrack = normalize(result.trackName)
        val nArtist = normalize(result.artistName)
        val nLookupTitle = normalize(candidate.title)
        val nLookupArtist = normalize(candidate.artist)

        if (nTrack.isBlank()) return 0

        var score = 0

        // title match
        if (nTrack == nLookupTitle) score += 50
        else if (nTrack.contains(nLookupTitle) || nLookupTitle.contains(nTrack)) {
            val shorter = if (nTrack.length < nLookupTitle.length) nTrack else nLookupTitle
            val longer = if (nTrack.length < nLookupTitle.length) nLookupTitle else nTrack
            // partial match bonus proportional to how much of the shorter is in the longer
            if (longer.contains(shorter) && shorter.length >= 4) {
                score += 20
            }
        }

        // artist match
        if (nArtist == nLookupArtist) score += 40
        else if (nArtist.contains(nLookupArtist) || nLookupArtist.contains(nArtist)) {
            val shorter = if (nArtist.length < nLookupArtist.length) nArtist else nLookupArtist
            val longer = if (nArtist.length < nLookupArtist.length) nLookupArtist else nArtist
            if (longer.contains(shorter) && shorter.length >= 3) score += 15
        }

        // duration bonus
        if (durationSec != null && result.duration > 0) {
            val diff = kotlin.math.abs(durationSec - result.duration)
            if (diff <= 2) score += 30
            else if (diff <= 5) score += 15
            else if (diff <= 10) score += 5
        }

        // lyrics type bonus
        if (result.syncedLyrics.isNotBlank()) score += 10
        else if (result.plainLyrics.isNotBlank()) score += 5

        return score
    }

    private fun buildResult(response: LyricsResponse): LyricsResult {
        // prefer synced lyrics, fall back to plain
        val lyricsText = if (response.syncedLyrics.isNotBlank()) response.syncedLyrics
            else response.plainLyrics
        return LyricsResult(
            lyrics = lyricsText,
            synced = response.syncedLyrics.isNotBlank(),
            matchedTrack = response.trackName,
            matchedArtist = response.artistName
        )
    }

    // lowercase, strip punctuation, collapse spaces
    private fun normalize(s: String): String {
        return s.lowercase()
            .replace(Regex("""[^\w\s]"""), "")
            .replace(multiSpaceRegex, " ")
            .trim()
    }

    private fun cleanJunk(input: String): String {
        return input.replace(junkRegex, "")
            .replace(multiSpaceRegex, " ")
            .trim()
    }
}

// internal data class matching lrclib response shape
private data class LyricsResponse(
    val id: Long,
    val trackName: String,
    val artistName: String,
    val albumName: String?,
    val duration: Int,
    val instrumental: Boolean,
    val plainLyrics: String,
    val syncedLyrics: String
)
