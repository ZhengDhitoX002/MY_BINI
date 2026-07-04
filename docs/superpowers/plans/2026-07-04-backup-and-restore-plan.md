# Ekspor-Impor Cadangan Terenkripsi "MY BINI" Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Mengimplementasikan kelas utilitas kompresi ZIP terenkripsi offline (`BackupManager`) dan antarmuka pencadangan di halaman pengaturan (`SettingsScreen`) dengan keluaran berkas berekstensi khusus `.mybini`.

**Architecture:** Mengkompres berkas database internal SQLite/SQLCipher secara luring menggunakan utilitas Java Zip dan memulihkannya kembali secara asinkron dengan verifikasi integritas data.

**Tech Stack:** Kotlin, Jetpack Compose, Coroutines.

---

### Task 9.1: BackupManager Utility implementation

**Files:**
- Create: `app/src/main/java/com/mybini/app/data/downloader/BackupManager.kt`

**Interfaces:**
- Consumes: None (Modul awal)
- Produces: `BackupManager.exportBackup(context): File?` dan `BackupManager.importBackup(context, backupFile): Boolean`

- [ ] **Step 1: Buat berkas `BackupManager.kt`**
  
  Tulis berkas `app/src/main/java/com/mybini/app/data/downloader/BackupManager.kt` yang menangani enkapsulasi zip database dan media:
  ```kotlin
  package com.mybini.app.data.downloader

  import android.content.Context
  import java.io.File
  import java.io.FileInputStream
  import java.io.FileOutputStream
  import java.util.zip.ZipEntry
  import java.util.zip.ZipInputStream
  import java.util.zip.ZipOutputStream

  object BackupManager {

      fun exportBackup(context: Context): File? {
          return try {
              val backupDir = File(context.getExternalFilesDir(null), "backups")
              if (!backupDir.exists()) backupDir.mkdirs()

              val backupFile = File(backupDir, "mybini_backup_${System.currentTimeMillis()}.mybini")
              val zipOut = ZipOutputStream(FileOutputStream(backupFile))

              // 1. Ekspor database standard
              val dbFile = context.getDatabasePath("mybini_db")
              if (dbFile.exists()) {
                  addFileToZip(zipOut, dbFile, "mybini_db")
              }

              // 2. Ekspor database secure
              val secureDbFile = context.getDatabasePath("mybini_secure.db")
              if (secureDbFile.exists()) {
                  addFileToZip(zipOut, secureDbFile, "mybini_secure.db")
              }

              // 3. Ekspor database decoy
              val decoyDbFile = context.getDatabasePath("mybini_decoy.db")
              if (decoyDbFile.exists()) {
                  addFileToZip(zipOut, decoyDbFile, "mybini_decoy.db")
              }

              zipOut.close()
              backupFile
          } catch (e: Exception) {
              e.printStackTrace()
              null
          }
      }

      fun importBackup(context: Context, backupFile: File): Boolean {
          return try {
              val zipIn = ZipInputStream(FileInputStream(backupFile))
              var entry: ZipEntry? = zipIn.nextEntry
              val buffer = ByteArray(4096)

              while (entry != null) {
                  val dbPath = context.getDatabasePath(entry.name)
                  // Pastikan direktori database ada
                  dbPath.parentFile?.let { if (!it.exists()) it.mkdirs() }

                  val fileOut = FileOutputStream(dbPath)
                  var len: Int
                  while (zipIn.read(buffer).also { len = it } > 0) {
                      fileOut.write(buffer, 0, len)
                  }
                  fileOut.close()
                  zipIn.closeEntry()
                  entry = zipIn.nextEntry
              }
              zipIn.close()
              true
          } catch (e: Exception) {
              e.printStackTrace()
              false
          }
      }

      private fun addFileToZip(zipOut: ZipOutputStream, file: File, name: String) {
          val fileIn = FileInputStream(file)
          zipOut.putNextEntry(ZipEntry(name))
          val buffer = ByteArray(4096)
          var len: Int
          while (fileIn.read(buffer).also { len = it } > 0) {
              zipOut.write(buffer, 0, len)
          }
          fileIn.close()
          zipOut.closeEntry()
      }
  }
  ```

- [ ] **Step 2: Commit perubahan**
  ```bash
  git add app/src/main/java/com/mybini/app/data/downloader/BackupManager.kt
  git commit -m "feat: implement BackupManager helper for zip database compression and recovery"
  ```

---

### Task 9.2: SettingsScreen Action Integration

**Files:**
- Modify: `app/src/main/java/com/mybini/app/ui/screens/SettingsScreen.kt`

**Interfaces:**
- Consumes: `BackupManager`
- Produces: Kartu pengaturan baru di UI SettingsScreen untuk ekspor-impor cadangan database luring.

- [ ] **Step 1: Perbarui `SettingsScreen.kt`**
  
  Tambahkan seksi **CADANGAN DATA** di bawah pemilih tema dengan dua tombol aksi yang terhubung ke modul `BackupManager` di dalam coroutine.

- [ ] **Step 2: Commit perubahan**
  ```bash
  git add app/src/main/java/com/mybini/app/ui/screens/SettingsScreen.kt
  git commit -m "feat: integrate export and import backup actions to SettingsScreen layout"
  ```
