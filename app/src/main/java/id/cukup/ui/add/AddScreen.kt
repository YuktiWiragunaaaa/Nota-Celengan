package id.cukup.ui.add

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Switch
import androidx.compose.material3.TextButton
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import id.cukup.domain.Rupiah
import id.cukup.domain.TxType
import id.cukup.ui.components.Choice
import id.cukup.ui.components.Eyebrow
import id.cukup.ui.components.Gutter
import id.cukup.ui.components.Hairline
import id.cukup.ui.components.InkButton
import id.cukup.ui.components.Keypad
import id.cukup.ui.components.LineField
import id.cukup.ui.components.PocketPicker
import id.cukup.ui.components.SplitPreview
import id.cukup.ui.components.TopBar
import id.cukup.ui.theme.Type
import id.cukup.ui.theme.colors

@Composable
fun AddScreen(onClose: () -> Unit, vm: AddViewModel = hiltViewModel()) {
    val s by vm.state.collectAsStateWithLifecycle()
    val c = colors
    LaunchedEffect(Unit) {
        vm.effects.collect { if (it is AddEffect.Saved) onClose() }
    }

    Column(Modifier.fillMaxSize().background(c.paper).systemBarsPadding().imePadding()) {
        TopBar(
            title = when (s.type) {
                TxType.EXPENSE -> "Uang keluar"
                TxType.INCOME -> "Uang masuk"
                TxType.MOVE -> "Pindah antar kantong"
            },
            onBack = onClose,
        )
        Row(Modifier.padding(horizontal = Gutter), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Choice("Keluar", s.type == TxType.EXPENSE, { vm.onIntent(AddIntent.Type(TxType.EXPENSE)) })
            Choice("Masuk", s.type == TxType.INCOME, { vm.onIntent(AddIntent.Type(TxType.INCOME)) })
            Choice("Pindah", s.type == TxType.MOVE, { vm.onIntent(AddIntent.Type(TxType.MOVE)) })
        }
        // Nominal & keypad tetap di tempat; hanya detail di tengah yang bergulir.
        Text(
            Rupiah.format(s.amount),
            style = Type.hero,
            color = if (s.amount > 0) c.ink else c.faint,
            textAlign = TextAlign.Center,
            maxLines = 1,
            modifier = Modifier.fillMaxWidth().padding(horizontal = Gutter).padding(top = 16.dp, bottom = 4.dp),
        )
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {

            when (s.type) {
                TxType.EXPENSE -> {
                    Label("Ambil dari kantong")
                    PocketPicker(s.pockets, s.pocketId, { vm.onIntent(AddIntent.From(it)) })
                    s.pocketId?.let { id ->
                        val bal = s.balances[id] ?: 0
                        val after = bal - if (s.isPaylater) 0 else s.amount
                        Text(
                            "Sisa kantong setelah ini: ${Rupiah.format(after)}",
                            style = Type.bodySmall,
                            color = if (after < 0) c.over else c.mute,
                            modifier = Modifier.padding(horizontal = Gutter, vertical = 8.dp),
                        )
                    }
                    Column(Modifier.padding(horizontal = Gutter).padding(top = 8.dp)) {
                        LineField(s.merchant, { vm.onIntent(AddIntent.Merchant(it)) }, "Untuk apa? (mis. makan siang, bensin, Indomaret)")
                        Spacer(Modifier.height(8.dp))
                        LineField(s.note, { vm.onIntent(AddIntent.Note(it)) }, "Catatan (opsional)")
                    }
                    Row(
                        Modifier.fillMaxWidth().padding(horizontal = Gutter, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text("Pakai paylater", style = Type.strong, color = c.ink)
                            Text("Dicatat sebagai hutang. Isi kantong belum berkurang.", style = Type.bodySmall, color = c.mute)
                        }
                        Switch(
                            checked = s.isPaylater,
                            onCheckedChange = { vm.onIntent(AddIntent.Paylater(it)) },
                            colors = SwitchDefaults.colors(checkedTrackColor = c.ink, checkedThumbColor = c.paper, uncheckedBorderColor = c.faint, uncheckedThumbColor = c.faint, uncheckedTrackColor = c.paper),
                        )
                    }
                }
                TxType.INCOME -> {
                    Label("Masukkan ke")
                    Row(Modifier.padding(horizontal = Gutter), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Choice("Bagi ke semua kantong", s.split, { vm.onIntent(AddIntent.Split(true)) })
                        Choice("Satu kantong saja", !s.split, { vm.onIntent(AddIntent.Split(false)) })
                    }
                    Spacer(Modifier.height(10.dp))
                    if (s.split) {
                        Hairline()
                        SplitPreview(s.amount, s.pockets, Modifier.padding(vertical = 6.dp))
                        Hairline()
                    } else {
                        PocketPicker(s.pockets, s.toPocketId, { vm.onIntent(AddIntent.To(it)) })
                    }
                    Column(Modifier.padding(horizontal = Gutter).padding(top = 12.dp)) {
                        LineField(s.merchant, { vm.onIntent(AddIntent.Merchant(it)) }, "Dari mana? (mis. gaji, uang saku, jualan)")
                    }
                }
                TxType.MOVE -> {
                    Label("Dari kantong")
                    PocketPicker(s.pockets, s.pocketId, { vm.onIntent(AddIntent.From(it)) })
                    Label("Ke kantong")
                    PocketPicker(s.pockets.filter { it.id != s.pocketId }, s.toPocketId, { vm.onIntent(AddIntent.To(it)) })
                    Column(Modifier.padding(horizontal = Gutter).padding(top = 12.dp)) {
                        LineField(s.note, { vm.onIntent(AddIntent.Note(it)) }, "Alasan (opsional)")
                    }
                }
            }

            Row(
                Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = Gutter, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Choice("Hari ini", s.daysAgo == 0, { vm.onIntent(AddIntent.Day(0)) })
                Choice("Kemarin", s.daysAgo == 1, { vm.onIntent(AddIntent.Day(1)) })
                Choice("2 hari lalu", s.daysAgo == 2, { vm.onIntent(AddIntent.Day(2)) })
            }
        }
        Hairline()
        Keypad(s.amount, { vm.onIntent(AddIntent.Amount(it)) }, Modifier.padding(horizontal = Gutter, vertical = 4.dp), keyHeight = 50.dp)
        InkButton(
            "Simpan",
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
            confirmButton = {
                TextButton({ vm.onIntent(AddIntent.ConfirmSave) }) { Text("Tetap simpan", color = c.over) }
            },
            dismissButton = {
                TextButton({ vm.onIntent(AddIntent.CancelConfirm) }) { Text("Nggak jadi", color = c.ink) }
            },
            containerColor = c.paper,
        )
    }
}

@Composable
private fun Label(text: String) {
    Eyebrow(text, Modifier.padding(horizontal = Gutter).padding(top = 16.dp, bottom = 10.dp))
}
