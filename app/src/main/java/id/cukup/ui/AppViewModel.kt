package id.cukup.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import id.cukup.data.MoneyRepository
import id.cukup.data.Overview
import id.cukup.data.Settings
import id.cukup.data.SettingsStore
import id.cukup.domain.Account
import id.cukup.domain.Category
import id.cukup.domain.Goal
import id.cukup.domain.PlanBasis
import id.cukup.domain.PlanPos
import id.cukup.domain.PlanPreset
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Satu ViewModel untuk layar-layar yang hanya membaca [Overview] dan memanggil aksi sederhana.
 * Layar dengan formulir panjang (Catat, Pengenalan) punya ViewModel sendiri.
 */
@HiltViewModel
class AppViewModel @Inject constructor(
    val repository: MoneyRepository,
    private val store: SettingsStore,
    private val backup: id.cukup.data.Backup,
    private val noticeLog: id.cukup.data.NoticeLog,
) : ViewModel() {

    /** Notifikasi keuangan terakhir + kesehatan pembaca notifikasi. */
    val notices = noticeLog.state
    fun dismissGap() = noticeLog.dismissGap()
    fun clearNotices() = noticeLog.clear()

    /** Hasil cadangan/pulihkan terakhir untuk ditampilkan sebagai pesan singkat. */
    val backupMessage = kotlinx.coroutines.flow.MutableStateFlow<String?>(null)

    fun exportBackup(uri: android.net.Uri) = go {
        backupMessage.value = runCatching { backup.export(uri) }
            .fold({ "Tersimpan: ${it.accounts} dompet, ${it.transactions} catatan." }, { "Gagal menyimpan: ${it.message}" })
    }

    /** Pindah otomatis yang keliru: kembalikan jadi Keluar + Masuk. */
    fun splitTransfer(id: Long) = go { repository.splitTransfer(id) }

    /** Ganti dompet sebuah catatan; untuk notifikasi, aplikasi itu ikut diarahkan ke dompet ini. */
    fun moveTo(tx: id.cukup.domain.Transaction, accountId: Long) = go { repository.save(tx.copy(accountId = accountId)) }

    /** Cadangan otomatis di HP, terbaru dulu. */
    fun autoBackups(): List<java.io.File> = backup.autoBackups()

    fun restoreAuto(file: java.io.File) = go {
        backupMessage.value = runCatching { backup.restore(file) }
            .fold({ "Dipulihkan dari ${file.nameWithoutExtension.removePrefix("auto-")}: ${it.accounts} dompet, ${it.transactions} catatan." }, { "Gagal memulihkan: ${it.message}" })
    }

    fun restoreBackup(uri: android.net.Uri, onDone: () -> Unit = {}) = go {
        backupMessage.value = runCatching { backup.restore(uri) }
            .fold({ onDone(); "Dipulihkan: ${it.accounts} dompet, ${it.transactions} catatan." }, { "Gagal memulihkan: ${it.message}" })
    }

    val overview: StateFlow<Overview?> = repository.overview.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private fun go(block: suspend () -> Unit) {
        viewModelScope.launch { block() }
    }

    fun settings(transform: (Settings) -> Settings) = go { store.update(transform) }

    /** Notifikasi aplikasi [brandKey] masuk ke dompet [accountId]; null = lepas tautan. */
    fun linkApp(brandKey: String, accountId: Long?) = go {
        store.update { s -> s.copy(appLinks = if (accountId == null) s.appLinks - brandKey else s.appLinks + (brandKey to accountId)) }
    }

    // Dompet
    fun saveAccount(a: Account) = go { repository.saveAccount(a) }
    fun setAccountBalance(id: Long, actual: Long) = go { repository.setAccountBalance(id, actual) }
    fun archiveAccount(id: Long) = go { repository.archiveAccount(id) }
    fun setDefaultAccount(id: Long) = go { repository.setDefaultAccount(id) }

    // Kategori
    fun saveCategory(c: Category) = go { repository.saveCategory(c) }
    fun archiveCategory(id: Long) = go { repository.archiveCategory(id) }

    // Transaksi
    fun confirm(id: Long, accountId: Long?, categoryId: Long?) = go { repository.confirm(id, accountId, categoryId) }
    fun dismiss(id: Long) = go { repository.dismiss(id) }
    fun delete(id: Long) = go { repository.delete(id) }

    // Rencana
    fun applyPlanPreset(p: PlanPreset) = go { repository.applyPlanPreset(p) }
    fun savePlan(pos: List<PlanPos>, links: Map<Long, Long?>) = go { repository.savePlan(pos, links) }
    fun clearPlan() = go { repository.clearPlan() }
    fun setPlanBasis(b: PlanBasis) = go { repository.setPlanBasis(b) }

    // Target
    fun saveGoal(g: Goal) = go { repository.saveGoal(g) }
    fun addToGoal(id: Long, amount: Long) = go { repository.addToGoal(id, amount) }
    fun deleteGoal(id: Long) = go { repository.deleteGoal(id) }

    fun chart(type: String) = settings { it.copy(chart = type) }
    fun eraseEverything() = go { repository.eraseEverything() }
}
