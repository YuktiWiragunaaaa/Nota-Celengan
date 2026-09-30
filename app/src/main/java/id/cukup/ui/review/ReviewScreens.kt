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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import id.cukup.data.Overview
import id.cukup.domain.Brands
import id.cukup.domain.AccountKind
import id.cukup.domain.CategoryKind
import id.cukup.domain.Rupiah
import id.cukup.domain.Transaction
import id.cukup.domain.TxSource
import id.cukup.domain.TxStatus
import id.cukup.domain.TxType
import id.cukup.ui.AppViewModel
import id.cukup.ui.components.CardShape
import id.cukup.ui.components.ChipPicker
import id.cukup.ui.components.Eyebrow
import id.cukup.ui.components.Gutter
import id.cukup.ui.components.Hairline
import id.cukup.ui.components.InkButton
import id.cukup.ui.components.LineButton
import id.cukup.ui.components.PickItem
import id.cukup.ui.components.SectionHeader
import id.cukup.ui.components.TextAction
import id.cukup.ui.components.TopBar
import id.cukup.ui.components.colorOf
import id.cukup.ui.components.dayLabel
import id.cukup.ui.components.italicize
import id.cukup.ui.components.localDate
import id.cukup.ui.components.time
import id.cukup.ui.theme.Type
import id.cukup.ui.theme.colors

// ——— Perlu dicek ———

@Composable
fun InboxScreen(onBack: () -> Unit, vm: AppViewModel = hiltViewModel()) {
    val o by vm.overview.collectAsStateWithLifecycle()
    val c = colors
    Column(Modifier.fillMaxSize().background(c.paper).systemBarsPadding()) {
        TopBar("Perlu dicek", onBack = onBack)
        val data = o ?: return@Column
        val pending = data.pending
        if (pending.isEmpty()) {
            Column(Modifier.padding(Gutter)) {
                Text(italicize("Nggak ada yang perlu dicek"), style = Type.display, color = c.ink)
                Spacer(Modifier.height(8.dp))
                Text("Nanti transaksi dari notifikasi e-wallet & bank muncul di sini.", style = Type.body, color = c.mute)
            }
            return@Column
        }
        Text(
            "Ini dibaca dari notifikasi HP-mu. Cek dompet dan kategorinya, lalu simpan. Setelah disimpan, saldo dompet ikut berubah.",
            style = Type.bodySmall, color = c.mute,
            modifier = Modifier.padding(horizontal = Gutter).padding(bottom = 12.dp),
        )
        LazyColumn(Modifier.fillMaxSize()) {
            items(pending, key = { it.id }) { tx ->
                PendingCard(tx, data, onConfirm = { a, cat -> vm.confirm(tx.id, a, cat) }, onDismiss = { vm.dismiss(tx.id) })
            }
            item { Spacer(Modifier.height(24.dp)) }
        }
    }
}

@Composable
private fun PendingCard(tx: Transaction, o: Overview, onConfirm: (Long?, Long?) -> Unit, onDismiss: () -> Unit) {
    val c = colors
    val isIncome = tx.type == TxType.INCOME
    var accountId by rememberSaveable(tx.id) { mutableStateOf(tx.accountId) }
    var categoryId by rememberSaveable(tx.id) { mutableStateOf(tx.categoryId) }
    Column(
        Modifier.padding(horizontal = Gutter, vertical = 6.dp).fillMaxWidth()
            .border(1.dp, c.line, CardShape).background(c.card, CardShape).padding(vertical = 16.dp),
    ) {
        Row(Modifier.padding(horizontal = 16.dp), verticalAlignment = Alignment.Top) {
            Column(Modifier.weight(1f)) {
                Eyebrow(listOfNotNull(tx.sourceApp, dayLabel(localDate(tx.occurredAt)), time(tx.occurredAt)).joinToString(" · "))
                Spacer(Modifier.height(4.dp))
                Text(tx.merchant.ifBlank { if (isIncome) "Uang masuk" else "Uang keluar" }, style = Type.strong, color = c.ink)
                if (tx.note.isNotBlank()) Text(tx.note, style = Type.bodySmall, color = c.caution)
            }
            Text((if (isIncome) "+" else "−") + Rupiah.format(tx.amount), style = Type.number, color = c.ink)
        }
        Eyebrow(if (isIncome) "Masuk ke" else "Dari dompet", Modifier.padding(horizontal = 16.dp).padding(top = 14.dp, bottom = 8.dp))
        ChipPicker(
            o.accounts.map { PickItem(it.account.id, it.account.name, it.account.emoji, colorOf(it.account), mark = Brands.forAccountName(it.account.name)?.mark) },
            accountId, { accountId = it },
        )
        Eyebrow("Kategori", Modifier.padding(horizontal = 16.dp).padding(top = 12.dp, bottom = 8.dp))
        val kind = if (isIncome) CategoryKind.INCOME else CategoryKind.EXPENSE
        ChipPicker(
            o.categories.filter { it.kind == kind }.map { PickItem(it.id, it.name, it.emoji, colorOf(it)) },
            categoryId, { categoryId = it },
        )
        Spacer(Modifier.height(12.dp))
        Row(Modifier.padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
            TextAction("Abaikan", onDismiss, color = c.mute)
            Spacer(Modifier.weight(1f))
            InkButton(
                "Simpan",
                onClick = { onConfirm(accountId, categoryId) },
                enabled = accountId != null,
                modifier = Modifier.fillMaxWidth(0.5f).height(44.dp),
            )
        }
    }
}

