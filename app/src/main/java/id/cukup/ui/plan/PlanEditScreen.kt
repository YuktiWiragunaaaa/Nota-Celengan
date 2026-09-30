package id.cukup.ui.plan

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
import id.cukup.domain.Planner
import id.cukup.domain.Rupiah
import id.cukup.ui.AppViewModel
import id.cukup.ui.components.CardShape
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
    LaunchedEffect(data) {
        if (!loaded) {
            pos.addAll(data.plan)
            data.expenseCategories().forEach { links[it.id] = it.planId }
            loaded = true
        }
    }
    val total = pos.sumOf { it.percent }
    val preview = Planner.split(data.planStatus.basis, pos).toMap()

    Column(Modifier.fillMaxSize().background(c.paper).systemBarsPadding().imePadding()) {
        TopBar("Ubah rencana", onBack)
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
            InfoBox(
                "Cara kerjanya",
                listOf(
                    "1. Tiap pos dapat persen dari uang yang dibagi (${Rupiah.short(data.planStatus.basis)}).",
                    "2. Pilih kategori mana yang dihitung ke pos mana.",
                    "3. Tiap kamu catat pengeluaran di kategori itu, sisa batas posnya berkurang.",
                ),
                Modifier.padding(horizontal = Gutter),
            )
            SectionHeader("Pos", trailing = {
                Text(
                    "Total $total%", style = Type.strong,
                    color = when {
                        total > 100 -> c.over
                        total < 100 -> c.caution
                        else -> c.good
                    },
                )
            })
            if (total != 100) {
                Text(
                    if (total > 100) "Lebih dari 100%. Batasnya dibagi rata supaya totalnya tetap sama dengan uang yang dibagi."
                    else "Sisa ${100 - total}% tidak masuk pos mana pun (bebas).",
                    style = Type.bodySmall, color = c.mute, modifier = Modifier.padding(horizontal = Gutter),
                )
            }
            pos.forEachIndexed { i, p ->
                Column(Modifier.padding(horizontal = Gutter, vertical = 8.dp).fillMaxWidth().clip(CardShape).background(c.card).padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(p.emoji, fontSize = 22.sp, modifier = Modifier.clip(Pill).clickable { looking = i }.padding(4.dp))
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
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 8.dp)) {
                        Text("${p.percent}%", style = Type.number, color = c.ink, modifier = Modifier.width(70.dp))
                        Slider(
                            value = p.percent.toFloat(), onValueChange = { v -> pos[i] = p.copy(percent = (v / 5f).roundToInt() * 5) },
                            valueRange = 0f..100f, modifier = Modifier.weight(1f),
                            colors = SliderDefaults.colors(thumbColor = colorOf(p), activeTrackColor = colorOf(p), inactiveTrackColor = c.line),
                        )
                    }
                    Text("= ${Rupiah.format(preview[p.id] ?: 0)}", style = Type.bodySmall, color = c.mute)
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
                    Text("${cat.emoji}  ${cat.name}", style = Type.strong, color = c.ink, modifier = Modifier.padding(horizontal = Gutter))
                    Row(
                        Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = Gutter, vertical = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        pos.forEach { p -> Choice("${p.emoji} ${p.name}", links[cat.id] == p.id, { links[cat.id] = p.id }) }
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

    val lookIndex = looking
    val lookPos = lookIndex?.let { pos.getOrNull(it) }
    if (lookIndex != null && lookPos != null) {
        val i = lookIndex
        val p = lookPos
        run {
            AlertDialog(
                onDismissRequest = { looking = null },
                title = { Text("Ikon & warna", style = Type.title) },
                text = { Column(Modifier.verticalScroll(rememberScrollState())) { LookPicker(p.emoji, p.color, { pos[i] = pos[i].copy(emoji = it) }, { pos[i] = pos[i].copy(color = it) }) } },
                confirmButton = { TextButton({ looking = null }) { Text("Selesai", color = c.ink) } },
                containerColor = c.card,
            )
        }
    }
}
