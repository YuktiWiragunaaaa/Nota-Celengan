package id.cukup.ui.pockets

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import id.cukup.domain.PocketKind
import id.cukup.domain.Rupiah
import id.cukup.ui.components.Eyebrow
import id.cukup.ui.components.Gutter
import id.cukup.ui.components.Hairline
import id.cukup.ui.components.InkButton
import id.cukup.ui.components.LineButton
import id.cukup.ui.components.PocketEditor
import id.cukup.ui.components.PocketRing
import id.cukup.ui.components.TextAction
import id.cukup.ui.components.TopBar
import id.cukup.ui.home.PocketRow
import id.cukup.ui.onboarding.italicize
import id.cukup.ui.theme.Type
import id.cukup.ui.theme.colors

@Composable
fun PocketsScreen(
    contentPadding: PaddingValues,
    onOpenPocket: (Long) -> Unit,
    onMove: () -> Unit,
    vm: PocketsViewModel = hiltViewModel(),
) {
    val s by vm.state.collectAsStateWithLifecycle()
    val c = colors
    val summary = s.summary ?: return

    val editing = s.editing
    if (editing != null) {
        BackHandler { vm.cancel() }
        Column(Modifier.fillMaxSize().background(c.paper).padding(contentPadding).imePadding()) {
            TopBar("Atur persentase", onBack = vm::cancel)
            Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
                Text(
                    "Perubahan berlaku untuk pemasukan berikutnya. Pembagian yang sudah tercatat tidak berubah.",
                    style = Type.bodySmall, color = c.mute,
                    modifier = Modifier.padding(horizontal = Gutter),
                )
                PocketEditor(editing, onChange = vm::edit)
                Text(
                    "Menghapus pos hanya menyembunyikannya. Riwayatnya tetap tersimpan; pindahkan dulu saldonya ke pos lain.",
                    style = Type.bodySmall, color = c.faint,
                    modifier = Modifier.padding(horizontal = Gutter, vertical = 8.dp),
                )
            }
            Hairline()
            InkButton(
                "Simpan",
                onClick = vm::save,
                enabled = s.canSave,
                modifier = Modifier.fillMaxWidth().padding(horizontal = Gutter, vertical = 12.dp),
            )
        }
        return
    }

    LazyColumn(Modifier.fillMaxSize().background(c.paper), contentPadding = contentPadding) {
        item {
            Column(Modifier.padding(horizontal = Gutter).padding(top = 24.dp)) {
                Text(italicize("Semua pos, <i>dalam persen.</i>"), style = Type.display, color = c.ink)
                Spacer(Modifier.height(8.dp))
                Text(
                    "Setiap uang masuk dibagi ke pos sesuai persentase ini. Ketuk pos untuk melihat riwayatnya.",
                    style = Type.body, color = c.mute,
                )
            }
        }
        item {
            Box(Modifier.fillMaxWidth().padding(vertical = 28.dp), contentAlignment = Alignment.Center) {
                Box(Modifier.size(200.dp), contentAlignment = Alignment.Center) {
                    PocketRing(
                        summary.pockets.mapIndexed { i, pb -> Triple(c.pocket(i), pb.pocket.percent / 100f, 1f - pb.usedRatio) },
                        Modifier.fillMaxSize(),
                        stroke = 14.dp,
                    )
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Eyebrow("Total")
                        Text(Rupiah.short(summary.total), style = Type.number, color = c.ink)
                    }
                }
            }
        }
        item {
            Row(Modifier.padding(horizontal = Gutter).padding(bottom = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                LineButton("Atur persentase", onClick = vm::startEditing, height = 44.dp)
                Spacer(Modifier.size(8.dp))
                LineButton("Pindah uang", onClick = onMove, height = 44.dp)
            }
        }
        item { Hairline() }
        items(summary.pockets, key = { it.pocket.id }) { pb ->
            PocketRow(pb, summary.pockets.indexOf(pb), onClick = { onOpenPocket(pb.pocket.id) })
            Hairline()
        }
        item {
            val save = summary.pockets.filter { it.pocket.kind == PocketKind.SAVE }.sumOf { it.balance }
            Column(Modifier.padding(horizontal = Gutter, vertical = 24.dp)) {
                Eyebrow("Keterangan")
                Spacer(Modifier.height(8.dp))
                Text("Pakai: dihitung di \"aman dipakai hari ini\".", style = Type.bodySmall, color = c.mute)
                Text("Simpan: tidak ikut dihitung. Sekarang terkumpul ${Rupiah.format(save)}.", style = Type.bodySmall, color = c.mute)
                Text("Cicilan: untuk membayar hutang dan paylater.", style = Type.bodySmall, color = c.mute)
            }
        }
    }
}

