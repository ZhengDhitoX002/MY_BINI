package com.mybini.app.data.downloader

import android.content.Context
import android.content.Intent
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters

class DownloadWorker(
    private val context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        val downloadId = inputData.getString(DownloadService.EXTRA_DOWNLOAD_ID) ?: return Result.failure()
        val url = inputData.getString(DownloadService.EXTRA_URL) ?: return Result.failure()
        val filePath = inputData.getString(DownloadService.EXTRA_FILE_PATH) ?: return Result.failure()
        val title = inputData.getString(DownloadService.EXTRA_TITLE) ?: "Downloading"

        // Pemicu Foreground Service secara aman untuk download luring
        val intent = Intent(context, DownloadService::class.java).apply {
            action = DownloadService.ACTION_START_DOWNLOAD
            putExtra(DownloadService.EXTRA_DOWNLOAD_ID, downloadId)
            putExtra(DownloadService.EXTRA_URL, url)
            putExtra(DownloadService.EXTRA_FILE_PATH, filePath)
            putExtra(DownloadService.EXTRA_TITLE, title)
        }

        try {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            return Result.retry()
        }
        return Result.success()
    }
}
