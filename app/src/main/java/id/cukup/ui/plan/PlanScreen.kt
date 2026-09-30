package id.cukup.ui.plan

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import id.cukup.data.Overview
import id.cukup.domain.Goal
import id.cukup.domain.GoalProgress
import id.cukup.domain.PlanBasis
import id.cukup.domain.PlanKind
import id.cukup.domain.PosStatus
import id.cukup.domain.Presets
import id.cukup.domain.Rupiah
import id.cukup.ui.AppViewModel
import id.cukup.ui.components.AmountDialog
import id.cukup.ui.components.CardShape
import id.cukup.ui.components.ChipPicker
import id.cukup.ui.components.Eyebrow
import id.cukup.ui.components.Gutter
import id.cukup.ui.components.InfoBox
import id.cukup.ui.components.InkButton
import id.cukup.ui.components.LineField
import id.cukup.ui.components.LineButton
import id.cukup.ui.components.LookPicker
import id.cukup.ui.components.Option
import id.cukup.ui.components.PickItem
import id.cukup.ui.components.PocketRing
import id.cukup.ui.components.SectionHeader
import id.cukup.ui.components.TextAction
import id.cukup.ui.components.UsageBar
import id.cukup.ui.components.colorOf
import id.cukup.ui.theme.Type
import id.cukup.ui.theme.colors

/**
 * RENCANA. Batas yang kamu tetapkan sendiri, dibandingkan dengan catatan.
 * Tidak ada uang yang dipindah di sini.
 */
