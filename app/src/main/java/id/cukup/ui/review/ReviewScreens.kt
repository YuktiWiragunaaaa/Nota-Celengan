package id.cukup.ui.review

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import id.cukup.data.MoneyRepository
import id.cukup.domain.Allocation
import id.cukup.domain.Pocket
import id.cukup.domain.Rupiah
import id.cukup.domain.Transaction
import id.cukup.domain.TxSource
import id.cukup.domain.TxStatus
import id.cukup.domain.TxType
import id.cukup.ui.components.Choice
import id.cukup.ui.components.Eyebrow
import id.cukup.ui.components.Gutter
import id.cukup.ui.components.Hairline
import id.cukup.ui.components.InkButton
import id.cukup.ui.components.LineButton
import id.cukup.ui.components.PocketDot
import id.cukup.ui.components.PocketPicker
import id.cukup.ui.components.TextAction
import id.cukup.ui.components.TopBar
import id.cukup.ui.components.dayLabel
import id.cukup.ui.components.localDate
import id.cukup.ui.components.time
import id.cukup.ui.onboarding.italicize
import id.cukup.ui.theme.Type
import id.cukup.ui.theme.colors
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

// ——— Perlu dicek ———

data class InboxState(val loaded: Boolean = false, val pending: List<Transaction> = emptyList(), val pockets: List<Pocket> = emptyList())

@HiltViewModel
class InboxViewModel @Inject constructor(private val repository: MoneyRepository) : ViewModel() {
    val state: StateFlow<InboxState> = combine(repository.pending, repository.pockets) { p, pockets -> InboxState(true, p, pockets) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), InboxState())

    fun confirm(tx: Transaction, pocketId: Long?, paylater: Boolean) {
        viewModelScope.launch { repository.confirm(tx.id, pocketId, paylater) }
    }

    fun dismiss(tx: Transaction) {
        viewModelScope.launch { repository.dismiss(tx.id) }
    }
}

@Composable
fun InboxScreen(onBack: () -> Unit, vm: InboxViewModel = hiltViewModel()) {
    val s by vm.state.collectAsStateWithLifecycle()
    val c = colors
    Column(Modifier.fillMaxSize().background(c.paper).systemBarsPadding()) {
        TopBar("Perlu dicek", onBack = onBack)
        if (s.loaded && s.pending.isEmpty()) {
            Column(Modifier.padding(Gutter)) {
                Text(italicize("Semua sudah <i>beres.</i>"), style = Type.display, color = c.ink)
                Spacer(Modifier.height(8.dp))
                Text("Transaksi yang terbaca dari notifikasi akan muncul di sini.", style = Type.body, color = c.mute)
            }
            return@Column
        }
        Text(
            "Terbaca dari notifikasi. Pastikan nominal dan posnya, lalu simpan.",
            style = Type.bodySmall, color = c.mute,
            modifier = Modifier.padding(horizontal = Gutter).padding(bottom = 12.dp),
        )
        LazyColumn(Modifier.fillMaxSize()) {
            items(s.pending, key = { it.id }) { tx ->
                PendingCard(tx, s.pockets, onConfirm = { p, pl -> vm.confirm(tx, p, pl) }, onDismiss = { vm.dismiss(tx) })
            }
            item { Spacer(Modifier.height(24.dp)) }
        }
    }
}

