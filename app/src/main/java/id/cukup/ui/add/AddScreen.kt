package id.cukup.ui.add

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import id.cukup.data.Overview
import id.cukup.domain.AccountKind
import id.cukup.domain.CategoryKind
import id.cukup.domain.PlanKind
import id.cukup.domain.Rupiah
import id.cukup.domain.TxType
import id.cukup.ui.components.CardShape
import id.cukup.ui.components.ChipPicker
import id.cukup.ui.components.Choice
import id.cukup.ui.components.Eyebrow
import id.cukup.ui.components.Gutter
import id.cukup.ui.components.Hairline
import id.cukup.ui.components.InkButton
import id.cukup.ui.components.Keypad
import id.cukup.ui.components.LineField
import id.cukup.ui.components.PickItem
import id.cukup.ui.components.TextAction
import id.cukup.ui.components.TopBar
import id.cukup.ui.components.colorOf
import id.cukup.ui.theme.Type
import id.cukup.ui.theme.colors

@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
fun AddScreen(onClose: () -> Unit, onNewAccount: () -> Unit, vm: AddViewModel = hiltViewModel()) {
    val s by vm.state.collectAsStateWithLifecycle()
    val c = colors
    LaunchedEffect(Unit) {
        vm.effects.collect { if (it is AddEffect.Saved) onClose() }
    }
    val o = s.overview

    Column(Modifier.fillMaxSize().background(c.paper).systemBarsPadding().imePadding()) {
        TopBar(
            title = when {
                s.editingId > 0 -> "Ubah catatan"
                s.type == TxType.EXPENSE -> "Uang keluar"
                s.type == TxType.INCOME -> "Uang masuk"
                else -> "Pindah antar dompet"
            },
            onBack = onClose,
        )
        Row(Modifier.padding(horizontal = Gutter), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Choice("Keluar", s.type == TxType.EXPENSE, { vm.onIntent(AddIntent.Type(TxType.EXPENSE)) })
            Choice("Masuk", s.type == TxType.INCOME, { vm.onIntent(AddIntent.Type(TxType.INCOME)) })
            Choice("Pindah", s.type == TxType.TRANSFER, { vm.onIntent(AddIntent.Type(TxType.TRANSFER)) })
        }
        Text(
            Rupiah.format(s.amount),
            style = Type.hero,
            color = if (s.amount > 0) c.ink else c.faint,
            textAlign = TextAlign.Center,
            maxLines = 1,
            modifier = Modifier.fillMaxWidth().padding(horizontal = Gutter).padding(top = 12.dp, bottom = 2.dp),
        )
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
            if (o != null) {
                val accountItems = o.accounts.map {
                    PickItem(it.account.id, it.account.name, it.account.emoji, colorOf(it.account), Rupiah.short(it.balance), id.cukup.domain.Brands.forAccountName(it.account.name)?.mark)
                }
                val newAccount: @Composable () -> Unit = { TextAction("+ Dompet", onNewAccount) }
                when (s.type) {
                    TxType.TRANSFER -> {
                        Label("Dari dompet")
                        ChipPicker(accountItems, s.accountId, { vm.onIntent(AddIntent.Account(it)) }, trailing = newAccount)
                        Label("Ke dompet")
                        ChipPicker(accountItems.filter { it.id != s.accountId }, s.toAccountId, { vm.onIntent(AddIntent.ToAccount(it)) })
                    }
                    else -> {
                        Label(if (s.type == TxType.EXPENSE) "Bayar pakai" else "Masuk ke")
                        ChipPicker(accountItems, s.accountId, { vm.onIntent(AddIntent.Account(it)) }, trailing = newAccount)
                        Label("Kategori")
                        val kind = if (s.type == TxType.INCOME) CategoryKind.INCOME else CategoryKind.EXPENSE
                        ChipPicker(
                            o.categories.filter { it.kind == kind }.map { PickItem(it.id, it.name, it.emoji, colorOf(it)) },
                            s.categoryId,
                            { vm.onIntent(AddIntent.Category(it)) },
                        )
                    }
                }
                Effect(s, o)
            }
            Column(Modifier.padding(horizontal = Gutter).padding(top = 8.dp)) {
                when (s.type) {
                    TxType.EXPENSE -> LineField(s.merchant, { vm.onIntent(AddIntent.Merchant(it)) }, "Untuk apa? (mis. makan siang, Indomaret)")
                    TxType.INCOME -> LineField(s.merchant, { vm.onIntent(AddIntent.Merchant(it)) }, "Dari mana? (mis. gaji, jualan, kiriman)")
                    TxType.TRANSFER -> Unit
                }
                Spacer(Modifier.height(8.dp))
                LineField(s.note, { vm.onIntent(AddIntent.Note(it)) }, if (s.type == TxType.TRANSFER) "Catatan (mis. top up GoPay)" else "Catatan (opsional)")
            }
            Row(
                Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = Gutter, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Choice("Hari ini", s.daysAgo == 0, { vm.onIntent(AddIntent.Day(0)) })
                Choice("Kemarin", s.daysAgo == 1, { vm.onIntent(AddIntent.Day(1)) })
                Choice("2 hari lalu", s.daysAgo == 2, { vm.onIntent(AddIntent.Day(2)) })
                if (s.daysAgo > 2) Choice("${s.daysAgo} hari lalu", true, {})
            }
        }
        Hairline()
        // Saat keyboard HP terbuka (mengetik nama/catatan), keypad angka disembunyikan supaya kolom teks tidak tertutup.
        val typing = WindowInsets.isImeVisible
        AnimatedVisibility(!typing) {
            Keypad(s.amount, { vm.onIntent(AddIntent.Amount(it)) }, Modifier.padding(horizontal = Gutter, vertical = 4.dp), keyHeight = 48.dp)
        }
        InkButton(
            if (s.editingId > 0) "Simpan perubahan" else "Simpan",
            onClick = { vm.onIntent(AddIntent.Save) },
            enabled = s.canSave,
            modifier = Modifier.fillMaxWidth().padding(horizontal = Gutter, vertical = 12.dp),
        )
    }
    s.confirm?.let { message ->
        AlertDialog(
            onDismissRequest = { vm.onIntent(AddIntent.CancelConfirm) },
            title = { Text("Yakin mau disimpan?", style = Type.title) },
            text = { Text(message, style = Type.body) },
            confirmButton = { TextButton({ vm.onIntent(AddIntent.ConfirmSave) }) { Text("Tetap simpan", color = c.over) } },
            dismissButton = { TextButton({ vm.onIntent(AddIntent.CancelConfirm) }) { Text("Nggak jadi", color = c.ink) } },
            containerColor = c.card,
        )
    }
}

