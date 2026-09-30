package id.cukup.ui.accounts

import id.cukup.domain.Brands
import id.cukup.ui.components.AccountIcon
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import id.cukup.domain.Account
import id.cukup.domain.AccountKind
import id.cukup.domain.Presets
import id.cukup.domain.Rupiah
import id.cukup.domain.TxType
import id.cukup.ui.AppViewModel
import id.cukup.ui.components.AmountDialog
import id.cukup.ui.components.CardShape
import id.cukup.ui.components.Choice
import id.cukup.ui.components.Eyebrow
import id.cukup.ui.components.Gutter
import id.cukup.ui.components.InfoBox
import id.cukup.ui.components.InkButton
import id.cukup.ui.components.LineButton
import id.cukup.ui.components.LineField
import id.cukup.ui.components.LookPicker
import id.cukup.ui.components.SectionHeader
import id.cukup.ui.components.TopBar
import id.cukup.ui.components.TxRow
import id.cukup.ui.components.colorOf
import id.cukup.ui.components.label
import id.cukup.ui.theme.Type
import id.cukup.ui.theme.colors

/** Detail dompet. [id] 0 = membuat dompet baru. */
@Composable
fun AccountScreen(id: Long, onBack: () -> Unit, onOpenTx: (Long) -> Unit, onAdd: (TxType, Long) -> Unit, vm: AppViewModel = hiltViewModel()) {
    if (id == 0L) {
        AccountForm(Account(0, "", "💵", AccountKind.CASH), isNew = true, onBack = onBack) { a, _ -> vm.saveAccount(a); onBack() }
        return
    }
    val o by vm.overview.collectAsStateWithLifecycle()
    val c = colors
    val data = o ?: return
    val ab = data.accounts.firstOrNull { it.account.id == id }
    if (ab == null) {
        Column(Modifier.fillMaxSize().background(c.paper).systemBarsPadding()) { TopBar("Dompet", onBack) }
        return
    }
    var editing by remember { mutableStateOf(false) }
    var adjusting by remember { mutableStateOf(false) }
    var archiving by remember { mutableStateOf(false) }
    if (editing) {
        AccountForm(ab.account, isNew = false, onBack = { editing = false }) { a, _ -> vm.saveAccount(a); editing = false }
        return
    }
    val txs = data.confirmed.filter { it.accountId == id || it.toAccountId == id }
    val isDefault = data.settings.defaultAccountId == id

    LazyColumn(Modifier.fillMaxSize().background(c.paper).systemBarsPadding()) {
        item { TopBar(ab.account.name, onBack) }
        item {
            Column(Modifier.padding(horizontal = Gutter).fillMaxWidth().clip(CardShape).background(c.card).padding(18.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    AccountIcon(ab.account, size = 52.dp)
                    Spacer(Modifier.padding(4.dp))
                    Column {
                        Text(ab.account.kind.label() + if (isDefault) " · dompet utama" else "", style = Type.bodySmall, color = c.mute)
                        Text(Rupiah.format(ab.balance), style = Type.display, color = if (ab.balance < 0) c.over else c.ink)
                    }
                }
                if (ab.account.kind == AccountKind.PAYLATER) {
                    Text("Minus = hutang yang belum dibayar. Bayar tagihan dengan Pindah dari dompet lain ke sini.", style = Type.bodySmall, color = c.mute, modifier = Modifier.padding(top = 8.dp))
                }
                Spacer(Modifier.height(14.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    LineButton("Keluar", { onAdd(TxType.EXPENSE, id) }, Modifier.weight(1f), height = 44.dp)
                    LineButton("Masuk", { onAdd(TxType.INCOME, id) }, Modifier.weight(1f), height = 44.dp)
                    LineButton("Pindah", { onAdd(TxType.TRANSFER, id) }, Modifier.weight(1f), height = 44.dp)
                }
            }
        }
        item {
            Row(
                Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = Gutter, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Choice("Samakan saldo", false, { adjusting = true })
                Choice("Ubah", false, { editing = true })
                if (!isDefault) Choice("Jadikan utama", false, { vm.setDefaultAccount(id) })
                Choice("Hapus", false, { archiving = true })
            }
            Text(
                "\"Samakan saldo\" dipakai kalau angka di sini beda dengan aplikasi bank/e-wallet. Riwayat catatan tidak berubah.",
                style = Type.bodySmall, color = c.faint, modifier = Modifier.padding(horizontal = Gutter),
            )
        }
        if (ab.account.kind != AccountKind.CASH) {
            item {
                val accounts = data.accounts.map { it.account }
                // Aplikasi yang notifikasinya sekarang masuk ke dompet ini (tautan atau tebakan otomatis).
                val linked = Brands.all.filter { b -> b.packages.any { Brands.accountFor(it, accounts, data.settings.appLinks) == id } }
                SectionHeader("Notifikasi dari")
                Row(
                    Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = Gutter),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Brands.all.forEach { b ->
                        val on = b in linked
                        Choice(b.name, on, { vm.linkApp(b.key, if (on) null else id) })
                    }
                }
                Text(
                    if (linked.isEmpty()) "Pilih aplikasi bank/e-wallet yang notifikasinya dicatat ke ${ab.account.name}."
                    else "Notifikasi ${linked.joinToString { it.name }} dicatat ke ${ab.account.name}.",
                    style = Type.bodySmall, color = c.faint, modifier = Modifier.padding(horizontal = Gutter, vertical = 8.dp),
                )
            }
        }
        item { SectionHeader("Riwayat dompet ini") }
        if (txs.isEmpty()) {
            item { Text("Belum ada catatan di dompet ini.", style = Type.body, color = c.faint, modifier = Modifier.padding(horizontal = Gutter)) }
        }
        items(txs, key = { it.id }) { tx ->
            val cat = tx.categoryId?.let(data.categoryById::get)
            TxRow(tx, cat, data.accountById[tx.accountId], data.accountById[tx.toAccountId], colorOf(cat), { onOpenTx(tx.id) })
        }
        item { Spacer(Modifier.height(24.dp)) }
    }

    if (adjusting) {
        AmountDialog(
            "Samakan saldo ${ab.account.name}",
            "Isi saldo yang benar sekarang (lihat di aplikasi bank/e-wallet atau hitung uang tunai).",
            ab.balance, onDismiss = { adjusting = false }, allowNegative = true,
        ) { vm.setAccountBalance(id, it); adjusting = false }
    }
    if (archiving) {
        AlertDialog(
            onDismissRequest = { archiving = false },
            title = { Text("Hapus ${ab.account.name}?", style = Type.title) },
            text = { Text("Dompet disembunyikan dan saldonya tidak dihitung lagi. Catatan lamanya tetap ada di Riwayat.", style = Type.body) },
            confirmButton = { TextButton({ vm.archiveAccount(id); archiving = false; onBack() }) { Text("Hapus", color = c.over) } },
            dismissButton = { TextButton({ archiving = false }) { Text("Batal", color = c.ink) } },
            containerColor = c.card,
        )
    }
}

