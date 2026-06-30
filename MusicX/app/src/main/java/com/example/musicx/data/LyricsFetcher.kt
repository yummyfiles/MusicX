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

data class LyricsResult(
    val lyrics: String,
    val synced: Boolean,
    val matchedTrack: String?,
    val matchedArtist: String?
)

data class LookupCandidate(
    val title: String,
    val artist: String
)

class LyricsFetcher {
    companion object {
        private val sharedClient = OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .build()

        private val quotedRegex = Regex("""[""]([^""]+)[""]""")
        private val bracketParenOpenRegex = Regex("""\[[^\]]*\]""")
        private val parenContentOpenRegex = Regex("""\([^)]*\)""")
        private val artistSplitRegex = Regex("""(?i)\s+(?:feat\.|ft\.|featuring|&|,|/|and|xd)\s+""")
        private val multiSpaceRegex = Regex("\\s+")
        private val junkRegex = Regex(
            """(?i)\b(?:official\s+(?:music\s+)?video|official\s+lyric\s+video|""" +
            """lyric\s+video|lyrics?|visualizer|official\s+audio|audio|AMV|remastered|live|""" +
            """4k|hd|60fps|reaction|cover|tribute)\b"""
        )
    }

    private val client get() = sharedClient
    private val USER_AGENT = "MusicX/1.6.0 (https://github.com/yummyfiles/MusicX)"

    suspend fun fetchLyrics(
        title: String,
        artist: String,
        album: String? = null,
        durationMs: Long? = null
    ): LyricsResult? = withContext(Dispatchers.IO) {
        val rawTitle = title.trim()
        val rawArtist = artist.trim()
        val durationSec = durationMs?.let { (it / 1000).toInt() }

        Log.d("LyricsFetcher", "looking up: \"$rawTitle\" by \"$rawArtist\" " +
                "album=$album duration=${durationSec}s")

        val candidates = generateCandidates(rawTitle, rawArtist)
        Log.d("LyricsFetcher", "generated ${candidates.size} lookup candidates")
        for ((i, c) in candidates.withIndex()) {
            Log.d("LyricsFetcher", "  candidate $i: title=\"${c.title}\" artist=\"${c.artist}\"")
        }

        val cleanedTitle = cleanForLookup(rawTitle)
        val cleanedArtist = cleanForLookup(rawArtist)

        for (cand in candidates) {
            if (cand.title.isBlank() || cand.artist.isBlank()) continue
            Log.d("LyricsFetcher", "trying LRCLIB field search: \"${cand.title}\" / \"${cand.artist}\"")
            val results = tryFieldSearch(cand.title, cand.artist)
            if (results.isNotEmpty()) {
                Log.d("LyricsFetcher", "LRCLIB field search returned ${results.size} results")
                val best = pickBestSyncedResult(results, cleanedTitle, cleanedArtist, durationSec)
                if (best != null) return@withContext best
            }
        }

        if (cleanedTitle.isNotBlank() && cleanedArtist.isNotBlank()) {
            if (candidates.none { it.title == cleanedTitle && it.artist == cleanedArtist }) {
                Log.d("LyricsFetcher", "trying LRCLIB field search: \"$cleanedTitle\" / \"$cleanedArtist\"")
                val results = tryFieldSearch(cleanedTitle, cleanedArtist)
                if (results.isNotEmpty()) {
                    Log.d("LyricsFetcher", "cleaned field search returned ${results.size} results")
                    val best = pickBestSyncedResult(results, cleanedTitle, cleanedArtist, durationSec)
                    if (best != null) return@withContext best
                }
            }
        }

        for (cand in candidates) {
            val query = "${cand.title} ${cand.artist}".trim()
            if (query.isBlank()) continue
            Log.d("LyricsFetcher", "trying LRCLIB q search: \"$query\"")
            val results = tryQSearch(query)
            if (results.isNotEmpty()) {
                Log.d("LyricsFetcher", "LRCLIB q search returned ${results.size} results")
                val best = pickBestSyncedResult(results, cleanedTitle, cleanedArtist, durationSec)
                if (best != null) return@withContext best
            }
        }

        for (cand in candidates) {
            val query = "${cand.artist} ${cand.title}".trim()
            if (query.isBlank()) continue
            Log.d("LyricsFetcher", "trying LRCLIB q search: \"$query\"")
            val results = tryQSearch(query)
            if (results.isNotEmpty()) {
                Log.d("LyricsFetcher", "LRCLIB q search returned ${results.size} results")
                val best = pickBestSyncedResult(results, cleanedTitle, cleanedArtist, durationSec)
                if (best != null) return@withContext best
            }
        }

        Log.d("LyricsFetcher", "no synced lyrics found for \"$rawTitle\" by \"$rawArtist\"")
        null
    }