@Composable
fun PocketDetailScreen(
    onBack: () -> Unit,
    onOpenTx: (Long) -> Unit,
    onAdd: (Long) -> Unit,
    vm: PocketDetailViewModel = hiltViewModel(),
) {
    val s by vm.state.collectAsStateWithLifecycle()
    val c = colors
    val pb = s.balance
    Column(Modifier.fillMaxSize().background(c.paper)) {
        TopBar(pb?.let { "${it.pocket.emoji} ${it.pocket.name}" } ?: "", onBack = onBack, modifier = Modifier.padding(top = 24.dp))
        if (pb == null) return@Column
        LazyColumn(Modifier.weight(1f)) {
            item {
                Column(Modifier.padding(horizontal = Gutter).padding(top = 8.dp, bottom = 20.dp)) {
                    Eyebrow("Saldo pos")
                    Text(Rupiah.format(pb.balance), style = Type.hero, color = if (pb.balance < 0) c.over else c.ink)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        italicize(
                            "<i>${pb.pocket.percent}%</i> dari setiap pemasukan · " +
                                when (pb.pocket.kind) {
                                    PocketKind.SPEND -> "pos pakai"
                                    PocketKind.SAVE -> "pos simpan"
                                    PocketKind.DEBT -> "pos cicilan"
                                },
                        ),
                        style = Type.statement, color = c.mute,
                    )
                    Spacer(Modifier.height(16.dp))
                    Row {
                        Stat("Masuk siklus ini", pb.inThisCycle, Modifier.weight(1f))
                        Stat("Keluar siklus ini", pb.outThisCycle, Modifier.weight(1f))
                    }
                }
            }
            item { Hairline() }
            if (s.entries.isEmpty()) {
                item {
                    Text(
                        "Belum ada catatan untuk pos ini.",
                        style = Type.body, color = c.faint,
                        modifier = Modifier.padding(Gutter),
                    )
                }
            }
            items(s.entries, key = { it.tx.id }) { e ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clickable { onOpenTx(e.tx.id) }
                        .padding(horizontal = Gutter, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(e.label, style = Type.strong, color = c.ink, maxLines = 1)
                        Text(
                            buildString {
                                append(id.cukup.ui.components.dayLabel(id.cukup.ui.components.localDate(e.tx.occurredAt)))
                                if (e.percent != null) append(" · ${e.percent}% dari ${Rupiah.format(e.tx.amount)}")
                                if (e.tx.isPaylater) append(" · paylater")
                            },
                            style = Type.bodySmall, color = c.mute, maxLines = 1,
                        )
                    }
                    Text(
                        (if (e.amount > 0) "+" else "") + Rupiah.format(e.amount),
                        style = Type.amount,
                        color = if (e.amount >= 0) c.ink else c.mute,
                    )
                }
                Hairline()
            }
        }
        Hairline()
        Row(Modifier.padding(horizontal = Gutter, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            InkButton("Catat dari pos ini", onClick = { onAdd(pb.pocket.id) }, modifier = Modifier.weight(1f))
        }
    }
}

@Composable
private fun Stat(label: String, amount: Long, modifier: Modifier = Modifier) {
    val c = colors
    Column(modifier) {
        Eyebrow(label)
        Text(Rupiah.format(amount), style = Type.amount, color = c.ink)
    }
}
