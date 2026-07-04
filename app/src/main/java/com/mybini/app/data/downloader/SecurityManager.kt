package com.mybini.app.data.downloader

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.security.KeyStore
import java.security.SecureRandom
import java.security.spec.KeySpec
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

object SecurityManager {
    private const val KEYSTORE_PROVIDER = "AndroidKeyStore"
    private const val KEY_ALIAS = "MyBiniMasterKeyAlias"
    private const val ITERATIONS = 10000
    private const val KEY_LENGTH = 256

    init {
        // Inisialisasi Android Keystore
        val keyStore = KeyStore.getInstance(KEYSTORE_PROVIDER).apply { load(null) }
        if (!keyStore.containsAlias(KEY_ALIAS)) {
            generateMasterKey()
        }
    }

    // 1. Derviasi Kunci PIN (PBKDF2)
    fun generateSalt(): ByteArray {
        val random = SecureRandom()
        val salt = ByteArray(16)
        random.nextBytes(salt)
        return salt
    }

    fun deriveKeyFromPasscode(passcode: CharArray, salt: ByteArray): ByteArray {
        val spec: KeySpec = PBEKeySpec(passcode, salt, ITERATIONS, KEY_LENGTH)
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        return factory.generateSecret(spec).encoded
    }

    // 2. Hardware-Backed Master Key (Android Keystore)
    private fun generateMasterKey() {
        val keyGenerator = KeyGenerator.getInstance(
            KeyProperties.KEY_ALGORITHM_AES,
            KEYSTORE_PROVIDER
        )
        val spec = KeyGenParameterSpec.Builder(
            KEY_ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setUserAuthenticationRequired(true) // Memerlukan Biometrik (Sidik Jari/Wajah)
            .setInvalidatedByBiometricEnrollment(true) // Kunci hancur jika sidik jari baru ditambahkan (Lebih aman!)
            .build()

        keyGenerator.init(spec)
        keyGenerator.generateKey()
    }

    private fun getMasterKey(): SecretKey {
        val keyStore = KeyStore.getInstance(KEYSTORE_PROVIDER).apply { load(null) }
        return keyStore.getKey(KEY_ALIAS, null) as SecretKey
    }

    // 3. Enkripsi/Dekripsi Kunci menggunakan Biometrik
    fun getCipherForEncryption(): Cipher {
        val cipher = Cipher.getInstance("${KeyProperties.KEY_ALGORITHM_AES}/${KeyProperties.BLOCK_MODE_GCM}/${KeyProperties.ENCRYPTION_PADDING_NONE}")
        cipher.init(Cipher.ENCRYPT_MODE, getMasterKey())
        return cipher
    }

    fun getCipherForDecryption(iv: ByteArray): Cipher {
        val cipher = Cipher.getInstance("${KeyProperties.KEY_ALGORITHM_AES}/${KeyProperties.BLOCK_MODE_GCM}/${KeyProperties.ENCRYPTION_PADDING_NONE}")
        val spec = GCMParameterSpec(128, iv)
        cipher.init(Cipher.DECRYPT_MODE, getMasterKey(), spec)
        return cipher
    }

    fun encryptMasterKey(masterKeyToEncrypt: ByteArray, cipher: Cipher): Pair<ByteArray, ByteArray> {
        val encryptedKey = cipher.doFinal(masterKeyToEncrypt)
        return Pair(encryptedKey, cipher.iv)
    }

    fun decryptMasterKey(encryptedKey: ByteArray, cipher: Cipher): ByteArray {
        return cipher.doFinal(encryptedKey)
    }
}