    private fun pickBestSyncedResult(
        results: List<LyricsResponse>,
        localTitle: String,
        localArtist: String,
        durationSec: Int?
    ): LyricsResult? {
        val nLocalTitle = normalize(localTitle)
        val nLocalArtist = normalize(localArtist)

        val candidates = results.filter { it.syncedLyrics.isNotBlank() }

        if (candidates.isEmpty()) {
            Log.d("LyricsFetcher", "no results have synced lyrics, skipping")
            return null
        }

        val scored = candidates.map { r ->
            val titleScore = titleMatchScore(nLocalTitle, normalize(r.trackName))
            val artistScore = artistMatchScore(nLocalArtist, normalize(r.artistName))
            Triple(r, titleScore, artistScore)
        }

        val exactArtistMatches = scored.filter { (_, _, a) -> a >= 0.85f }
            .sortedByDescending { (_, t, a) -> t * 0.5f + a * 0.5f }

        if (exactArtistMatches.isNotEmpty()) {
            val (r, tScore, aScore) = exactArtistMatches.first()
            val totalScore = (tScore * 45 + aScore * 55).toInt()
            if (totalScore >= 60) {
                Log.d("LyricsFetcher", "synced match: \"${r.trackName}\" by \"${r.artistName}\" " +
                        "titleScore=${"%.2f".format(tScore)} artistScore=${"%.2f".format(aScore)} total=$totalScore")
                return buildResult(r)
            }
        }

        val goodArtistMatches = scored.filter { (_, _, a) -> a >= 0.6f }
            .sortedByDescending { (r, t, a) ->
                var s = t * 45 + a * 55
                if (durationSec != null && r.duration > 0) {
                    val diff = kotlin.math.abs(durationSec - r.duration)
                    if (diff <= 3) s += 20
                    else if (diff <= 8) s += 10
                }
                s
            }

        if (goodArtistMatches.isNotEmpty()) {
            val (r, tScore, aScore) = goodArtistMatches.first()
            val totalScore = tScore * 45 + aScore * 55
            if (tScore >= 0.3f && totalScore >= 50) {
                Log.d("LyricsFetcher", "synced match (lenient artist): \"${r.trackName}\" by \"${r.artistName}\" " +
                        "titleScore=${"%.2f".format(tScore)} artistScore=${"%.2f".format(aScore)} total=$totalScore")
                return buildResult(r)
            }
        }

        return null
    }

