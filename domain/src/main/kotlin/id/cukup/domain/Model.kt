package id.cukup.domain

/*
 * Cukup v0.6 memisahkan dua hal:
 *
 * CATATAN — apa yang benar-benar terjadi.
 *   Dompet (tunai, rekening, e-wallet) punya saldo sungguhan.
 *   Setiap transaksi mengubah saldo dompet: Keluar mengurangi, Masuk menambah, Pindah memindahkan.
 *   Kategori hanya label ("Makan", "Transport") supaya tahu uang habis ke mana.
 *
 * RENCANA — batas yang kamu tetapkan sendiri. Tidak memindahkan uang apa pun.
 *   Pos rencana (mis. Kebutuhan 50%, Keinginan 30%, Tabungan 20%) dihitung dari perkiraan uang masuk.
 *   Cukup lalu membandingkan rencana itu dengan catatanmu.
 *   Target tabungan juga bagian dari rencana.
 */

/** Jenis dompet. PAYLATER = hutang: saldonya biasanya minus. */
enum class AccountKind { CASH, BANK, EWALLET, SAVINGS, PAYLATER }

/** Tempat uangmu berada. Saldo = [initialBalance] + semua transaksi yang menyentuhnya. */
data class Account(
    val id: Long,
    val name: String,
    val emoji: String,
    val kind: AccountKind,
    /** Saldo saat dompet mulai dicatat. */
    val initialBalance: Long = 0,
    val sortOrder: Int = 0,
    val color: Int? = null,
    /** Pos rencana tempat dompet ini dikelompokkan (mis. Krom → Investasi). null = belum dikelompokkan. */
    val planId: Long? = null,
)

/** Kategori untuk uang keluar atau uang masuk. */
enum class CategoryKind { EXPENSE, INCOME }

/** Makna kategori, dipakai untuk menebak kategori dari nama merchant dan pos rencana bawaan. */
enum class Tag { FOOD, FUN, SHOPPING, TRANSPORT, BILLS, HEALTH, EDUCATION, FAMILY, DEBT, SAVINGS, SALARY, OTHER }

data class Category(
    val id: Long,
    val name: String,
    val emoji: String,
    val kind: CategoryKind,
    val tag: Tag = Tag.OTHER,
    val sortOrder: Int = 0,
    val color: Int? = null,
    /** Pos rencana tempat pengeluaran kategori ini dihitung. null = tidak masuk rencana mana pun. */
    val planId: Long? = null,
)

enum class TxType { EXPENSE, INCOME, TRANSFER }
enum class TxStatus { CONFIRMED, PENDING, DISMISSED }
enum class TxSource { MANUAL, NOTIFICATION }

/**
 * Satu kejadian uang. Nominal selalu positif dalam rupiah penuh.
 * - EXPENSE: keluar dari [accountId], kategori [categoryId].
 * - INCOME: masuk ke [accountId], kategori [categoryId].
 * - TRANSFER: dari [accountId] ke [toAccountId]. Bukan pengeluaran, bukan pemasukan.
 */
data class Transaction(
    val id: Long = 0,
    val type: TxType,
    val amount: Long,
    val accountId: Long? = null,
    val toAccountId: Long? = null,
    val categoryId: Long? = null,
    val merchant: String = "",
    val note: String = "",
    val occurredAt: Long,
    val source: TxSource = TxSource.MANUAL,
    val sourceApp: String? = null,
    val status: TxStatus = TxStatus.CONFIRMED,
    val fingerprint: String? = null,
)

/** Pos rencana hanya membatasi belanja (SPEND) atau menargetkan sisihan (SAVE). */
enum class PlanKind { SPEND, SAVE }

/** Satu pos rencana, mis. "Kebutuhan 50%". */
data class PlanPos(
    val id: Long,
    val name: String,
    val emoji: String,
    val percent: Int,
    val kind: PlanKind,
    val sortOrder: Int = 0,
    val color: Int? = null,
    /** Batas bernominal tetap (rupiah). Bila > 0, [percent] diabaikan. */
    val amount: Long = 0,
    /** Jangka batas bernominal tetap. Pos berpersen selalu satu periode gajian. */
    val period: PosPeriod = PosPeriod.CYCLE,
) {
    val fixed: Boolean get() = amount > 0

    /** "50%" atau "Rp300 rb/minggu". */
    val share: String get() = if (fixed) "${Rupiah.short(amount)}/${period.word}" else "$percent%"
}

/** Jangka sebuah pos: satu periode gajian, per minggu (Senin–Minggu), atau per hari. */
enum class PosPeriod(val word: String) { CYCLE("gajian"), WEEK("minggu"), DAY("hari") }

/**
 * Dari mana rencana menghitung "uang masuk" yang dibagi ke pos.
 * - FIXED: angka tetap yang kamu isi (mis. gaji Rp4 jt).
 * - LAST_PERIOD: total uang masuk periode lalu (dari catatan).
 * - THIS_PERIOD: total uang masuk periode ini (dari catatan).
 */
data class PlanBasis(val mode: Mode = Mode.FIXED, val fixedAmount: Long = 0) {
    enum class Mode { FIXED, LAST_PERIOD, THIS_PERIOD }
}

/** Target tabungan. Bila [accountId] diisi, terkumpul = saldo dompet itu; bila tidak, [saved] diisi sendiri. */
data class Goal(
    val id: Long,
    val name: String,
    val emoji: String,
    val target: Long,
    val saved: Long = 0,
    val accountId: Long? = null,
    val color: Int? = null,
    val sortOrder: Int = 0,
)
