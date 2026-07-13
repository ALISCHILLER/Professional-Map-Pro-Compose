package com.msa.professionalmap.core.offline.data

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.nio.ByteBuffer
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/** Encrypts WorkManager input with an app-scoped Android Keystore AES-GCM key. */
internal class OfflineWorkPayloadCipher(
    context: Context,
) {
    private val keyAlias = "${context.applicationContext.packageName}.offline_work_payload.v1"

    fun encrypt(plaintext: ByteArray): String {
        check(plaintext.isNotEmpty()) { "Offline request payload must not be empty." }
        val cipher = Cipher.getInstance(Transformation).apply {
            init(Cipher.ENCRYPT_MODE, getOrCreateKey())
            updateAAD(AssociatedData)
        }
        val ciphertext = cipher.doFinal(plaintext)
        val envelope = ByteBuffer.allocate(Int.SIZE_BYTES + cipher.iv.size + ciphertext.size)
            .putInt(cipher.iv.size)
            .put(cipher.iv)
            .put(ciphertext)
            .array()
        return Base64.encodeToString(envelope, Base64.NO_WRAP)
    }

    fun decrypt(encodedEnvelope: String): ByteArray {
        require(encodedEnvelope.isNotBlank()) { "Encrypted offline request is missing." }
        val envelope = ByteBuffer.wrap(Base64.decode(encodedEnvelope, Base64.NO_WRAP))
        val ivSize = envelope.int
        require(ivSize in MinIvSize..MaxIvSize && envelope.remaining() > ivSize) {
            "Encrypted offline request is invalid."
        }
        val iv = ByteArray(ivSize).also(envelope::get)
        val ciphertext = ByteArray(envelope.remaining()).also(envelope::get)
        return Cipher.getInstance(Transformation).run {
            init(Cipher.DECRYPT_MODE, getOrCreateKey(), GCMParameterSpec(TagLengthBits, iv))
            updateAAD(AssociatedData)
            doFinal(ciphertext)
        }
    }

    private fun getOrCreateKey(): SecretKey = synchronized(KeyCreationLock) {
        val keyStore = KeyStore.getInstance(AndroidKeyStore).apply { load(null) }
        (keyStore.getKey(keyAlias, null) as? SecretKey)?.let { return@synchronized it }
        KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, AndroidKeyStore).run {
            init(
                KeyGenParameterSpec.Builder(
                    keyAlias,
                    KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
                )
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .setRandomizedEncryptionRequired(true)
                    .build()
            )
            generateKey()
        }
    }

    private companion object {
        const val AndroidKeyStore = "AndroidKeyStore"
        const val Transformation = "AES/GCM/NoPadding"
        const val TagLengthBits = 128
        const val MinIvSize = 12
        const val MaxIvSize = 32
        val AssociatedData = "ProfessionalMapPro:OfflineWork:v1".encodeToByteArray()
        val KeyCreationLock = Any()
    }
}
