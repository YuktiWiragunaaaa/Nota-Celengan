package id.cukup.ui.add

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import id.cukup.data.MoneyRepository
import id.cukup.data.Overview
import id.cukup.domain.CategoryKind
import id.cukup.domain.Rupiah
import id.cukup.domain.Transaction
import id.cukup.domain.TxStatus
import id.cukup.domain.TxType
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import javax.inject.Inject

data class AddState(
    val loaded: Boolean = false,
    val editingId: Long = 0,
    val type: TxType = TxType.EXPENSE,
    val amount: Long = 0,
    val accountId: Long? = null,
    val toAccountId: Long? = null,
    val categoryId: Long? = null,
    val merchant: String = "",
    val note: String = "",
    val daysAgo: Int = 0,
    /** Waktu asli transaksi yang diedit (dipertahankan bila tanggal tidak diubah). */
    val originalAt: Long = 0,
    val categoryTouched: Boolean = false,
    val saving: Boolean = false,
    /** Pesan konfirmasi yang sedang ditampilkan (null = tidak ada). */
    val confirm: String? = null,
    val overview: Overview? = null,
) {
    val canSave: Boolean
        get() = amount > 0 && !saving && accountId != null && when (type) {
            TxType.TRANSFER -> toAccountId != null && toAccountId != accountId
            else -> true
        }
}

sealed interface AddIntent {
    data class Type(val type: TxType) : AddIntent
    data class Amount(val value: Long) : AddIntent
    data class Account(val id: Long) : AddIntent
    data class ToAccount(val id: Long) : AddIntent
    data class Category(val id: Long) : AddIntent
    data class Merchant(val value: String) : AddIntent
    data class Note(val value: String) : AddIntent
    data class Day(val daysAgo: Int) : AddIntent
    data object Save : AddIntent
    data object ConfirmSave : AddIntent
    data object CancelConfirm : AddIntent
}

sealed interface AddEffect {
    data object Saved : AddEffect
}

