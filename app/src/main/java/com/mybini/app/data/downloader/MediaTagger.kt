package com.mybini.app.data.downloader

import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.io.RandomAccessFile

class MediaTagger(private val client: OkHttpClient) {

    /**
     * Menyuntikkan tag ID3v2 dasar (Judul, Artis, Album Art) ke berkas MP3 hasil download.
     */
    fun tagAudioFile(
        audioFile: File,
        title: String,
        artist: String,
        albumArtUrl: String?
    ) {
        try {
            val artBytes = if (!albumArtUrl.isNullOrEmpty()) {
                downloadAlbumArt(albumArtUrl)
            } else {
                null
            }

            // Baca konten MP3 asli
            val originalBytes = audioFile.readBytes()

            // Buat berkas baru dengan tag ID3v2.3 di awal berkas
            val outputStream = FileOutputStream(audioFile)

            // Header ID3v2.3: "ID3" + version (03 00) + flags (00) + size (placeholder)
            val id3Header = byteArrayOf(0x49, 0x44, 0x33, 0x03, 0x00, 0x00)
            outputStream.write(id3Header)

            val frameDataStream = java.io.ByteArrayOutputStream()

            // 1. Tulis Frame Judul (TIT2)
            writeTextFrame(frameDataStream, "TIT2", title)

            // 2. Tulis Frame Artis (TPE1)
            writeTextFrame(frameDataStream, "TPE1", artist)

            // 3. Tulis Frame Album Art (APIC) jika tersedia
            if (artBytes != null) {
                writePictureFrame(frameDataStream, artBytes)
            }

            val frameBytes = frameDataStream.toByteArray()
            val size = frameBytes.size

            // Hitung ukuran tag untuk dimasukkan ke header (format synchsafe integer 4-byte)
            val sizeBytes = ByteArray(4)
            sizeBytes[0] = ((size shr 21) and 0x7F).toByte()
            sizeBytes[1] = ((size shr 14) and 0x7F).toByte()
            sizeBytes[2] = ((size shr 7) and 0x7F).toByte()
            sizeBytes[3] = (size and 0x7F).toByte()

            outputStream.write(sizeBytes)
            outputStream.write(frameBytes)

            // Tulis ulang sisa data MP3 asli
            outputStream.write(originalBytes)
            outputStream.flush()
            outputStream.close()

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun writeTextFrame(output: java.io.ByteArrayOutputStream, frameId: String, text: String) {
        val textBytes = text.toByteArray(Charsets.UTF_8)
        
        // Frame ID (4 bytes)
        output.write(frameId.toByteArray(Charsets.US_ASCII))
        
        // Size (4 bytes, excludes frame header)
        val size = textBytes.size + 1 // +1 untuk encoding byte
        output.write(byteArrayOf(
            ((size shr 24) and 0xFF).toByte(),
            ((size shr 16) and 0xFF).toByte(),
            ((size shr 8) and 0xFF).toByte(),
            (size and 0xFF).toByte()
        ))
        
        // Flags (2 bytes)
        output.write(byteArrayOf(0x00, 0x00))
        
        // Text encoding: 0x03 untuk UTF-8
        output.write(0x03)
        output.write(textBytes)
    }

    private fun writePictureFrame(output: java.io.ByteArrayOutputStream, pictureBytes: ByteArray) {
        // Frame ID (4 bytes): APIC
        output.write("APIC".toByteArray(Charsets.US_ASCII))

        val mimeType = "image/jpeg".toByteArray(Charsets.US_ASCII)
        val size = 1 + mimeType.size + 1 + 1 + 1 + pictureBytes.size // encoding + mime + null + pictureType + descNull + data

        // Size (4 bytes)
        output.write(byteArrayOf(
            ((size shr 24) and 0xFF).toByte(),
            ((size shr 16) and 0xFF).toByte(),
            ((size shr 8) and 0xFF).toByte(),
            (size and 0xFF).toByte()
        ))

        // Flags (2 bytes)
        output.write(byteArrayOf(0x00, 0x00))

        // Text encoding: 0x03 (UTF-8)
        output.write(0x03)

        // MIME Type + null terminator
        output.write(mimeType)
        output.write(0x00)

        // Picture type: 0x03 (Cover / Front)
        output.write(0x03)

        // Description null terminator (empty description)
        output.write(0x00)

        // Picture binary data
        output.write(pictureBytes)
    }

    private fun downloadAlbumArt(url: String): ByteArray? {
        return try {
            val request = Request.Builder().url(url).build()
            val response = client.newCall(request).execute()
            if (response.isSuccessful) {
                response.body?.bytes()
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }
}