    private fun generateCandidates(title: String, artist: String): List<LookupCandidate> {
        val candidates = mutableListOf<LookupCandidate>()
        val rawTitle = title.trim()
        val rawArtist = artist.trim()

        candidates.add(LookupCandidate(rawTitle, rawArtist))

        val quoted = quotedRegex.find(rawTitle)
        val quotedText = quoted?.groupValues?.get(1)?.trim()

        val pipeIndex = rawTitle.indexOf("|")
        val beforePipe = if (pipeIndex >= 0) rawTitle.substring(0, pipeIndex).trim() else null
        val afterPipe = if (pipeIndex >= 0) {
            rawTitle.substring(pipeIndex + 1).trim()
                .replace(bracketParenOpenRegex, "")
                .replace(parenContentOpenRegex, "")
                .trim()
        } else null

        if (quotedText != null && afterPipe != null && afterPipe.isNotBlank()) {
            candidates.add(LookupCandidate(quotedText, afterPipe))
        }

        if (quotedText != null) {
            candidates.add(LookupCandidate(quotedText, rawArtist))
        }

        if (beforePipe != null && afterPipe != null && afterPipe.isNotBlank()) {
            candidates.add(LookupCandidate(beforePipe, afterPipe))
        }

        if (beforePipe != null && rawArtist.isNotBlank()) {
            candidates.add(LookupCandidate(beforePipe, rawArtist))
        }

        val bracketStripped = rawTitle.replace(bracketParenOpenRegex, "").trim()
        if (bracketStripped != rawTitle && bracketStripped.isNotBlank()) {
            val pipeInStrip = bracketStripped.indexOf("|")
            val stripBefore = if (pipeInStrip >= 0) bracketStripped.substring(0, pipeInStrip).trim() else bracketStripped
            val stripAfter = if (pipeInStrip >= 0) {
                bracketStripped.substring(pipeInStrip + 1).trim()
                    .replace(bracketParenOpenRegex, "")
                    .replace(parenContentOpenRegex, "")
                    .trim()
            } else null

            if (quotedText != null) {
                candidates.add(LookupCandidate(quotedText, if (stripAfter != null) stripAfter else rawArtist))
            }
            if (stripAfter != null && afterPipe == null) {
                candidates.add(LookupCandidate(stripBefore, stripAfter))
            }
            candidates.add(LookupCandidate(stripBefore, rawArtist))
        }

        val stripped = rawTitle
            .replace(bracketParenOpenRegex, "")
            .replace(parenContentOpenRegex, "")
            .trim()
        if (stripped != bracketStripped && stripped.isNotBlank()) {
            val pipeInStrip = stripped.indexOf("|")
            val stripBefore = if (pipeInStrip >= 0) stripped.substring(0, pipeInStrip).trim() else stripped
            val stripAfter = if (pipeInStrip >= 0) stripped.substring(pipeInStrip + 1).trim() else null

            if (quotedText != null && stripAfter != null) {
                candidates.add(LookupCandidate(quotedText, stripAfter))
            }
            if (stripAfter != null) {
                candidates.add(LookupCandidate(stripBefore, stripAfter))
            }
            candidates.add(LookupCandidate(stripBefore, rawArtist))
        }

        val cleanTitle = cleanJunk(rawTitle)
        if (cleanTitle != rawTitle && cleanTitle.isNotBlank()) {
            candidates.add(LookupCandidate(cleanTitle, rawArtist))
        }

        val hasQuoted = quotedText != null
        val hasPipe = pipeIndex >= 0
        if (!hasQuoted && !hasPipe && rawTitle.contains(" - ") && rawArtist.isNotBlank()) {
            val parts = rawTitle.split(" - ", limit = 2)
            if (parts.size == 2) {
                candidates.add(LookupCandidate(parts[1].trim(), parts[0].trim()))
            }
        }

        val artistParts = splitArtists(rawArtist)
        if (artistParts.size > 1) {
            for (part in artistParts) {
                if (part.lowercase() != rawArtist.lowercase()) {
                    if (quotedText != null) candidates.add(LookupCandidate(quotedText, part))
                    if (beforePipe != null) candidates.add(LookupCandidate(beforePipe, part))
                    candidates.add(LookupCandidate(stripped, part))
                }
            }
        }

        return candidates.distinctBy { "${it.title.lowercase()}|${it.artist.lowercase()}" }
    }

