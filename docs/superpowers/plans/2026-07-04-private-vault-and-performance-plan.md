# Bilik Rahasia & Optimasi Performa "MY BINI" Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Mengimplementasikan fitur Bilik Rahasia (Private Vault) terenkripsi AES-256-GCM lokal dengan streaming dekripsi di RAM (ExoPlayer) dan pemuatan dinamis model AI luring untuk optimasi performa Android 14+.

**Architecture:** Menerapkan enkripsi hardware-backed via Android Keystore & BiometricPrompt, Room database terenkripsi SQLCipher dengan fitur Decoy PIN, pemutar media ExoPlayer terintegrasi dengan kustom `AesGcmDataSource` untuk pemutaran memori langsung, serta `DynamicAssetManager` untuk mengunduh model Whisper secara dinamis.

**Tech Stack:** Kotlin, Jetpack Compose, Room Database, SQLCipher, Biometric API, Media3 ExoPlayer, OkHttp.

## Global Constraints
- Target platform: Android 14 (API 34) dan di atasnya (Minimum SDK = 34).
- Keamanan kunci enkripsi: AES-256-GCM untuk file, PBKDF2 (10.000 iterasi, SHA-256) untuk derivasi kunci PIN.
- Database: Room terenkripsi dengan SQLCipher.
- Memory safety: Dekripsi file dilakukan 100% di dalam RAM secara on-the-fly saat diputar, tanpa menulis cache file biasa ke disk.

---

### Task 1: Room DB & SQLCipher Setup (Decoy Database Configuration)

**Files:**
- Create: `app/src/main/java/com/mybini/app/data/database/MyBiniDatabase.kt`
- Create: `app/src/main/java/com/mybini/app/data/database/Entities.kt`
- Modify: `app/build.gradle.kts:80-90`

**Interfaces:**
- Consumes: None (Langkah dasar)
- Produces: `MyBiniDatabase` (akses database terenkripsi lokal untuk riwayat & vault)

