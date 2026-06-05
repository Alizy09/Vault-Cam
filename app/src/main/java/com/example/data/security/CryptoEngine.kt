package com.example.data.security

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.io.File
import java.io.InputStream
import java.io.OutputStream
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.CipherInputStream
import javax.crypto.CipherOutputStream
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

object CryptoEngine {
    private const val KEY_ALIAS = "VaultCamSecretKey"
    private const val TRANSFORMATION = "AES/GCM/NoPadding"

    init {
        getOrCreateKey()
    }

    private fun getOrCreateKey(): SecretKey {
        val keyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        val existingKey = keyStore.getKey(KEY_ALIAS, null) as? SecretKey
        if (existingKey != null) return existingKey

        val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
        keyGenerator.init(
            KeyGenParameterSpec.Builder(
                KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setUserAuthenticationRequired(false) // Allows background encryption/decryption without showing a prompt
                .build()
        )
        return keyGenerator.generateKey()
    }

    /**
     * Encrypts a source file (e.g. temporary raw recording) and writes the encrypted bytes to destFile.
     * The IV is prepended to the encrypted stream so that the file is self-contained.
     */
    fun encryptFile(sourceFile: File, destFile: File) {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, getOrCreateKey())
        val iv = cipher.iv

        sourceFile.inputStream().use { input ->
            destFile.outputStream().use { output ->
                output.write(iv.size)
                output.write(iv)
                CipherOutputStream(output, cipher).use { encryptedOutput ->
                    input.copyTo(encryptedOutput)
                }
            }
        }
    }

    /**
     * Decrypts an encrypted file back into a temporary readable play file.
     */
    fun decryptFile(sourceFile: File, destFile: File) {
        sourceFile.inputStream().use { input ->
            val ivSize = input.read()
            if (ivSize <= 0) throw IllegalArgumentException("Invalid IV size in encrypted file")
            val iv = ByteArray(ivSize)
            input.read(iv)

            val cipher = Cipher.getInstance(TRANSFORMATION)
            val spec = GCMParameterSpec(128, iv)
            cipher.init(Cipher.DECRYPT_MODE, getOrCreateKey(), spec)

            destFile.outputStream().use { output ->
                CipherInputStream(input, cipher).use { decryptedInput ->
                    decryptedInput.copyTo(output)
                }
            }
        }
    }
}