/**
 * "Apa dampaknya?" — kalimat sederhana tentang saldo dompet sebelum dan sesudah,
 * serta (bila ada rencana) pos rencana yang terpengaruh.
 */
@Composable
private fun Effect(s: AddState, o: Overview) {
    val c = colors
    val from = o.accounts.firstOrNull { it.account.id == s.accountId }
    val to = o.accounts.firstOrNull { it.account.id == s.toAccountId }
    // Saat mengedit, saldo sekarang sudah memuat transaksi lama; hitung ulang dari sebelum transaksi itu.
    val lines = buildList {
        if (s.amount <= 0 || from == null) return@buildList
        val editing = s.editingId > 0
        when (s.type) {
            TxType.EXPENSE -> if (!editing) add("${from.account.name}: ${Rupiah.format(from.balance)} → ${Rupiah.format(from.balance - s.amount)}")
            TxType.INCOME -> if (!editing) add("${from.account.name}: ${Rupiah.format(from.balance)} → ${Rupiah.format(from.balance + s.amount)}")
            TxType.TRANSFER -> if (!editing && to != null) {
                add("${from.account.name}: ${Rupiah.format(from.balance)} → ${Rupiah.format(from.balance - s.amount)}")
                add("${to.account.name}: ${Rupiah.format(to.balance)} → ${Rupiah.format(to.balance + s.amount)}")
                add("Total uangmu tidak berubah, cuma pindah tempat.")
                if (to.account.kind == AccountKind.SAVINGS) add("Dihitung sebagai menabung di rencana.")
            }
        }
        if (s.type == TxType.EXPENSE && from.account.kind == AccountKind.PAYLATER) add("Dicatat sebagai hutang di ${from.account.name}.")
        if (s.type == TxType.EXPENSE && o.planStatus.active) {
            val planId = o.categoryById[s.categoryId]?.planId
            val row = o.planStatus.rows.firstOrNull { it.pos.id == planId }
            when {
                row == null && s.categoryId != null -> add("Kategori ini tidak masuk rencana mana pun.")
                row != null && row.pos.kind == PlanKind.SPEND && !editing ->
                    add("Rencana ${row.pos.name}: sisa ${Rupiah.short(row.left)} → ${Rupiah.short(row.left - s.amount)}")
                else -> Unit
            }
        }
    }
    if (lines.isEmpty()) return
    Column(
        Modifier.padding(horizontal = Gutter).padding(top = 14.dp).fillMaxWidth().clip(CardShape).background(c.surface).padding(14.dp),
    ) {
        Eyebrow("Dampaknya")
        Spacer(Modifier.height(4.dp))
        lines.forEach { Text(it, style = Type.bodySmall, color = if (it.contains("→ −")) c.over else c.ink, modifier = Modifier.padding(vertical = 2.dp)) }
    }
}

@Composable
private fun Label(text: String) {
    Eyebrow(text, Modifier.padding(horizontal = Gutter).padding(top = 16.dp, bottom = 10.dp))
}