/** Formulir dompet: jenis, nama, saldo (hanya saat baru), ikon & warna. */
@Composable
fun AccountForm(initial: Account, isNew: Boolean, onBack: () -> Unit, onSave: (Account, Long) -> Unit) {
    val c = colors
    var name by remember { mutableStateOf(initial.name) }
    var emoji by remember { mutableStateOf(initial.emoji) }
    var color by remember { mutableStateOf(initial.color) }
    var kind by remember { mutableStateOf(initial.kind) }
    var balance by remember { mutableStateOf(if (initial.initialBalance != 0L) kotlin.math.abs(initial.initialBalance).toString() else "") }
    Column(Modifier.fillMaxSize().background(c.paper).systemBarsPadding().imePadding()) {
        TopBar(if (isNew) "Dompet baru" else "Ubah dompet", onBack)
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = Gutter)) {
            if (isNew) {
                Eyebrow("Pilih cepat")
                Spacer(Modifier.height(8.dp))
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Presets.accounts.forEach { seed ->
                        Choice(seed.name, name == seed.name, { name = seed.name; emoji = seed.emoji; kind = seed.kind; color = seed.color?.toInt() })
                    }
                }
                Spacer(Modifier.height(16.dp))
            }
            LineField(name, { name = it.take(24) }, "Nama dompet (mis. BCA, GoPay, Tunai)")
            Spacer(Modifier.height(16.dp))
            Eyebrow("Jenis")
            Spacer(Modifier.height(8.dp))
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                AccountKind.entries.forEach { k -> Choice(k.label(), kind == k, { kind = k }) }
            }
            Text(
                when (kind) {
                    AccountKind.SAVINGS -> "Uang yang dipindah ke sini dihitung sebagai menabung di Rencana."
                    AccountKind.PAYLATER -> "Belanja dari sini jadi hutang (saldo minus). Tidak dihitung di total uangmu."
                    else -> "Dihitung di total uangmu."
                },
                style = Type.bodySmall, color = c.mute, modifier = Modifier.padding(top = 6.dp),
            )
            if (isNew) {
                Spacer(Modifier.height(16.dp))
                Eyebrow(if (kind == AccountKind.PAYLATER) "Hutang sekarang" else "Saldo sekarang")
                Spacer(Modifier.height(8.dp))
                LineField(balance, { balance = it.filter(Char::isDigit).take(12) }, "0", keyboardType = KeyboardType.Number)
                balance.toLongOrNull()?.let { Text(Rupiah.format(it), style = Type.bodySmall, color = c.mute, modifier = Modifier.padding(top = 4.dp)) }
                InfoBox(
                    "Kenapa perlu saldo?",
                    listOf("Supaya angka \"Uangmu sekarang\" sama dengan kenyataan. Lihat saldonya di aplikasi bank/e-wallet, atau hitung uang tunai."),
                    Modifier.padding(top = 12.dp),
                )
            }
            Spacer(Modifier.height(16.dp))
            LookPicker(emoji, color, { emoji = it }, { color = it })
            Spacer(Modifier.height(24.dp))
        }
        InkButton(
            "Simpan",
            onClick = {
                val amount = balance.toLongOrNull() ?: 0
                val start = if (kind == AccountKind.PAYLATER) -amount else amount
                onSave(
                    initial.copy(name = name.trim(), emoji = emoji, color = color, kind = kind, initialBalance = if (isNew) start else initial.initialBalance),
                    start,
                )
            },
            enabled = name.isNotBlank(),
            modifier = Modifier.fillMaxWidth().padding(horizontal = Gutter, vertical = 12.dp),
        )
    }
}
