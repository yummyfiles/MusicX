package com.example.musicx.ui.settings

import org.junit.Assert.assertEquals
import org.junit.Test

class UpdateReleaseParserTest {
    @Test
    fun parsesLatestVersionAndApkDownloadUrl() {
        val body = """
            {
              "tag_name": "v1.6.1",
              "assets": [
                {
                  "name": "app-release.apk",
                  "browser_download_url": "https://example.com/app-release.apk"
                }
              ]
            }
        """.trimIndent()

        val result = UpdateReleaseParser.parse(body, "1.6.0")

        assertEquals("1.6.1", result.version)
        assertEquals("https://example.com/app-release.apk", result.downloadUrl)
    }
}
