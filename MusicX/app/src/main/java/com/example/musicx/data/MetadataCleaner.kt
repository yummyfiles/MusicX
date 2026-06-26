package com.example.musicx.data

// cleans up cursed youtube/nerdcore titles
// example: AKAZA RAP "Unholy Hymn" | FabvL [Demon Slayer]
// becomes: Unholy Hymn - FabvL
object MetadataCleaner {

    private val quotedRegex = Regex("""[""]([^""]+)[""]""")
    private val bracketTagRegex = Regex("""\[[^\]]*\]""")
    private val parenTagRegex = Regex("""\([^)]*\)""")
    private val junkPatterns = listOf(
        Regex("""\bofficial\s+music\s+video\b""", RegexOption.IGNORE_CASE),
        Regex("""\bofficial\s+lyric\s+video\b""", RegexOption.IGNORE_CASE),
        Regex("""\blyric\s+video\b""", RegexOption.IGNORE_CASE),
        Regex("""\blyrics?\b""", RegexOption.IGNORE_CASE),
        Regex("""\bvisualizer\b""", RegexOption.IGNORE_CASE),
        Regex("""\bofficial\s+audio\b""", RegexOption.IGNORE_CASE),
        Regex("""\bAMV\b""", RegexOption.IGNORE_CASE),
        Regex("""\baudio\b""", RegexOption.IGNORE_CASE),
        Regex("""\bmusic\s+video\b""", RegexOption.IGNORE_CASE)
    )
    private val trailingBracketRegex = Regex("""\[[^\]]*\]\s*$""")
    private val trailingParenRegex = Regex("""\([^)]*\)\s*$""")
    private val emptyBracketRegex = Regex("""\s*\(\)""")
    private val emptySquareRegex = Regex("""\s*\[\]""")
    private val multiSpaceRegex = Regex("""\s+""")
    private val emptyAtEndRegex = Regex("""\s*\(\)\s*$|\s*\[\]\s*$""")

    data class CleanMetadata(
        val title: String,
        val artist: String
    )

    fun clean(
        rawTitle: String?,
        rawArtist: String?,
        fileNameWithoutExtension: String? = null
    ): CleanMetadata {
        var title = rawTitle?.trim().orEmpty()
        var artist = rawArtist?.trim().orEmpty()

        // save the original title before we mess with it
        // because apparently metadata likes to be dramatic
        val originalForParsing = title

        // first try to grab the title inside quotes
        // nerdcore songs love doing this for some reason
        val quoted = quotedRegex.find(originalForParsing)
        if (quoted != null) {
            title = quoted.groupValues[1].trim()
        }

        // pipe usually means the artist is chilling after it
        if (originalForParsing.contains("|")) {
            val parts = originalForParsing.split("|", limit = 2)
            val artistPart = parts[1].trim()
                .replace(bracketTagRegex, "")
                .replace(parenTagRegex, "")
                .trim()
            if (artistPart.isNotBlank()) {
                artist = artistPart
            }
            if (quoted == null) {
                title = parts[0].trim()
            }
        }

        // if we still have a " - " pattern and artist is unknown, try splitting
        if (title.contains(" - ") && isUnknown(artist)) {
            val nameParts = title.split(" - ", limit = 2)
            val possibleArtist = nameParts[0].trim()
            val possibleTitle = nameParts[1].trim()
            if (possibleTitle.isNotBlank()) {
                artist = possibleArtist
                title = possibleTitle
            }
        }

        // if still nothing useful, try the filename
        if (title.isBlank() || title.equals("Unknown", ignoreCase = true)) {
            title = fileNameWithoutExtension ?: ""
        }

        // filename might have " - " pattern too
        if (title.contains(" - ") && isUnknown(artist)) {
            val nameParts = title.split(" - ", limit = 2)
            val possibleArtist = nameParts[0].trim()
            val possibleTitle = nameParts[1].trim()
            if (possibleTitle.isNotBlank()) {
                artist = possibleArtist
                title = possibleTitle
            }
        }

        // remove random youtube words that are not the actual song title
        title = cleanJunk(title)
        artist = cleanJunk(artist)

        // if everything fails, just use unknown and move on
        if (title.isBlank()) title = "Unknown Title"
        if (artist.isBlank()) artist = "Unknown Artist"

        return CleanMetadata(title = title.trim(), artist = artist.trim())
    }

    // this version is only for lyrics lookup, not display
    // keeps the raw titles visible but extracts what we need for searching
    fun cleanForLyricsLookup(rawTitle: String, rawArtist: String): CleanMetadata {
        var title = rawTitle.trim()
        var artist = rawArtist.trim()
        val originalForParsing = title

        // grab the title inside quotes - that's usually the real song name
        val quoted = quotedRegex.find(originalForParsing)
        if (quoted != null) {
            title = quoted.groupValues[1].trim()
        }

        // pipe usually splits title | artist
        if (originalForParsing.contains("|")) {
            val parts = originalForParsing.split("|", limit = 2)
            val artistPart = parts[1].trim()
                .replace(bracketTagRegex, "")
                .replace(parenTagRegex, "")
                .trim()
            if (artistPart.isNotBlank()) {
                artist = artistPart
            }
            if (quoted == null) {
                title = parts[0].trim()
            }
        }

        // if we still have " - " and artist is still the original, try splitting
        if (title.contains(" - ") && artist == rawArtist.trim()) {
            val nameParts = title.split(" - ", limit = 2)
            val possibleArtist = nameParts[0].trim()
            val possibleTitle = nameParts[1].trim()
            if (possibleTitle.isNotBlank()) {
                artist = possibleArtist
                title = possibleTitle
            }
        }

        // remove youtube/lyrics junk
        title = cleanJunk(title)
        artist = cleanJunk(artist)

        if (title.isBlank()) title = "Unknown Title"
        if (artist.isBlank()) artist = "Unknown Artist"

        return CleanMetadata(title = title.trim(), artist = artist.trim())
    }

    // checks if a value counts as unknown
    private fun isUnknown(value: String): Boolean {
        return value.isBlank() ||
            value.equals("Unknown", ignoreCase = true) ||
            value.equals("Unknown Artist", ignoreCase = true) ||
            value.equals("Unknown Title", ignoreCase = true)
    }

    // gets rid of the usual youtube/lyrics metadata junk
    private fun cleanJunk(input: String): String {
        var result = input.trim()

        for (pattern in junkPatterns) {
            result = pattern.replace(result, "").trim()
        }

        // strip trailing brackets like [Demon Slayer] and (Official Video)
        result = result
            .replace(trailingBracketRegex, "")
            .replace(trailingParenRegex, "")
            .trim()

        // clean up any leftover empty brackets
        result = result
            .replace(emptyBracketRegex, "")
            .replace(emptySquareRegex, "")
            .trim()

        // remove multiple spaces
        result = result.replace(multiSpaceRegex, " ").trim()

        return result
    }
}