    private fun tryFieldSearch(trackName: String, artistName: String): List<LyricsResponse> {
        return try {
            val params = mutableListOf(
                "track_name=${URLEncoder.encode(trackName, "UTF-8")}",
                "artist_name=${URLEncoder.encode(artistName, "UTF-8")}"
            )
            val url = "https://lrclib.net/api/search?${params.joinToString("&")}"
            val request = Request.Builder().url(url).header("User-Agent", USER_AGENT).build()
            val response = client.newCall(request).execute()
            if (response.isSuccessful) {
                val body = response.body?.string() ?: return emptyList()
                if (body.trimStart().startsWith("[")) {
                    val arr = JSONArray(body)
                    (0 until arr.length()).mapNotNull { i ->
                        parseLyricsResponse(arr.getJSONObject(i).toString())
                    }
                } else {
                    val obj = JSONObject(body)
                    val parsed = parseLyricsResponse(obj.toString())
                    if (parsed != null) listOf(parsed) else emptyList()
                }
            } else emptyList()
        } catch (e: Exception) {
            Log.w("LyricsFetcher", "field search failed: ${e.message}")
            emptyList()
        }
    }

    private fun tryQSearch(query: String): List<LyricsResponse> {
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
            Log.w("LyricsFetcher", "q search failed: ${e.message}")
            emptyList()
        }
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

    private fun titleMatchScore(nLocalTitle: String, nResultTitle: String): Float {
        if (nLocalTitle.isBlank() || nResultTitle.isBlank()) return 0f

        if (nLocalTitle == nResultTitle) return 1f

        if (nLocalTitle.contains(nResultTitle) || nResultTitle.contains(nLocalTitle)) {
            val shorter = if (nLocalTitle.length < nResultTitle.length) nLocalTitle else nResultTitle
            val longer = if (nLocalTitle.length < nResultTitle.length) nResultTitle else nLocalTitle
            if (shorter.length >= 4) return shorter.length.toFloat() / longer.length
        }

        return 0f
    }

    private fun artistMatchScore(nLocalArtist: String, nResultArtist: String): Float {
        if (nLocalArtist.isBlank() || nResultArtist.isBlank()) return 0f

        if (nLocalArtist == nResultArtist) return 1f

        if (nLocalArtist.contains(nResultArtist) || nResultArtist.contains(nLocalArtist)) {
            val shorter = if (nLocalArtist.length < nResultArtist.length) nLocalArtist else nResultArtist
            val longer = if (nLocalArtist.length < nResultArtist.length) nResultArtist else nLocalArtist
            if (shorter.length >= 3) return shorter.length.toFloat() / longer.length
        }

        val localParts = splitArtists(nLocalArtist)
        val resultParts = splitArtists(nResultArtist)
        if (localParts.isEmpty() || resultParts.isEmpty()) return 0f

        var localMatched = 0
        for (lp in localParts) {
            val best = resultParts.maxOfOrNull { rp -> singleArtistMatch(lp, rp) } ?: 0f
            if (best >= 0.5f) localMatched++
        }

        var resultMatched = 0
        for (rp in resultParts) {
            val best = localParts.maxOfOrNull { lp -> singleArtistMatch(lp, rp) } ?: 0f
            if (best >= 0.5f) resultMatched++
        }

        val forwardRatio = localMatched.toFloat() / localParts.size
        val reverseRatio = resultMatched.toFloat() / resultParts.size

        return forwardRatio * 0.7f + reverseRatio * 0.3f
    }

    private fun singleArtistMatch(a: String, b: String): Float {
        if (a == b) return 1f
        if (a.contains(b) || b.contains(a)) {
            val shorter = if (a.length < b.length) a else b
            val longer = if (a.length < b.length) b else a
            if (shorter.length >= 3) return shorter.length.toFloat() / longer.length
        }
        return 0f
    }

    private fun splitArtists(artist: String): List<String> {
        return artist.split(artistSplitRegex)
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .map { it.replace(multiSpaceRegex, " ") }
    }

    private fun buildResult(response: LyricsResponse): LyricsResult {
        Log.d("LyricsFetcher", "found synced lyrics: \"${response.trackName}\" by \"${response.artistName}\"")
        return LyricsResult(
            lyrics = response.syncedLyrics,
            synced = true,
            matchedTrack = response.trackName,
            matchedArtist = response.artistName
        )
    }

    private fun cleanForLookup(input: String): String {
        return input.replace(junkRegex, "")
            .replace(multiSpaceRegex, " ")
            .trim()
    }

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
