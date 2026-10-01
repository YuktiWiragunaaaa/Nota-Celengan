package id.cukup.data

import android.content.Context
import android.net.Uri
import androidx.room.withTransaction
import dagger.hilt.android.qualifiers.ApplicationContext
import id.cukup.data.db.AccountEntity
import id.cukup.data.db.CategoryEntity
import id.cukup.data.db.CukupDao
import id.cukup.data.db.CukupDatabase
import id.cukup.data.db.GoalEntity
import id.cukup.data.db.MerchantRuleEntity
import id.cukup.data.db.PlanPosEntity
import id.cukup.data.db.TransactionEntity
import id.cukup.domain.Frequency
import id.cukup.domain.PlanBasis
import id.cukup.widget.WidgetRefresher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.io.File
import java.time.LocalDate
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Cadangan ke file JSON pilihan pengguna (Drive, folder Download, dll.).
 * PIN tidak ikut, jadi file cadangan tidak bisa dipakai membuka kunci.
 */
@Singleton
class Backup @Inject constructor(
    @ApplicationContext private val context: Context,
    private val db: CukupDatabase,
    private val settings: SettingsStore,
    private val widgets: WidgetRefresher,
) {
    private val dao: CukupDao = db.dao()

    class Summary(val accounts: Int, val transactions: Int)

    suspend fun export(uri: Uri): Summary = withContext(Dispatchers.IO) {
        val (root, summary) = snapshot()
        val out = context.contentResolver.openOutputStream(uri, "wt") ?: error("Tidak bisa menulis file")
        out.bufferedWriter().use { it.write(root.toString()) }
        summary
    }

    private suspend fun snapshot(): Pair<JSONObject, Summary> {
        val s = settings.current()
        val txs = dao.allTransactions()
        val accounts = dao.allAccounts()
        val root = JSONObject()
            .put("app", "cukup")
            .put("version", VERSION)
            .put("exportedAt", System.currentTimeMillis())
            .put(
                "settings",
                JSONObject()
                    .put("name", s.name)
                    .put("frequency", s.schedule.frequency.name)
                    .put("monthDay", s.schedule.monthDay)
                    .put("weekday", s.schedule.weekday)
                    .put("anchor", s.schedule.anchor)
                    .put("autoConfirm", s.autoConfirm)
                    .put("budgetAlerts", s.budgetAlerts)
                    .put("singleLimit", s.singleLimit)
                    .put("basisMode", s.planBasis.mode.name)
                    .put("basisAmount", s.planBasis.fixedAmount)
                    .put("defaultAccountId", s.defaultAccountId)
                    .put("chart", s.chart)
                    .put("theme", s.theme)
                    .put("widgetHide", s.widgetHide)
                    .put("appLinks", JSONObject(s.appLinks as Map<*, *>)),
            )
            .put("accounts", accounts.json { a -> put("id", a.id).put("name", a.name).put("emoji", a.emoji).put("kind", a.kind).put("initialBalance", a.initialBalance).put("sortOrder", a.sortOrder).put("color", a.color).put("archived", a.archived).put("planId", a.planId) })
            .put("categories", dao.allCategories().json { c -> put("id", c.id).put("name", c.name).put("emoji", c.emoji).put("kind", c.kind).put("tag", c.tag).put("sortOrder", c.sortOrder).put("color", c.color).put("planId", c.planId).put("archived", c.archived) })
            .put("transactions", txs.json { t -> put("id", t.id).put("type", t.type).put("amount", t.amount).put("accountId", t.accountId).put("toAccountId", t.toAccountId).put("categoryId", t.categoryId).put("merchant", t.merchant).put("note", t.note).put("occurredAt", t.occurredAt).put("source", t.source).put("sourceApp", t.sourceApp).put("status", t.status).put("fingerprint", t.fingerprint) })
            .put("plan", dao.plan().json { p -> put("id", p.id).put("name", p.name).put("emoji", p.emoji).put("percent", p.percent).put("kind", p.kind).put("sortOrder", p.sortOrder).put("color", p.color).put("amount", p.amount).put("period", p.period) })
            .put("goals", dao.allGoals().json { g -> put("id", g.id).put("name", g.name).put("emoji", g.emoji).put("target", g.target).put("saved", g.saved).put("accountId", g.accountId).put("color", g.color).put("sortOrder", g.sortOrder) })
            .put("rules", dao.merchantRules().json { r -> put("merchantKey", r.merchantKey).put("categoryId", r.categoryId).put("updatedAt", r.updatedAt) })
        return root to Summary(accounts.count { !it.archived }, txs.size)
    }

    /** Mengganti semua data dengan isi cadangan. Melempar exception bila file bukan cadangan Cukup. */
    suspend fun restore(uri: Uri): Summary = withContext(Dispatchers.IO) {
        val input = context.contentResolver.openInputStream(uri) ?: error("File tidak bisa dibuka")
        restoreText(input.bufferedReader().use { it.readText() })
    }

    suspend fun restore(file: File): Summary = withContext(Dispatchers.IO) { restoreText(file.readText()) }

    private suspend fun restoreText(text: String): Summary {
        val root = JSONObject(text)
        require(root.optString("app") == "cukup") { "Ini bukan file cadangan Cukup" }
        require(root.optInt("version") <= VERSION) { "Cadangan dari versi Cukup yang lebih baru" }
        // Simpan isi sekarang dulu, supaya pemulihan yang keliru pun bisa dibatalkan.
        runCatching { if (settings.current().onboarded) File(DbGuard.backupDir(context), "before-restore-${System.currentTimeMillis()}.json").writeText(snapshot().first.toString()) }

        val accounts = root.list("accounts") { AccountEntity(getLong("id"), getString("name"), getString("emoji"), getString("kind"), getLong("initialBalance"), getInt("sortOrder"), intOrNull("color"), optBoolean("archived"), longOrNull("planId")) }
        val categories = root.list("categories") { CategoryEntity(getLong("id"), getString("name"), getString("emoji"), getString("kind"), getString("tag"), getInt("sortOrder"), intOrNull("color"), longOrNull("planId"), optBoolean("archived")) }
        val txs = root.list("transactions") { TransactionEntity(getLong("id"), getString("type"), getLong("amount"), longOrNull("accountId"), longOrNull("toAccountId"), longOrNull("categoryId"), optString("merchant"), optString("note"), getLong("occurredAt"), getString("source"), stringOrNull("sourceApp"), getString("status"), stringOrNull("fingerprint")) }
        val plan = root.list("plan") { PlanPosEntity(getLong("id"), getString("name"), getString("emoji"), getInt("percent"), getString("kind"), getInt("sortOrder"), intOrNull("color"), optLong("amount"), optString("period", "CYCLE")) }
        val goals = root.list("goals") { GoalEntity(getLong("id"), getString("name"), getString("emoji"), getLong("target"), getLong("saved"), longOrNull("accountId"), intOrNull("color"), getInt("sortOrder")) }
        val rules = root.list("rules") { MerchantRuleEntity(getString("merchantKey"), getLong("categoryId"), getLong("updatedAt")) }

        // Uji semua baris dulu (jenis, status, dll. harus dikenal) sebelum data lama dihapus.
        runCatching {
            accounts.forEach { it.toDomain() }
            categories.forEach { it.toDomain() }
            txs.forEach { it.toDomain() }
            plan.forEach { it.toDomain() }
            goals.forEach { it.toDomain() }
        }.onFailure { throw IllegalArgumentException("File cadangan rusak: ${it.message}") }

        db.withTransaction {
            db.clearAllTables()
            dao.restoreAccounts(accounts)
            dao.restorePlan(plan)
            dao.restoreCategories(categories)
            dao.restoreTransactions(txs)
            dao.restoreGoals(goals)
            dao.restoreRules(rules)
        }
        val s = root.getJSONObject("settings")
        settings.update { old ->
            old.copy(
                name = s.optString("name"),
                schedule = old.schedule.copy(
                    frequency = runCatching { Frequency.valueOf(s.getString("frequency")) }.getOrDefault(old.schedule.frequency),
                    monthDay = s.optInt("monthDay", old.schedule.monthDay),
                    weekday = s.optInt("weekday", old.schedule.weekday),
                    anchor = s.optLong("anchor", old.schedule.anchor),
                ),
                onboarded = true,
                autoConfirm = s.optBoolean("autoConfirm"),
                budgetAlerts = s.optBoolean("budgetAlerts", true),
                singleLimit = s.optLong("singleLimit"),
                planBasis = PlanBasis(
                    mode = runCatching { PlanBasis.Mode.valueOf(s.getString("basisMode")) }.getOrDefault(PlanBasis.Mode.FIXED),
                    fixedAmount = s.optLong("basisAmount"),
                ),
                defaultAccountId = s.optLong("defaultAccountId"),
                chart = s.optString("chart", "DONUT"),
                theme = s.optString("theme", old.theme),
                widgetHide = s.optBoolean("widgetHide", old.widgetHide),
                appLinks = s.optJSONObject("appLinks")?.let { o -> o.keys().asSequence().associateWith { o.getLong(it) } } ?: old.appLinks,
            )
        }
        widgets.refresh()
        return Summary(accounts.count { !it.archived }, txs.size)
    }

    /**
     * Cadangan harian otomatis ke penyimpanan aplikasi (maks. sekali sehari, 7 terakhir disimpan).
     * Dilewati kalau belum ada data, supaya cadangan kosong tidak menimpa yang berisi.
     */
    suspend fun autoBackup(): File? = withContext(Dispatchers.IO) {
        if (!settings.current().onboarded) return@withContext null
        val dir = DbGuard.backupDir(context)
        val today = File(dir, "auto-${LocalDate.now()}.json")
        if (today.exists()) return@withContext null
        val (root, summary) = snapshot()
        if (summary.accounts == 0) return@withContext null
        val tmp = File(dir, today.name + ".tmp")
        tmp.writeText(root.toString())
        if (!tmp.renameTo(today)) { tmp.delete(); return@withContext null }
        autoBackups().drop(KEEP_AUTO).forEach { it.delete() }
        today
    }

    /** Salinan pengaman sebelum Cukup mengubah catatan lama secara otomatis; muncul di daftar cadangan otomatis. */
    suspend fun safetyCopy(tag: String): File? = withContext(Dispatchers.IO) {
        val (root, summary) = snapshot()
        if (summary.accounts == 0) return@withContext null
        File(DbGuard.backupDir(context), "auto-${LocalDate.now()}-sebelum-$tag.json").also { it.writeText(root.toString()) }
    }

    /** Cadangan otomatis, terbaru dulu. */
    fun autoBackups(): List<File> =
        DbGuard.backupDir(context).listFiles { f -> f.isFile && f.name.startsWith("auto-") && f.name.endsWith(".json") }
            ?.sortedByDescending { it.name }.orEmpty()

    private companion object {
        const val VERSION = 1
        const val KEEP_AUTO = 7
    }
}

private inline fun <T> List<T>.json(crossinline fill: JSONObject.(T) -> Unit): JSONArray =
    JSONArray().also { arr -> forEach { arr.put(JSONObject().apply { fill(it) }) } }

private fun <T> JSONObject.list(key: String, read: JSONObject.() -> T): List<T> {
    val arr = optJSONArray(key) ?: return emptyList()
    return (0 until arr.length()).map { arr.getJSONObject(it).read() }
}

private fun JSONObject.longOrNull(k: String): Long? = if (isNull(k)) null else getLong(k)
private fun JSONObject.intOrNull(k: String): Int? = if (isNull(k)) null else getInt(k)
private fun JSONObject.stringOrNull(k: String): String? = if (isNull(k)) null else getString(k)
