package com.mybini.app.data.scraper

import okhttp3.OkHttpClient
import okhttp3.Request
import org.jsoup.Jsoup
import java.net.URI
import java.net.URLEncoder

class SpotifyMatcher(
    private val client: OkHttpClient,
    private val youtubeScraper: YouTubeScraper
) : MediaScraper {

    override suspend fun extractMediaUrl(url: String): ScrapeResult {
        return try {
            val trackId = extractTrackId(url) ?: return ScrapeResult.Error("URL Spotify tidak valid.")
            
            // Menggunakan open.spotify.com/embed/track/{id} untuk mengambil metadata secara publik & bersih
            val embedUrl = "https://open.spotify.com/embed/track/$trackId"
            val request = Request.Builder()
                .url(embedUrl)
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                .build()

            val response = client.newCall(request).execute()
            if (!response.isSuccessful) {
                return ScrapeResult.Error("Gagal mengambil metadata Spotify: Code ${response.code}")
            }

            val html = response.body?.string() ?: return ScrapeResult.Error("Metadata Spotify kosong.")
            
            // Gunakan Jsoup untuk parsing tag HTML
            val doc = Jsoup.parse(html)
            
            // Ambil Judul & Artis dari tag JSON script bawaan embed atau tag meta og:
            val metaTitle = doc.select("meta[property=og:title]").attr("content")
            val metaDescription = doc.select("meta[property=og:description]").attr("content")
            
            if (metaTitle.isEmpty()) {
                return ScrapeResult.Error("Gagal memparsing metadata lagu Spotify.")
            }

            // Contoh metaDescription: "Song · ArtistName · Year" atau "ArtistName · Song"
            val artistName = metaDescription.split("·").getOrNull(1)?.trim() ?: ""
            val trackTitle = metaTitle

            // Cari lagu yang cocok di YouTube
            val searchQuery = "$trackTitle $artistName audio"
            val youtubeVideoId = searchOnYoutube(searchQuery)
                ?: return ScrapeResult.Error("Gagal mencocokkan lagu Spotify di database audio YouTube.")

            // Ekstrak stream audio menggunakan YouTubeScraper
            val youtubeUrl = "https://www.youtube.com/watch?v=$youtubeVideoId"
            val youtubeResult = youtubeScraper.extractMediaUrl(youtubeUrl)

            when (youtubeResult) {
                is ScrapeResult.Success -> {
                    // Filter untuk mengambil format audio saja
                    val audioItems = youtubeResult.mediaItems.filter { 
                        it.mimeType.startsWith("audio") || it.resolution == "Audio" 
                    }
                    if (audioItems.isEmpty()) {
                        ScrapeResult.Error("Gagal mengekstrak berkas audio saja dari YouTube untuk lagu ini.")
                    } else {
                        // Pilih format audio terbaik (biasanya Opus atau AAC)
                        val bestAudio = audioItems.maxByOrNull { it.mimeType.contains("opus") } ?: audioItems.first()
                        
                        // Kembalikan item sukses dengan judul Spotify yang rapi
                        ScrapeResult.Success(
                            listOf(
                                bestAudio.copy(
                                    title = "$trackTitle - $artistName"
                                )
                            )
                        )
                    }
                }
                is ScrapeResult.Error -> {
                    ScrapeResult.Error("Gagal mengekstrak audio: ${youtubeResult.message}")
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            ScrapeResult.Error("Error Spotify Matcher: ${e.localizedMessage}")
        }
    }

    private fun extractTrackId(url: String): String? {
        return try {
            val uri = URI(url)
            val path = uri.path
            val parts = path.split("/").filter { it.isNotEmpty() }
            val index = parts.indexOf("track")
            if (index != -1 && index + 1 < parts.size) {
                // Potong jika ada parameter query seperti ?si=...
                parts[index + 1].split("?").firstOrNull()
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }

    private fun searchOnYoutube(query: String): String? {
        return try {
            val encodedQuery = URLEncoder.encode(query, "UTF-8")
            val searchUrl = "https://www.youtube.com/results?search_query=$encodedQuery"

            val request = Request.Builder()
                .url(searchUrl)
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                .build()

            val response = client.newCall(request).execute()
            if (!response.isSuccessful) return null

            val html = response.body?.string() ?: return null
            
            // Cari kecocokan pola "/watch?v=VIDEO_ID" pertama di dalam JSON halaman YouTube
            val pattern = "/watch\\?v=([a-zA-Z0-9_-]{11})"
            val compiledPattern = java.util.regex.Pattern.compile(pattern)
            val matcher = compiledPattern.matcher(html)
            
            if (matcher.find()) {
                matcher.group(1)
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }
}
