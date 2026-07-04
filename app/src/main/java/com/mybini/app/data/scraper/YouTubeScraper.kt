package com.mybini.app.data.scraper

import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.regex.Pattern

class YouTubeScraper(private val client: OkHttpClient) : MediaScraper {

    override suspend fun extractMediaUrl(url: String): ScrapeResult {
        return try {
            val videoId = extractVideoId(url) ?: return ScrapeResult.Error("URL YouTube tidak valid atau ID video tidak ditemukan.")
            val videoPageUrl = "https://www.youtube.com/watch?v=$videoId"

            val request = Request.Builder()
                .url(videoPageUrl)
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                .build()

            val response = client.newCall(request).execute()
            if (!response.isSuccessful) {
                return ScrapeResult.Error("Gagal mengambil halaman YouTube: Code ${response.code}")
            }

            val html = response.body?.string() ?: return ScrapeResult.Error("Halaman YouTube kosong.")
            val playerResponseJson = extractPlayerResponse(html) 
                ?: return ScrapeResult.Error("Gagal mengekstrak ytInitialPlayerResponse dari YouTube.")

            val jsonObject = JSONObject(playerResponseJson)
            val videoDetails = jsonObject.optJSONObject("videoDetails")
            val title = videoDetails?.optString("title") ?: "YouTube Video"
            
            val streamingData = jsonObject.optJSONObject("streamingData") 
                ?: return ScrapeResult.Error("Data streaming tidak ditemukan pada video ini.")

            val mediaItems = mutableListOf<ScrapedMediaItem>()

            // 1. Ekstrak format standar (Video + Audio tergabung, biasanya 360p atau 720p)
            val formats = streamingData.optJSONArray("formats")
            if (formats != null) {
                for (i in 0 until formats.length()) {
                    val format = formats.getJSONObject(i)
                    val downloadUrl = format.optString("url")
                    if (downloadUrl.isNotEmpty()) {
                        val mimeType = format.optString("mimeType")
                        val qualityLabel = format.optString("qualityLabel")
                        mediaItems.add(
                            ScrapedMediaItem(
                                downloadUrl = downloadUrl,
                                title = title,
                                mimeType = mimeType,
                                resolution = qualityLabel,
                                isAdaptive = false
                            )
                        )
                    }
                }
            }

            // 2. Ekstrak format adaptif (Hanya Video atau Hanya Audio terpisah untuk resolusi tinggi)
            val adaptiveFormats = streamingData.optJSONArray("adaptiveFormats")
            if (adaptiveFormats != null) {
                for (i in 0 until adaptiveFormats.length()) {
                    val format = adaptiveFormats.getJSONObject(i)
                    val downloadUrl = format.optString("url")
                    if (downloadUrl.isNotEmpty()) {
                        val mimeType = format.optString("mimeType")
                        val isVideo = mimeType.startsWith("video")
                        val resolution = if (isVideo) format.optString("qualityLabel") else "Audio"
                        mediaItems.add(
                            ScrapedMediaItem(
                                downloadUrl = downloadUrl,
                                title = title,
                                mimeType = mimeType,
                                resolution = resolution,
                                isAdaptive = true
                            )
                        )
                    }
                }
            }

            if (mediaItems.isEmpty()) {
                ScrapeResult.Error("Tidak ada tautan download langsung yang tersedia (mungkin dilindungi Cipher Signature).")
            } else {
                ScrapeResult.Success(mediaItems)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            ScrapeResult.Error("Error saat melakukan scraping: ${e.localizedMessage}")
        }
    }

    private fun extractVideoId(url: String): String? {
        val pattern = "(?<=watch\\?v=|/videos/|embed/|youtu.be/|/v/|/e/|watch\\?v%3D|watch\\?feature=player_embedded&v=|%2Fvideos%2F|embed%\u200B2F|youtu.be%\u200B2F|v%3D)[^#&?\\n]*"
        val compiledPattern = Pattern.compile(pattern)
        val matcher = compiledPattern.matcher(url)
        return if (matcher.find()) matcher.group() else null
    }

    private fun extractPlayerResponse(html: String): String? {
        val patterns = listOf(
            "ytInitialPlayerResponse\\s*=\\s*(\\{.*?\\});",
            "var ytInitialPlayerResponse\\s*=\\s*(\\{.*?\\});",
            "window\\[\"ytInitialPlayerResponse\"\\]\\s*=\\s*(\\{.*?\\});"
        )
        for (patternStr in patterns) {
            val pattern = Pattern.compile(patternStr)
            val matcher = pattern.matcher(html)
            if (matcher.find()) {
                return matcher.group(1)
            }
        }
        return null
    }
}
