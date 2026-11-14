package com.example.security

import android.content.Context
import android.os.ParcelFileDescriptor
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.io.File
import java.io.FileOutputStream
import java.io.RandomAccessFile
import java.security.KeyStore
import java.util.UUID
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Handles AES-256-GCM encrypted temporary storage in app-private cache using Android Keystore,
 * and securely zeroes and deletes transient decrypted PDF descriptors on lock/exit.
 */
class EncryptedPdfCacheManager(private val context: Context) {

    private val vaultDir: File by lazy {
        File(context.cacheDir, "secure_pdf_vault").apply { mkdirs() }
    }

    private fun getOrCreateKey(): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        val existingKey = keyStore.getKey(KEY_ALIAS, null) as? SecretKey
        if (existingKey != null) return existingKey

        val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
        val spec = KeyGenParameterSpec.Builder(
            KEY_ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(256)
            .build()
        keyGenerator.init(spec)
        return keyGenerator.generateKey()
    }

    /**
     * Encrypts raw PDF bytes into an app-private `.enc` file, then decrypts on demand into an
     * ephemeral unlinked/short-lived descriptor for PdfRenderer and wipes plaintext immediately
     * when [releaseSession] is called.
     */
    fun openProtectedPdfDescriptor(
        documentId: String,
        pdfBase64: String
    ): Pair<ParcelFileDescriptor, File> {
        val rawPdfBytes = Base64.decode(pdfBase64, Base64.DEFAULT)

        // 1. Encrypt payload at rest in app-private encrypted cache
        val cipher = Cipher.getInstance(AES_GCM_NO_PADDING)
        cipher.init(Cipher.ENCRYPT_MODE, getOrCreateKey())
        val iv = cipher.iv
        val ciphertext = cipher.doFinal(rawPdfBytes)

        val encFile = File(vaultDir, "${sanitize(documentId)}.enc")
        FileOutputStream(encFile).use { fos ->
            fos.write(iv.size)
            fos.write(iv)
            fos.write(ciphertext)
            fos.fd.sync()
        }

        // 2. Decrypt to an ephemeral session file solely for PdfRenderer ParcelFileDescriptor
        val decryptedBytes = decryptFile(encFile)
        val tempFile = File(vaultDir, "session_${UUID.randomUUID()}.tmp")
        FileOutputStream(tempFile).use { fos ->
            fos.write(decryptedBytes)
            fos.fd.sync()
        }
        decryptedBytes.fill(0)
        rawPdfBytes.fill(0)

        val pfd = ParcelFileDescriptor.open(tempFile, ParcelFileDescriptor.MODE_READ_ONLY)
        return pfd to tempFile
    }

    private fun decryptFile(encFile: File): ByteArray {
        val all = encFile.readBytes()
        val ivLen = all[0].toInt() and 0xFF
        val iv = all.copyOfRange(1, 1 + ivLen)
        val ciphertext = all.copyOfRange(1 + ivLen, all.size)
        val cipher = Cipher.getInstance(AES_GCM_NO_PADDING)
        cipher.init(Cipher.DECRYPT_MODE, getOrCreateKey(), GCMParameterSpec(128, iv))
        return cipher.doFinal(ciphertext)
    }

    fun wipeFileSecurely(file: File?) {
        if (file == null || !file.exists()) return
        try {
            val length = file.length()
            if (length > 0) {
                RandomAccessFile(file, "rw").use { raf ->
                    val zeros = ByteArray(4096)
                    var remaining = length
                    while (remaining > 0) {
                        val toWrite = minOf(remaining, zeros.size.toLong()).toInt()
                        raf.write(zeros, 0, toWrite)
                        remaining -= toWrite
                    }
                    raf.fd.sync()
                }
            }
        } catch (_: Exception) {
            // Fall back to direct deletion
        } finally {
            file.delete()
        }
    }

    fun wipeAllTemporaryFiles() {
        vaultDir.listFiles()?.forEach { file ->
            wipeFileSecurely(file)
        }
    }

    private fun sanitize(input: String): String =
        input.replace(Regex("[^a-zA-Z0-9_\\-]"), "_")

    private companion object {
        const val ANDROID_KEYSTORE = "AndroidKeyStore"
        const val KEY_ALIAS = "secure_study_pdf_cache_key_v1"
        const val AES_GCM_NO_PADDING = "AES/GCM/NoPadding"
    }
}
