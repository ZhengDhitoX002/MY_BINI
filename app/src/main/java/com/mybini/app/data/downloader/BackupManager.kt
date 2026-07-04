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
