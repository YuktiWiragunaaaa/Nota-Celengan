package id.cukup.domain

/**
 * Laporan satu periode gajian yang sudah selesai.
 * [notice] = kalimat ringan untuk notifikasi; [findings] dan [advice] untuk layar laporan.
 */
data class PeriodReport(
    val totals: Totals,
    val parts: List<CategoryAmount>,
    val notice: String,
    val findings: List<Insight>,
    val advice: List<String>,
) {
    val empty: Boolean get() = totals.income == 0L && totals.expense == 0L
}

/**
 * Penyimpul laporan periode. Aturan tetap yang dihitung di HP dari catatan sendiri (bukan model AI,
 * tanpa internet), supaya setiap kalimat bisa ditelusuri ke angkanya.
 */
object Reporter {

    private const val SMALL = 30_000L

    /**
     * [from, to) = periode yang dilaporkan; [prevFrom, from) = periode sebelumnya sebagai pembanding.
     * [days] = panjang periode dalam hari.
     */
    fun of(
        transactions: List<Transaction>,
        categories: List<Category>,
        from: Long,
        to: Long,
        prevFrom: Long,
        days: Int,
    ): PeriodReport {
        val totals = Ledger.totals(transactions, from, to)
        val before = Ledger.totals(transactions, prevFrom, from)
        val parts = Ledger.byCategory(transactions, categories, TxType.EXPENSE, from, to)
        val prevParts = Ledger.byCategory(transactions, categories, TxType.EXPENSE, prevFrom, from)
            .associate { it.category?.id to it.amount }
        val expenses = transactions.filter {
            it.status == TxStatus.CONFIRMED && it.type == TxType.EXPENSE && it.occurredAt in from until to
        }
        val top = parts.firstOrNull()
        val topName = top?.category?.name ?: "Tanpa kategori"
        val income = totals.income
        val spent = totals.expense
        fun pct(part: Long, whole: Long) = if (whole > 0) part * 100 / whole else 0

        val notice = when {
            top == null -> if (income > 0) "Tidak ada pengeluaran tercatat. Uang masuk ${Rupiah.short(income)}." else ""
            income > 0 -> "$topName paling banyak: ${Rupiah.short(top.amount)}, ${pct(top.amount, income)}% dari uang masuk ${Rupiah.short(income)}."
            else -> "$topName paling banyak: ${Rupiah.short(top.amount)}, ${pct(top.amount, spent)}% dari total keluar ${Rupiah.short(spent)}."
        }

        val findings = mutableListOf<Insight>()
        val advice = mutableListOf<String>()

        // 1. Keluar dibanding masuk.
        when {
            income > 0 && spent > income -> {
                findings += Insight("Keluar ${Rupiah.short(spent)}, lebih besar ${Rupiah.short(spent - income)} dari yang masuk (${Rupiah.short(income)}).", Insight.Tone.WARN, "net")
                advice += "Periode ini tekor ${Rupiah.short(spent - income)}. Pasang batas di Rencana sebesar uang masukmu supaya diingatkan sebelum lewat."
            }
            income > 0 -> {
                findings += Insight("Dari uang masuk ${Rupiah.short(income)}, terpakai ${pct(spent, income)}% dan tersisa ${Rupiah.short(income - spent)}.", Insight.Tone.GOOD, "net")
                if (income - spent >= 50_000) {
                    advice += "Masih ada sisa ${Rupiah.short(income - spent)}. Pindahkan separuhnya (${Rupiah.short((income - spent) / 2)}) ke tabungan sekarang, sebelum ikut terpakai."
                }
            }
            spent > 0 -> findings += Insight("Keluar ${Rupiah.short(spent)}, tapi tidak ada uang masuk yang tercatat. Persen dari gaji belum bisa dihitung.", Insight.Tone.INFO, "net")
        }

        // 2. Kategori terbesar.
        if (top != null && spent > 0) {
            findings += Insight("$topName paling besar: ${Rupiah.short(top.amount)}, ${pct(top.amount, spent)}% dari semua pengeluaran.", Insight.Tone.INFO, "top")
            if (pct(top.amount, spent) >= 40 && parts.size > 1) {
                val weekly = top.amount * 9 / 10 * 7 / days.coerceAtLeast(7)
                advice += "$topName memakan ${if (pct(top.amount, spent) >= 50) "lebih dari separuh" else "hampir separuh"} pengeluaran. Coba batasi ${Rupiah.short(weekly)} per minggu (10% lebih hemat dari periode ini)."
            }
        }

        // 3. Naik paling tajam dibanding periode sebelumnya.
        val rise = parts
            .mapNotNull { p -> prevParts[p.category?.id]?.takeIf { it > 0 }?.let { old -> Triple(p, old, p.amount - old) } }
            .filter { (_, old, diff) -> diff >= 20_000 && diff * 100 / old >= 20 }
            .maxByOrNull { it.third }
        if (rise != null) {
            val (p, old, diff) = rise
            findings += Insight("${p.category?.name ?: "Tanpa kategori"} naik ${Rupiah.short(diff)} dari periode sebelumnya (${Rupiah.short(old)} jadi ${Rupiah.short(p.amount)}).", Insight.Tone.WARN, "rise")
        } else if (before.expense > 0 && spent > 0) {
            val diff = spent - before.expense
            findings += if (diff <= 0) Insight("Total keluar turun ${Rupiah.short(-diff)} dari periode sebelumnya.", Insight.Tone.GOOD, "trend")
            else Insight("Total keluar naik ${Rupiah.short(diff)} dari periode sebelumnya.", Insight.Tone.INFO, "trend")
        }

        // 4. Belanja kecil yang menumpuk.
        val small = expenses.filter { it.amount < SMALL }
        if (small.size >= 5 && spent > 0 && pct(small.sumOf { it.amount }, spent) >= 15) {
            val sum = small.sumOf { it.amount }
            findings += Insight("${small.size} kali belanja di bawah ${Rupiah.short(SMALL)}, totalnya ${Rupiah.short(sum)} (${pct(sum, spent)}% pengeluaran).", Insight.Tone.INFO, "small")
            advice += "Belanja kecil jadi ${Rupiah.short(sum)}. Beri jatah harian ${Rupiah.short(sum / days.coerceAtLeast(1) * 8 / 10)} untuk jajan supaya tetap terasa tapi tidak menumpuk."
        }

        // 5. Sekali belanja terbesar.
        expenses.maxByOrNull { it.amount }?.takeIf { spent > 0 && pct(it.amount, spent) >= 25 && expenses.size > 1 }?.let { big ->
            val what = big.merchant.ifBlank { categories.firstOrNull { it.id == big.categoryId }?.name ?: "satu belanja" }
            findings += Insight("Satu belanja besar: $what ${Rupiah.short(big.amount)}, ${pct(big.amount, spent)}% dari pengeluaran periode ini.", Insight.Tone.INFO, "big")
        }

        // 6. Yang belum berkategori membuat laporan kurang tajam.
        parts.firstOrNull { it.category == null }?.takeIf { spent > 0 && pct(it.amount, spent) >= 20 }?.let {
            advice += "${Rupiah.short(it.amount)} belum punya kategori. Beri kategori di Riwayat supaya laporan berikutnya lebih jelas."
        }

        if (advice.isEmpty() && spent > 0) {
            advice += "Polanya stabil. Pertahankan, dan pakai rata-rata ${Rupiah.short(spent / days.coerceAtLeast(1))} per hari sebagai patokan periode depan."
        }
        return PeriodReport(totals, parts, notice, findings.take(5), advice.take(3))
    }
}
