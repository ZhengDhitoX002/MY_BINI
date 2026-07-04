package com.mybini.app.data.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "download_history")
data class DownloadItem(
    @PrimaryKey val id: String = java.util.UUID.randomUUID().toString(),
    val title: String,
    val mediaType: String, // "VIDEO" or "AUDIO"
    val sizeBytes: Long,
    val filePath: String,
    val sourcePlatform: String = "TORRENT",
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "private_vault")
data class VaultItem(
    @PrimaryKey val id: String,
    val originalTitle: String,
    val encryptedFilePath: String,
    val initVector: String, // Base64 encoded Initialization Vector untuk AES-GCM
    val sizeBytes: Long,
    val dateAdded: Long
)
