package com.domina.cycle.security

import java.security.SecureRandom
import java.util.Base64
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

/** Salted PBKDF2 PIN hashing. Stored form is "salt:hash" in Base64. */
object PinManager {
    private const val ITERATIONS = 120_000
    private const val KEY_LEN = 256

    fun hash(pin: String): String {
        val salt = ByteArray(16).also { SecureRandom().nextBytes(it) }
        val hash = pbkdf2(pin, salt)
        return "${b64(salt)}:${b64(hash)}"
    }

    fun verify(pin: String, stored: String): Boolean {
        val parts = stored.split(":")
        val salt = Base64.getDecoder().decode(parts[0])
        val expectedHash = Base64.getDecoder().decode(parts[1])
        return constantEquals(pbkdf2(pin, salt), expectedHash)
    }

    private fun pbkdf2(pin: String, salt: ByteArray): ByteArray {
        val spec = PBEKeySpec(pin.toCharArray(), salt, ITERATIONS, KEY_LEN)
        return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded
    }

    private fun constantEquals(a: ByteArray, b: ByteArray): Boolean {
        if (a.size != b.size) return false
        var r = 0; for (i in a.indices) r = r or (a[i].toInt() xor b[i].toInt()); return r == 0
    }

    private fun b64(b: ByteArray) = Base64.getEncoder().encodeToString(b)
}