// ——— Detail transaksi ———

@Composable
fun TxDetailScreen(id: Long, onBack: () -> Unit, onEdit: (Long) -> Unit, vm: AppViewModel = hiltViewModel()) {
    val o by vm.overview.collectAsStateWithLifecycle()
    val c = colors
    var confirmDelete by remember { mutableStateOf(false) }
    val data = o
    val tx = data?.transactions?.firstOrNull { it.id == id }
    Column(Modifier.fillMaxSize().background(c.paper).systemBarsPadding()) {
        TopBar("Catatan", onBack = onBack)
        if (data == null || tx == null) return@Column
        val cat = tx.categoryId?.let(data.categoryById::get)
        val from = tx.accountId?.let(data.accountById::get)
        val to = tx.toAccountId?.let(data.accountById::get)
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
            Column(Modifier.padding(horizontal = Gutter).padding(top = 8.dp, bottom = 20.dp)) {
                Eyebrow(
                    when (tx.type) {
                        TxType.EXPENSE -> "Uang keluar"
                        TxType.INCOME -> "Uang masuk"
                        TxType.TRANSFER -> "Pindah antar dompet"
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
            when (tx.type) {
                TxType.TRANSFER -> {
                    Info("Dari", from?.let { it.name } ?: "—")
                    Info("Ke", to?.let { it.name } ?: "—")
                }
                else -> {
                    Info(if (tx.type == TxType.EXPENSE) "Dompet" else "Masuk ke", from?.let { it.name } ?: "Dompet terhapus")
                    Info("Kategori", cat?.let { it.name } ?: "Tanpa kategori")
                    if (tx.type == TxType.EXPENSE) {
                        val pos = data.plan.firstOrNull { it.id == cat?.planId }
                        Info("Dihitung di rencana", pos?.let { it.name } ?: "Tidak")
                    }
                }
            }
            if (tx.type != TxType.TRANSFER) {
                SectionHeader("Pindahkan ke dompet")
                ChipPicker(
                    data.accounts.map { PickItem(it.account.id, it.account.name, it.account.emoji, colorOf(it.account), mark = Brands.forAccountName(it.account.name)?.mark) },
                    tx.accountId, { vm.moveTo(tx, it) },
                )
                if (tx.source == TxSource.NOTIFICATION) {
                    Text(
                        "Notifikasi ${tx.sourceApp ?: "ini"} berikutnya ikut masuk ke dompet yang kamu pilih.",
                        style = Type.bodySmall, color = c.faint, modifier = Modifier.padding(horizontal = Gutter, vertical = 6.dp),
                    )
                }
            }
            if (tx.type == TxType.TRANSFER && tx.source == TxSource.NOTIFICATION) {
                Column(Modifier.padding(Gutter)) {
                    LineButton("Bukan pindah, pisahkan", onClick = { vm.splitTransfer(tx.id) }, modifier = Modifier.fillMaxWidth())
                    Text(
                        "Jadi dua catatan: uang keluar dari ${from?.name ?: "dompet asal"} dan uang masuk ke ${to?.name ?: "dompet tujuan"}.",
                        style = Type.bodySmall, color = c.faint, modifier = Modifier.padding(top = 6.dp),
                    )
                }
            }
            Info("Sumber", if (tx.source == TxSource.NOTIFICATION) "Notifikasi ${tx.sourceApp ?: ""}" else "Dicatat manual")
            if (tx.status == TxStatus.PENDING) Info("Status", "Perlu dicek")
            if (tx.type == TxType.EXPENSE && from?.kind == AccountKind.PAYLATER) {
                Text("Dari paylater: dicatat sebagai hutang.", style = Type.bodySmall, color = c.mute, modifier = Modifier.padding(Gutter))
            }
        }
        Hairline()
        Row(Modifier.padding(horizontal = Gutter, vertical = 12.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            LineButton("Hapus", onClick = { confirmDelete = true }, modifier = Modifier.weight(1f))
            InkButton("Ubah", onClick = { onEdit(tx.id) }, modifier = Modifier.weight(1f))
        }
    }
    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Hapus catatan ini?", style = Type.title) },
            text = { Text("Saldo dompet dihitung ulang seolah catatan ini tidak pernah ada.", style = Type.body) },
            confirmButton = { TextButton({ confirmDelete = false; vm.delete(id); onBack() }) { Text("Hapus", color = c.over) } },
            dismissButton = { TextButton({ confirmDelete = false }) { Text("Batal", color = c.ink) } },
            containerColor = c.card,
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