@Composable
fun PlanScreen(contentPadding: PaddingValues, onEditPlan: () -> Unit, vm: AppViewModel = hiltViewModel()) {
    val o by vm.overview.collectAsStateWithLifecycle()
    val c = colors
    val data = o ?: return
    var editBasis by remember { mutableStateOf(false) }
    var confirmClear by remember { mutableStateOf(false) }
    var goalEditing by remember { mutableStateOf<Goal?>(null) }
    var goalAdding by remember { mutableStateOf<Goal?>(null) }

    Column(Modifier.fillMaxSize().background(c.paper).verticalScroll(rememberScrollState()).padding(contentPadding)) {
        Text("Rencana", style = Type.display, color = c.ink, modifier = Modifier.padding(horizontal = Gutter).padding(top = 24.dp, bottom = 12.dp))
        InfoBox(
            "Rencana tidak memindahkan uang",
            listOf(
                "Uangmu tetap di dompet seperti yang tercatat. Di sini kamu hanya menetapkan batas, misalnya \"belanja keinginan maksimal 30% dari gaji\".",
                "Cukup lalu membandingkan batas itu dengan catatanmu, dan memberi tahu kalau hampir lewat.",
            ),
            Modifier.padding(horizontal = Gutter),
        )

        if (data.plan.isEmpty()) {
            SectionHeader("Pilih cara membagi")
            Text(
                "Pilih satu untuk mulai. Persen dan kategorinya bisa diubah kapan saja.",
                style = Type.bodySmall, color = c.mute, modifier = Modifier.padding(horizontal = Gutter),
            )
            Spacer(Modifier.height(10.dp))
            Presets.plans.forEach { p ->
                Column(
                    Modifier.padding(horizontal = Gutter, vertical = 5.dp).fillMaxWidth().clip(CardShape).background(c.card)
                        .clickable { vm.applyPlanPreset(p); if (data.settings.planBasis.fixedAmount <= 0 && data.settings.planBasis.mode == PlanBasis.Mode.FIXED) editBasis = true }
                        .padding(16.dp),
                ) {
                    Text(p.title, style = Type.title, color = c.ink)
                    Text(p.subtitle, style = Type.bodySmall, color = c.mute)
                    Spacer(Modifier.height(8.dp))
                    Text(p.pos.joinToString("  ·  ") { "${it.emoji} ${it.name} ${it.percent}%" }, style = Type.bodySmall, color = c.ink)
                }
            }
        } else {
            Basis(data) { editBasis = true }
            Totals(data)
            SectionHeader("Pos rencana", trailing = { TextAction("Ubah", onEditPlan) })
            data.planStatus.rows.forEach { row -> PosRow(row, data) }
            Row(Modifier.padding(horizontal = Gutter, vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                LineButton("Ubah rencana", onEditPlan, Modifier.weight(1f), height = 44.dp)
                LineButton("Hapus rencana", { confirmClear = true }, Modifier.weight(1f), height = 44.dp)
            }
        }

        Goals(data, onNew = { goalEditing = Goal(0, "", "🎯", 0) }, onEdit = { goalEditing = it }, onAdd = { goalAdding = it })
        Spacer(Modifier.height(24.dp))
    }

    if (editBasis) BasisDialog(data, onDismiss = { editBasis = false }) { vm.setPlanBasis(it); editBasis = false }
    if (confirmClear) {
        AlertDialog(
            onDismissRequest = { confirmClear = false },
            title = { Text("Hapus rencana?", style = Type.title) },
            text = { Text("Pos rencana dihapus. Catatan dan saldo dompet tidak berubah sama sekali.", style = Type.body) },
            confirmButton = { TextButton({ vm.clearPlan(); confirmClear = false }) { Text("Hapus", color = c.over) } },
            dismissButton = { TextButton({ confirmClear = false }) { Text("Batal", color = c.ink) } },
            containerColor = c.card,
        )
    }
    goalEditing?.let { g ->
        GoalDialog(g, data, onDismiss = { goalEditing = null }, onDelete = { vm.deleteGoal(g.id); goalEditing = null }) {
            vm.saveGoal(it); goalEditing = null
        }
    }
    goalAdding?.let { g ->
        AmountDialog(
            "Tambah ke ${g.name}",
            "Isi berapa yang sudah kamu sisihkan. Ini hanya catatan target; saldo dompet tidak berubah.",
            0, onDismiss = { goalAdding = null }, allowNegative = true, confirmText = "Tambah",
        ) { vm.addToGoal(g.id, it); goalAdding = null }
    }
}

@Composable
private fun Basis(o: Overview, onEdit: () -> Unit) {
    val c = colors
    val b = o.settings.planBasis
    Column(
        Modifier.padding(horizontal = Gutter).padding(top = 16.dp).fillMaxWidth().clip(CardShape).background(c.card).clickable(onClick = onEdit).padding(16.dp),
    ) {
        Eyebrow("Uang yang dibagi ${o.periodName}")
        Text(Rupiah.format(o.planStatus.basis), style = Type.number, color = c.ink)
        Text(
            when (b.mode) {
                PlanBasis.Mode.FIXED -> "Angka tetap yang kamu isi. Ketuk untuk ubah."
                PlanBasis.Mode.LAST_PERIOD -> "Dari total uang masuk periode lalu (tercatat). Ketuk untuk ubah."
                PlanBasis.Mode.THIS_PERIOD -> "Dari uang masuk periode ini, bertambah tiap ada pemasukan. Ketuk untuk ubah."
            },
            style = Type.bodySmall, color = c.mute,
        )
        if (o.planStatus.basis <= 0) Text("Masih nol, jadi rencana belum bisa dihitung.", style = Type.bodySmall, color = c.caution)
    }
}

@Composable
private fun Totals(o: Overview) {
    val c = colors
    val s = o.planStatus
    if (!s.active) return
    Row(Modifier.padding(horizontal = Gutter).padding(top = 10.dp).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Stat("Batas belanja", Rupiah.short(s.spendLimit), c.ink, Modifier.weight(1f))
        Stat("Terpakai", Rupiah.short(s.spendUsed), if (s.spendLeft < 0) c.over else c.ink, Modifier.weight(1f))
        Stat("Aman per hari", Rupiah.short(s.perDay), c.good, Modifier.weight(1f))
    }
}

@Composable
private fun Stat(label: String, value: String, color: Color, modifier: Modifier) {
    val c = colors
    Column(modifier.clip(CardShape).background(c.card).padding(12.dp)) {
        Text(label, style = Type.label, color = c.mute)
        Text(value, style = Type.amount, color = color)
    }
}

@Composable
private fun PosRow(row: PosStatus, o: Overview) {
    val c = colors
    val color = colorOf(row.pos)
    var open by rememberSaveable(row.pos.id) { mutableStateOf(false) }
    val cats = o.expenseCategories().filter { it.planId == row.pos.id }
    Column(Modifier.fillMaxWidth().clickable { open = !open }.padding(horizontal = Gutter, vertical = 12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(row.pos.emoji, fontSize = 20.sp)
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text("${row.pos.name} · ${row.pos.percent}%", style = Type.strong, color = c.ink)
                Text(
                    if (row.pos.kind == PlanKind.SPEND) "Batas belanja ${Rupiah.format(row.limit)}" else "Target sisihan ${Rupiah.format(row.limit)}",
                    style = Type.bodySmall, color = c.mute,
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    when {
                        row.pos.kind == PlanKind.SAVE -> Rupiah.short(row.used)
                        row.left >= 0 -> "sisa ${Rupiah.short(row.left)}"
                        else -> "lewat ${Rupiah.short(-row.left)}"
                    },
                    style = Type.amount, color = if (row.pos.kind == PlanKind.SPEND && row.left < 0) c.over else c.ink,
                )
                Text(if (row.pos.kind == PlanKind.SAVE) "sudah disisihkan" else "${Rupiah.short(row.used)} terpakai", style = Type.label, color = c.faint)
            }
        }
        Spacer(Modifier.height(8.dp))
        UsageBar(row.ratio, if (row.pos.kind == PlanKind.SAVE) c.good else color)
        if (open) {
            Spacer(Modifier.height(8.dp))
            Text(
                if (row.pos.kind == PlanKind.SAVE) {
                    "Dihitung dari: uang yang kamu pindah ke dompet jenis Tabungan" + if (cats.isEmpty()) "." else ", dan pengeluaran ${cats.joinToString { it.name }}."
                } else if (cats.isEmpty()) {
                    "Belum ada kategori yang dihitung di sini. Ketuk Ubah untuk memilih."
                } else {
                    "Dihitung dari kategori: ${cats.joinToString { "${it.emoji} ${it.name}" }}"
                },
                style = Type.bodySmall, color = c.mute,
            )
        }
    }
}

