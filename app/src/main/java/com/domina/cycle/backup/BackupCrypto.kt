package com.domina.cycle.backup

import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

object BackupCrypto {
    private const val SALT_LEN = 16
    private const val IV_LEN = 12
    private const val ITERATIONS = 120_000
    private const val KEY_BITS = 256
    private const val TAG_BITS = 128

    fun seal(plaintext: ByteArray, passphrase: CharArray): ByteArray {
        val salt = ByteArray(SALT_LEN).also { SecureRandom().nextBytes(it) }
        val iv = ByteArray(IV_LEN).also { SecureRandom().nextBytes(it) }
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, deriveKey(passphrase, salt), GCMParameterSpec(TAG_BITS, iv))
        val ct = cipher.doFinal(plaintext)
        return salt + iv + ct
    }

    fun open(sealed: ByteArray, passphrase: CharArray): ByteArray {
        val salt = sealed.copyOfRange(0, SALT_LEN)
        val iv = sealed.copyOfRange(SALT_LEN, SALT_LEN + IV_LEN)
        val ct = sealed.copyOfRange(SALT_LEN + IV_LEN, sealed.size)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, deriveKey(passphrase, salt), GCMParameterSpec(TAG_BITS, iv))
        return cipher.doFinal(ct)
    }

    private fun deriveKey(passphrase: CharArray, salt: ByteArray): SecretKeySpec {
        val spec = PBEKeySpec(passphrase, salt, ITERATIONS, KEY_BITS)
        val keyBytes = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded
        return SecretKeySpec(keyBytes, "AES")
    }
}
