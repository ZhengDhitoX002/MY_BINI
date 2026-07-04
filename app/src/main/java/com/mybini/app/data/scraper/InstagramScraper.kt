package com.mybini.app.data.scraper

import android.webkit.CookieManager
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.net.URI

class InstagramScraper(private val client: OkHttpClient) : MediaScraper {

    override suspend fun extractMediaUrl(url: String): ScrapeResult {
        return try {
            val shortcode = extractShortcode(url) ?: return ScrapeResult.Error("URL Instagram tidak valid.")
            
            // Format API Instagram modern dengan __d=dis untuk memaksa JSON output
            val jsonApiUrl = "https://www.instagram.com/p/$shortcode/?__a=1&__d=dis"

            // Ambil cookie Instagram dari WebView Android secara dinamis
            val cookieManager = CookieManager.getInstance()
            val cookies = cookieManager.getCookie("https://www.instagram.com") ?: ""

            val requestBuilder = Request.Builder()
                .url(jsonApiUrl)
                .header("User-Agent", "Mozilla/5.0 (iPhone; CPU iPhone OS 16_5 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/16.5 Mobile/15E148 Safari/604.1")
                .header("Accept", "*/*")
                .header("Sec-Fetch-Site", "same-origin")

            if (cookies.isNotEmpty()) {
                requestBuilder.header("Cookie", cookies)
            }

            val request = requestBuilder.build()
            val response = client.newCall(request).execute()
            
            if (!response.isSuccessful) {
                return ScrapeResult.Error("Instagram menolak akses: Code ${response.code} (Pastikan Anda sudah login Instagram di browser MY BINI).")
            }

            val responseBody = response.body?.string() ?: return ScrapeResult.Error("Respon Instagram kosong.")
            val jsonObject = JSONObject(responseBody)
            
            val mediaItems = mutableListOf<ScrapedMediaItem>()

            // Cara 1: Membaca struktur dari array "items"
            val items = jsonObject.optJSONArray("items")
            if (items != null && items.length() > 0) {
                val firstItem = items.getJSONObject(0)
                val title = firstItem.optJSONObject("caption")?.optString("text") ?: "Instagram Media"
                
                // Cari video
                val videoVersions = firstItem.optJSONArray("video_versions")
                if (videoVersions != null && videoVersions.length() > 0) {
                    val bestVideo = videoVersions.getJSONObject(0)
                    val downloadUrl = bestVideo.optString("url")
                    mediaItems.add(
                        ScrapedMediaItem(
                            downloadUrl = downloadUrl,
                            title = title,
                            mimeType = "video/mp4",
                            resolution = "HD"
                        )
                    )
                } else {
                    // Cari gambar jika bukan video
                    val imageVersions = firstItem.optJSONObject("image_versions2")
                    val candidates = imageVersions?.optJSONArray("candidates")
                    if (candidates != null && candidates.length() > 0) {
                        val bestImage = candidates.getJSONObject(0)
                        val downloadUrl = bestImage.optString("url")
                        mediaItems.add(
                            ScrapedMediaItem(
                                downloadUrl = downloadUrl,
                                title = title,
                                mimeType = "image/jpeg",
                                resolution = "Original"
                            )
                        )
                    }
                }
            } else {
                // Cara 2: Membaca struktur alternatif "graphql / shortcode_media"
                val graphql = jsonObject.optJSONObject("graphql")
                val shortcodeMedia = graphql?.optJSONObject("shortcode_media")
                if (shortcodeMedia != null) {
                    val title = shortcodeMedia.optJSONObject("edge_media_to_caption")
                        ?.optJSONArray("edges")?.optJSONObject(0)
                        ?.optJSONObject("node")?.optString("text") ?: "Instagram Media"
                    
                    val isVideo = shortcodeMedia.optBoolean("is_video", false)
                    if (isVideo) {
                        val downloadUrl = shortcodeMedia.optString("video_url")
                        mediaItems.add(
                            ScrapedMediaItem(
                                downloadUrl = downloadUrl,
                                title = title,
                                mimeType = "video/mp4",
                                resolution = "HD"
                            )
                        )
                    } else {
                        val downloadUrl = shortcodeMedia.optString("display_url")
                        mediaItems.add(
                            ScrapedMediaItem(
                                downloadUrl = downloadUrl,
                                title = title,
                                mimeType = "image/jpeg",
                                resolution = "Original"
                            )
                        )
                    }
                }
            }

            if (mediaItems.isEmpty()) {
                ScrapeResult.Error("Gagal menemukan tautan download media. Instagram mungkin membatasi akun Anda.")
            } else {
                ScrapeResult.Success(mediaItems)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            ScrapeResult.Error("Error Instagram Scraper: ${e.localizedMessage}")
        }
    }

    private fun extractShortcode(url: String): String? {
        return try {
            val uri = URI(url)
            val path = uri.path
            val parts = path.split("/").filter { it.isNotEmpty() }
            val index = parts.indexOfFirst { it == "p" || it == "reel" || it == "reels" || it == "tv" }
            if (index != -1 && index + 1 < parts.size) {
                parts[index + 1]
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }
}
