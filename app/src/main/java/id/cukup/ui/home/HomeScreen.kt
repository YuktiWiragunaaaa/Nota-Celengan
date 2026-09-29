package id.cukup.ui.home

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import id.cukup.domain.PocketBalance
import id.cukup.domain.PocketKind
import id.cukup.domain.Rupiah
import id.cukup.ui.components.Eyebrow
import id.cukup.ui.components.Gutter
import id.cukup.ui.components.Hairline
import id.cukup.ui.components.Id
import id.cukup.ui.components.PocketDot
import id.cukup.ui.components.PocketRing
import id.cukup.ui.components.SectionHeader
import id.cukup.ui.components.TextAction
import id.cukup.ui.components.TxRow
import id.cukup.ui.components.UsageBar
import id.cukup.ui.onboarding.italicize
import id.cukup.ui.theme.Type
import id.cukup.ui.theme.colors
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Composable
fun HomeScreen(
    contentPadding: PaddingValues,
    onOpenPocket: (Long) -> Unit,
    onOpenPockets: () -> Unit,
    onOpenInbox: () -> Unit,
    onOpenTx: (Long) -> Unit,
    onOpenHistory: () -> Unit,
    vm: HomeViewModel = hiltViewModel(),
) {
    val s by vm.state.collectAsStateWithLifecycle()
    val c = colors
    val summary = s.summary ?: return

    LazyColumn(Modifier.fillMaxSize().background(c.paper), contentPadding = contentPadding) {
        item {
            Column(Modifier.padding(horizontal = Gutter).padding(top = 20.dp)) {
                Eyebrow(greeting(s.name))
                Spacer(Modifier.height(28.dp))
                Eyebrow("Aman dipakai hari ini")
                Spacer(Modifier.height(6.dp))
                Text(Rupiah.format(summary.safeToSpendToday), style = Type.hero, color = c.ink, maxLines = 1)
                Spacer(Modifier.height(8.dp))
                Text(
                    italicize(statement(summary.daysLeft, s.nextPayday)),
                    style = Type.statement,
                    color = c.mute,
                )
            }
        }

        if (s.pendingCount > 0) {
            item {
                Row(
                    Modifier
                        .padding(horizontal = Gutter)
                        .padding(top = 24.dp)
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(2.dp))
                        .background(c.dark)
                        .clickable(role = Role.Button, onClick = onOpenInbox)
                        .padding(horizontal = 18.dp, vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Eyebrow("Perlu dicek", color = c.onDarkMute)
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "${s.pendingCount} transaksi terbaca dari notifikasi",
                            style = Type.body, color = c.onDark,
                        )
                    }
                    Text("→", style = Type.title, color = c.onDark)
                }
            }
        }

        item {
            Row(
                Modifier.padding(horizontal = Gutter).padding(top = 32.dp).fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(Modifier.size(112.dp).clickable(onClick = onOpenPockets), contentAlignment = Alignment.Center) {
                    PocketRing(
                        summary.pockets.mapIndexed { i, pb -> Triple(c.pocket(i), pb.pocket.percent / 100f, 1f - pb.usedRatio) },
                        Modifier.fillMaxSize(),
                    )
                }
                Spacer(Modifier.width(20.dp))
                Column(Modifier.weight(1f)) {
                    Eyebrow("Total uang")
                    Text(Rupiah.format(summary.total), style = Type.number, color = c.ink, maxLines = 1)
                    Spacer(Modifier.height(10.dp))
                    MiniStat("Masuk siklus ini", summary.incomeThisCycle)
                    MiniStat("Keluar siklus ini", summary.spentThisCycle)
                    if (summary.paylaterDebt > 0) MiniStat("Hutang paylater", summary.paylaterDebt, warn = true)
                }
            }
        }

        item { SectionHeader("Pos", trailing = { TextAction("Atur", onOpenPockets, color = c.mute) }) }
        item { Hairline() }
        items(summary.pockets, key = { "p" + it.pocket.id }) { pb ->
            PocketRow(pb, s.pocketIndex[pb.pocket.id] ?: 0, onClick = { onOpenPocket(pb.pocket.id) })
            Hairline()
        }

        item {
            SectionHeader("Terakhir", trailing = { if (s.recent.isNotEmpty()) TextAction("Semua", onOpenHistory, color = c.mute) })
        }
        if (s.recent.isEmpty()) {
            item {
                Text(
                    "Belum ada transaksi. Ketuk + untuk mencatat, atau aktifkan baca notifikasi di Setelan.",
                    style = Type.body, color = c.faint,
                    modifier = Modifier.padding(horizontal = Gutter, vertical = 8.dp),
                )
            }
        } else {
            item { Hairline() }
            items(s.recent, key = { "t" + it.id }) { tx ->
                TxRow(
                    tx = tx,
                    pocket = tx.pocketId?.let(s.pocketsById::get),
                    toPocket = tx.toPocketId?.let(s.pocketsById::get),
                    pocketColor = c.pocket(s.pocketIndex[tx.pocketId] ?: 0),
                    onClick = { onOpenTx(tx.id) },
                )
            }
        }
        item { Spacer(Modifier.height(24.dp)) }
    }
}

