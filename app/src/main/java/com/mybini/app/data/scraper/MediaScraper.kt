package com.mybini.app.data.scraper

interface MediaScraper {
    suspend fun extractMediaUrl(url: String): ScrapeResult
}

sealed class ScrapeResult {
    data class Success(val mediaItems: List<ScrapedMediaItem>) : ScrapeResult()
    data class Error(val message: String) : ScrapeResult()
}

data class ScrapedMediaItem(
    val downloadUrl: String,
    val title: String,
    val mimeType: String,
    val resolution: String = "",
    val headers: Map<String, String> = emptyMap(),
    val isAdaptive: Boolean = false // Menandai jika file ini terpisah (misal: hanya video saja / audio saja)
)