@Composable
private fun PendingCard(tx: Transaction, pockets: List<Pocket>, onConfirm: (Long?, Boolean) -> Unit, onDismiss: () -> Unit) {
    val c = colors
    val isIncome = tx.type == TxType.INCOME
    var pocketId by rememberSaveable(tx.id) { mutableStateOf(if (isIncome) null else tx.pocketId) }
    var paylater by rememberSaveable(tx.id) { mutableStateOf(tx.isPaylater) }
    Column(
        Modifier
            .padding(horizontal = Gutter, vertical = 6.dp)
            .fillMaxWidth()
            .border(1.dp, c.line, RoundedCornerShape(2.dp))
            .padding(vertical = 16.dp),
    ) {
        Row(Modifier.padding(horizontal = 16.dp), verticalAlignment = Alignment.Top) {
            Column(Modifier.weight(1f)) {
                Eyebrow(listOfNotNull(tx.sourceApp, dayLabel(localDate(tx.occurredAt)), time(tx.occurredAt)).joinToString(" · "))
                Spacer(Modifier.height(4.dp))
                Text(
                    tx.merchant.ifBlank { if (isIncome) "Uang masuk" else "Pengeluaran" },
                    style = Type.strong, color = c.ink,
                )
                if (tx.note.isNotBlank()) Text(tx.note, style = Type.bodySmall, color = c.caution)
            }
            Text(
                (if (isIncome) "+" else "−") + Rupiah.format(tx.amount),
                style = Type.number, color = c.ink,
            )
        }
        Spacer(Modifier.height(14.dp))
        if (isIncome) {
            Row(Modifier.padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Choice("Bagi ke semua pos", pocketId == null, { pocketId = null })
            }
            Spacer(Modifier.height(8.dp))
            PocketPicker(pockets, pocketId, { pocketId = it }, Modifier.padding(start = 0.dp))
        } else {
            PocketPicker(pockets, pocketId, { pocketId = it })
            Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("Paylater", style = Type.bodySmall, color = c.mute, modifier = Modifier.weight(1f))
                Switch(
                    checked = paylater, onCheckedChange = { paylater = it },
                    colors = SwitchDefaults.colors(checkedTrackColor = c.ink, checkedThumbColor = c.paper, uncheckedBorderColor = c.faint, uncheckedThumbColor = c.faint, uncheckedTrackColor = c.paper),
                )
            }
        }
        Spacer(Modifier.height(12.dp))
        Row(Modifier.padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
            TextAction("Abaikan", onDismiss, color = c.mute)
            Spacer(Modifier.weight(1f))
            InkButton(
                "Simpan",
                onClick = { onConfirm(pocketId, paylater) },
                enabled = isIncome || pocketId != null,
                modifier = Modifier.fillMaxWidth(0.5f).height(44.dp),
            )
        }
    }
}

// ——— Detail transaksi ———

data class TxDetailState(
    val tx: Transaction? = null,
    val pockets: List<Pocket> = emptyList(),
    val allocations: List<Allocation> = emptyList(),
    val gone: Boolean = false,
)

@HiltViewModel
class TxDetailViewModel @Inject constructor(
    private val repository: MoneyRepository,
    savedState: SavedStateHandle,
) : ViewModel() {
    private val id: Long = checkNotNull(savedState.get<Long>("id"))

    val state: StateFlow<TxDetailState> = combine(repository.transactions, repository.pockets, repository.allocations) { txs, pockets, allocs ->
        val tx = txs.firstOrNull { it.id == id }
        TxDetailState(tx, pockets, allocs.filter { it.transactionId == id }, gone = tx == null)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TxDetailState())

    fun changePocket(pocketId: Long) {
        viewModelScope.launch { repository.changePocket(id, pocketId) }
    }

    fun delete(onDone: () -> Unit) {
        viewModelScope.launch {
            repository.delete(id)
            onDone()
        }
    }
}

