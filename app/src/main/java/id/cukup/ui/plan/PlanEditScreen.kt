package id.cukup.ui.plan

import id.cukup.ui.components.GlassIcon
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import id.cukup.domain.PlanKind
import id.cukup.domain.PlanPos
import id.cukup.domain.PosPeriod
import id.cukup.domain.Planner
import id.cukup.domain.Rupiah
import id.cukup.ui.AppViewModel
import id.cukup.ui.components.AmountDialog
import id.cukup.ui.components.CardShape
import id.cukup.ui.components.cardSurface
import id.cukup.ui.components.Choice
import id.cukup.ui.components.Gutter
import id.cukup.ui.components.Hairline
import id.cukup.ui.components.InfoBox
import id.cukup.ui.components.InkButton
import id.cukup.ui.components.LineField
import id.cukup.ui.components.LookPicker
import id.cukup.ui.components.Pill
import id.cukup.ui.components.SectionHeader
import id.cukup.ui.components.TextAction
import id.cukup.ui.components.TopBar
import id.cukup.ui.components.colorOf
import id.cukup.ui.theme.Type
import id.cukup.ui.theme.colors
import kotlin.math.roundToInt

/** Ubah persen satu pos; kelebihan di atas 100% diambil dari pos lain, yang terbesar dulu. */
private fun setPercent(pos: MutableList<PlanPos>, i: Int, value: Int) {
    pos[i] = pos[i].copy(percent = value)
    var excess = pos.filter { !it.fixed }.sumOf { it.percent } - 100
    while (excess > 0) {
        val j = pos.indices.filter { it != i && !pos[it].fixed && pos[it].percent > 0 }.maxByOrNull { pos[it].percent } ?: break
        val cut = minOf(5, excess, pos[j].percent)
        pos[j] = pos[j].copy(percent = pos[j].percent - cut)
        excess -= cut
    }
}

