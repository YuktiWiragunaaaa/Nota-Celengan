package id.cukup.ui.home

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
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
import id.cukup.domain.Insight
import id.cukup.domain.PocketBalance
import id.cukup.domain.PocketKind
import id.cukup.domain.Rupiah
import id.cukup.domain.Summary
import id.cukup.domain.Warning
import id.cukup.ui.components.Eyebrow
import id.cukup.ui.components.Gutter
import id.cukup.ui.components.Hairline
import id.cukup.ui.components.Id
import id.cukup.ui.components.InkButton
import id.cukup.ui.components.LineButton
import id.cukup.ui.components.PocketDot
import id.cukup.ui.components.SectionHeader
import id.cukup.ui.components.TextAction
import id.cukup.ui.components.TxRow
import id.cukup.ui.components.UsageBar
import id.cukup.ui.components.label
import id.cukup.ui.theme.Type
import id.cukup.ui.theme.colors
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Composable
fun HomeScreen(
    contentPadding: PaddingValues,
    onIncome: () -> Unit,
    onExpense: () -> Unit,
    onOpenPocket: (Long) -> Unit,
    onEditSplit: () -> Unit,
    onOpenInbox: () -> Unit,
    onOpenTx: (Long) -> Unit,
    onOpenHistory: () -> Unit,
    vm: HomeViewModel = hiltViewModel(),
) {
    val s by vm.state.collectAsStateWithLifecycle()
    val c = colors
    val summary = s.summary
    if (summary == null) {
        Box(Modifier.fillMaxSize().background(c.paper))
        return
    }

    LazyColumn(Modifier.fillMaxSize().background(c.paper), contentPadding = contentPadding) {
        item {
            Text(
                greeting(s.name),
                style = Type.bodySmall, color = c.mute,
                modifier = Modifier.padding(horizontal = Gutter).padding(top = 20.dp, bottom = 12.dp),
            )
        }
        item { BudgetCard(summary, s.schedule.periodName, s.nextPayday) }

        item {
            Row(
                Modifier.padding(horizontal = Gutter).padding(top = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                LineButton("+ Uang masuk", onClick = onIncome, modifier = Modifier.weight(1f))
                InkButton("− Uang keluar", onClick = onExpense, modifier = Modifier.weight(1f))
            }
        }

        if (s.insights.isNotEmpty()) {
            item { SectionHeader("Catatan ${s.schedule.periodName}") }
            items(s.insights, key = { "i" + it.kind }) { insight -> InsightRow(insight) }
        }

        if (s.pendingCount > 0) {
            item {
                Row(
                    Modifier
                        .padding(horizontal = Gutter)
                        .padding(top = 16.dp)
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(2.dp))
                        .background(c.dark)
                        .clickable(role = Role.Button, onClick = onOpenInbox)
                        .padding(horizontal = 18.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        "Ada ${s.pendingCount} transaksi baru dari notifikasi. Cek sebentar?",
                        style = Type.body, color = c.onDark, modifier = Modifier.weight(1f),
                    )
                    Text("→", style = Type.title, color = c.onDark)
                }
            }
        }

        item {
            SectionHeader("Kantong", trailing = { TextAction("Ubah pembagian", onEditSplit, color = c.mute) })
        }
        item { Hairline() }
        items(summary.pockets, key = { "p" + it.pocket.id }) { pb ->
            PocketRow(pb, s.pocketIndex[pb.pocket.id] ?: 0, onClick = { onOpenPocket(pb.pocket.id) })
            Hairline()
        }
        if (summary.paylaterDebt > 0) {
            item {
                Row(Modifier.fillMaxWidth().padding(horizontal = Gutter, vertical = 14.dp)) {
                    Text("Hutang paylater", style = Type.body, color = c.mute, modifier = Modifier.weight(1f))
                    Text(Rupiah.format(summary.paylaterDebt), style = Type.amount, color = c.over)
                }
                Hairline()
            }
        }

        item {
            SectionHeader("Terakhir dicatat", trailing = { if (s.recent.isNotEmpty()) TextAction("Lihat semua", onOpenHistory, color = c.mute) })
        }
        if (s.recent.isEmpty()) {
            item {
                Text(
                    "Belum ada catatan. Tiap belanja, termasuk pakai uang tunai, tinggal tekan Uang keluar.",
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

private val dayFmt = DateTimeFormatter.ofPattern("EEEE", Id)

/** Kartu utama: jatah belanja periode ini, sisa, dan status dalam satu kalimat. */
@Composable
private fun BudgetCard(summary: Summary, periodName: String, nextPayday: LocalDate?) {
    val c = colors
    val warning = summary.warning
    val tone = when (warning) {
        Warning.CALM -> c.ink
        Warning.NEAR -> c.caution
        Warning.OVER -> c.over
    }
    val progress by animateFloatAsState(summary.budgetRatio.coerceIn(0f, 1f), tween(700), label = "budget")
    Column(
        Modifier
            .padding(horizontal = Gutter)
            .fillMaxWidth()
            .border(1.dp, c.line, RoundedCornerShape(2.dp))
            .padding(18.dp),
    ) {
        Eyebrow("Jatah belanja $periodName")
        Spacer(Modifier.height(6.dp))
        if (summary.budget <= 0 && summary.budgetUsed == 0L) {
            Text("Belum ada uang masuk", style = Type.number, color = c.faint)
            Spacer(Modifier.height(6.dp))
            Text("Catat gaji atau uang sakumu dulu lewat tombol Uang masuk.", style = Type.bodySmall, color = c.mute)
            return@Column
        }
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                Rupiah.format(summary.budgetLeft.coerceAtLeast(0)),
                style = Type.hero, color = tone, maxLines = 1,
                modifier = Modifier.weight(1f, fill = false),
            )
            Text(" sisa", style = Type.body, color = c.mute, modifier = Modifier.padding(bottom = 10.dp))
        }
        Spacer(Modifier.height(12.dp))
        Box(Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)).background(c.line)) {
            Box(Modifier.fillMaxWidth(progress).height(8.dp).clip(RoundedCornerShape(4.dp)).background(tone))
        }
        Spacer(Modifier.height(8.dp))
        Row {
            Text(
                "Terpakai ${Rupiah.format(summary.budgetUsed)}",
                style = Type.bodySmall, color = c.mute, modifier = Modifier.weight(1f),
            )
            Text("dari ${Rupiah.format(summary.budget)}", style = Type.bodySmall, color = c.mute)
        }
        Spacer(Modifier.height(12.dp))
        val payday = nextPayday?.format(dayFmt)?.replaceFirstChar { it.titlecase(Id) } ?: ""
        val perDay = summary.budgetLeft.coerceAtLeast(0) / summary.daysLeft.coerceAtLeast(1)
        Text(
            when (warning) {
                Warning.CALM -> "Aman. Kira-kira ${Rupiah.format(perDay)} sehari sampai gajian hari $payday."
                Warning.NEAR -> "Sudah kepakai ${(summary.budgetRatio * 100).toInt()}%. Sisanya kira-kira ${Rupiah.format(perDay)} sehari sampai $payday."
                Warning.OVER -> "Kelebihan ${Rupiah.format(-summary.budgetLeft)}. Coba tahan dulu sampai $payday."
            },
            style = Type.body, color = if (warning == Warning.CALM) c.ink else tone,
        )
    }
}

@Composable
internal fun PocketRow(pb: PocketBalance, index: Int, onClick: () -> Unit) {
    val c = colors
    Column(Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = Gutter, vertical = 14.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            PocketDot(c.pocket(index))
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    "${pb.pocket.emoji}  ${pb.pocket.name}",
                    style = Type.strong, color = c.ink, maxLines = 1, overflow = TextOverflow.Ellipsis,
                )
                Text("${pb.pocket.percent}% · ${pb.pocket.kind.label().lowercase(Id)}", style = Type.bodySmall, color = c.faint)
            }
            Text(Rupiah.format(pb.balance), style = Type.amount, color = if (pb.balance < 0) c.over else c.ink)
        }
        if (pb.pocket.kind == PocketKind.SPEND) {
            Spacer(Modifier.height(10.dp))
            UsageBar(pb.usedRatio, c.pocket(index), Modifier.padding(start = 20.dp))
        }
    }
}

private fun greeting(name: String): String {
    val h = java.time.LocalTime.now().hour
    val time = when (h) {
        in 4..10 -> "Pagi"
        in 11..14 -> "Siang"
        in 15..17 -> "Sore"
        else -> "Malam"
    }
    return if (name.isBlank()) "$time." else "$time, $name."
}

@Composable
private fun InsightRow(insight: Insight) {
    val c = colors
    Row(Modifier.fillMaxWidth().padding(horizontal = Gutter, vertical = 7.dp), verticalAlignment = Alignment.Top) {
        Box(
            Modifier.padding(top = 8.dp).width(6.dp).height(6.dp).clip(RoundedCornerShape(3.dp)).background(
                when (insight.tone) {
                    Insight.Tone.GOOD -> c.pocket(2)
                    Insight.Tone.INFO -> c.faint
                    Insight.Tone.WARN -> c.caution
                },
            ),
        )
        Spacer(Modifier.width(12.dp))
        Text(insight.text, style = Type.body, color = c.ink)
    }
}