@Composable
fun TxDetailScreen(onBack: () -> Unit, vm: TxDetailViewModel = hiltViewModel()) {
    val s by vm.state.collectAsStateWithLifecycle()
    val c = colors
    var confirmDelete by remember { mutableStateOf(false) }
    val tx = s.tx
    Column(Modifier.fillMaxSize().background(c.paper).systemBarsPadding()) {
        TopBar("Transaksi", onBack = onBack)
        if (tx == null) return@Column
        val pocketsById = s.pockets.associateBy { it.id }
        val index = s.pockets.mapIndexed { i, p -> p.id to i }.toMap()
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
            Column(Modifier.padding(horizontal = Gutter).padding(top = 8.dp, bottom = 20.dp)) {
                Eyebrow(
                    when (tx.type) {
                        TxType.EXPENSE -> if (tx.isPaylater) "Pengeluaran · paylater" else "Pengeluaran"
                        TxType.INCOME -> "Pemasukan"
                        TxType.MOVE -> "Pindah pos"
                    },
                )
                Text(Rupiah.format(tx.amount), style = Type.hero, color = c.ink)
                Spacer(Modifier.height(6.dp))
                Text(
                    listOf(tx.merchant, tx.note).filter { it.isNotBlank() }.joinToString(" · ").ifBlank { "Tanpa keterangan" },
                    style = Type.statement, color = c.mute,
                )
            }
            Hairline()
            Info("Waktu", "${dayLabel(localDate(tx.occurredAt))}, ${time(tx.occurredAt)}")
            Info("Sumber", if (tx.source == TxSource.NOTIFICATION) "Notifikasi ${tx.sourceApp ?: ""}" else "Dicatat manual")
            if (tx.status == TxStatus.PENDING) Info("Status", "Perlu dicek")

            when (tx.type) {
                TxType.EXPENSE -> {
                    Eyebrow("Pos", Modifier.padding(horizontal = Gutter).padding(top = 20.dp, bottom = 10.dp))
                    PocketPicker(s.pockets, tx.pocketId, vm::changePocket)
                    Text(
                        "Mengganti pos juga mengajari Cukup untuk transaksi berikutnya di tempat yang sama.",
                        style = Type.bodySmall, color = c.faint,
                        modifier = Modifier.padding(horizontal = Gutter, vertical = 10.dp),
                    )
                }
                TxType.INCOME -> {
                    Eyebrow("Dibagi ke", Modifier.padding(horizontal = Gutter).padding(top = 20.dp, bottom = 6.dp))
                    if (tx.toPocketId != null) {
                        Info("Satu pos", pocketsById[tx.toPocketId]?.let { "${it.emoji} ${it.name}" } ?: "Pos terhapus")
                    } else {
                        s.allocations.forEach { a ->
                            val p = pocketsById[a.pocketId]
                            Row(Modifier.fillMaxWidth().padding(horizontal = Gutter, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                                PocketDot(c.pocket(index[a.pocketId] ?: 0))
                                Text(
                                    "  ${p?.emoji ?: ""} ${p?.name ?: "Pos terhapus"}",
                                    style = Type.body, color = c.ink, modifier = Modifier.weight(1f),
                                )
                                Text("${a.percentAtTime}%  ", style = Type.bodySmall, color = c.faint)
                                Text(Rupiah.format(a.amount), style = Type.amount, color = c.ink)
                            }
                        }
                    }
                }
                TxType.MOVE -> {
                    Info("Dari", pocketsById[tx.pocketId]?.name ?: "—")
                    Info("Ke", pocketsById[tx.toPocketId]?.name ?: "—")
                }
            }
        }
        Hairline()
        Row(Modifier.padding(horizontal = Gutter, vertical = 12.dp)) {
            LineButton("Hapus", onClick = { confirmDelete = true }, modifier = Modifier.fillMaxWidth())
        }
    }
    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Hapus transaksi?", style = Type.title) },
            text = { Text("Saldo pos akan dihitung ulang. Tidak bisa dibatalkan.", style = Type.body) },
            confirmButton = { TextButton({ confirmDelete = false; vm.delete(onBack) }) { Text("Hapus", color = c.over) } },
            dismissButton = { TextButton({ confirmDelete = false }) { Text("Batal", color = c.ink) } },
            containerColor = c.paper,
        )
    }
}

@Composable
private fun Info(label: String, value: String) {
    val c = colors
    Row(Modifier.fillMaxWidth().padding(horizontal = Gutter, vertical = 12.dp)) {
        Text(label, style = Type.body, color = c.mute, modifier = Modifier.weight(1f))
        Text(value, style = Type.body, color = c.ink)
    }
    Hairline()
}
