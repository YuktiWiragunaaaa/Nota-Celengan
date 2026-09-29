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
import androidx.compose.material3.Switch
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
                TxType.MOVE -> "Pindah pos"
            },
            onBack = onClose,
        )
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
            Row(Modifier.padding(horizontal = Gutter), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Choice("Keluar", s.type == TxType.EXPENSE, { vm.onIntent(AddIntent.Type(TxType.EXPENSE)) })
                Choice("Masuk", s.type == TxType.INCOME, { vm.onIntent(AddIntent.Type(TxType.INCOME)) })
                Choice("Pindah", s.type == TxType.MOVE, { vm.onIntent(AddIntent.Type(TxType.MOVE)) })
            }
            Text(
                Rupiah.format(s.amount),
                style = Type.hero,
                color = if (s.amount > 0) c.ink else c.faint,
                textAlign = TextAlign.Center,
                maxLines = 1,
                modifier = Modifier.fillMaxWidth().padding(horizontal = Gutter).padding(top = 28.dp, bottom = 20.dp),
            )

            when (s.type) {
                TxType.EXPENSE -> {
                    Label("Dari pos")
                    PocketPicker(s.pockets, s.pocketId, { vm.onIntent(AddIntent.From(it)) })
                    s.pocketId?.let { id ->
                        val bal = s.balances[id] ?: 0
                        val after = bal - if (s.isPaylater) 0 else s.amount
                        Text(
                            "Sisa pos setelah ini: ${Rupiah.format(after)}",
                            style = Type.bodySmall,
                            color = if (after < 0) c.over else c.mute,
                            modifier = Modifier.padding(horizontal = Gutter, vertical = 8.dp),
                        )
                    }
                    Column(Modifier.padding(horizontal = Gutter).padding(top = 8.dp)) {
                        LineField(s.merchant, { vm.onIntent(AddIntent.Merchant(it)) }, "Di mana? (mis. Indomaret, Kopi Kenangan)")
                        Spacer(Modifier.height(8.dp))
                        LineField(s.note, { vm.onIntent(AddIntent.Note(it)) }, "Catatan (opsional)")
                    }
                    Row(
                        Modifier.fillMaxWidth().padding(horizontal = Gutter, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text("Pakai paylater", style = Type.strong, color = c.ink)
                            Text("Dicatat sebagai hutang, tidak mengurangi pos sekarang.", style = Type.bodySmall, color = c.mute)
                        }
                        Switch(
                            checked = s.isPaylater,
                            onCheckedChange = { vm.onIntent(AddIntent.Paylater(it)) },
                            colors = SwitchDefaults.colors(checkedTrackColor = c.ink, checkedThumbColor = c.paper, uncheckedBorderColor = c.line),
                        )
                    }
                }
                TxType.INCOME -> {
                    Label("Masuk ke")
                    Row(Modifier.padding(horizontal = Gutter), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Choice("Bagi sesuai persen", s.split, { vm.onIntent(AddIntent.Split(true)) })
                        Choice("Satu pos saja", !s.split, { vm.onIntent(AddIntent.Split(false)) })
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
                        LineField(s.merchant, { vm.onIntent(AddIntent.Merchant(it)) }, "Dari mana? (mis. Gaji, Freelance)")
                    }
                }
                TxType.MOVE -> {
                    Label("Dari")
                    PocketPicker(s.pockets, s.pocketId, { vm.onIntent(AddIntent.From(it)) })
                    Label("Ke")
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
            Keypad(s.amount, { vm.onIntent(AddIntent.Amount(it)) }, Modifier.padding(horizontal = Gutter, vertical = 8.dp))
        }
        Hairline()
        InkButton(
            "Simpan",
            onClick = { vm.onIntent(AddIntent.Save) },
            enabled = s.canSave,
            modifier = Modifier.fillMaxWidth().padding(horizontal = Gutter, vertical = 12.dp),
        )
    }
}

@Composable
private fun Label(text: String) {
    Eyebrow(text, Modifier.padding(horizontal = Gutter).padding(top = 16.dp, bottom = 10.dp))
}