@Composable
fun PlanEditScreen(onBack: () -> Unit, vm: AppViewModel = hiltViewModel()) {
    val o by vm.overview.collectAsStateWithLifecycle()
    val c = colors
    val data = o ?: return
    // Salinan yang bisa diubah. Pos baru diberi id negatif sampai disimpan.
    val pos = remember { mutableStateListOf<PlanPos>() }
    val links = remember { mutableStateMapOf<Long, Long?>() }
    var loaded by remember { mutableStateOf(false) }
    var nextTemp by remember { mutableStateOf(-1L) }
    var looking by remember { mutableStateOf<Int?>(null) }
    var typingPercent by remember { mutableStateOf<Int?>(null) }
    var typingAmount by remember { mutableStateOf<Int?>(null) }
    LaunchedEffect(data) {
        if (!loaded) {
            pos.addAll(data.plan)
            data.expenseCategories().forEach { links[it.id] = it.planId }
            loaded = true
        }
    }
    val total = pos.filter { !it.fixed }.sumOf { it.percent }
    val anyPercent = pos.any { !it.fixed }
    val preview = Planner.split(data.planStatus.basis, pos).toMap()

    Column(Modifier.fillMaxSize().background(c.paper).systemBarsPadding().imePadding()) {
        TopBar("Ubah rencana", onBack)
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
            InfoBox(
                "Cara kerjanya",
                listOf(
                    "1. Tiap pos punya batas. Pilih sendiri: nominal bebas (mis. Rp300 rb per minggu) atau persen dari uang yang dibagi (${Rupiah.short(data.planStatus.basis)}).",
                    "2. Pilih kategori mana yang dihitung ke pos mana.",
                    "3. Tiap kamu catat pengeluaran di kategori itu, sisa batas posnya berkurang.",
                ),
                Modifier.padding(horizontal = Gutter),
            )
            SectionHeader("Pos", trailing = {
                if (anyPercent) Text(
                    "Total persen $total%", style = Type.strong,
                    color = when {
                        total > 100 -> c.over
                        total < 100 -> c.caution
                        else -> c.good
                    },
                )
            })
            if (anyPercent && total != 100) {
                Text(
                    if (total > 100) "Lebih dari 100%. Batasnya dibagi rata supaya totalnya tetap sama dengan uang yang dibagi."
                    else "Sisa ${100 - total}% tidak masuk pos mana pun (bebas).",
                    style = Type.bodySmall, color = c.mute, modifier = Modifier.padding(horizontal = Gutter),
                )
            }
            if (pos.isEmpty()) {
                Text(
                    "Belum ada pos. Tekan \"+ Tambah pos\", beri nama (mis. Makan), lalu isi batasnya.",
                    style = Type.body, color = c.mute, modifier = Modifier.padding(horizontal = Gutter, vertical = 8.dp),
                )
            }
            pos.forEachIndexed { i, p ->
                Column(Modifier.padding(horizontal = Gutter, vertical = 8.dp).fillMaxWidth().cardSurface().padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        GlassIcon(p.emoji, colorOf(p), Modifier.clickable { looking = i }, size = 40.dp)
                        Spacer(Modifier.width(8.dp))
                        Column(Modifier.weight(1f)) {
                            LineField(p.name, { v -> pos[i] = p.copy(name = v.take(20)) }, "Nama pos")
                        }
                        Icon(
                            Icons.Rounded.Close, "Hapus pos", tint = c.faint,
                            modifier = Modifier.padding(start = 8.dp).clip(Pill).clickable {
                                links.keys.toList().forEach { k -> if (links[k] == p.id) links[k] = null }
                                pos.removeAt(i)
                            }.padding(6.dp),
                        )
                    }
                    Row(Modifier.padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Choice("Nominal bebas", p.fixed, { if (!p.fixed) typingAmount = i })
                        Choice("Persen", !p.fixed, { pos[i] = p.copy(amount = 0, period = PosPeriod.CYCLE) })
                    }
                    if (p.fixed) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 10.dp)) {
                            Text(
                                Rupiah.format(p.amount), style = Type.number, color = c.ink,
                                modifier = Modifier.clip(CardShape).clickable { typingAmount = i }.padding(vertical = 4.dp),
                            )
                            Spacer(Modifier.width(8.dp))
                            Text("ketuk untuk ubah", style = Type.bodySmall, color = c.faint)
                        }
                        Row(Modifier.padding(top = 6.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Choice("per gajian", p.period == PosPeriod.CYCLE, { pos[i] = p.copy(period = PosPeriod.CYCLE) })
                            Choice("per minggu", p.period == PosPeriod.WEEK, { pos[i] = p.copy(period = PosPeriod.WEEK) })
                            Choice("per hari", p.period == PosPeriod.DAY, { pos[i] = p.copy(period = PosPeriod.DAY) })
                        }
                        Text(
                            when (p.period) {
                                PosPeriod.WEEK -> "Batasnya kembali penuh tiap Senin."
                                PosPeriod.DAY -> "Batasnya kembali penuh tiap hari."
                                PosPeriod.CYCLE -> "Batasnya kembali penuh tiap gajian."
                            },
                            style = Type.bodySmall, color = c.mute, modifier = Modifier.padding(top = 6.dp),
                        )
                    } else {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 8.dp)) {
                            Text(
                                "${p.percent}%", style = Type.number, color = c.ink,
                                modifier = Modifier.width(70.dp).clip(CardShape).clickable { typingPercent = i },
                            )
                            Slider(
                                value = p.percent.toFloat(), onValueChange = { v -> setPercent(pos, i, (v / 5f).roundToInt() * 5) },
                                valueRange = 0f..100f, modifier = Modifier.weight(1f),
                                colors = SliderDefaults.colors(thumbColor = colorOf(p), activeTrackColor = colorOf(p), inactiveTrackColor = c.line),
                            )
                        }
                        Text("= ${Rupiah.format(preview[p.id] ?: 0)} per gajian · ketuk angkanya untuk isi persen sendiri", style = Type.bodySmall, color = c.mute)
                    }
                    Row(Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Choice("Batas belanja", p.kind == PlanKind.SPEND, { pos[i] = p.copy(kind = PlanKind.SPEND) })
                        Choice("Untuk ditabung", p.kind == PlanKind.SAVE, { pos[i] = p.copy(kind = PlanKind.SAVE) })
                    }
                    Text(
                        if (p.kind == PlanKind.SPEND) "Batas maksimal belanja. Cukup mengingatkan kalau hampir lewat."
                        else "Target yang disisihkan. Terhitung saat kamu pindah uang ke dompet Tabungan.",
                        style = Type.bodySmall, color = c.faint, modifier = Modifier.padding(top = 6.dp),
                    )
                }
            }
            TextAction(
                "+ Tambah pos",
                {
                    pos.add(PlanPos(nextTemp, "Pos baru", "✨", 0, PlanKind.SPEND, pos.size))
                    nextTemp -= 1
                },
                Modifier.padding(horizontal = Gutter),
            )

            SectionHeader("Kategori dihitung ke pos mana?")
            Text(
                "Hanya kategori uang keluar. \"Tidak dihitung\" artinya pengeluaran itu tercatat tapi tidak memotong batas mana pun.",
                style = Type.bodySmall, color = c.mute, modifier = Modifier.padding(horizontal = Gutter).padding(bottom = 8.dp),
            )
            data.expenseCategories().forEach { cat ->
                Column(Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                    Row(Modifier.padding(horizontal = Gutter), verticalAlignment = Alignment.CenterVertically) {
                        GlassIcon(cat.emoji, colorOf(cat), size = 30.dp)
                        Spacer(Modifier.width(10.dp))
                        Text(cat.name, style = Type.strong, color = c.ink)
                    }
                    Row(
                        Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = Gutter, vertical = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        pos.forEach { p -> Choice(p.name, links[cat.id] == p.id, { links[cat.id] = p.id }) }
                        Choice("Tidak dihitung", links[cat.id] == null, { links[cat.id] = null })
                    }
                }
                Hairline()
            }
            Spacer(Modifier.height(16.dp))
        }
        InkButton(
            "Simpan rencana",
            onClick = { vm.savePlan(pos.toList(), links.toMap()); onBack() },
            enabled = pos.isNotEmpty() && pos.all { it.name.isNotBlank() },
            modifier = Modifier.fillMaxWidth().padding(horizontal = Gutter, vertical = 12.dp),
        )
    }

    typingAmount?.let { i ->
        val p = pos.getOrNull(i)
        if (p == null) typingAmount = null
        else AmountDialog(
            "Batas ${p.name}", "Isi jumlahnya langsung. Jangkanya (per gajian, minggu, atau hari) dipilih setelah ini.", p.amount,
            onDismiss = { typingAmount = null },
        ) { v ->
            // Nol berarti kembali ke persen.
            pos[i] = if (v > 0) p.copy(amount = v) else p.copy(amount = 0, period = PosPeriod.CYCLE)
            typingAmount = null
        }
    }
    typingPercent?.let { i ->
        val p = pos.getOrNull(i)
        if (p == null) typingPercent = null
        else PercentDialog(p.name, p.percent, { typingPercent = null }) { v -> setPercent(pos, i, v); typingPercent = null }
    }

    val lookIndex = looking
    val lookPos = lookIndex?.let { pos.getOrNull(it) }
    if (lookIndex != null && lookPos != null) {
        val i = lookIndex
        val p = lookPos
        run {
            AlertDialog(
                onDismissRequest = { looking = null },
                title = { Text("Ikon & warna", style = Type.title) },
                text = { Column(Modifier.verticalScroll(rememberScrollState())) { LookPicker(p.emoji, p.color, { pos[i] = pos[i].copy(emoji = it) }, { pos[i] = pos[i].copy(color = it) }, fallback = colorOf(p)) } },
                confirmButton = { TextButton({ looking = null }) { Text("Selesai", color = c.ink) } },
                containerColor = c.card,
            )
        }
    }
}

/** Isi persen sendiri (1–100), tidak harus kelipatan 5. */
@Composable
private fun PercentDialog(name: String, initial: Int, onDismiss: () -> Unit, onConfirm: (Int) -> Unit) {
    val c = colors
    var draft by remember { mutableStateOf(initial.toString()) }
    val value = (draft.toIntOrNull() ?: 0).coerceIn(0, 100)
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Persen $name", style = Type.title) },
        text = {
            Column {
                Text("Angka bebas dari 0 sampai 100. Kalau totalnya lewat 100%, pos lain dikurangi otomatis.", style = Type.body, color = c.mute)
                Spacer(Modifier.height(12.dp))
                LineField(draft, { v -> draft = v.filter(Char::isDigit).take(3) }, "0", keyboardType = androidx.compose.ui.text.input.KeyboardType.Number)
            }
        },
        confirmButton = { TextButton({ onConfirm(value) }) { Text("Simpan", color = c.ink) } },
        dismissButton = { TextButton(onDismiss) { Text("Batal", color = c.mute) } },
        containerColor = c.card,
    )
}
