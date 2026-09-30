package id.cukup.domain

import java.security.MessageDigest
import java.security.SecureRandom

/** PIN aplikasi. Yang disimpan hanya "garam:hash", bukan PIN-nya. */
object PinCode {
    const val LENGTH = 4
    /** Salah berturut-turut sebanyak ini → tunggu [COOLDOWN_MS]. */
    const val MAX_TRIES = 5
    const val COOLDOWN_MS = 30_000L

    fun isValid(pin: String): Boolean = pin.length == LENGTH && pin.all(Char::isDigit)

    fun make(pin: String, random: SecureRandom = SecureRandom()): String {
        require(isValid(pin))
        val salt = ByteArray(16).also(random::nextBytes).toHex()
        return "$salt:${hash(pin, salt)}"
    }

    fun verify(pin: String, stored: String): Boolean {
        val salt = stored.substringBefore(':', "")
        val expected = stored.substringAfter(':', "")
        if (salt.isEmpty() || expected.isEmpty()) return false
        return MessageDigest.isEqual(hash(pin, salt).toByteArray(), expected.toByteArray())
    }

    private fun hash(pin: String, salt: String): String {
        val md = MessageDigest.getInstance("SHA-256")
        var out = (salt + pin).toByteArray()
        repeat(10_000) { out = md.digest(out) }
        return out.toHex()
    }

    private fun ByteArray.toHex() = joinToString("") { "%02x".format(it) }
}