@Composable
private fun BasisDialog(o: Overview, onDismiss: () -> Unit, onSave: (PlanBasis) -> Unit) {
    val c = colors
    val b = o.settings.planBasis
    var mode by remember { mutableStateOf(b.mode) }
    var amount by remember { mutableStateOf(if (b.fixedAmount > 0) b.fixedAmount.toString() else "") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Uang yang dibagi", style = Type.title) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                Text("Rencana membagi angka ini ke pos-pos sesuai persen. Pilih sumbernya:", style = Type.bodySmall, color = c.mute)
                Spacer(Modifier.height(8.dp))
                Option("Angka tetap", "Misalnya gajimu Rp4 jt sebulan. Cocok kalau penghasilan rutin.", mode == PlanBasis.Mode.FIXED) { mode = PlanBasis.Mode.FIXED }
                if (mode == PlanBasis.Mode.FIXED) {
                    LineField(amount, { amount = it.filter(Char::isDigit).take(12) }, "Misalnya 4000000", keyboardType = KeyboardType.Number)
                    amount.toLongOrNull()?.let { Text(Rupiah.format(it), style = Type.bodySmall, color = c.mute, modifier = Modifier.padding(top = 4.dp)) }
                }
                Option(
                    "Uang masuk periode lalu",
                    "Otomatis dari catatan: ${Rupiah.format(o.lastTotals.income)}. Contoh: belanja maksimal 50% dari gaji minggu kemarin.",
                    mode == PlanBasis.Mode.LAST_PERIOD,
                ) { mode = PlanBasis.Mode.LAST_PERIOD }
                Option(
                    "Uang masuk periode ini",
                    "Otomatis dari catatan: ${Rupiah.format(o.totals.income)} sejauh ini. Batas ikut naik tiap ada pemasukan.",
                    mode == PlanBasis.Mode.THIS_PERIOD,
                ) { mode = PlanBasis.Mode.THIS_PERIOD }
            }
        },
        confirmButton = { TextButton({ onSave(PlanBasis(mode, amount.toLongOrNull() ?: 0)) }) { Text("Simpan", color = c.ink) } },
        dismissButton = { TextButton(onDismiss) { Text("Batal", color = c.mute) } },
        containerColor = c.card,
    )
}

// ——— Target tabungan ———

