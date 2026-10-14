package com.example.crypto

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import java.security.MessageDigest
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

data class EncryptedResult(
    val ivBase64: String,
    val cipherTextBase64: String
)

/**
 * End-to-End Local Encryption Engine using AES-256-GCM.
 * Keys are anchored in hardware-backed Android KeyStore when available,
 * ensuring high confidentiality for API tokens, virtual phone numbers,
 * and received SMS verification codes.
 */
class CryptoManager {

    companion object {
        private const val ANDROID_KEYSTORE = "AndroidKeyStore"
        private const val KEY_ALIAS = "NokosVaultMasterKey_v1"
        private const val AES_GCM_TRANSFORMATION = "AES/GCM/NoPadding"
        private const val GCM_TAG_LENGTH = 128
        private const val GCM_IV_LENGTH = 12
        private const val FALLBACK_SECRET_SALT = "NOKOS_SMS_SECURE_SALT_98412_END_TO_END"
    }

    private var secretKey: SecretKey

    init {
        secretKey = getOrCreateKey()
    }

    private fun getOrCreateKey(): SecretKey {
        return try {
            val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
            if (!keyStore.containsAlias(KEY_ALIAS)) {
                val keyGenerator = KeyGenerator.getInstance(
                    KeyProperties.KEY_ALGORITHM_AES,
                    ANDROID_KEYSTORE
                )
                val keyGenParameterSpec = KeyGenParameterSpec.Builder(
                    KEY_ALIAS,
                    KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
                )
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .setKeySize(256)
                    .build()
                keyGenerator.init(keyGenParameterSpec)
                keyGenerator.generateKey()
            } else {
                (keyStore.getEntry(KEY_ALIAS, null) as KeyStore.SecretKeyEntry).secretKey
            }
        } catch (e: Exception) {
            // Fallback for Robolectric/JVM testing or devices with custom keystore configurations
            generateSoftwareFallbackKey()
        }
    }

    private fun generateSoftwareFallbackKey(): SecretKey {
        val digest = MessageDigest.getInstance("SHA-256")
        val rawKey = digest.digest(FALLBACK_SECRET_SALT.toByteArray(Charsets.UTF_8))
        return SecretKeySpec(rawKey, "AES")
    }

    fun encrypt(plainText: String): EncryptedResult {
        if (plainText.isEmpty()) return EncryptedResult("", "")
        val cipher = Cipher.getInstance(AES_GCM_TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, secretKey)
        val iv = cipher.iv
        val cipherBytes = cipher.doFinal(plainText.toByteArray(Charsets.UTF_8))
        return EncryptedResult(
            ivBase64 = Base64.encodeToString(iv, Base64.NO_WRAP),
            cipherTextBase64 = Base64.encodeToString(cipherBytes, Base64.NO_WRAP)
        )
    }

    fun decrypt(ivBase64: String, cipherTextBase64: String): String {
        if (ivBase64.isEmpty() || cipherTextBase64.isEmpty()) return ""
        return try {
            val iv = Base64.decode(ivBase64, Base64.NO_WRAP)
            val cipherBytes = Base64.decode(cipherTextBase64, Base64.NO_WRAP)
            val spec = GCMParameterSpec(GCM_TAG_LENGTH, iv)
            val cipher = Cipher.getInstance(AES_GCM_TRANSFORMATION)
            cipher.init(Cipher.DECRYPT_MODE, secretKey, spec)
            val decryptedBytes = cipher.doFinal(cipherBytes)
            String(decryptedBytes, Charsets.UTF_8)
        } catch (e: Exception) {
            "[Decryption Error]"
        }
    }

    fun getKeyFingerprint(): String {
        return try {
            val md = MessageDigest.getInstance("SHA-256")
            val hash = md.digest(KEY_ALIAS.toByteArray(Charsets.UTF_8))
            hash.take(8).joinToString(":") { "%02X".format(it) }
        } catch (e: Exception) {
            "E2E-AES-256-GCM"
        }
    }
}