- [ ] **Step 1: Tambahkan dependensi SQLCipher ke Gradle**
  
  Buka [app/build.gradle.kts](file:///C:/Users/gamem/Downloads/MY%20BINI/app/build.gradle.kts) dan tambahkan pustaka SQLCipher:
  ```kotlin
  // Di dalam blok dependencies {}
  implementation("isActive.sqlite:sqlite-android:3.42.0") // SQLCipher sqlite wrapper
  implementation("org.zetetic:sqlcipher-android:4.5.4")
  ```

- [ ] **Step 2: Buat entitas database di `Entities.kt`**
  
  Tulis berkas `app/src/main/java/com/mybini/app/data/database/Entities.kt`:
  ```kotlin
  package com.mybini.app.data.database

  import androidx.room.Entity
  import androidx.room.PrimaryKey

  @Entity(tableName = "download_history")
  data class DownloadItem(
      @PrimaryKey val id: String,
      val title: String,
      val mediaType: String, // "VIDEO" or "AUDIO"
      val sizeBytes: Long,
      val filePath: String,
      val sourcePlatform: String, // "YOUTUBE", "INSTAGRAM", "SPOTIFY"
      val timestamp: Long
  )

  @Entity(tableName = "private_vault")
  data class VaultItem(
      @PrimaryKey val id: String,
      val originalTitle: String,
      val encryptedFilePath: String,
      val initVector: String, // Base64 IV untuk AES-GCM
      val sizeBytes: Long,
      val dateAdded: Long
  )
  ```

- [ ] **Step 3: Buat kelas Database Room `MyBiniDatabase.kt`**
  
  Tulis berkas `app/src/main/java/com/mybini/app/data/database/MyBiniDatabase.kt` dengan integrasi SQLCipher factory:
  ```kotlin
  package com.mybini.app.data.database

  import android.content.Context
  import androidx.room.Database
  import androidx.room.Room
  import androidx.room.RoomDatabase
  import androidx.room.TypeConverters
  import net.sqlcipher.database.SupportFactory
  import net.sqlcipher.database.SQLiteDatabase

  @Database(entities = [DownloadItem::class, VaultItem::class], version = 1, exportSchema = false)
  abstract class MyBiniDatabase : RoomDatabase() {
      // Deklarasi DAO di sini (misal: abstract fun downloadDao(): DownloadDao)
      
      companion object {
          @Volatile
          private var INSTANCE: MyBiniDatabase? = null

          fun getDatabase(context: Context, passphrase: ByteArray, dbName: String): MyBiniDatabase {
              return INSTANCE ?: synchronized(this) {
                  SQLiteDatabase.loadLibs(context)
                  val factory = SupportFactory(passphrase)
                  val instance = Room.databaseBuilder(
                      context.applicationContext,
                      MyBiniDatabase::class.java,
                      dbName
                  )
                  .openHelperFactory(factory)
                  .fallbackToDestructiveMigration()
                  .build()
                  INSTANCE = instance
                  instance
              }
          }
      }
  }
  ```

- [ ] **Step 4: Commit perubahan**
  ```bash
  git add app/build.gradle.kts app/src/main/java/com/mybini/app/data/database/
  git commit -m "feat: setup Room Database with SQLCipher encryption support"
  ```

---

### Task 2: PBKDF2 Key Derivation & Android Keystore Biometrics

**Files:**
- Create: `app/src/main/java/com/mybini/app/data/downloader/SecurityManager.kt`

**Interfaces:**
- Consumes: None (Utilitas Keamanan)
- Produces: `SecurityManager.deriveKeyFromPasscode(passcode: String, salt: ByteArray)` dan integrasi Keystore.

- [ ] **Step 1: Buat kelas `SecurityManager.kt`**
  
  Tulis berkas `app/src/main/java/com/mybini/app/data/downloader/SecurityManager.kt`:
  ```kotlin
  package com.mybini.app.data.downloader

  import java.security.spec.KeySpec
  import javax.crypto.SecretKeyFactory
  import javax.crypto.spec.PBEKeySpec
  import javax.crypto.spec.SecretKeySpec
  import java.security.SecureRandom

  object SecurityManager {
      private const val ITERATIONS = 10000
      private const val KEY_LENGTH = 256

      fun generateSalt(): ByteArray {
          val random = SecureRandom()
          val salt = ByteArray(16)
          random.nextBytes(salt)
          return salt
      }

      fun deriveKeyFromPasscode(passcode: CharArray, salt: ByteArray): ByteArray {
          val spec: KeySpec = PBEKeySpec(passcode, salt, ITERATIONS, KEY_LENGTH)
          val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
          val keyBytes = factory.generateSecret(spec).encoded
          return keyBytes
      }
  }
  ```

- [ ] **Step 2: Commit perubahan**
  ```bash
  git add app/src/main/java/com/mybini/app/data/downloader/SecurityManager.kt
  git commit -m "feat: add SecurityManager utility for PBKDF2 key derivation"
  ```

---

### Task 3: ExoPlayer Custom `AesGcmDataSource` for Streaming Decryption

**Files:**
- Create: `app/src/main/java/com/mybini/app/media/AesGcmDataSource.kt`

**Interfaces:**
- Consumes: None (Integrasi Media3 DataSource)
- Produces: `AesGcmDataSource` untuk menyuplai byte data ExoPlayer terenkripsi luring.

- [ ] **Step 1: Buat berkas `AesGcmDataSource.kt`**
  
  Tulis berkas `app/src/main/java/com/mybini/app/media/AesGcmDataSource.kt` untuk melakukan dekripsi on-the-fly di RAM:
  ```kotlin
  package com.mybini.app.media

  import android.net.Uri
  import androidx.media3.datasource.BaseDataSource
  import androidx.media3.datasource.DataSpec
  import androidx.media3.common.C
  import java.io.RandomAccessFile
  import javax.crypto.Cipher
  import javax.crypto.spec.GCMParameterSpec
  import javax.crypto.spec.SecretKeySpec

  class AesGcmDataSource(
      private val secretKey: ByteArray
  ) : BaseDataSource(true) {

      private var file: RandomAccessFile? = null
      private var uri: Uri? = null
      private var bytesRemaining: Long = 0

      override fun open(dataSpec: DataSpec): Long {
          uri = dataSpec.uri
          val filePath = uri?.path ?: throw IllegalArgumentException("URI path is null")
          file = RandomAccessFile(filePath, "r")
          
          val fileLength = file!!.length()
          val position = dataSpec.position
          file!!.seek(position)
          
          bytesRemaining = if (dataSpec.length == C.LENGTH_UNSET.toLong()) {
              fileLength - position
          } else {
              dataSpec.length
          }
          
          transferStarted(dataSpec)
          return bytesRemaining
      }

      override fun read(buffer: ByteArray, offset: Int, readLength: Int): Int {
          if (readLength == 0) return 0
          if (bytesRemaining == 0L) return C.RESULT_END_OF_INPUT

          val file = file ?: return C.RESULT_END_OF_INPUT
          val bytesToRead = Math.min(bytesRemaining, readLength.toLong()).toInt()
          
          // Baca potongan byte terenkripsi
          val encryptedData = ByteArray(bytesToRead)
          val bytesRead = file.read(encryptedData, 0, bytesToRead)
          
          if (bytesRead == -1) return C.RESULT_END_OF_INPUT

          // Dekripsi byte yang dibaca menggunakan AES-GCM
          val decryptedData = decryptChunk(encryptedData)
          System.arraycopy(decryptedData, 0, buffer, offset, bytesRead)

          bytesRemaining -= bytesRead
          bytesTransferred(bytesRead)
          return bytesRead
      }

      override fun getUri(): Uri? = uri

      override fun close() {
          file?.close()
          file = null
          transferEnded()
      }

      private fun decryptChunk(encrypted: ByteArray): ByteArray {
          val cipher = Cipher.getInstance("AES/GCM/NoPadding")
          val keySpec = SecretKeySpec(secretKey, "AES")
          // Menggunakan IV dummy statis untuk potongan stream karena ExoPlayer membaca byte acak
          val gcmSpec = GCMParameterSpec(128, ByteArray(12)) 
          cipher.init(Cipher.DECRYPT_MODE, keySpec, gcmSpec)
          return cipher.doFinal(encrypted)
      }
  }
  ```

- [ ] **Step 2: Commit perubahan**
  ```bash
  git add app/src/main/java/com/mybini/app/media/AesGcmDataSource.kt
  git commit -m "feat: implement AesGcmDataSource for in-memory on-the-fly decryption"
  ```

---

### Task 4: Dynamic Asset Delivery (AI Model Downloader)

**Files:**
- Create: `app/src/main/java/com/mybini/app/data/downloader/DynamicAssetManager.kt`

**Interfaces:**
- Consumes: `OkHttpClient` (untuk koneksi HTTP)
- Produces: `DynamicAssetManager.downloadWhisperModel(context: Context, onProgress: (Float) -> Unit, onSuccess: () -> Unit)`

- [ ] **Step 1: Buat berkas `DynamicAssetManager.kt`**
  
  Tulis berkas `app/src/main/java/com/mybini/app/data/downloader/DynamicAssetManager.kt`:
  ```kotlin
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
          if (targetFile.exists() && targetFile.length() > 10000000) {
              return@withContext targetFile
          }

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
      }
  }
  ```

- [ ] **Step 2: Commit perubahan**
  ```bash
  git add app/src/main/java/com/mybini/app/data/downloader/DynamicAssetManager.kt
  git commit -m "feat: add DynamicAssetManager for runtime Whisper AI model download"
  ```
