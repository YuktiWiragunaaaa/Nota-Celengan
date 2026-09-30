package id.cukup.data

import androidx.room.withTransaction
import id.cukup.data.db.AccountEntity
import id.cukup.data.db.CategoryEntity
import id.cukup.data.db.CukupDatabase
import id.cukup.data.db.GoalEntity
import id.cukup.data.db.MerchantRuleEntity
import id.cukup.data.db.PlanPosEntity
import id.cukup.data.db.TransactionEntity
import id.cukup.domain.Account
import id.cukup.domain.AccountBalance
import id.cukup.domain.AccountKind
import id.cukup.domain.Category
import id.cukup.domain.CategoryKind
import id.cukup.domain.Goal
import id.cukup.domain.Ledger
import id.cukup.domain.MerchantClassifier
import id.cukup.domain.NotificationParser
import id.cukup.domain.PayCycle
import id.cukup.domain.PlanPos
import id.cukup.domain.PlanPreset
import id.cukup.domain.PlanStatus
import id.cukup.domain.Planner
import id.cukup.domain.Presets
import id.cukup.domain.Schedule
import id.cukup.domain.Totals
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

/** Semua yang dibutuhkan layar, dihitung sekali dari database. */
data class Overview(
    val settings: Settings,
    val accounts: List<AccountBalance>,
    /** Kategori aktif. */
    val categories: List<Category>,
    /** Semua kategori termasuk yang diarsipkan, untuk menamai transaksi lama. */
    val categoryById: Map<Long, Category>,
    val transactions: List<Transaction>,
    val plan: List<PlanPos>,
    val goals: List<Goal>,
    val cycle: PayCycle,
    val previous: PayCycle,
    val cycleStart: Long,
    val cycleEnd: Long,
    val previousStart: Long,
    /** Catatan periode ini. */
    val totals: Totals,
    val lastTotals: Totals,
    val planStatus: PlanStatus,
) {
    val netWorth: Long get() = Ledger.netWorth(accounts)
    val debt: Long get() = Ledger.debt(accounts)
    val accountById: Map<Long, Account> get() = accounts.associate { it.account.id to it.account }
    val pending: List<Transaction> get() = transactions.filter { it.status == TxStatus.PENDING }
    val confirmed: List<Transaction> get() = transactions.filter { it.status == TxStatus.CONFIRMED }
    val periodName: String get() = settings.schedule.periodName
    fun expenseCategories() = categories.filter { it.kind == CategoryKind.EXPENSE }
    fun incomeCategories() = categories.filter { it.kind == CategoryKind.INCOME }
}

