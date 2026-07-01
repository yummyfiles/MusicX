package com.example.musicx.ui.settings

import org.json.JSONObject

internal data class ParsedUpdateRelease(
    val version: String,
    val downloadUrl: String
)

internal object UpdateReleaseParser {
    fun parse(body: String, currentVersion: String): ParsedUpdateRelease {
        val json = JSONObject(body)
        val latestTag = json.optString("tag_name", "").removePrefix("v")
        val assets = json.optJSONArray("assets") ?: return ParsedUpdateRelease(latestTag, "")
        val apkAsset = (0 until assets.length())
            .mapNotNull { index -> assets.optJSONObject(index) }
            .firstOrNull { it.optString("name", "").contains(".apk", ignoreCase = true) }
        val downloadUrl = apkAsset?.optString("browser_download_url", "") ?: ""
        return ParsedUpdateRelease(latestTag, downloadUrl)
    }
}
