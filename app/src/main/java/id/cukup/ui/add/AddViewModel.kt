package id.cukup.ui.add

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import id.cukup.data.MoneyRepository
import id.cukup.domain.Pocket
import id.cukup.domain.PocketKind
import id.cukup.domain.Rupiah
import id.cukup.domain.TxType
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import javax.inject.Inject

data class AddState(
    val type: TxType = TxType.EXPENSE,
    val amount: Long = 0,
    val pockets: List<Pocket> = emptyList(),
    val balances: Map<Long, Long> = emptyMap(),
    val pocketId: Long? = null,
    val toPocketId: Long? = null,
    /** Pemasukan dibagi otomatis ke semua pos. */
    val split: Boolean = true,
    val merchant: String = "",
    val note: String = "",
    val isPaylater: Boolean = false,
    val daysAgo: Int = 0,
    val pocketTouched: Boolean = false,
    val saving: Boolean = false,
    /** Sisa jatah belanja periode ini dan batas sekali belanja, untuk konfirmasi sebelum simpan. */
    val budget: Long = 0,
    val budgetLeft: Long = 0,
    val singleLimit: Long = 0,
    /** Pesan konfirmasi yang sedang ditampilkan (null = tidak ada). */
    val confirm: String? = null,
) {
    val canSave: Boolean
        get() = amount > 0 && !saving && when (type) {
            TxType.EXPENSE -> pocketId != null
            TxType.INCOME -> split || toPocketId != null
            TxType.MOVE -> pocketId != null && toPocketId != null && pocketId != toPocketId
            TxType.ADJUST -> false
        }
}

sealed interface AddIntent {
    data class Type(val type: TxType) : AddIntent
    data class Amount(val value: Long) : AddIntent
    data class From(val id: Long) : AddIntent
    data class To(val id: Long) : AddIntent
    data class Split(val on: Boolean) : AddIntent
    data class Merchant(val value: String) : AddIntent
    data class Note(val value: String) : AddIntent
    data class Paylater(val on: Boolean) : AddIntent
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

    private val _state = MutableStateFlow(
        AddState(
            type = savedState.get<String>("type")?.let { runCatching { TxType.valueOf(it) }.getOrNull() } ?: TxType.EXPENSE,
            pocketId = savedState.get<Long>("pocket")?.takeIf { it > 0 },
            pocketTouched = (savedState.get<Long>("pocket") ?: 0L) > 0,
            toPocketId = savedState.get<Long>("to")?.takeIf { it > 0 },
            split = (savedState.get<Long>("to") ?: 0L) <= 0,
        ),
    )
    val state: StateFlow<AddState> = _state.asStateFlow()

    private val _effects = Channel<AddEffect>(Channel.BUFFERED)
    val effects = _effects.receiveAsFlow()

    private var suggestJob: Job? = null

    init {
        viewModelScope.launch {
            repository.settings.collect { st -> _state.update { it.copy(singleLimit = st.singleLimit) } }
        }
        viewModelScope.launch {
            repository.summary.collect { summary ->
                val pockets = summary.pockets.map { it.pocket }
                _state.update { s ->
                    s.copy(
                        pockets = pockets,
                        balances = summary.pockets.associate { it.pocket.id to it.balance },
                        budget = summary.budget,
                        budgetLeft = summary.budgetLeft,
                        pocketId = s.pocketId ?: pockets.firstOrNull { it.kind == PocketKind.SPEND }?.id ?: pockets.firstOrNull()?.id,
                    )
                }
            }
        }
    }

    fun onIntent(intent: AddIntent) {
        when (intent) {
            is AddIntent.Type -> _state.update { it.copy(type = intent.type, isPaylater = false) }
            is AddIntent.Amount -> _state.update { it.copy(amount = intent.value) }
            is AddIntent.From -> _state.update { it.copy(pocketId = intent.id, pocketTouched = true) }
            is AddIntent.To -> _state.update { it.copy(toPocketId = intent.id, split = false) }
            is AddIntent.Split -> _state.update { it.copy(split = intent.on, toPocketId = if (intent.on) null else it.toPocketId) }
            is AddIntent.Merchant -> {
                _state.update { it.copy(merchant = intent.value.take(40)) }
                suggest()
            }
            is AddIntent.Note -> _state.update { it.copy(note = intent.value.take(80)) }
            is AddIntent.Paylater -> {
                _state.update { it.copy(isPaylater = intent.on) }
            }
            is AddIntent.Day -> _state.update { it.copy(daysAgo = intent.daysAgo) }
            AddIntent.Save -> {
                val message = warningFor(_state.value)
                if (message != null) _state.update { it.copy(confirm = message) } else save()
            }
            AddIntent.ConfirmSave -> {
                _state.update { it.copy(confirm = null) }
                save()
            }
            AddIntent.CancelConfirm -> _state.update { it.copy(confirm = null) }
        }
    }

    /** Menebak pos dari nama toko, selama pengguna belum memilih pos sendiri. */
    private fun suggest() {
        val s = _state.value
        if (s.type != TxType.EXPENSE || s.pocketTouched) return
        suggestJob?.cancel()
        suggestJob = viewModelScope.launch {
            delay(250)
            val p = repository.suggestPocket(_state.value.merchant) ?: return@launch
            _state.update { if (it.pocketTouched) it else it.copy(pocketId = p.id) }
        }
    }

    /** Kalimat peringatan kalau belanja ini melewati jatah atau batas sekali belanja. */
    private fun warningFor(s: AddState): String? {
        if (s.type != TxType.EXPENSE || s.isPaylater) return null
        val pocket = s.pockets.firstOrNull { it.id == s.pocketId } ?: return null
        val overBudget = pocket.kind == PocketKind.SPEND && s.budget > 0 && s.amount > s.budgetLeft
        val overSingle = s.singleLimit > 0 && s.amount > s.singleLimit
        return when {
            overBudget && s.budgetLeft <= 0 -> "Jatah belanjamu sudah habis. Kalau disimpan, kelebihannya jadi ${Rupiah.format(s.amount - s.budgetLeft)}."
            overBudget -> "Sisa jatahmu tinggal ${Rupiah.format(s.budgetLeft)}. Belanja ini bikin lewat ${Rupiah.format(s.amount - s.budgetLeft)}."
            overSingle -> "Ini di atas batas sekali belanjamu (${Rupiah.format(s.singleLimit)})."
            else -> null
        }
    }

    private fun save() {
        val s = _state.value
        if (!s.canSave) return
        _state.update { it.copy(saving = true) }
        viewModelScope.launch {
            val zone = ZoneId.systemDefault()
            val at = if (s.daysAgo == 0) {
                System.currentTimeMillis()
            } else {
                LocalDate.now(zone).minusDays(s.daysAgo.toLong()).atTime(LocalTime.NOON).atZone(zone).toInstant().toEpochMilli()
            }
            when (s.type) {
                TxType.EXPENSE -> repository.addExpense(s.amount, s.pocketId!!, s.merchant, s.note, at, s.isPaylater)
                TxType.INCOME -> repository.addIncome(s.amount, s.merchant, s.note, at, if (s.split) null else s.toPocketId)
                TxType.MOVE -> repository.move(s.pocketId!!, s.toPocketId!!, s.amount, s.note, at)
                TxType.ADJUST -> Unit
            }
            _effects.send(AddEffect.Saved)
        }
    }
}
