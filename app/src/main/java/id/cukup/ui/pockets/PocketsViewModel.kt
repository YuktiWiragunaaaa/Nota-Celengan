package id.cukup.ui.pockets

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import id.cukup.data.MoneyRepository
import id.cukup.domain.Allocation
import id.cukup.domain.Allocator
import id.cukup.domain.Pocket
import id.cukup.domain.PocketBalance
import id.cukup.domain.Summary
import id.cukup.domain.Transaction
import id.cukup.domain.TxStatus
import id.cukup.domain.TxType
import id.cukup.ui.components.EditablePocket
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class PocketsState(
    val summary: Summary? = null,
    val editing: List<EditablePocket>? = null,
) {
    val canSave: Boolean
        get() = editing != null && editing.sumOf { it.percent } == 100 && editing.all { it.name.isNotBlank() }
}

@HiltViewModel
class PocketsViewModel @Inject constructor(private val repository: MoneyRepository) : ViewModel() {

    private val editing = MutableStateFlow<List<EditablePocket>?>(null)

    val state: StateFlow<PocketsState> = combine(repository.summary, editing) { summary, e -> PocketsState(summary, e) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), PocketsState())

    fun startEditing() {
        viewModelScope.launch {
            editing.value = repository.pockets.first().mapIndexed { i, p -> EditablePocket.from(p, i) }
        }
    }

    fun edit(list: List<EditablePocket>) {
        editing.value = list
    }

    fun cancel() {
        editing.value = null
    }

    fun save(onDone: () -> Unit = {}) {
        val list = editing.value ?: return
        val pockets = list.mapIndexed { i, p -> p.toPocket(i) }
        if (!Allocator.isValid(pockets)) return
        viewModelScope.launch {
            repository.savePockets(pockets)
            editing.value = null
            onDone()
        }
    }
}

/** Satu baris riwayat pos: alokasi dari pemasukan, pengeluaran, atau pindahan. */
data class PocketEntry(val tx: Transaction, val amount: Long, val label: String, val percent: Int? = null)

data class PocketDetailState(
    val balance: PocketBalance? = null,
    val index: Int = 0,
    val entries: List<PocketEntry> = emptyList(),
    val pockets: Map<Long, Pocket> = emptyMap(),
)

@HiltViewModel
class PocketDetailViewModel @Inject constructor(
    private val repository: MoneyRepository,
    savedState: SavedStateHandle,
) : ViewModel() {

    private val pocketId: Long = checkNotNull(savedState.get<Long>("id"))

    fun setBalance(target: Long) {
        viewModelScope.launch { repository.setBalance(pocketId, target) }
    }

    val state: StateFlow<PocketDetailState> = combine(
        repository.summary,
        repository.transactions,
        repository.allocations,
    ) { summary, txs, allocations ->
        val index = summary.pockets.indexOfFirst { it.pocket.id == pocketId }
        val pockets = summary.pockets.associate { it.pocket.id to it.pocket }
        PocketDetailState(
            balance = summary.pockets.getOrNull(index),
            index = index.coerceAtLeast(0),
            entries = entriesFor(pocketId, txs, allocations, pockets),
            pockets = pockets,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), PocketDetailState())

    private fun entriesFor(id: Long, txs: List<Transaction>, allocations: List<Allocation>, pockets: Map<Long, Pocket>): List<PocketEntry> {
        val allocByTx = allocations.filter { it.pocketId == id }.associateBy { it.transactionId }
        return txs.filter { it.status == TxStatus.CONFIRMED }.mapNotNull { t ->
            when (t.type) {
                TxType.INCOME -> when {
                    t.toPocketId == id -> PocketEntry(t, t.amount, t.merchant.ifBlank { "Pemasukan" })
                    allocByTx[t.id] != null -> allocByTx.getValue(t.id).let {
                        PocketEntry(t, it.amount, t.merchant.ifBlank { "Pemasukan" }, it.percentAtTime)
                    }
                    else -> null
                }
                TxType.EXPENSE -> if (t.pocketId == id) {
                    PocketEntry(t, if (t.isPaylater) 0 else -t.amount, t.merchant.ifBlank { t.note.ifBlank { "Pengeluaran" } })
                } else {
                    null
                }
                TxType.MOVE -> when (id) {
                    t.pocketId -> PocketEntry(t, -t.amount, "Pindah ke ${pockets[t.toPocketId]?.name ?: "kantong lain"}")
                    t.toPocketId -> PocketEntry(t, t.amount, "Pindah dari ${pockets[t.pocketId]?.name ?: "kantong lain"}")
                    else -> null
                }
                TxType.ADJUST -> when (id) {
                    t.pocketId -> PocketEntry(t, -t.amount, t.merchant)
                    t.toPocketId -> PocketEntry(t, t.amount, t.merchant)
                    else -> null
                }
            }
        }
    }
}