@HiltViewModel
class AddViewModel @Inject constructor(
    private val repository: MoneyRepository,
    savedState: SavedStateHandle,
) : ViewModel() {

    private val zone = ZoneId.systemDefault()
    private val _state = MutableStateFlow(
        AddState(
            type = savedState.get<String>("type")?.let { runCatching { TxType.valueOf(it) }.getOrNull() } ?: TxType.EXPENSE,
            accountId = savedState.get<Long>("account")?.takeIf { it > 0 },
            editingId = savedState.get<Long>("edit") ?: 0,
            categoryId = savedState.get<Long>("category")?.takeIf { it > 0 },
            categoryTouched = (savedState.get<Long>("category") ?: 0) > 0,
        ),
    )
    val state: StateFlow<AddState> = _state.asStateFlow()

    private val _effects = Channel<AddEffect>(Channel.BUFFERED)
    val effects = _effects.receiveAsFlow()

    private var suggestJob: Job? = null

    init {
        viewModelScope.launch {
            val first = repository.overview.first()
            val editing = _state.value.editingId.takeIf { it > 0 }?.let { repository.transaction(it) }
            _state.update { s ->
                if (editing != null) {
                    val days = ChronoUnit.DAYS.between(
                        java.time.Instant.ofEpochMilli(editing.occurredAt).atZone(zone).toLocalDate(), LocalDate.now(zone),
                    ).toInt()
                    s.copy(
                        loaded = true, type = editing.type, amount = editing.amount, accountId = editing.accountId,
                        toAccountId = editing.toAccountId, categoryId = editing.categoryId, merchant = editing.merchant,
                        note = editing.note, daysAgo = days, originalAt = editing.occurredAt, categoryTouched = true,
                    )
                } else {
                    val default = first.accounts.firstOrNull { it.account.id == first.settings.defaultAccountId }?.account?.id
                        ?: first.accounts.firstOrNull()?.account?.id
                    s.copy(loaded = true, accountId = s.accountId ?: default).withDefaultCategory(first)
                }
            }
            repository.overview.collect { o -> _state.update { it.copy(overview = o) } }
        }
    }

    private fun AddState.withDefaultCategory(o: Overview): AddState {
        if (categoryTouched || type == TxType.TRANSFER) return this
        val kind = if (type == TxType.INCOME) CategoryKind.INCOME else CategoryKind.EXPENSE
        val valid = o.categories.any { it.id == categoryId && it.kind == kind }
        return if (valid) this else copy(categoryId = null)
    }

    fun onIntent(intent: AddIntent) {
        when (intent) {
            is AddIntent.Type -> _state.update { s ->
                val next = s.copy(type = intent.type, categoryTouched = false, categoryId = null)
                s.overview?.let { next.withDefaultCategory(it) } ?: next
            }
            is AddIntent.Amount -> _state.update { it.copy(amount = intent.value) }
            is AddIntent.Account -> _state.update {
                it.copy(accountId = intent.id, toAccountId = it.toAccountId.takeIf { to -> to != intent.id })
            }
            is AddIntent.ToAccount -> _state.update { it.copy(toAccountId = intent.id) }
            is AddIntent.Category -> _state.update { it.copy(categoryId = intent.id, categoryTouched = true) }
            is AddIntent.Merchant -> {
                _state.update { it.copy(merchant = intent.value) }
                suggest(intent.value)
            }
            is AddIntent.Note -> _state.update { it.copy(note = intent.value) }
            is AddIntent.Day -> _state.update { it.copy(daysAgo = intent.daysAgo) }
            AddIntent.Save -> requestSave()
            AddIntent.ConfirmSave -> {
                _state.update { it.copy(confirm = null) }
                save()
            }
            AddIntent.CancelConfirm -> _state.update { it.copy(confirm = null) }
        }
    }

    /** Menebak kategori dari nama merchant, selama pengguna belum memilih sendiri. */
    private fun suggest(merchant: String) {
        val s = _state.value
        if (s.categoryTouched || s.type == TxType.TRANSFER || merchant.isBlank()) return
        suggestJob?.cancel()
        suggestJob = viewModelScope.launch {
            delay(250)
            val kind = if (s.type == TxType.INCOME) CategoryKind.INCOME else CategoryKind.EXPENSE
            val guess = repository.suggestCategory(merchant, kind)
            _state.update { if (it.categoryTouched || guess == null) it else it.copy(categoryId = guess.id) }
        }
    }

    /** Minta konfirmasi dulu bila belanja melebihi batas sekali belanja atau membuat dompet minus. */
    private fun requestSave() {
        val s = _state.value
        if (!s.canSave) return
        val o = s.overview
        if (s.type == TxType.EXPENSE && s.editingId == 0L && o != null) {
            val limit = o.settings.singleLimit
            val balance = o.accounts.firstOrNull { it.account.id == s.accountId }
            val reasons = buildList {
                if (limit > 0 && s.amount > limit) add("Ini di atas batas sekali belanjamu (${Rupiah.format(limit)}).")
                if (balance != null && balance.account.kind != id.cukup.domain.AccountKind.PAYLATER && balance.balance - s.amount < 0) {
                    add("Saldo ${balance.account.name} jadi minus ${Rupiah.format(s.amount - balance.balance)}. Mungkin saldonya belum dicatat?")
                }
            }
            if (reasons.isNotEmpty()) {
                _state.update { it.copy(confirm = reasons.joinToString("\n\n")) }
                return
            }
        }
        save()
    }

    private fun save() {
        val s = _state.value
        if (!s.canSave) return
        _state.update { it.copy(saving = true) }
        viewModelScope.launch {
            val today = LocalDate.now(zone)
            val at = if (s.originalAt > 0 && today.minusDays(s.daysAgo.toLong()) ==
                java.time.Instant.ofEpochMilli(s.originalAt).atZone(zone).toLocalDate()
            ) {
                s.originalAt
            } else if (s.daysAgo == 0) {
                System.currentTimeMillis()
            } else {
                today.minusDays(s.daysAgo.toLong()).atTime(LocalTime.NOON).atZone(zone).toInstant().toEpochMilli()
            }
            val old = s.editingId.takeIf { it > 0 }?.let { repository.transaction(it) }
            repository.save(
                Transaction(
                    id = s.editingId,
                    type = s.type,
                    amount = s.amount,
                    accountId = s.accountId,
                    toAccountId = if (s.type == TxType.TRANSFER) s.toAccountId else null,
                    categoryId = if (s.type == TxType.TRANSFER) null else s.categoryId,
                    merchant = if (s.type == TxType.TRANSFER) "" else s.merchant,
                    note = s.note,
                    occurredAt = at,
                    source = old?.source ?: id.cukup.domain.TxSource.MANUAL,
                    sourceApp = old?.sourceApp,
                    status = TxStatus.CONFIRMED,
                    fingerprint = old?.fingerprint,
                ),
            )
            _effects.send(AddEffect.Saved)
        }
    }
}
