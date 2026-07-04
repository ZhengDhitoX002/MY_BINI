package com.mybini.app.data.downloader

import android.content.Context
import com.mybini.app.data.database.DownloadItem
import com.mybini.app.data.database.MyBiniDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File

object TorrentEngine {
    private var isNativeLoaded = false

    init {
        try {
            // Mencoba memuat library native jLibtorrent jika tersedia di sistem
            System.loadLibrary("jlibtorrent")
            isNativeLoaded = true
        } catch (e: UnsatisfiedLinkError) {
            // Fallback aman jika berjalan di emulator dengan arsitektur CPU berbeda
            isNativeLoaded = false
        }
    }

    fun startDownload(
        context: Context,
        magnetUrl: String,
        onProgress: (progress: Float, speedMbs: Float, peers: Int, seeds: Int, status: String) -> Unit
    ) {
        val scope = CoroutineScope(Dispatchers.IO)
        scope.launch {
            // Simulasi siklus hidup unduhan torrent luring yang stabil
            var progress = 0.0f
            val fileTitle = "Big Buck Bunny (Torrent)"
            val targetFile = File(context.getExternalFilesDir(null), "big_buck_bunny_torrent.mp4")

            while (progress < 1.0f) {
                delay(1000)
                progress += 0.1f
                if (progress > 1.0f) progress = 1.0f
                
                val speed = (1.5f + (Math.random() * 1.7f)).toFloat()
                val peers = (5..15).random()
                val seeds = (3..8).random()
                
                onProgress(progress, speed, peers, seeds, "Downloading")
            }

            // Simpan hasil ke database Room jika selesai
            try {
                val db = MyBiniDatabase.getDatabase(context, ByteArray(32), "mybini_db")
                val downloadDao = db.downloadDao()
                
                // Buat dummy file jika belum ada
                if (!targetFile.exists()) {
                    targetFile.createNewFile()
                }

                val item = DownloadItem(
                    title = fileTitle,
                    filePath = targetFile.absolutePath,
                    mediaType = "Video (Torrent)",
                    sizeBytes = 250 * 1024 * 1024L
                )
                downloadDao.insertDownload(item)
                onProgress(1.0f, 0f, 0, 0, "Finished")
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
