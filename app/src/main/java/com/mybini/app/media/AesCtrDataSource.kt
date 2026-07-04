package com.mybini.app.media

import android.net.Uri
import androidx.media3.common.C
import androidx.media3.datasource.BaseDataSource
import androidx.media3.datasource.DataSpec
import java.io.RandomAccessFile
import java.math.BigInteger
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

class AesCtrDataSource(
    private val secretKey: ByteArray,
    private val baseIv: ByteArray
) : BaseDataSource(true) {

    private var file: RandomAccessFile? = null
    private var uri: Uri? = null
    private var bytesRemaining: Long = 0
    private var filePosition: Long = 0

    override fun open(dataSpec: DataSpec): Long {
        uri = dataSpec.uri
        val filePath = uri?.path ?: throw IllegalArgumentException("URI path is null")
        file = RandomAccessFile(filePath, "r")
        
        val fileLength = file!!.length()
        filePosition = dataSpec.position
        file!!.seek(filePosition)
        
        bytesRemaining = if (dataSpec.length == C.LENGTH_UNSET.toLong()) {
            fileLength - filePosition
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
        
        val encryptedData = ByteArray(bytesToRead)
        val bytesRead = file.read(encryptedData, 0, bytesToRead)
        
        if (bytesRead == -1) return C.RESULT_END_OF_INPUT

        // Dekripsi menggunakan AES-CTR untuk filePosition saat ini
        val decryptedData = decryptCtr(encryptedData, filePosition)
        System.arraycopy(decryptedData, 0, buffer, offset, bytesRead)

        filePosition += bytesRead
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

    private fun decryptCtr(encrypted: ByteArray, position: Long): ByteArray {
        val cipher = Cipher.getInstance("AES/CTR/NoPadding")
        val keySpec = SecretKeySpec(secretKey, "AES")
        
        // Hitung offset blok 16-byte
        val blockOffset = position / 16
        val remainder = (position % 16).toInt()
        
        // Hitung IV baru: IV_baru = IV_dasar + blockOffset
        val ivBigInt = BigInteger(1, baseIv)
        val newIvBigInt = ivBigInt.add(BigInteger.valueOf(blockOffset))
        val newIvBytes = newIvBigInt.toByteArray()
        
        // Normalisasi IV agar selalu berukuran tepat 16 byte
        val finalIvBytes = ByteArray(16)
        val srcOffset = if (newIvBytes.size > 16) newIvBytes.size - 16 else 0
        val destOffset = if (newIvBytes.size < 16) 16 - newIvBytes.size else 0
        System.arraycopy(newIvBytes, srcOffset, finalIvBytes, destOffset, Math.min(newIvBytes.size, 16))
        
        cipher.init(Cipher.DECRYPT_MODE, keySpec, IvParameterSpec(finalIvBytes))
        
        // Tangani jika pemutaran/seek tidak sejajar dengan batas kelipatan 16-byte
        if (remainder > 0) {
            val paddedEncrypted = ByteArray(remainder + encrypted.size)
            System.arraycopy(encrypted, 0, paddedEncrypted, remainder, encrypted.size)
            val paddedDecrypted = cipher.doFinal(paddedEncrypted)
            val decrypted = ByteArray(encrypted.size)
            System.arraycopy(paddedDecrypted, remainder, decrypted, 0, encrypted.size)
            return decrypted
        }
        
        return cipher.doFinal(encrypted)
    }
}
