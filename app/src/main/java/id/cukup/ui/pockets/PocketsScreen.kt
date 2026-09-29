package id.cukup.ui.pockets

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import id.cukup.ui.components.SplitBar
import id.cukup.ui.components.SplitEditor
import id.cukup.ui.components.TopBar
import id.cukup.ui.components.dayLabel
import id.cukup.ui.components.label
import id.cukup.ui.components.localDate
import id.cukup.ui.home.PocketRow
import id.cukup.ui.onboarding.italicize
import id.cukup.ui.theme.Type
import id.cukup.ui.theme.colors

/** Tab Kantong: semua kantong, pembagian persennya, dan pintasan untuk mengubah. */
@Composable
fun PocketsScreen(
    contentPadding: PaddingValues,
    onOpenPocket: (Long) -> Unit,
    onEditSplit: () -> Unit,
    onMove: () -> Unit,
    vm: PocketsViewModel = hiltViewModel(),
) {
    val s by vm.state.collectAsStateWithLifecycle()
    val c = colors
    val summary = s.summary ?: return

    LazyColumn(Modifier.fillMaxSize().background(c.paper), contentPadding = contentPadding) {
        item {
            Column(Modifier.padding(horizontal = Gutter).padding(top = 24.dp)) {
                Text(italicize("Kantong"), style = Type.display, color = c.ink)
                Spacer(Modifier.height(8.dp))
                Text(
                    "Setiap uang masuk dibagi ke kantong sesuai persen di bawah.",
                    style = Type.body, color = c.mute,
                )
                Spacer(Modifier.height(20.dp))
                SplitBar(summary.pockets.map { it.pocket.percent })
                Spacer(Modifier.height(8.dp))
                Row {
                    Text("Total uang", style = Type.bodySmall, color = c.mute, modifier = Modifier.weight(1f))
                    Text(Rupiah.format(summary.total), style = Type.amount, color = c.ink)
                }
                Spacer(Modifier.height(16.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    InkButton("Ubah pembagian", onClick = onEditSplit, modifier = Modifier.weight(1f))
                    LineButton("Pindah uang", onClick = onMove, modifier = Modifier.weight(1f))
                }
                Spacer(Modifier.height(20.dp))
            }
        }
        item { Hairline() }
        items(summary.pockets, key = { it.pocket.id }) { pb ->
            PocketRow(pb, summary.pockets.indexOf(pb), onClick = { onOpenPocket(pb.pocket.id) })
            Hairline()
        }
        item {
            Column(Modifier.padding(horizontal = Gutter, vertical = 24.dp)) {
                Eyebrow("Arti label")
                Spacer(Modifier.height(8.dp))
                Text("Untuk belanja: dihitung sebagai jatah belanja.", style = Type.bodySmall, color = c.mute)
                Text("Ditabung: disisihkan, tidak ikut jatah belanja.", style = Type.bodySmall, color = c.mute)
                Text("Bayar hutang: untuk cicilan dan paylater.", style = Type.bodySmall, color = c.mute)
            }
        }
    }
}

/** Layar khusus mengubah pembagian persen. */
@Composable
fun SplitScreen(onBack: () -> Unit, vm: PocketsViewModel = hiltViewModel()) {
    val s by vm.state.collectAsStateWithLifecycle()
    val c = colors
    LaunchedEffect(Unit) { vm.startEditing() }
    val editing = s.editing

    Column(Modifier.fillMaxSize().background(c.paper).systemBarsPadding().imePadding()) {
        TopBar("Ubah pembagian", onBack = onBack)
        if (editing == null) return@Column
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
            Text(
                "Geser persen tiap kantong. Perubahannya berlaku untuk uang yang masuk setelah ini.",
                style = Type.body, color = c.mute,
                modifier = Modifier.padding(horizontal = Gutter),
            )
            SplitEditor(editing, onChange = vm::edit)
            Text(
                "Kantong yang dihapus cuma disembunyikan, riwayatnya masih ada. Sebaiknya pindahkan dulu sisa uangnya.",
                style = Type.bodySmall, color = c.faint,
                modifier = Modifier.padding(horizontal = Gutter, vertical = 8.dp),
            )
        }
        Hairline()
        InkButton(
            "Simpan",
            onClick = { vm.save(onBack) },
            enabled = s.canSave,
            modifier = Modifier.fillMaxWidth().padding(horizontal = Gutter, vertical = 12.dp),
        )
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
    Column(Modifier.fillMaxSize().background(c.paper).systemBarsPadding()) {
        TopBar(pb?.let { "${it.pocket.emoji} ${it.pocket.name}" } ?: "", onBack = onBack)
        if (pb == null) return@Column
        LazyColumn(Modifier.weight(1f)) {
            item {
                Column(Modifier.padding(horizontal = Gutter).padding(top = 8.dp, bottom = 20.dp)) {
                    Eyebrow("Isi kantong")
                    Text(Rupiah.format(pb.balance), style = Type.hero, color = if (pb.balance < 0) c.over else c.ink)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Dapat ${pb.pocket.percent}% dari setiap uang masuk · ${pb.pocket.kind.label().lowercase()}",
                        style = Type.body, color = c.mute,
                    )
                    Spacer(Modifier.height(16.dp))
                    Row {
                        Stat("Masuk periode ini", pb.inThisCycle, Modifier.weight(1f))
                        Stat("Keluar periode ini", pb.outThisCycle, Modifier.weight(1f))
                    }
                }
            }
            item { Hairline() }
            if (s.entries.isEmpty()) {
                item {
                    Text("Belum ada catatan di kantong ini.", style = Type.body, color = c.faint, modifier = Modifier.padding(Gutter))
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
                                append(dayLabel(localDate(e.tx.occurredAt)))
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
        if (pb.pocket.kind != PocketKind.SAVE) {
            InkButton(
                "Catat uang keluar dari sini",
                onClick = { onAdd(pb.pocket.id) },
                modifier = Modifier.fillMaxWidth().padding(horizontal = Gutter, vertical = 12.dp),
            )
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
