package id.cukup.ui.plan

import id.cukup.ui.components.Pill
import androidx.compose.foundation.layout.fillMaxHeight
import id.cukup.ui.components.GlassIcon
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
import id.cukup.domain.PosPeriod
import id.cukup.domain.PosStatus
import id.cukup.domain.Presets
import id.cukup.domain.Rupiah
import id.cukup.ui.AppViewModel
import id.cukup.ui.components.AmountDialog
import id.cukup.ui.components.CardShape
import id.cukup.ui.components.cardSurface
import id.cukup.ui.components.ChipPicker
import id.cukup.ui.components.Eyebrow
import id.cukup.ui.components.Gutter
import id.cukup.ui.components.InfoBox
import id.cukup.ui.components.InkButton
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import id.cukup.ui.components.Hairline
import id.cukup.ui.components.LineField
import id.cukup.ui.components.ScheduleEditor
import id.cukup.ui.components.describe
import id.cukup.ui.components.Link
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
fun PlanScreen(contentPadding: PaddingValues, onEditPlan: () -> Unit, onOpenReport: () -> Unit, vm: AppViewModel = hiltViewModel()) {
    val o by vm.overview.collectAsStateWithLifecycle()
    val c = colors
    val data = o ?: return
    var editBasis by remember { mutableStateOf(false) }
    var confirmClear by remember { mutableStateOf(false) }
    var editSchedule by remember { mutableStateOf(false) }
    var goalEditing by remember { mutableStateOf<Goal?>(null) }
    var goalAdding by remember { mutableStateOf<Goal?>(null) }

    Column(Modifier.fillMaxSize().background(c.paper).verticalScroll(rememberScrollState()).padding(contentPadding)) {
        Text("Rencana", style = Type.display, color = c.ink, modifier = Modifier.padding(horizontal = Gutter).padding(top = 24.dp, bottom = 12.dp))
        InfoBox(
            "Rencana tidak memindahkan uang",
            listOf(
                "Uangmu tetap di dompet seperti yang tercatat. Di sini kamu hanya menetapkan batas, misalnya \"makan maksimal Rp300 rb per minggu\" atau \"keinginan maksimal 30% dari gaji\".",
                "Cukup lalu membandingkan batas itu dengan catatanmu, dan memberi tahu kalau hampir lewat.",
            ),
            Modifier.padding(horizontal = Gutter),
        )
        Spacer(Modifier.height(8.dp))
        PayRules(data, onIncome = { editBasis = true }, onSchedule = { editSchedule = true }, onSplit = onEditPlan)
        Link("Laporan periode lalu", "Pengeluaran terbanyak, kesimpulan, dan saran. Dikirim juga tiap tanggal gajian.", leading = "🧾", onClick = onOpenReport)

        if (data.plan.isEmpty()) {
            SectionHeader("Pilih cara membagi")
            Text(
                "Tentukan batasmu sendiri, atau mulai dari pola yang sudah jadi. Semuanya bisa diubah kapan saja.",
                style = Type.bodySmall, color = c.mute, modifier = Modifier.padding(horizontal = Gutter),
            )
            Spacer(Modifier.height(10.dp))
            Column(
                Modifier.padding(horizontal = Gutter, vertical = 5.dp).fillMaxWidth().clip(CardShape).background(c.accent.copy(alpha = 0.12f))
                    .clickable(onClick = onEditPlan).padding(16.dp),
            ) {
                Text("Atur sendiri", style = Type.title, color = c.ink)
                Text(
                    "Ketik batasnya langsung, misalnya Makan Rp300 rb per minggu atau Jajan Rp20 rb per hari. Tanpa persen, tanpa pola.",
                    style = Type.bodySmall, color = c.mute,
                )
            }
            Presets.plans.forEach { p ->
                Column(
                    Modifier.padding(horizontal = Gutter, vertical = 5.dp).fillMaxWidth().cardSurface()
                        .clickable { vm.applyPlanPreset(p); if (data.settings.planBasis.fixedAmount <= 0 && data.settings.planBasis.mode == PlanBasis.Mode.FIXED) editBasis = true }
                        .padding(16.dp),
                ) {
                    Text(p.title, style = Type.title, color = c.ink)
                    Text(p.subtitle, style = Type.bodySmall, color = c.mute)
                    Spacer(Modifier.height(8.dp))
                    PresetParts(p)
                }
            }
        } else {
            Totals(data)
            SectionHeader("Rencana", trailing = { TextAction("Ubah", onEditPlan) })
            data.planStatus.rows.forEach { row -> PosRow(row, data) }
            Row(Modifier.padding(horizontal = Gutter, vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                LineButton("Ubah rencana", onEditPlan, Modifier.weight(1f), height = 44.dp)
                LineButton("Hapus rencana", { confirmClear = true }, Modifier.weight(1f), height = 44.dp, danger = true)
            }
        }

        Goals(data, onNew = { goalEditing = Goal(0, "", "🎯", 0) }, onEdit = { goalEditing = it }, onAdd = { goalAdding = it })
        Spacer(Modifier.height(24.dp))
    }

    if (editBasis) BasisDialog(data, onDismiss = { editBasis = false }) { vm.setPlanBasis(it); editBasis = false }
    if (editSchedule) {
        var draft by remember { mutableStateOf(data.settings.schedule) }
        AlertDialog(
            onDismissRequest = { editSchedule = false },
            title = { Text("Tanggal gajian", style = Type.title) },
            text = { Column(Modifier.verticalScroll(rememberScrollState())) { ScheduleEditor(draft, { draft = it }) } },
            confirmButton = { TextButton({ vm.settings { it.copy(schedule = draft) }; editSchedule = false }) { Text("Simpan", color = c.ink) } },
            dismissButton = { TextButton({ editSchedule = false }) { Text("Batal", color = c.mute) } },
            containerColor = c.card,
        )
    }
    if (confirmClear) {
        AlertDialog(
            onDismissRequest = { confirmClear = false },
            title = { Text("Hapus rencana?", style = Type.title) },
            text = { Text("Rencana dihapus. Catatan dan saldo dompet tidak berubah sama sekali.", style = Type.body) },
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

/**
 * Aturan gaji: tiga hal yang menentukan seluruh rencana, di satu kartu.
 * Pendapatan = uang yang dibagi, tanggal gajian = kapan periode mulai, pembagian = batas tiap pos.
 */
@Composable
private fun PayRules(o: Overview, onIncome: () -> Unit, onSchedule: () -> Unit, onSplit: () -> Unit) {
    val c = colors
    val b = o.settings.planBasis
    Column(Modifier.padding(horizontal = Gutter).padding(top = 14.dp, bottom = 6.dp).fillMaxWidth().cardSurface()) {
        Eyebrow("Aturan gaji", Modifier.padding(start = 16.dp, top = 14.dp), color = c.accent)
        RuleRow(
            "Pendapatan",
            Rupiah.format(o.planStatus.basis) + when (b.mode) {
                PlanBasis.Mode.FIXED -> " · angka tetap"
                PlanBasis.Mode.LAST_PERIOD -> " · dari uang masuk periode lalu"
                PlanBasis.Mode.THIS_PERIOD -> " · dari uang masuk periode ini"
            },
            warn = o.planStatus.basis <= 0 && o.plan.any { !it.fixed },
            onClick = onIncome,
        )
        Hairline(Modifier.padding(horizontal = 16.dp))
        RuleRow("Tanggal gajian", o.settings.schedule.describe() + " · awal tiap periode", onClick = onSchedule)
        Hairline(Modifier.padding(horizontal = 16.dp))
        RuleRow(
            "Pembagian",
            if (o.plan.isEmpty()) "Belum diatur. Ketuk untuk membagi uangmu." else o.plan.joinToString(" · ") { "${it.name} ${it.share}" },
            onClick = onSplit,
        )
    }
}

@Composable
private fun RuleRow(title: String, value: String, warn: Boolean = false, onClick: () -> Unit) {
    val c = colors
    Row(Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, style = Type.strong, color = c.ink)
            Text(value, style = Type.bodySmall, color = if (warn) c.caution else c.mute)
        }
        Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, null, tint = c.faint)
    }
}

@Composable
private fun Basis(o: Overview, onEdit: () -> Unit) {
    val c = colors
    val b = o.settings.planBasis
    Column(
        Modifier.padding(horizontal = Gutter).padding(top = 16.dp).fillMaxWidth().cardSurface().clickable(onClick = onEdit).padding(16.dp),
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
        Stat("Aman/hari", Rupiah.short(s.perDay), if (s.perDay <= 0) c.over else c.good, Modifier.weight(1f))
    }
}

@Composable
private fun Stat(label: String, value: String, color: Color, modifier: Modifier) {
    val c = colors
    Column(modifier.cardSurface().padding(12.dp)) {
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
            GlassIcon(row.pos.emoji, colorOf(row.pos), size = 40.dp)
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text("${row.pos.name} · ${row.pos.share}", style = Type.strong, color = c.ink)
                Text(
                    (if (row.pos.kind == PlanKind.SPEND) "Batas belanja ${Rupiah.format(row.limit)}" else "Target sisihan ${Rupiah.format(row.limit)}") +
                        when (row.pos.period) {
                            PosPeriod.WEEK -> " minggu ini · ${row.daysLeft} hari lagi"
                            PosPeriod.DAY -> " hari ini"
                            PosPeriod.CYCLE -> ""
                        },
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
                    "Dihitung dari kategori: ${cats.joinToString { it.name }}"
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
                Text("Hanya untuk pos berpersen: angka ini yang dibagi sesuai persennya. Pos bernominal tidak terpengaruh. Pilih sumbernya:", style = Type.bodySmall, color = c.mute)
                Spacer(Modifier.height(8.dp))
                Option("Angka tetap", "Misalnya gajimu Rp4 jt sebulan. Cocok kalau penghasilan rutin.", mode == PlanBasis.Mode.FIXED) { mode = PlanBasis.Mode.FIXED }
                if (mode == PlanBasis.Mode.FIXED) {
                    LineField(amount, { amount = it.filter(Char::isDigit).take(12) }, "Misalnya 4000000", keyboardType = KeyboardType.Number)
                    amount.toLongOrNull()?.let { Text(Rupiah.format(it), style = Type.bodySmall, color = c.mute, modifier = Modifier.padding(top = 4.dp)) }
                }
                Option(
                    "Uang masuk periode lalu",
                    "Otomatis dari catatan: ${Rupiah.format(o.lastTotals.income)}. Cocok kalau penghasilan tidak tetap: yang masuk periode lalu, itu yang dibagi sekarang.",
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
                GlassIcon(g.emoji, color, size = 38.dp)
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(g.name, style = Type.strong, color = c.ink)
                Text("${Rupiah.short(progress.saved)} dari ${Rupiah.short(g.target)} · ${(progress.ratio * 100).toInt()}%", style = Type.bodySmall, color = c.mute)
                Text(
                    when {
                        progress.reached -> "Tercapai!"
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
                val items = listOf(PickItem(-1, "Isi sendiri", "✏️", c.mute)) +
                    o.accounts.map { PickItem(it.account.id, it.account.name, it.account.emoji, colorOf(it.account), Rupiah.short(it.balance), id.cukup.domain.Brands.forAccountName(it.account.name)?.mark) }
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
                LookPicker(emoji, color, { emoji = it }, { color = it }, fallback = colors.of(g.color, g.sortOrder))
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

/** Isi template: batang proporsi berwarna + ikon kaca tiap pos. */
@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
private fun PresetParts(p: id.cukup.domain.PlanPreset) {
    val c = colors
    Row(Modifier.fillMaxWidth().height(8.dp).clip(Pill), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
        p.pos.forEachIndexed { i, s -> Box(Modifier.weight(s.percent.coerceAtLeast(1).toFloat()).fillMaxHeight().background(c.pocket(i))) }
    }
    Spacer(Modifier.height(10.dp))
    androidx.compose.foundation.layout.FlowRow(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        p.pos.forEachIndexed { i, s ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                GlassIcon(s.emoji, c.pocket(i), size = 26.dp)
                Spacer(Modifier.width(6.dp))
                Text("${s.name} ${s.percent}%", style = Type.bodySmall, color = c.ink)
            }
        }
    }
}
