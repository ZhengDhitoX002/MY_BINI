package com.mybini.app.data.downloader

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream

class DynamicAssetManager(private val okHttpClient: OkHttpClient) {

    suspend fun downloadWhisperModel(
        context: Context,
        modelUrl: String,
        onProgress: (Float) -> Unit
    ): File? = withContext(Dispatchers.IO) {
        val targetFile = File(context.filesDir, "whisper-tiny.tflite")
        // Jika file sudah ada dan ukurannya valid (> 10MB), tidak perlu unduh ulang
        if (targetFile.exists() && targetFile.length() > 10000000) {
            return@withContext targetFile
        }

        try {
            val request = Request.Builder().url(modelUrl).build()
            val response = okHttpClient.newCall(request).execute()
            if (!response.isSuccessful) return@withContext null

            val body = response.body ?: return@withContext null
            val contentLength = body.contentLength()
            val inputStream = body.byteStream()
            val outputStream = FileOutputStream(targetFile)

            val buffer = ByteArray(4096)
            var bytesRead: Long = 0
            var read: Int

            while (inputStream.read(buffer).also { read = it } != -1) {
                outputStream.write(buffer, 0, read)
                bytesRead += read
                if (contentLength > 0) {
                    onProgress(bytesRead.toFloat() / contentLength)
                }
            }

            outputStream.flush()
            outputStream.close()
            inputStream.close()
            targetFile
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
