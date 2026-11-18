package com.example.mywallet.data

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.security.KeyStore
import java.security.SecureRandom
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

fun gzipBytes(b: ByteArray): ByteArray {
    val o = ByteArrayOutputStream()
    GZIPOutputStream(o).use { it.write(b) }
    return o.toByteArray()
}

fun gunzipBytes(b: ByteArray): ByteArray = GZIPInputStream(ByteArrayInputStream(b)).use { it.readBytes() }

fun isGzip(b: ByteArray) = b.size > 2 && b[0] == 0x1f.toByte() && b[1] == 0x8b.toByte()

/**
 * Keeps the app's own files (data + receipt photos) encrypted on the phone with a key that lives in the
 * Android Keystore (the key never leaves the secure hardware). Old plain files are still read, and if
 * encryption is ever unavailable the file is saved without it, so data is never lost because of this.
 */
object Vault {
    private const val ALIAS = "mywallet_data_key"
    private val MAGIC = byteArrayOf('M'.code.toByte(), 'W'.code.toByte(), 'E'.code.toByte(), '1'.code.toByte())

    private fun key(): SecretKey? = try {
        val ks = KeyStore.getInstance("AndroidKeyStore"); ks.load(null)
        (ks.getKey(ALIAS, null) as? SecretKey) ?: run {
            val g = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
            g.init(
                KeyGenParameterSpec.Builder(ALIAS, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM).setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE).setKeySize(256).build(),
            )
            g.generateKey()
        }
    } catch (e: Exception) { null }

    /** compress (optional) and encrypt */
    fun seal(plain: ByteArray, compress: Boolean = true): ByteArray {
        val z = if (compress) gzipBytes(plain) else plain
        val k = key() ?: return z
        return try {
            val c = Cipher.getInstance("AES/GCM/NoPadding"); c.init(Cipher.ENCRYPT_MODE, k)
            MAGIC + c.iv + c.doFinal(z)
        } catch (e: Exception) { z }
    }

    /** decrypt and decompress whatever seal() made; plain files pass through */
    fun open(data: ByteArray): ByteArray? {
        var b = data
        if (b.size > 16 && b[0] == MAGIC[0] && b[1] == MAGIC[1] && b[2] == MAGIC[2] && b[3] == MAGIC[3]) {
            val k = key() ?: return null
            b = try {
                val c = Cipher.getInstance("AES/GCM/NoPadding")
                c.init(Cipher.DECRYPT_MODE, k, GCMParameterSpec(128, b.copyOfRange(4, 16)))
                c.doFinal(b, 16, b.size - 16)
            } catch (e: Exception) { return null }
        }
        return if (isGzip(b)) try { gunzipBytes(b) } catch (e: Exception) { null } else b
    }
}

/** password-protected backup files: gzip, then AES-GCM with a key made from your password (PBKDF2) */
object BackupCrypto {
    private val MAGIC = "MWBK1".toByteArray(Charsets.US_ASCII)

    private fun keyFor(password: String, salt: ByteArray): SecretKeySpec {
        val f = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        return SecretKeySpec(f.generateSecret(PBEKeySpec(password.toCharArray(), salt, 150_000, 256)).encoded, "AES")
    }

    fun seal(plain: ByteArray, password: String?): ByteArray {
        val z = gzipBytes(plain)
        if (password.isNullOrEmpty()) return z
        val rnd = SecureRandom()
        val salt = ByteArray(16).also { rnd.nextBytes(it) }
        val iv = ByteArray(12).also { rnd.nextBytes(it) }
        val c = Cipher.getInstance("AES/GCM/NoPadding")
        c.init(Cipher.ENCRYPT_MODE, keyFor(password, salt), GCMParameterSpec(128, iv))
        return MAGIC + salt + iv + c.doFinal(z)
    }

    fun isEncrypted(b: ByteArray): Boolean = b.size > MAGIC.size + 28 && MAGIC.indices.all { b[it] == MAGIC[it] }

    /** null = wrong password */
    fun open(data: ByteArray, password: String): ByteArray? = try {
        val o = MAGIC.size
        val c = Cipher.getInstance("AES/GCM/NoPadding")
        c.init(Cipher.DECRYPT_MODE, keyFor(password, data.copyOfRange(o, o + 16)), GCMParameterSpec(128, data.copyOfRange(o + 16, o + 28)))
        gunzipBytes(c.doFinal(data, o + 28, data.size - o - 28))
    } catch (e: Exception) { null }
}
