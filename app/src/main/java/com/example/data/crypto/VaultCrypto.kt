package com.example.data.crypto

import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.security.MessageDigest
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

object VaultCrypto {

    fun hashPin(pin: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val bytes = digest.digest(pin.toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }

    fun verifyPin(enteredPin: String, storedHash: String): Boolean {
        return hashPin(enteredPin) == storedHash
    }

    private fun deriveKey(pin: String): SecretKeySpec {
        val digest = MessageDigest.getInstance("SHA-256")
        val keyBytes = digest.digest("SNAPLOAD_VAULT_SALT_$pin".toByteArray(Charsets.UTF_8))
        return SecretKeySpec(keyBytes, "AES")
    }

    private val FIXED_IV = "SnapLoad_IV_16_B".toByteArray(Charsets.UTF_8)

    fun encryptFile(inputFile: File, outputFile: File, pin: String) {
        val key = deriveKey(pin)
        val cipher = Cipher.getInstance("AES/CTR/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, key, IvParameterSpec(FIXED_IV))

        outputFile.parentFile?.mkdirs()
        FileInputStream(inputFile).use { input ->
            FileOutputStream(outputFile).use { output ->
                val buffer = ByteArray(64 * 1024)
                var bytesRead: Int
                while (input.read(buffer).also { bytesRead = it } != -1) {
                    val encryptedChunk = cipher.update(buffer, 0, bytesRead)
                    if (encryptedChunk != null && encryptedChunk.isNotEmpty()) {
                        output.write(encryptedChunk)
                    }
                }
                val finalBytes = cipher.doFinal()
                if (finalBytes != null && finalBytes.isNotEmpty()) {
                    output.write(finalBytes)
                }
            }
        }
    }

    fun decryptFile(encryptedFile: File, outputFile: File, pin: String) {
        val key = deriveKey(pin)
        val cipher = Cipher.getInstance("AES/CTR/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, key, IvParameterSpec(FIXED_IV))

        outputFile.parentFile?.mkdirs()
        FileInputStream(encryptedFile).use { input ->
            FileOutputStream(outputFile).use { output ->
                val buffer = ByteArray(64 * 1024)
                var bytesRead: Int
                while (input.read(buffer).also { bytesRead = it } != -1) {
                    val decryptedChunk = cipher.update(buffer, 0, bytesRead)
                    if (decryptedChunk != null && decryptedChunk.isNotEmpty()) {
                        output.write(decryptedChunk)
                    }
                }
                val finalBytes = cipher.doFinal()
                if (finalBytes != null && finalBytes.isNotEmpty()) {
                    output.write(finalBytes)
                }
            }
        }
    }
}
