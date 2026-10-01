package id.cukup.domain

/** Hasil membaca satu notifikasi keuangan. */
data class ParsedNotification(
    val type: TxType,
    val amount: Long,
    val merchant: String,
    val appLabel: String,
    val isPaylater: Boolean,
    /** Pembayaran tagihan/cicilan hutang (bukan hutang baru). */
    val isDebtPayment: Boolean = false,
)

/** Aplikasi keuangan yang notifikasinya dibaca. */
data class FinanceApp(val packageName: String, val label: String, val isPaylater: Boolean = false)

object NotificationParser {

    /** Diambil dari [Brands] supaya daftar bank/e-wallet cukup diatur di satu tempat. */
    val apps: List<FinanceApp> = Brands.all.flatMap { b ->
        b.packages.map { FinanceApp(it, b.name, isPaylater = b.kind == AccountKind.PAYLATER) }
    }
    private val byPackage = apps.associateBy { it.packageName }

    fun isSupported(packageName: String): Boolean = packageName in byPackage

    private val amountRegex = Regex("""(?:Rp\.?|IDR)\s?([0-9][0-9.,]*)""", RegexOption.IGNORE_CASE)

    /** Selalu dilewati: kode rahasia, transaksi gagal, pengingat. */
    private val blockWords = listOf(
        "otp", "kode verifikasi", "kode rahasia", "jangan berikan",
        "gagal", "dibatalkan", "ditolak", "tagihan kamu akan jatuh tempo", "pengingat",
    )

    /** Kata promosi. Hanya membuat notifikasi dilewati kalau tidak ada tanda transaksi yang benar-benar terjadi. */
    private val promoWords = listOf(
        "promo", "diskon", "voucher", "cashback s.d", "cashback hingga", "dapatkan", "yuk", "buruan", "ayo", "hemat hingga", "berlaku",
    )

    /** Tanda transaksi sudah terjadi. Bank sering menutup notifikasi transaksi dengan ajakan ("Yuk cek detailnya"). */
    private val doneWords = listOf("berhasil", "sukses", "diterima", "terkirim", "masuk ke", "dana masuk", "uang masuk", "transfer masuk", "menerima")

    private fun hasWord(lower: String, word: String): Boolean =
        if (word.contains(' ') || word.contains('.')) lower.contains(word) else Regex("""(?<![a-z])${Regex.escape(word)}(?![a-z])""").containsMatchIn(lower)

    /** Kata yang membuat notifikasi ini dilewati, atau null bila boleh dibaca. */
    private fun skipWord(lower: String): String? =
        blockWords.firstOrNull { hasWord(lower, it) }
            ?: promoWords.firstOrNull { hasWord(lower, it) }?.takeIf { doneWords.none { d -> lower.contains(d) } }
    private val incomeWords = listOf(
        "pemasukan", "menerima", "diterima", "masuk ke", "dana masuk", "uang masuk", "transfer masuk", "kredit",
        "telah menerima", "kamu dapat", "refund", "pengembalian dana", "saldo bertambah", "top up berhasil",
        "isi saldo berhasil", "transfer dari", "dana dari", "kiriman dari", "uang dari", "bunga", "received", "incoming",
    )
    private val expenseWords = listOf(
        "pengeluaran", "pembayaran", "bayar", "dibayar", "transfer ke", "kirim uang", "mengirim", "pembelian", "debit",
        "transaksi berhasil", "berhasil transfer", "tarik tunai", "belanja", "dipotong", "qris",
        "transfer berhasil", "transfer keluar", "uang keluar", "dana keluar", "terkirim", "dikirim", "mentransfer", "penarikan",
        "tarik saldo", "top up ke", "autodebet", "auto debet", "biaya admin", "sent", "outgoing",
    )
    private val debtPaymentWords = listOf("bayar tagihan", "pembayaran tagihan", "pelunasan", "angsuran", "bayar cicilan", "pembayaran cicilan", "tagihan berhasil dibayar", "tagihan kamu sudah lunas")
    private val paylaterWords = listOf("paylater", "pay later", "spaylater", "gopaylater", "cicilan", "kredivo", "akulaku")

    private val merchantRegexes = listOf(
        Regex("""\b(?:di|at)\s+([A-Za-z0-9*&'.\- ]{2,40}?)(?:\s+(?:sebesar|senilai|berhasil|pada|tgl|tanggal|dengan|menggunakan|pakai|via)\b|[.,!]|$)""", RegexOption.IGNORE_CASE),
        Regex("""\b(?:ke|kepada|to)\s+([A-Za-z0-9*&'.\- ]{2,40}?)(?:\s+(?:sebesar|senilai|berhasil|pada|tgl|tanggal|dengan|menggunakan|via)\b|[.,!]|$)""", RegexOption.IGNORE_CASE),
        Regex("""\b(?:dari|from)\s+([A-Za-z0-9*&'.\- ]{2,40}?)(?:\s+(?:sebesar|senilai|berhasil|pada|tgl|tanggal|ke)\b|[.,!]|$)""", RegexOption.IGNORE_CASE),
    )