@Composable
private fun MiniStat(label: String, amount: Long, warn: Boolean = false) {
    val c = colors
    Row(Modifier.fillMaxWidth().padding(vertical = 1.dp)) {
        Text(label, style = Type.bodySmall, color = c.mute, modifier = Modifier.weight(1f))
        Text(Rupiah.short(amount), style = Type.bodySmall, color = if (warn) c.over else c.ink)
    }
}

@Composable
internal fun PocketRow(pb: PocketBalance, index: Int, onClick: () -> Unit) {
    val c = colors
    Column(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = Gutter, vertical = 14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            PocketDot(c.pocket(index))
            Spacer(Modifier.width(12.dp))
            Text(
                "${pb.pocket.emoji}  ${pb.pocket.name}",
                style = Type.strong, color = c.ink, modifier = Modifier.weight(1f),
                maxLines = 1, overflow = TextOverflow.Ellipsis,
            )
            Text(
                "${pb.pocket.percent}%",
                style = Type.label, color = c.faint,
                modifier = Modifier.padding(end = 12.dp).border(1.dp, c.line, RoundedCornerShape(2.dp)).padding(horizontal = 6.dp, vertical = 3.dp),
            )
            Text(Rupiah.format(pb.balance), style = Type.amount, color = if (pb.balance < 0) c.over else c.ink)
        }
        Spacer(Modifier.height(10.dp))
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            UsageBar(pb.usedRatio, c.pocket(index), Modifier.weight(1f).padding(start = 20.dp))
            Text(
                when (pb.pocket.kind) {
                    PocketKind.SAVE -> "disimpan"
                    PocketKind.DEBT -> "cicilan · terpakai ${(pb.usedRatio * 100).toInt()}%"
                    PocketKind.SPEND -> "terpakai ${(pb.usedRatio * 100).toInt()}%"
                },
                style = Type.bodySmall, color = c.faint,
            )
        }
    }
}

private fun greeting(name: String): String {
    val h = java.time.LocalTime.now().hour
    val time = when (h) {
        in 4..10 -> "Selamat pagi"
        in 11..14 -> "Selamat siang"
        in 15..17 -> "Selamat sore"
        else -> "Selamat malam"
    }
    return if (name.isBlank()) time else "$time, $name"
}

private val paydayFmt = DateTimeFormatter.ofPattern("d MMMM", Id)

private fun statement(daysLeft: Int, next: LocalDate?): String {
    val date = next?.format(paydayFmt) ?: ""
    return when (daysLeft) {
        1 -> "Besok <i>gajian</i> ($date)."
        else -> "<i>$daysLeft hari</i> lagi sampai gajian, $date."
    }
}
