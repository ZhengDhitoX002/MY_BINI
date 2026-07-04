package com.mybini.app.data.downloader

import java.util.regex.Pattern

object LinkSniffer {
    // Regex untuk mendeteksi file media langsung (video & audio) dari URL request
    private val MEDIA_REGEX = Pattern.compile(
        ".*\\.(mp4|m3u8|mp3|m4a|aac|ogg|webm|mov)(\\?.*)?$",
        Pattern.CASE_INSENSITIVE
    )

    fun sniff(url: String): Boolean {
        if (url.isEmpty()) return false
        val matcher = MEDIA_REGEX.matcher(url)
        return matcher.matches()
    }
}
