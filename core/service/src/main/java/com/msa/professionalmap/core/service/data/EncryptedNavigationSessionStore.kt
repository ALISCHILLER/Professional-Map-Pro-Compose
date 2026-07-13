package com.msa.professionalmap.core.service.data

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.AtomicFile
import android.util.Base64
import com.msa.professionalmap.core.service.domain.NavigationSession
import org.json.JSONObject
import java.io.File
import java.nio.charset.StandardCharsets
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Persists only the route/session required to restore a foreground navigation runtime.
 *
 * The file lives under no-backup storage and is encrypted with an Android Keystore AES-GCM key.
 * Current/live device locations are intentionally never written to disk.
 */
internal class EncryptedNavigationSessionStore(
    context: Context,
) {
    private val appContext = context.applicationContext
    private val atomicFile = AtomicFile(File(appContext.noBackupFilesDir, FileName))

    fun save(session: NavigationSession) = synchronized(StoreLock) {
        val plaintext = NavigationSessionJsonCodec.encode(session)
        require(plaintext.size <= MaxPlaintextBytes) {
            "Navigation session is too large to persist safely."
        }
        val cipher = Cipher.getInstance(Transformation).apply {
            init(Cipher.ENCRYPT_MODE, getOrCreateKey())
        }
        val encrypted = cipher.doFinal(plaintext)
        val envelope = JSONObject()
            .put(JsonVersion, Version)
            .put(JsonIv, Base64.encodeToString(cipher.iv, Base64.NO_WRAP))
            .put(JsonPayload, Base64.encodeToString(encrypted, Base64.NO_WRAP))
            .toString()
            .toByteArray(StandardCharsets.UTF_8)

        val stream = atomicFile.startWrite()
        try {
            stream.write(envelope)
            stream.flush()
            atomicFile.finishWrite(stream)
        } catch (throwable: Throwable) {
            atomicFile.failWrite(stream)
            throw throwable
        }
    }

    fun read(): NavigationSession? = synchronized(StoreLock) {
        if (!atomicFile.baseFile.exists()) return null
        if (atomicFile.baseFile.length() !in 1..MaxEnvelopeBytes.toLong()) {
            clear()
            return null
        }
        return try {
            val envelope = JSONObject(String(atomicFile.readFully(), StandardCharsets.UTF_8))
            if (envelope.optInt(JsonVersion) != Version) {
                clear()
                return null
            }
            val iv = Base64.decode(envelope.getString(JsonIv), Base64.NO_WRAP)
            require(iv.size in MinIvBytes..MaxIvBytes) { "Invalid navigation session IV." }
            val encrypted = Base64.decode(envelope.getString(JsonPayload), Base64.NO_WRAP)
            val cipher = Cipher.getInstance(Transformation).apply {
                init(Cipher.DECRYPT_MODE, getOrCreateKey(), GCMParameterSpec(TagLengthBits, iv))
            }
            val plaintext = cipher.doFinal(encrypted)
            NavigationSessionJsonCodec.decode(plaintext)
        } catch (_: Throwable) {
            clear()
            null
        }
    }

    fun clear() = synchronized(StoreLock) {
        runCatching { atomicFile.delete() }
        Unit
    }

    private fun getOrCreateKey(): SecretKey {
        val keyStore = KeyStore.getInstance(AndroidKeyStore).apply { load(null) }
        (keyStore.getKey(KeyAlias, null) as? SecretKey)?.let { return it }

        return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, AndroidKeyStore).run {
            init(
                KeyGenParameterSpec.Builder(
                    KeyAlias,
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
        /** Serializes AtomicFile and Android Keystore access across controller/service instances. */
        val StoreLock = Any()
        const val AndroidKeyStore = "AndroidKeyStore"
        const val KeyAlias = "professional_map_navigation_session_v1"
        const val Transformation = "AES/GCM/NoPadding"
        const val TagLengthBits = 128
        const val FileName = "navigation-session.enc"
        const val Version = 2
        const val JsonVersion = "version"
        const val JsonIv = "iv"
        const val JsonPayload = "payload"
        const val MaxPlaintextBytes = 4 * 1024 * 1024
        const val MaxEnvelopeBytes = 6 * 1024 * 1024
        const val MinIvBytes = 12
        const val MaxIvBytes = 32
    }
}
