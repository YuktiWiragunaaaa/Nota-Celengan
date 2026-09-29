package id.cukup.domain

import kotlin.math.abs
import kotlin.math.floor

object Rupiah {
    /** 1250000 → "Rp1.250.000"; negatif → "−Rp1.250.000". */
    fun format(amount: Long, withSymbol: Boolean = true): String {
        val digits = abs(amount).toString().reversed().chunked(3).joinToString(".").reversed()
        val sign = if (amount < 0) "−" else ""
        return if (withSymbol) "${sign}Rp$digits" else "$sign$digits"
    }

    /** 1250000 → "1,2 jt"; 45000 → "45 rb". Untuk label ringkas. */
    fun short(amount: Long): String {
        val a = abs(amount)
        val sign = if (amount < 0) "−" else ""
        return when {
            a >= 1_000_000_000 -> sign + trim(a / 1_000_000_000.0) + " M"
            a >= 1_000_000 -> sign + trim(a / 1_000_000.0) + " jt"
            a >= 1_000 -> sign + trim(a / 1_000.0) + " rb"
            else -> sign + a.toString()
        }
    }

    private fun trim(v: Double): String {
        val r = floor(v * 10) / 10
        return if (r % 1.0 == 0.0) r.toLong().toString() else r.toString().replace('.', ',')
    }

    /**
     * Membaca nominal gaya Indonesia maupun internasional:
     * "25.000", "25.000,00", "25,000.00", "25000", "1.250.000,50" → rupiah penuh (sen dibuang).
     */
    fun parse(raw: String): Long? {
        val s = raw.trim().replace(" ", "")
        if (s.none { it.isDigit() }) return null
        val lastDot = s.lastIndexOf('.')
        val lastComma = s.lastIndexOf(',')
        val decimalSep: Char? = when {
            lastDot >= 0 && lastComma >= 0 -> if (lastDot > lastComma) '.' else ','
            lastComma >= 0 && s.length - lastComma - 1 in 1..2 -> ','
            lastDot >= 0 && s.length - lastDot - 1 in 1..2 && s.count { it == '.' } == 1 -> '.'
            else -> null
        }
        val integerPart = if (decimalSep != null) s.substring(0, s.lastIndexOf(decimalSep)) else s
        return integerPart.filter { it.isDigit() }.toLongOrNull()
    }
}