    private val kategoriRegex = Regex("""\s+di kategori\b[^.]*""", RegexOption.IGNORE_CASE)

    /**
     * Membaca notifikasi. Mengembalikan null bila bukan transaksi yang jelas
     * (OTP, promo, pengingat, tanpa nominal, atau jenis tidak bisa ditentukan).
     */
    fun parse(packageName: String, title: String?, text: String?): ParsedNotification? {
        val app = byPackage[packageName] ?: return null
        val body = listOfNotNull(title, text).joinToString(". ").replace('\n', ' ').trim()
        if (body.isEmpty()) return null
        val lower = body.lowercase()
        if (skipWord(lower) != null) return null

        val amount = amountRegex.findAll(body)
            .mapNotNull { Rupiah.parse(it.groupValues[1]) }
            .firstOrNull { it > 0 } ?: return null

        val incomeHit = incomeWords.indexOfFirstIn(lower)
        val expenseHit = expenseWords.indexOfFirstIn(lower)
        val type = when {
            incomeHit == null && expenseHit == null -> return null
            incomeHit == null -> TxType.EXPENSE
            expenseHit == null -> TxType.INCOME
            // Keduanya muncul ("Pembayaran diterima"): kata yang muncul lebih dulu menang.
            else -> if (incomeHit < expenseHit) TxType.INCOME else TxType.EXPENSE
        }

        val isDebtPayment = type == TxType.EXPENSE && debtPaymentWords.any { lower.contains(it) }
        val isPaylater = type == TxType.EXPENSE && !isDebtPayment &&
            (app.isPaylater || paylaterWords.any { lower.contains(it) })

        return ParsedNotification(
            type = type,
            amount = amount,
            merchant = extractMerchant(body, type),
            appLabel = app.label,
            isPaylater = isPaylater,
            isDebtPayment = isDebtPayment,
        )
    }

    private val secretWords = listOf("otp", "kode verifikasi", "kode rahasia", "kode keamanan", "jangan berikan", "password", "kata sandi", " pin ", "pin:", "cvv", "token")

    /** True bila teks tampak berisi kode rahasia (OTP, PIN, kata sandi): tidak boleh disimpan di mana pun. */
    fun isSecret(text: String): Boolean {
        val lower = " ${text.lowercase()} "
        return secretWords.any { lower.contains(it) }
    }

    /** Alasan singkat kenapa [parse] melewati notifikasi ini; null bila terbaca. */
    fun whySkipped(packageName: String, title: String?, text: String?): String? {
        if (packageName !in byPackage) return "Aplikasi ini tidak dibaca"
        val body = listOfNotNull(title, text).joinToString(". ").replace('\n', ' ').trim()
        if (body.isEmpty()) return "Notifikasinya kosong"
        val lower = body.lowercase()
        skipWord(lower)?.let { return "Dianggap bukan transaksi (ada kata \"$it\")" }
        if (amountRegex.findAll(body).none { (Rupiah.parse(it.groupValues[1]) ?: 0) > 0 }) return "Tidak ada nominal rupiah"
        if (incomeWords.indexOfFirstIn(lower) == null && expenseWords.indexOfFirstIn(lower) == null) return "Tidak jelas uang masuk atau keluar"
        return null
    }

    private fun List<String>.indexOfFirstIn(text: String): Int? =
        mapNotNull { w -> text.indexOf(w).takeIf { it >= 0 } }.minOrNull()

    internal fun extractMerchant(body: String, type: TxType): String {
        val candidates = if (type == TxType.INCOME) listOf(merchantRegexes[2], merchantRegexes[0])
        else listOf(merchantRegexes[0], merchantRegexes[1])
        // myBCA menambahkan "di kategori ..." yang bukan nama merchant.
        val clean = body.replace(kategoriRegex, "")
        for (r in candidates) {
            val m = r.findAll(clean)
                .map { it.groupValues[1].trim().trimEnd('.', '-') }
                .firstOrNull { it.isNotBlank() && !it.startsWith("Rp", ignoreCase = true) && it.any(Char::isLetter) }
            if (m != null) return m.take(40)
        }
        return ""
    }

    /** Kunci anti-duplikat: aplikasi + nominal + menit + merchant ternormalisasi. */
    fun fingerprint(packageName: String, amount: Long, postedAtMillis: Long, merchant: String): String =
        "$packageName|$amount|${postedAtMillis / 60_000}|${MerchantClassifier.key(merchant)}"
}