@Singleton
class MoneyRepository @Inject constructor(
    private val db: CukupDatabase,
    private val settingsStore: SettingsStore,
    private val widgets: WidgetRefresher,
    private val alerts: BudgetAlerts,
) {
    private val dao = db.dao()
    private val zone: ZoneId get() = ZoneId.systemDefault()

    val settings: Flow<Settings> = settingsStore.settings

    private val records = combine(
        dao.observeAccounts(),
        dao.observeAllCategories(),
        dao.observeTransactions(),
    ) { a, c, t -> Triple(a.map { it.toDomain() }, c, t.map { it.toDomain() }) }

    private val planning = combine(dao.observePlan(), dao.observeGoals()) { p, g ->
        p.map { it.toDomain() } to g.map { it.toDomain() }
    }

    val overview: Flow<Overview> = combine(records, planning, settingsStore.settings) { (accounts, cats, txs), (plan, goals), s ->
        build(accounts, cats, txs, plan, goals, s)
    }

    private fun millis(d: LocalDate) = d.atStartOfDay(zone).toInstant().toEpochMilli()

    fun cycle(schedule: Schedule): PayCycle = PayCycle.of(LocalDate.now(zone), schedule)

    private fun build(
        accounts: List<Account>,
        cats: List<CategoryEntity>,
        txs: List<Transaction>,
        plan: List<PlanPos>,
        goals: List<Goal>,
        s: Settings,
    ): Overview {
        val today = LocalDate.now(zone)
        val cycle = PayCycle.of(today, s.schedule)
        val previous = PayCycle.of(cycle.start.minusDays(1), s.schedule)
        val start = millis(cycle.start)
        val end = millis(cycle.nextPayday)
        val prevStart = millis(previous.start)
        val totals = Ledger.totals(txs, start, end)
        val last = Ledger.totals(txs, prevStart, start)
        val categories = cats.map { it.toDomain() }
        val active = cats.filter { !it.archived }.map { it.toDomain() }
        val basis = Planner.basis(s.planBasis, totals.income, last.income)
        return Overview(
            settings = s,
            accounts = Ledger.balances(accounts, txs),
            categories = active,
            categoryById = categories.associateBy { it.id },
            transactions = txs,
            plan = plan,
            goals = goals,
            cycle = cycle,
            previous = previous,
            cycleStart = start,
            cycleEnd = end,
            previousStart = prevStart,
            totals = totals,
            lastTotals = last,
            planStatus = Planner.status(plan, categories, accounts, txs, basis, start, end, cycle.daysLeft(today)),
        )
    }

    suspend fun current(): Overview = overview.first()

    // ——— Pengenalan ———

    /** Isi awal: dompet dengan saldonya, kategori bawaan, dan (opsional) rencana. */
    suspend fun setup(accounts: List<Account>, preset: PlanPreset?) {
        db.withTransaction {
            db.clearAllTables()
            accounts.forEachIndexed { i, a -> dao.upsertAccount(AccountEntity.from(a.copy(id = 0, sortOrder = i))) }
            dao.upsertCategories(
                Presets.categories.mapIndexed { i, c ->
                    CategoryEntity(name = c.name, emoji = c.emoji, kind = c.kind.name, tag = c.tag.name, sortOrder = i)
                },
            )
            if (preset != null) writePreset(preset)
        }
        val first = dao.accounts().firstOrNull()?.id ?: 0
        settingsStore.update { it.copy(defaultAccountId = first) }
        changed()
    }

    // ——— Dompet ———

    suspend fun saveAccount(a: Account): Long {
        val order = if (a.id == 0L) dao.accounts().size else a.sortOrder
        val id = dao.upsertAccount(AccountEntity.from(a.copy(sortOrder = order)))
        changed()
        return if (a.id == 0L) id else a.id
    }

    /**
     * Menyamakan saldo dompet dengan kenyataan (mis. setelah cek aplikasi bank).
     * Saldo awal digeser, jadi riwayat transaksi tidak berubah.
     */
    suspend fun setAccountBalance(id: Long, actual: Long) {
        val a = dao.account(id) ?: return
        val now = current().accounts.firstOrNull { it.account.id == id }?.balance ?: return
        dao.upsertAccount(a.copy(initialBalance = a.initialBalance + (actual - now)))
        changed()
    }

    suspend fun archiveAccount(id: Long) {
        dao.archiveAccount(id)
        if (settingsStore.current().defaultAccountId == id) {
            val next = dao.accounts().firstOrNull()?.id ?: 0
            settingsStore.update { s -> s.copy(defaultAccountId = next) }
        }
        changed()
    }

    suspend fun setDefaultAccount(id: Long) = settingsStore.update { it.copy(defaultAccountId = id) }

    // ——— Kategori ———

    suspend fun saveCategory(c: Category): Long {
        val order = if (c.id == 0L) dao.categories().size else c.sortOrder
        val id = dao.upsertCategory(CategoryEntity.from(c.copy(sortOrder = order)))
        changed()
        return if (c.id == 0L) id else c.id
    }

    suspend fun archiveCategory(id: Long) {
        dao.archiveCategory(id)
        changed()
    }

    // ——— Transaksi ———

    /** Menyimpan transaksi baru atau hasil edit. */
    suspend fun save(t: Transaction) {
        val clean = t.copy(merchant = t.merchant.trim(), note = t.note.trim())
        if (clean.id == 0L) dao.insertTransaction(TransactionEntity.from(clean)) else dao.upsertTransaction(TransactionEntity.from(clean))
        if (clean.type != TxType.TRANSFER) clean.categoryId?.let { learn(clean.merchant, it) }
        changed()
        if (clean.type == TxType.EXPENSE && clean.id == 0L) runCatching { alerts.checkSingle(clean.amount, clean.merchant) }
    }

    suspend fun transaction(id: Long): Transaction? = dao.transaction(id)?.toDomain()

    /** Mengonfirmasi transaksi dari notifikasi dengan dompet & kategori pilihan pengguna. */
    suspend fun confirm(id: Long, accountId: Long?, categoryId: Long?) {
        val tx = dao.transaction(id)?.toDomain() ?: return
        save(tx.copy(status = TxStatus.CONFIRMED, accountId = accountId ?: tx.accountId, categoryId = categoryId ?: tx.categoryId))
    }

    suspend fun dismiss(id: Long) {
        val tx = dao.transaction(id) ?: return
        dao.upsertTransaction(tx.copy(status = TxStatus.DISMISSED.name))
        changed()
    }

    suspend fun delete(id: Long) {
        dao.deleteTransaction(id)
        changed()
    }

    private suspend fun learn(merchant: String, categoryId: Long) {
        val key = MerchantClassifier.key(merchant)
        if (key.length >= 2) dao.upsertRule(MerchantRuleEntity(key, categoryId, System.currentTimeMillis()))
    }

    suspend fun suggestCategory(merchant: String, kind: CategoryKind, isDebtPayment: Boolean = false): Category? {
        val cats = dao.categories().map { it.toDomain() }
        val learned = dao.merchantRules().associate { it.merchantKey to it.categoryId }
        return MerchantClassifier.suggest(merchant, cats, learned, kind, isDebtPayment)
    }

    // ——— Notifikasi ———

    /**
     * Membaca notifikasi dan menyimpannya sebagai transaksi. Mengembalikan true bila tersimpan.
     * Dompet ditebak dari nama aplikasi (mis. notifikasi GoPay → dompet bernama "GoPay").
     * Nominal serupa dalam ±3 menit selalu masuk "Perlu dicek" (kemungkinan duplikat bank + e-wallet).
     */
    suspend fun ingest(packageName: String, title: String?, text: String?, postedAt: Long): Boolean {
        val parsed = NotificationParser.parse(packageName, title, text) ?: return false
        val s = settingsStore.current()
        if (!s.onboarded) return false
        val fingerprint = NotificationParser.fingerprint(packageName, parsed.amount, postedAt, parsed.merchant)
        val similar = dao.countSimilar(parsed.amount, parsed.type.name, postedAt - 180_000, postedAt + 180_000)
        val auto = s.autoConfirm && similar == 0
        val kind = if (parsed.type == TxType.INCOME) CategoryKind.INCOME else CategoryKind.EXPENSE
        val category = suggestCategory(parsed.merchant, kind, parsed.isDebtPayment)
        val account = guessAccount(parsed.appLabel, parsed.isPaylater, s.defaultAccountId)

        val tx = Transaction(
            type = parsed.type,
            amount = parsed.amount,
            accountId = account,
            categoryId = category?.id,
            merchant = parsed.merchant.ifBlank { if (parsed.isDebtPayment) "Bayar tagihan ${parsed.appLabel}" else "" },
            note = if (similar > 0) "Mungkin duplikat" else "",
            occurredAt = postedAt,
            source = TxSource.NOTIFICATION,
            sourceApp = parsed.appLabel,
            status = if (auto) TxStatus.CONFIRMED else TxStatus.PENDING,
            fingerprint = fingerprint,
        )
        val saved = dao.insertTransaction(TransactionEntity.from(tx)) > 0
        if (saved) {
            changed()
            if (tx.type == TxType.EXPENSE) runCatching { alerts.checkSingle(tx.amount, tx.merchant) }
        }
        return saved
    }

    private suspend fun guessAccount(appLabel: String, paylater: Boolean, fallback: Long): Long? {
        val accounts = dao.accounts().map { it.toDomain() }
        if (paylater) accounts.firstOrNull { it.kind == AccountKind.PAYLATER }?.let { return it.id }
        val label = appLabel.lowercase()
        accounts.firstOrNull { a ->
            val n = a.name.lowercase()
            n.isNotBlank() && (label.contains(n) || n.contains(label.substringBefore(' ')))
        }?.let { return it.id }
        return accounts.firstOrNull { it.id == fallback }?.id ?: accounts.firstOrNull()?.id
    }

    // ——— Rencana ———

    private suspend fun writePreset(preset: PlanPreset) {
        val old = dao.plan().map { it.id }
        if (old.isNotEmpty()) {
            dao.unlinkPlan(old)
            dao.deletePlan(old)
        }
        val ids = dao.upsertPlan(
            preset.pos.mapIndexed { i, p -> PlanPosEntity(name = p.name, emoji = p.emoji, percent = p.percent, kind = p.kind.name, sortOrder = i) },
        )
        val cats = dao.categories()
        dao.upsertCategories(
            cats.map { c ->
                val index = preset.pos.indexOfFirst { c.tag in it.tags.map { t -> t.name } }
                c.copy(planId = if (c.kind == CategoryKind.EXPENSE.name && index >= 0) ids[index] else null)
            },
        )
    }

    suspend fun applyPlanPreset(preset: PlanPreset) {
        db.withTransaction { writePreset(preset) }
        changed()
    }

    /**
     * Menyimpan pos rencana dan ke pos mana tiap kategori dihitung.
     * Pos baru memakai id negatif sementara di [links]; diganti id sungguhan setelah disimpan.
     */
    suspend fun savePlan(pos: List<PlanPos>, links: Map<Long, Long?>) {
        db.withTransaction {
            val existing = dao.plan().map { it.id }.toSet()
            val keep = pos.map { it.id }.filter { it > 0 }.toSet()
            val removed = (existing - keep).toList()
            if (removed.isNotEmpty()) {
                dao.unlinkPlan(removed)
                dao.deletePlan(removed)
            }
            val saved = dao.upsertPlan(pos.mapIndexed { i, p -> PlanPosEntity.from(p.copy(id = p.id.coerceAtLeast(0), sortOrder = i)) })
            val realId = pos.mapIndexed { i, p -> p.id to (if (p.id > 0) p.id else saved[i]) }.toMap()
            val cats = dao.categories()
            dao.upsertCategories(
                cats.map { c ->
                    if (!links.containsKey(c.id)) c else c.copy(planId = links[c.id]?.let { realId[it] })
                },
            )
        }
        changed()
    }

    suspend fun clearPlan() {
        db.withTransaction {
            val ids = dao.plan().map { it.id }
            if (ids.isNotEmpty()) {
                dao.unlinkPlan(ids)
                dao.deletePlan(ids)
            }
        }
        changed()
    }

    suspend fun setPlanBasis(basis: id.cukup.domain.PlanBasis) {
        settingsStore.update { it.copy(planBasis = basis) }
        changed()
    }

    // ——— Target tabungan ———

    suspend fun saveGoal(g: Goal): Long {
        val id = dao.upsertGoal(GoalEntity.from(g))
        return if (g.id == 0L) id else g.id
    }

    /** Menambah (atau mengurangi, bila negatif) jumlah terkumpul target tanpa dompet. */
    suspend fun addToGoal(id: Long, amount: Long) {
        val g = dao.goal(id) ?: return
        dao.upsertGoal(g.copy(saved = (g.saved + amount).coerceAtLeast(0)))
    }

    suspend fun deleteGoal(id: Long) = dao.deleteGoal(id)

    // ——— Lain-lain ———

    /** Menghapus semua data dan pengaturan. Aplikasi kembali ke awal. */
    suspend fun eraseEverything() {
        db.withTransaction { db.clearAllTables() }
        settingsStore.update { Settings() }
        widgets.refresh()
    }

    /** Setelah data berubah: perbarui widget dan periksa rencana belanja. */
    private suspend fun changed() {
        widgets.refresh()
        runCatching { alerts.check(current().planStatus) }
    }
}
