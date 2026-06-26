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

// lrclib has the songs, we were just asking badly
// docs: https://lrclib.net/docs
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
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS)
            .build()

        private val quotedRegex = Regex("""[""]([^""]+)[""]""")
        private val bracketParenOpenRegex = Regex("""\[[^\]]*\]""")
        private val parenContentOpenRegex = Regex("""\([^)]*\)""")
        private val artistSplitRegex = Regex("""(?i)\s+(?:feat\.|ft\.|featuring|&|,|/|and)\s+""")
        private val multiSpaceRegex = Regex("\\s+")
        private val junkRegex = Regex(
            """(?i)\b(?:official\s+(?:music\s+)?video|official\s+lyric\s+video|""" +
            """lyric\s+video|lyrics?|visualizer|official\s+audio|audio|AMV|remastered|live)\b"""
        )
    }

    private val client get() = sharedClient
    private val USER_AGENT = "MusicX/1.4.6 (https://github.com/yummyfiles/MusicX)"

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

        // clean this only for lookup, not display
        val cleanedTitle = cleanForLookup(rawTitle)
        val cleanedArtist = cleanForLookup(rawArtist)

        // step 1: field search with track_name + artist_name for each candidate
        // this is the most precise lrclib lookup
        for (cand in candidates) {
            if (cand.title.isBlank() || cand.artist.isBlank()) continue
            Log.d("LyricsFetcher", "trying LRCLIB field search: \"${cand.title}\" / \"${cand.artist}\"")
            val results = tryFieldSearch(cand.title, cand.artist)
            if (results.isNotEmpty()) {
                Log.d("LyricsFetcher", "LRCLIB field search returned ${results.size} results")
                val best = pickBestResult(results, cleanedTitle, cleanedArtist, durationSec)
                if (best != null) return@withContext best
            }
        }

        // step 2: field search with cleaned fallback title/artist
        if (cleanedTitle.isNotBlank() && cleanedArtist.isNotBlank()) {
            val fullClean = "$cleanedTitle $cleanedArtist"
            if (candidates.none { it.title == cleanedTitle && it.artist == cleanedArtist }) {
                Log.d("LyricsFetcher", "trying LRCLIB field search: \"$cleanedTitle\" / \"$cleanedArtist\"")
                val results = tryFieldSearch(cleanedTitle, cleanedArtist)
                if (results.isNotEmpty()) {
                    Log.d("LyricsFetcher", "cleaned field search returned ${results.size} results")
                    val best = pickBestResult(results, cleanedTitle, cleanedArtist, durationSec)
                    if (best != null) return@withContext best
                }
            }
        }

        // step 3: q search with "title artist" for each candidate
        for (cand in candidates) {
            val query = "${cand.title} ${cand.artist}".trim()
            if (query.isBlank()) continue
            Log.d("LyricsFetcher", "trying LRCLIB q search: \"$query\"")
            val results = tryQSearch(query)
            if (results.isNotEmpty()) {
                Log.d("LyricsFetcher", "LRCLIB q search returned ${results.size} results")
                val best = pickBestResult(results, cleanedTitle, cleanedArtist, durationSec)
                if (best != null) return@withContext best
            }
        }

        // step 4: q search with "artist title" for each candidate
        for (cand in candidates) {
            val query = "${cand.artist} ${cand.title}".trim()
            if (query.isBlank()) continue
            Log.d("LyricsFetcher", "trying LRCLIB q search: \"$query\"")
            val results = tryQSearch(query)
            if (results.isNotEmpty()) {
                Log.d("LyricsFetcher", "LRCLIB q search returned ${results.size} results")
                val best = pickBestResult(results, cleanedTitle, cleanedArtist, durationSec)
                if (best != null) return@withContext best
            }
        }

        // step 5: title-only search as absolute last resort
        // title-only search is risky because everybody names songs the same thing
        if (cleanedTitle.isNotBlank()) {
            Log.d("LyricsFetcher", "trying title-only search: \"$cleanedTitle\"")
            val results = tryQSearch(cleanedTitle)
            if (results.isNotEmpty()) {
                Log.d("LyricsFetcher", "title-only search returned ${results.size} results")
                val best = pickTitleOnlyResult(results, cleanedTitle, cleanedArtist, durationSec)
                if (best != null) return@withContext best
                Log.d("LyricsFetcher", "title-only search was too risky, no lyrics")
            }
        }

        Log.d("LyricsFetcher", "no confident lyrics found")
        null
    }

    // pick the best result from a set, preferring ones with good artist match
    private fun pickBestResult(
        results: List<LyricsResponse>,
        localTitle: String,
        localArtist: String,
        durationSec: Int?
    ): LyricsResult? {
        val scored = results.map { it to scoreResult(it, localTitle, localArtist, durationSec) }
            .sortedByDescending { it.second }

        var bestWithArtist: Pair<LyricsResponse, Int>? = null
        var bestOverall: Pair<LyricsResponse, Int>? = null

        for ((r, s) in scored) {
            if (bestOverall == null || s > bestOverall.second) {
                bestOverall = r to s
            }
            val aScore = artistMatchScore(localArtist, r.artistName)
            if (aScore >= 0.5f && (bestWithArtist == null || s > bestWithArtist.second)) {
                bestWithArtist = r to s
            }
        }

        // dont grab random lyrics just because they showed up first
        if (bestWithArtist != null && bestWithArtist.second >= 65) {
            val r = bestWithArtist.first
            val lyricType = if (r.syncedLyrics.isNotBlank()) "synced" else "plain"
            Log.d("LyricsFetcher", "best match: \"${r.trackName}\" by \"${r.artistName}\" " +
                    "score=${bestWithArtist.second} ($lyricType lyrics)")
            return buildResult(r)
        }

        if (bestOverall != null && bestOverall.second >= 80) {
            val r = bestOverall.first
            val aScore = artistMatchScore(localArtist, r.artistName)
            // title matched but artist looked wrong, skipping
            Log.d("LyricsFetcher", "skipped sketchy match: \"${r.trackName}\" by \"${r.artistName}\" " +
                    "score=${bestOverall.second} artistScore=${String.format("%.2f", aScore)}")
        }

        return null
    }

    // title-only: only accept if extremely confident
    private fun pickTitleOnlyResult(
        results: List<LyricsResponse>,
        localTitle: String,
        localArtist: String,
        durationSec: Int?
    ): LyricsResult? {
        val nLocal = normalize(localTitle)

        // filter to only exact or near-exact title matches
        val exact = results.filter { normalize(it.trackName) == nLocal }

        if (exact.isEmpty()) return null

        val scored = exact.map { it to scoreResult(it, localTitle, localArtist, durationSec) }
            .sortedByDescending { it.second }

        val best = scored.first()
        val nBestArtist = normalize(best.first.artistName)

        // check for competing results with same title but different artist
        val competitors = exact.filter {
            normalize(it.artistName) != nBestArtist &&
            scoreResult(it, localTitle, localArtist, durationSec) >= best.second - 15
        }

        if (competitors.isNotEmpty()) {
            Log.d("LyricsFetcher", "title-only search too risky - ${competitors.size} " +
                    "competing results with same title, different artists")
            return null
        }

        // only accept if score is very high
        if (best.second >= 85) {
            val r = best.first
            val lyricType = if (r.syncedLyrics.isNotBlank()) "synced" else "plain"
            Log.d("LyricsFetcher", "title-only accepted: \"${r.trackName}\" by \"${r.artistName}\" " +
                    "score=${best.second} ($lyricType lyrics)")
            return buildResult(r)
        }

        return null
    }

    // generate lookup candidates from messy YouTube-style titles
    // clean this only for lookup, not display
    private fun generateCandidates(title: String, artist: String): List<LookupCandidate> {
        val candidates = mutableListOf<LookupCandidate>()
        val rawTitle = title.trim()
        val rawArtist = artist.trim()

        // candidate 1: raw title + raw artist (just in case)
        candidates.add(LookupCandidate(rawTitle, rawArtist))

        // extract quoted text - this is likely the real song title
        val quoted = quotedRegex.find(rawTitle)
        val quotedText = quoted?.groupValues?.get(1)?.trim()

        // check for pipe - text before is description, text after is artist
        val pipeIndex = rawTitle.indexOf("|")
        val beforePipe = if (pipeIndex >= 0) rawTitle.substring(0, pipeIndex).trim() else null
        val afterPipe = if (pipeIndex >= 0) {
            rawTitle.substring(pipeIndex + 1).trim()
                .replace(bracketParenOpenRegex, "")
                .replace(parenContentOpenRegex, "")
                .trim()
        } else null

        // candidate 2: quoted title + pipe artist
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

        // candidate 5: before pipe + raw artist
        if (beforePipe != null && rawArtist.isNotBlank()) {
            candidates.add(LookupCandidate(beforePipe, rawArtist))
        }

        // candidate 6: strip brackets only, keep parens
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

        // candidate 7: strip brackets AND parens
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

        // candidate 8: clean junk from title
        val cleanTitle = cleanJunk(rawTitle)
        if (cleanTitle != rawTitle && cleanTitle.isNotBlank()) {
            candidates.add(LookupCandidate(cleanTitle, rawArtist))
        }

        // candidate 9: dash split - "Artist - Title" pattern
        // only use if we have no quoted or pipe info to avoid confusion
        val hasQuoted = quotedText != null
        val hasPipe = pipeIndex >= 0
        if (!hasQuoted && !hasPipe && rawTitle.contains(" - ") && rawArtist.isNotBlank()) {
            val parts = rawTitle.split(" - ", limit = 2)
            if (parts.size == 2) {
                candidates.add(LookupCandidate(parts[1].trim(), parts[0].trim()))
            }
        }

        // split artist on feat/ft/etc and generate more candidates
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

        // remove duplicates (same title+artist combo)
        return candidates.distinctBy { "${it.title.lowercase()}|${it.artist.lowercase()}" }
    }

    // /api/search with track_name + artist_name params
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
                    // could be a single object response
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

    // /api/search with q= param
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

    // score how well an lrclib result matches what we looked up
    private fun scoreResult(
        result: LyricsResponse,
        localTitle: String,
        localArtist: String,
        durationSec: Int?
    ): Int {
        // instrumental = reject/no lyrics
        if (result.instrumental) return -100

        val tScore = titleMatchScore(localTitle, result.trackName)
        val aScore = artistMatchScore(localArtist, result.artistName)

        if (tScore <= 0f) return 0

        var score = (tScore * 45 + aScore * 55).toInt()

        // bonus when both match well
        if (tScore >= 0.9f && aScore >= 0.7f) score += 25
        if (tScore >= 0.95f && aScore >= 0.95f) score += 35

        // duration bonus
        if (durationSec != null && result.duration > 0) {
            val diff = kotlin.math.abs(durationSec - result.duration)
            when {
                diff <= 1 -> score += 25
                diff <= 3 -> score += 15
                diff <= 6 -> score += 8
                diff <= 10 -> score += 3
            }
        }

        // synced lyrics bonus
        if (result.syncedLyrics.isNotBlank()) score += 5

        return score
    }

    // how well does the result title match what we looked up
    // returns 0.0 to 1.0
    private fun titleMatchScore(localTitle: String, resultTitle: String): Float {
        val nLocal = normalize(localTitle)
        val nResult = normalize(resultTitle)
        if (nLocal.isBlank() || nResult.isBlank()) return 0f

        if (nLocal == nResult) return 1f

        if (nLocal.contains(nResult) || nResult.contains(nLocal)) {
            val shorter = if (nLocal.length < nResult.length) nLocal else nResult
            val longer = if (nLocal.length < nResult.length) nResult else nLocal
            if (shorter.length >= 4) return shorter.length.toFloat() / longer.length
        }

        return 0f
    }

    // how well does the result artist match what we looked up
    // splits multi-artist strings and checks each part
    // returns 0.0 to 1.0
    private fun artistMatchScore(localArtist: String, resultArtist: String): Float {
        val nLocal = normalize(localArtist)
        val nResult = normalize(resultArtist)
        if (nLocal.isBlank() || nResult.isBlank()) return 0f

        val localParts = splitArtists(nLocal)
        val resultParts = splitArtists(nResult)
        if (localParts.isEmpty() || resultParts.isEmpty()) return 0f

        var localMatched = 0
        for (lp in localParts) {
            val best = resultParts.maxOfOrNull { rp -> singleArtistMatch(lp, rp) } ?: 0f
            if (best >= 0.6f) localMatched++
        }

        var resultMatched = 0
        for (rp in resultParts) {
            val best = localParts.maxOfOrNull { lp -> singleArtistMatch(lp, rp) } ?: 0f
            if (best >= 0.6f) resultMatched++
        }

        val forwardRatio = localMatched.toFloat() / localParts.size
        val reverseRatio = resultMatched.toFloat() / resultParts.size

        return forwardRatio * 0.7f + reverseRatio * 0.3f
    }

    // match two individual artist names
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
        val lyricsText = if (response.syncedLyrics.isNotBlank()) response.syncedLyrics
            else response.plainLyrics
        val lyricType = if (response.syncedLyrics.isNotBlank()) "synced" else "plain"
        Log.d("LyricsFetcher", "found $lyricType lyrics: \"${response.trackName}\" by \"${response.artistName}\"")
        return LyricsResult(
            lyrics = lyricsText,
            synced = response.syncedLyrics.isNotBlank(),
            matchedTrack = response.trackName,
            matchedArtist = response.artistName
        )
    }

    // clean for lookup only - strip junk terms
    private fun cleanForLookup(input: String): String {
        return input.replace(junkRegex, "")
            .replace(multiSpaceRegex, " ")
            .trim()
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
