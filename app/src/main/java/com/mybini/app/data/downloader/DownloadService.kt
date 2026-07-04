package com.mybini.app.data.downloader

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.mybini.app.R
import com.mybini.app.data.database.MyBiniDatabase
import com.mybini.app.data.database.DownloadItem
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import okhttp3.OkHttpClient

class DownloadService : Service() {

    companion object {
        private const val CHANNEL_ID = "MyBiniDownloadChannel"
        private const val NOTIFICATION_ID = 101
        
        const val ACTION_START_DOWNLOAD = "com.mybini.app.ACTION_START_DOWNLOAD"
        const val ACTION_PAUSE_DOWNLOAD = "com.mybini.app.ACTION_PAUSE_DOWNLOAD"
        const val ACTION_CANCEL_DOWNLOAD = "com.mybini.app.ACTION_CANCEL_DOWNLOAD"
        
        const val EXTRA_DOWNLOAD_ID = "extra_download_id"
        const val EXTRA_URL = "extra_url"
        const val EXTRA_FILE_PATH = "extra_file_path"
        const val EXTRA_TITLE = "extra_title"
    }

    private lateinit var notificationManager: NotificationManager

    override fun onCreate() {
        super.onCreate()
        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action
        val downloadId = intent?.getStringExtra(EXTRA_DOWNLOAD_ID) ?: ""

        when (action) {
            ACTION_START_DOWNLOAD -> {
                val url = intent?.getStringExtra(EXTRA_URL) ?: ""
                val filePath = intent?.getStringExtra(EXTRA_FILE_PATH) ?: ""
                val title = intent?.getStringExtra(EXTRA_TITLE) ?: "Downloading File"
                
                // Menjalankan service di foreground sesuai standar Android 14
                startForegroundService(title)
                
                // Mulai tugas download (Task 2.6 akan mengimplementasikan logika download thread-nya)
                startDownloadTask(downloadId, url, filePath, title)
            }
            ACTION_PAUSE_DOWNLOAD -> {
                pauseDownloadTask(downloadId)
            }
            ACTION_CANCEL_DOWNLOAD -> {
                cancelDownloadTask(downloadId)
            }
        }

        return START_NOT_STICKY
    }

    private fun startForegroundService(title: String) {
        val notification = buildProgressNotification(title, 0, "0 KB/s")
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // Sesuai kepatuhan Android 14+ (Wajib tipe DATA_SYNC untuk unduhan file background)
            startForeground(
                NOTIFICATION_ID, 
                notification, 
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private var activeTask: DownloadTask? = null
    private val okHttpClient = OkHttpClient()

    private fun startDownloadTask(id: String, url: String, filePath: String, title: String) {
        activeTask?.cancel()
        
        val task = DownloadTask(
            client = okHttpClient,
            downloadUrl = url,
            targetFilePath = filePath,
            listener = object : DownloadTask.DownloadListener {
                override fun onProgress(progress: Int, speedKbps: Long) {
                    val speedText = if (speedKbps > 1024) "${speedKbps / 1024} MB/s" else "$speedKbps KB/s"
                    updateProgress(title, progress, speedText)
                }

                override fun onSuccess() {
                    // Simpan data unduhan sukses ke database Room luring
                    val db = MyBiniDatabase.getDatabase(applicationContext, ByteArray(32), "mybini_db")
                    val file = File(filePath)
                    val size = if (file.exists()) file.length() else 0L
                    
                    val platform = when {
                        url.contains("youtube") || url.contains("youtu.be") -> "YOUTUBE"
                        url.contains("instagram") -> "INSTAGRAM"
                        url.contains("spotify") -> "SPOTIFY"
                        else -> "WEB"
                    }
                    
                    val item = DownloadItem(
                        id = java.util.UUID.randomUUID().toString(),
                        title = title,
                        mediaType = if (filePath.endsWith(".mp3") || filePath.endsWith(".m4a")) "AUDIO" else "VIDEO",
                        sizeBytes = size,
                        filePath = filePath,
                        sourcePlatform = platform,
                        timestamp = System.currentTimeMillis()
                    )
                    
                    CoroutineScope(Dispatchers.IO).launch {
                        db.downloadDao().insertDownload(item)
                    }

                    stopForeground(STOP_FOREGROUND_REMOVE)
                    stopSelf()
                }

                override fun onError(message: String) {
                    stopForeground(STOP_FOREGROUND_REMOVE)
                    stopSelf()
                }
            }
        )
        
        activeTask = task
        Thread { task.run() }.start()
    }

    private fun pauseDownloadTask(id: String) {
        activeTask?.pause()
    }

    private fun cancelDownloadTask(id: String) {
        activeTask?.cancel()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun updateProgress(title: String, progress: Int, speed: String) {
        val notification = buildProgressNotification(title, progress, speed)
        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    private fun buildProgressNotification(title: String, progress: Int, speed: String): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText("Progress: $progress% | Speed: $speed")
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setProgress(100, progress, false)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Unduhan MY BINI",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Menampilkan progres unduhan file media MY BINI"
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
