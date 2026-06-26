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
    private val USER_AGENT = "MusicX/1.4.4 (https://github.com/yummyfiles/MusicX)"

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
                val score = scoreResult(result, cand.title, cand.artist, durationSec)
                if (score >= 75) {
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
                val score = scoreResult(result, cand.title, cand.artist, durationSec)
                if (score >= 75) {
                    Log.d("LyricsFetcher", "get matched: \"${result.trackName}\" " +
                            "by \"${result.artistName}\" score=$score")
                    return@withContext buildResult(result)
                }
                Log.d("LyricsFetcher", "get low score=$score for \"${result.trackName}\" - skipping")
            }
        }

        // step 3: build search queries and search
        val searchQueries = buildSearchQueries(candidates)
        val allResults = mutableListOf<LyricsResponse>()

        for (query in searchQueries) {
            val results = trySearch(query)
            if (results.isNotEmpty()) {
                Log.d("LyricsFetcher", "search \"$query\" returned ${results.size} results")
                allResults.addAll(results)
            }
        }

        if (allResults.isEmpty()) {
            Log.d("LyricsFetcher", "no search results found")
            return@withContext null
        }

        // deduplicate by id
        val uniqueResults = allResults.distinctBy { it.id }

        // group by normalized title
        val resultsByTitle = mutableMapOf<String, MutableList<LyricsResponse>>()
        for (r in uniqueResults) {
            val nTitle = normalize(r.trackName)
            resultsByTitle.getOrPut(nTitle) { mutableListOf() }.add(r)
        }

        if (resultsByTitle.size > 1) {
            Log.d("LyricsFetcher", "multiple title groups found: ${resultsByTitle.size} different titles")
        }

        // for each title group, find best result
        var bestWithArtist: Pair<LyricsResponse, Int>? = null
        var bestOverall: Pair<LyricsResponse, Int>? = null

        for ((nTitle, entries) in resultsByTitle) {
            val scored = entries.map { it to scoreResult(it, title, artist, durationSec) }
            val bestInGroup = scored.maxByOrNull { it.second } ?: continue
            val aScore = artistMatchScore(artist, bestInGroup.first.artistName)

            Log.d("LyricsFetcher", "title group \"$nTitle\": best=\"${bestInGroup.first.artistName}\" " +
                    "score=${bestInGroup.second} artistScore=${String.format("%.2f", aScore)}")

            if (aScore >= 0.5f && bestInGroup.second >= 65) {
                if (bestWithArtist == null || bestInGroup.second > bestWithArtist.second) {
                    bestWithArtist = bestInGroup
                }
            }
            if (bestOverall == null || bestInGroup.second > bestOverall.second) {
                bestOverall = bestInGroup
            }
        }

        // prefer result with artist match
        if (bestWithArtist != null) {
            Log.d("LyricsFetcher", "selected result with artist match: " +
                    "\"${bestWithArtist.first.trackName}\" by \"${bestWithArtist.first.artistName}\" " +
                    "score=${bestWithArtist.second}")
            return@withContext buildResult(bestWithArtist.first)
        }

        // title-only fallback - very strict
        if (bestOverall != null) {
            val tScore = titleMatchScore(title, bestOverall.first.trackName)
            val bestNArtist = normalize(bestOverall.first.artistName)

            Log.d("LyricsFetcher", "title-only fallback candidate: " +
                    "\"${bestOverall.first.trackName}\" by \"${bestOverall.first.artistName}\" " +
                    "tScore=${String.format("%.2f", tScore)} total=${bestOverall.second}")

            if (tScore >= 0.95f && bestOverall.second >= 85) {
                // check for competing results with different artists at similar scores
                val competitors = uniqueResults.filter { r ->
                    normalize(r.trackName) == normalize(bestOverall.first.trackName) &&
                    normalize(r.artistName) != bestNArtist &&
                    scoreResult(r, title, artist, durationSec) >= bestOverall.second - 10
                }
                if (competitors.isEmpty()) {
                    Log.d("LyricsFetcher", "title-only search accepted: " +
                            "\"${bestOverall.first.trackName}\" by \"${bestOverall.first.artistName}\"")
                    return@withContext buildResult(bestOverall.first)
                } else {
                    Log.d("LyricsFetcher", "title-only search was too risky - multiple possible " +
                            "artists (${competitors.size} competitors), no lyrics")
                }
            } else {
                Log.d("LyricsFetcher", "title-only search was too risky - " +
                        "weak title match or low total score, no lyrics")
            }
        }

        Log.d("LyricsFetcher", "no confident lyrics found")
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
        val artistParts = splitArtists(rawArtist)
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
            val rev = "${c.artist} ${c.title}".trim()
            if (rev.isNotBlank() && rev != q) queries.add(rev)
        }
        return queries.distinct()
    }

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

        // count how many local artists are represented in the result
        var localMatched = 0
        for (lp in localParts) {
            val best = resultParts.maxOfOrNull { rp -> singleArtistMatch(lp, rp) } ?: 0f
            if (best >= 0.6f) localMatched++
        }

        // also check reverse (result artists represented in local)
        var resultMatched = 0
        for (rp in resultParts) {
            val best = localParts.maxOfOrNull { lp -> singleArtistMatch(lp, rp) } ?: 0f
            if (best >= 0.6f) resultMatched++
        }

        val forwardRatio = localMatched.toFloat() / localParts.size
        val reverseRatio = resultMatched.toFloat() / resultParts.size

        // weight forward more heavily (all our artists should be there)
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

    // split on feat/ft/& etc
    private fun splitArtists(artist: String): List<String> {
        return artist.split(artistSplitRegex)
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .map { it.replace(multiSpaceRegex, " ") }
    }

    // score how well an lrclib result matches what we're looking for
    private fun scoreResult(
        result: LyricsResponse,
        localTitle: String,
        localArtist: String,
        durationSec: Int?
    ): Int {
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

    private fun buildResult(response: LyricsResponse): LyricsResult {
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
