package id.cukup.domain

/** Jenis pos menentukan bagaimana saldonya dihitung di "aman dipakai". */
enum class PocketKind { SPEND, SAVE, DEBT }

/** Petunjuk makna pos, dipakai untuk menebak pos dari nama merchant. */
enum class PocketTag { NEEDS, WANTS, FOOD, BILLS, TRANSPORT, FUN, SHOPPING, DEBT, SAVINGS, OTHER }

data class Pocket(
    val id: Long,
    val name: String,
    val emoji: String,
    val percent: Int,
    val kind: PocketKind,
    val tag: PocketTag = PocketTag.OTHER,
    val sortOrder: Int = 0,
    /** Warna pilihan pengguna (ARGB). null = warna bawaan sesuai urutan. */
    val color: Int? = null,
    /** Target tabungan (rupiah). null = tanpa target. */
    val target: Long? = null,
)

/** ADJUST = penyesuaian saldo kantong (bukan belanja, bukan pemasukan). */
enum class TxType { INCOME, EXPENSE, MOVE, ADJUST }
enum class TxStatus { CONFIRMED, PENDING, DISMISSED }
enum class TxSource { MANUAL, NOTIFICATION }

/**
 * Satu kejadian uang. Nominal selalu positif dalam rupiah penuh.
 * - EXPENSE: keluar dari [pocketId].
 * - INCOME: dibagi ke semua pos (lihat [Allocation]) atau, bila [toPocketId] diisi, masuk ke satu pos saja.
 * - MOVE: pindah dari [pocketId] ke [toPocketId].
 */
data class Transaction(
    val id: Long = 0,
    val type: TxType,
    val amount: Long,
    val pocketId: Long? = null,
    val toPocketId: Long? = null,
    val merchant: String = "",
    val note: String = "",
    val occurredAt: Long,
    val source: TxSource = TxSource.MANUAL,
    val sourceApp: String? = null,
    val status: TxStatus = TxStatus.CONFIRMED,
    val isPaylater: Boolean = false,
    val fingerprint: String? = null,
)

/** Bagian pemasukan yang masuk ke satu pos. Disimpan agar riwayat tidak berubah saat persentase diubah. */
data class Allocation(
    val transactionId: Long,
    val pocketId: Long,
    val amount: Long,
    val percentAtTime: Int,
)

/**
 * Cara menghitung jatah belanja per periode.
 * - POCKETS: bagian kantong belanja dari uang yang masuk periode ini.
 * - LAST_INCOME: [percent]% dari total uang masuk periode sebelumnya
 *   (bila periode lalu kosong, memakai uang masuk periode ini).
 */
data class BudgetRule(val mode: Mode = Mode.POCKETS, val percent: Int = 50) {
    enum class Mode { POCKETS, LAST_INCOME }
}
