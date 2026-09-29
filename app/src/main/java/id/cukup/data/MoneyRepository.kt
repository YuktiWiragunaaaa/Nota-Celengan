package id.cukup.data

import androidx.room.withTransaction
import id.cukup.data.db.AllocationEntity
import id.cukup.data.db.CukupDatabase
import id.cukup.data.db.MerchantRuleEntity
import id.cukup.data.db.PocketEntity
import id.cukup.data.db.TransactionEntity
import id.cukup.domain.Allocation
import id.cukup.domain.Allocator
import id.cukup.domain.Balances
import id.cukup.domain.MerchantClassifier
import id.cukup.domain.NotificationParser
import id.cukup.domain.PayCycle
import id.cukup.domain.Pocket
import id.cukup.domain.PocketKind
import id.cukup.domain.Preset
import id.cukup.domain.Summary
import id.cukup.domain.Transaction
import id.cukup.domain.TxSource
import id.cukup.domain.TxStatus
import id.cukup.domain.TxType
import id.cukup.widget.WidgetRefresher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MoneyRepository @Inject constructor(
    private val db: CukupDatabase,
    private val settingsStore: SettingsStore,
    private val widgets: WidgetRefresher,
) {
    private val dao = db.dao()
    private val zone: ZoneId get() = ZoneId.systemDefault()

    val pockets: Flow<List<Pocket>> = dao.observePockets().map { list -> list.map { it.toDomain() } }
    val transactions: Flow<List<Transaction>> = dao.observeTransactions().map { list -> list.map { it.toDomain() } }
    val pending: Flow<List<Transaction>> = dao.observePending().map { list -> list.map { it.toDomain() } }
    val allocations: Flow<List<Allocation>> = dao.observeAllocations().map { list -> list.map { it.toDomain() } }
    val settings: Flow<Settings> = settingsStore.settings

    val summary: Flow<Summary> = combine(pockets, transactions, allocations, settings) { p, t, a, s ->
        summarize(p, t, a, s.payday)
    }

    fun cycle(payday: Int, today: LocalDate = LocalDate.now(zone)): PayCycle = PayCycle.of(today, payday)

    fun summarize(p: List<Pocket>, t: List<Transaction>, a: List<Allocation>, payday: Int): Summary {
        val today = LocalDate.now(zone)
        val cycle = cycle(payday, today)
        val start = cycle.start.atStartOfDay(zone).toInstant().toEpochMilli()
        return Balances.compute(p, t, a, start, cycle.daysLeft(today))
    }

    suspend fun currentSummary(): Summary =
        summarize(pockets.first(), transactions.first(), allocations.first(), settingsStore.current().payday)

    // ——— Pos ———

    /** Menyimpan urutan & persentase pos. Pos yang tidak ada di [pockets] diarsipkan (riwayatnya tetap). */
    suspend fun savePockets(pockets: List<Pocket>) {
        db.withTransaction {
            val existing = dao.pockets().map { it.id }.toSet()
            val keep = pockets.map { it.id }.filter { it != 0L }.toSet()
            dao.upsertPockets(pockets.mapIndexed { i, p -> PocketEntity.from(p.copy(sortOrder = i)) })
            val removed = existing - keep
            if (removed.isNotEmpty()) dao.archivePockets(removed.toList())
        }
        widgets.refresh()
    }

    suspend fun applyPreset(preset: Preset) {
        val current = dao.pockets().map { it.id }
        db.withTransaction {
            if (current.isNotEmpty()) dao.archivePockets(current)
            dao.upsertPockets(
                preset.pockets.mapIndexed { i, p ->
                    PocketEntity(name = p.name, emoji = p.emoji, percent = p.percent, kind = p.kind.name, tag = p.tag.name, sortOrder = i)
                },
            )
        }
        widgets.refresh()
    }

    // ——— Transaksi ———

    suspend fun addExpense(amount: Long, pocketId: Long, merchant: String, note: String, at: Long, isPaylater: Boolean) {
        dao.insertTransaction(
            TransactionEntity.from(
                Transaction(
                    type = TxType.EXPENSE, amount = amount, pocketId = pocketId, merchant = merchant.trim(),
                    note = note.trim(), occurredAt = at, isPaylater = isPaylater,
                ),
            ),
        )
        learn(merchant, pocketId)
        widgets.refresh()
    }

    /** [toPocketId] null = dibagi ke semua pos sesuai persentase saat ini. */
    suspend fun addIncome(amount: Long, merchant: String, note: String, at: Long, toPocketId: Long?) {
        saveIncome(
            Transaction(type = TxType.INCOME, amount = amount, toPocketId = toPocketId, merchant = merchant.trim(), note = note.trim(), occurredAt = at),
        )
        widgets.refresh()
    }

    suspend fun move(fromPocketId: Long, toPocketId: Long, amount: Long, note: String, at: Long) {
        dao.insertTransaction(
            TransactionEntity.from(
                Transaction(type = TxType.MOVE, amount = amount, pocketId = fromPocketId, toPocketId = toPocketId, note = note.trim(), occurredAt = at),
            ),
        )
        widgets.refresh()
    }

    /** Menyimpan pemasukan beserta alokasinya dalam satu transaksi database. */
    private suspend fun saveIncome(tx: Transaction): Long = db.withTransaction {
        val id = dao.upsertTransaction(TransactionEntity.from(tx))
        val txId = if (tx.id != 0L) tx.id else id
        dao.deleteAllocations(txId)
        if (tx.toPocketId == null && tx.status == TxStatus.CONFIRMED) {
            val pockets = dao.pockets().map { it.toDomain() }
            dao.insertAllocations(
                Allocator.split(tx.amount, pockets)
                    .filter { it.second > 0 }
                    .map { (p, v) -> AllocationEntity(transactionId = txId, pocketId = p.id, amount = v, percentAtTime = p.percent) },
            )
        }
        txId
    }

    /**
     * Mengonfirmasi transaksi dari notifikasi.
     * Pengeluaran: [pocketId] = pos sumber. Pemasukan: [pocketId] null = dibagi, selain itu masuk ke satu pos.
     */
    suspend fun confirm(id: Long, pocketId: Long?, isPaylater: Boolean? = null) {
        val tx = dao.transaction(id)?.toDomain() ?: return
        when (tx.type) {
            TxType.INCOME -> saveIncome(tx.copy(status = TxStatus.CONFIRMED, toPocketId = pocketId))
            else -> {
                dao.upsertTransaction(
                    TransactionEntity.from(
                        tx.copy(status = TxStatus.CONFIRMED, pocketId = pocketId ?: tx.pocketId, isPaylater = isPaylater ?: tx.isPaylater),
                    ),
                )
                if (pocketId != null) learn(tx.merchant, pocketId)
            }
        }
        widgets.refresh()
    }

    suspend fun dismiss(id: Long) {
        val tx = dao.transaction(id) ?: return
        dao.upsertTransaction(tx.copy(status = TxStatus.DISMISSED.name))
        dao.deleteAllocations(id)
        widgets.refresh()
    }

    suspend fun delete(id: Long) {
        dao.deleteTransaction(id)
        widgets.refresh()
    }

    /** Mengganti pos sebuah pengeluaran dan mengingat pilihan itu untuk merchant yang sama. */
    suspend fun changePocket(id: Long, pocketId: Long) {
        val tx = dao.transaction(id)?.toDomain() ?: return
        if (tx.type != TxType.EXPENSE) return
        dao.upsertTransaction(TransactionEntity.from(tx.copy(pocketId = pocketId)))
        learn(tx.merchant, pocketId)
        widgets.refresh()
    }

    private suspend fun learn(merchant: String, pocketId: Long) {
        val key = MerchantClassifier.key(merchant)
        if (key.length >= 2) dao.upsertRule(MerchantRuleEntity(key, pocketId, System.currentTimeMillis()))
    }

    suspend fun suggestPocket(merchant: String, isPaylaterPayment: Boolean = false): Pocket? {
        val pockets = dao.pockets().map { it.toDomain() }
        val learned = dao.merchantRules().associate { it.merchantKey to it.pocketId }
        return MerchantClassifier.suggest(merchant, pockets, learned, isPaylaterPayment)
    }

    // ——— Notifikasi ———

    /**
     * Membaca notifikasi dan menyimpannya sebagai transaksi. Mengembalikan true bila tersimpan.
     * Notifikasi yang sama (fingerprint) diabaikan; nominal serupa dari aplikasi lain dalam ±3 menit
     * tetap disimpan tetapi selalu masuk "Perlu dicek" (kemungkinan duplikat bank + e-wallet).
     */
    suspend fun ingest(packageName: String, title: String?, text: String?, postedAt: Long): Boolean {
        val parsed = NotificationParser.parse(packageName, title, text) ?: return false
        val s = settingsStore.current()
        if (!s.onboarded) return false
        val fingerprint = NotificationParser.fingerprint(packageName, parsed.amount, postedAt, parsed.merchant)
        val similar = dao.countSimilar(parsed.amount, parsed.type.name, postedAt - 180_000, postedAt + 180_000)
        val auto = s.autoConfirm && similar == 0
        val pocket = if (parsed.type == TxType.EXPENSE) suggestPocket(parsed.merchant, parsed.isDebtPayment) else null

        val tx = Transaction(
            type = parsed.type,
            amount = parsed.amount,
            pocketId = pocket?.id,
            merchant = parsed.merchant.ifBlank { if (parsed.isDebtPayment) "Bayar tagihan ${parsed.appLabel}" else "" },
            note = if (similar > 0) "Mungkin duplikat" else "",
            occurredAt = postedAt,
            source = TxSource.NOTIFICATION,
            sourceApp = parsed.appLabel,
            status = if (auto) TxStatus.CONFIRMED else TxStatus.PENDING,
            isPaylater = parsed.isPaylater,
            fingerprint = fingerprint,
        )
        val saved = if (tx.type == TxType.INCOME && auto) {
            saveIncome(tx) > 0
        } else {
            dao.insertTransaction(TransactionEntity.from(tx)) > 0
        }
        if (saved) widgets.refresh()
        return saved
    }

    suspend fun hasPockets(): Boolean = dao.pocketCount() > 0

    suspend fun pendingCount(): Int = pending.first().size

    fun spendPockets(all: List<Pocket>) = all.filter { it.kind != PocketKind.SAVE }
}