@Composable
private fun Goals(o: Overview, onNew: () -> Unit, onEdit: (Goal) -> Unit, onAdd: (Goal) -> Unit) {
    val c = colors
    SectionHeader("Target tabungan", trailing = { TextAction("+ Target", onNew) })
    if (o.goals.isEmpty()) {
        Column(
            Modifier.padding(horizontal = Gutter).fillMaxWidth().clip(CardShape).border(1.dp, c.line, CardShape).clickable(onClick = onNew).padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(Icons.Rounded.Add, null, tint = c.mute)
            Text("Mau nabung buat apa?", style = Type.strong, color = c.ink)
            Text("HP baru, liburan, dana darurat. Cukup hitung kapan kira-kira tercapai.", style = Type.bodySmall, color = c.mute)
        }
        return
    }
    val perPeriod = o.planStatus.rows.filter { it.pos.kind == PlanKind.SAVE }.sumOf { it.limit } / o.goals.size.coerceAtLeast(1)
    val balances = o.accounts.associate { it.account.id to it.balance }
    o.goals.forEachIndexed { i, g ->
        val saved = g.accountId?.let { balances[it] } ?: g.saved
        val progress = GoalProgress.of(saved.coerceAtLeast(0), g.target, perPeriod)
        val color = c.of(g.color, i)
        Row(
            Modifier.fillMaxWidth().clickable { onEdit(g) }.padding(horizontal = Gutter, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(Modifier.size(56.dp), contentAlignment = Alignment.Center) {
                PocketRing(listOf(Triple(color, 1f, progress.ratio)), Modifier.size(56.dp), stroke = 6.dp)
                Text(g.emoji, fontSize = 20.sp)
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(g.name, style = Type.strong, color = c.ink)
                Text("${Rupiah.short(progress.saved)} dari ${Rupiah.short(g.target)} · ${(progress.ratio * 100).toInt()}%", style = Type.bodySmall, color = c.mute)
                Text(
                    when {
                        progress.reached -> "Tercapai! 🎉"
                        progress.periodsLeft == null -> if (g.accountId != null) "Mengikuti saldo dompet" else "Tambah sedikit-sedikit, ya"
                        else -> "Kira-kira ${progress.periodsLeft} periode lagi, sesuai rencana tabungan"
                    },
                    style = Type.label, color = if (progress.reached) c.good else c.faint,
                )
            }
            if (g.accountId == null) TextAction("+ Isi", { onAdd(g) })
        }
    }
}

@Composable
private fun GoalDialog(g: Goal, o: Overview, onDismiss: () -> Unit, onDelete: () -> Unit, onSave: (Goal) -> Unit) {
    val c = colors
    var name by remember { mutableStateOf(g.name) }
    var emoji by remember { mutableStateOf(g.emoji) }
    var color by remember { mutableStateOf(g.color) }
    var target by remember { mutableStateOf(if (g.target > 0) g.target.toString() else "") }
    var saved by remember { mutableStateOf(if (g.saved > 0) g.saved.toString() else "") }
    var account by remember { mutableStateOf(g.accountId) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (g.id == 0L) "Target baru" else "Ubah target", style = Type.title) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                LineField(name, { name = it.take(24) }, "Nama (mis. HP baru)")
                Spacer(Modifier.height(8.dp))
                LineField(target, { target = it.filter(Char::isDigit).take(12) }, "Target, mis. 3000000", keyboardType = KeyboardType.Number)
                target.toLongOrNull()?.let { Text(Rupiah.format(it), style = Type.bodySmall, color = c.mute, modifier = Modifier.padding(top = 4.dp)) }
                Spacer(Modifier.height(14.dp))
                Eyebrow("Terkumpul dihitung dari")
                Spacer(Modifier.height(8.dp))
                val items = listOf(PickItem(-1, "Isi sendiri", "✍️", c.mute)) +
                    o.accounts.map { PickItem(it.account.id, it.account.name, it.account.emoji, colorOf(it.account), Rupiah.short(it.balance)) }
                ChipPicker(items, account ?: -1, { account = it.takeIf { id -> id > 0 } }, Modifier.padding(horizontal = 0.dp))
                Text(
                    if (account == null) "Kamu tambahkan sendiri tiap menyisihkan uang." else "Terkumpul = saldo dompet itu. Cocok kalau punya rekening/celengan khusus.",
                    style = Type.bodySmall, color = c.mute, modifier = Modifier.padding(top = 6.dp),
                )
                if (account == null) {
                    Spacer(Modifier.height(8.dp))
                    LineField(saved, { saved = it.filter(Char::isDigit).take(12) }, "Sudah terkumpul (opsional)", keyboardType = KeyboardType.Number)
                }
                Spacer(Modifier.height(14.dp))
                LookPicker(emoji, color, { emoji = it }, { color = it })
                if (g.id != 0L) {
                    Spacer(Modifier.height(14.dp))
                    TextAction("Hapus target ini", onDelete, color = c.over)
                }
            }
        },
        confirmButton = {
            TextButton(
                {
                    onSave(g.copy(name = name.trim().ifBlank { "Target" }, emoji = emoji, color = color, target = target.toLongOrNull() ?: 0, saved = saved.toLongOrNull() ?: 0, accountId = account))
                },
                enabled = (target.toLongOrNull() ?: 0) > 0,
            ) { Text("Simpan", color = c.ink) }
        },
        dismissButton = { TextButton(onDismiss) { Text("Batal", color = c.mute) } },
        containerColor = c.card,
    )
}

/** Dipakai layar lain yang butuh tombol utama menuju rencana. */
@Composable
fun PlanCta(onClick: () -> Unit, modifier: Modifier = Modifier) = InkButton("Buat rencana", onClick, modifier)
