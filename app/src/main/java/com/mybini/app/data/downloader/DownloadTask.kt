package com.mybini.app.data.downloader

import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.RandomAccessFile
import java.io.InputStream

class DownloadTask(
    private val client: OkHttpClient,
    private val downloadUrl: String,
    private val targetFilePath: String,
    private val listener: DownloadListener
) {

    interface DownloadListener {
        fun onProgress(progress: Int, speedKbps: Long)
        fun onSuccess()
        fun onError(message: String)
    }

    private var isPaused = false
    private var isCancelled = false

    fun pause() {
        isPaused = true
    }

    fun cancel() {
        isCancelled = true
    }

    fun run() {
        var inputStream: InputStream? = null
        var randomAccessFile: RandomAccessFile? = null
        try {
            val targetFile = File(targetFilePath)
            val existingLength = if (targetFile.exists()) targetFile.length() else 0L

            // Buat request dengan Range Header untuk mendukung resume unduhan
            val request = Request.Builder()
                .url(downloadUrl)
                .header("Range", "bytes=$existingLength-")
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)")
                .build()

            val response = client.newCall(request).execute()
            if (!response.isSuccessful && response.code != 206) {
                listener.onError("Server merespon dengan kode error: ${response.code}")
                return
            }

            val body = response.body ?: throw Exception("Body respon jaringan kosong")
            val totalBytes = body.contentLength() + existingLength
            
            inputStream = body.byteStream()
            randomAccessFile = RandomAccessFile(targetFile, "rw")
            randomAccessFile.seek(existingLength)

            val buffer = ByteArray(8192)
            var bytesRead: Int
            var lastProgressTime = System.currentTimeMillis()
            var bytesDownloadedInInterval = 0L
            var currentDownloaded = existingLength

            while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                if (isCancelled) {
                    listener.onError("Unduhan dibatalkan")
                    return
                }
                if (isPaused) {
                    listener.onError("Unduhan dijeda")
                    return
                }

                randomAccessFile.write(buffer, 0, bytesRead)
                currentDownloaded += bytesRead
                bytesDownloadedInInterval += bytesRead

                val currentTime = System.currentTimeMillis()
                val timePassed = currentTime - lastProgressTime
                
                // Update progres setiap 500ms
                if (timePassed >= 500) {
                    val progress = ((currentDownloaded * 100) / totalBytes).toInt()
                    val speedKbps = (bytesDownloadedInInterval * 1000 / timePassed) / 1024
                    listener.onProgress(progress, speedKbps)
                    
                    lastProgressTime = currentTime
                    bytesDownloadedInInterval = 0
                }
            }

            listener.onSuccess()

        } catch (e: Exception) {
            e.printStackTrace()
            listener.onError("Gagal mengunduh: ${e.localizedMessage}")
        } finally {
            inputStream?.close()
            randomAccessFile?.close()
        }
    }
}
