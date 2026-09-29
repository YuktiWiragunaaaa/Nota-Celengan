package id.cukup.ui.home

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.Lightbulb
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.SwapVert
import androidx.compose.material.icons.rounded.TrendingUp
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import id.cukup.domain.Insight
import id.cukup.domain.PocketBalance
import id.cukup.domain.PocketKind
import id.cukup.domain.Rupiah
import id.cukup.domain.Summary
import id.cukup.domain.Warning
import id.cukup.ui.components.Bubble
import id.cukup.ui.components.BubbleChart
import id.cukup.ui.components.CardShape
import id.cukup.ui.components.Gutter
import id.cukup.ui.components.LightStatusBarIcons
import id.cukup.ui.components.Pill
import id.cukup.ui.components.PocketBadge
import id.cukup.ui.components.SectionHeader
import id.cukup.ui.components.TextAction
import id.cukup.ui.components.TxRow
import id.cukup.ui.components.UsageBar
import id.cukup.ui.theme.Type
import id.cukup.ui.theme.colors

@Composable
fun HomeScreen(
    contentPadding: PaddingValues,
    onIncome: () -> Unit,
    onExpense: () -> Unit,
    onMove: () -> Unit,
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
        Box(Modifier.fillMaxSize().background(c.brandBrush))
        return
    }
    var selected by rememberSaveable { mutableStateOf<Long?>(null) }

    LazyColumn(
        Modifier.fillMaxSize().background(c.paper),
        contentPadding = PaddingValues(bottom = contentPadding.calculateBottomPadding()),
    ) {
        item {
            Hero(
                name = s.name,
                summary = summary,
                periodName = s.schedule.periodName,
                pendingCount = s.pendingCount,
                selected = selected,
                onSelect = { selected = if (selected == it) null else it },
                onOpenPocket = onOpenPocket,
                onOpenInbox = onOpenInbox,
            )
        }
        item {
            Row(
                Modifier.padding(horizontal = Gutter).offset(y = (-26).dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                ActionPill("Masuk", Icons.Rounded.ArrowDownward, onIncome, Modifier.weight(1f))
                Box(
                    Modifier.size(52.dp).clip(CircleShape).background(c.card).clickable(role = Role.Button, onClick = onMove),
                    contentAlignment = Alignment.Center,
                ) { Icon(Icons.Rounded.SwapVert, "Pindah uang", tint = c.ink) }
                ActionPill("Keluar", Icons.Rounded.ArrowUpward, onExpense, Modifier.weight(1f))
            }
        }
        if (s.insights.isNotEmpty()) {
            item { InsightPager(s.insights) }
        }
        item {
            SectionHeader("Transaksi terakhir", trailing = {
                if (s.recent.isNotEmpty()) TextAction("Lihat semua", onOpenHistory)
            })
        }
        if (s.recent.isEmpty()) {
            item {
                Text(
                    "Belum ada catatan. Tekan Keluar setiap kali belanja, termasuk pakai uang tunai.",
                    style = Type.body, color = c.faint,
                    modifier = Modifier.padding(horizontal = Gutter, vertical = 8.dp),
                )
            }
        } else {
            items(s.recent, key = { "t" + it.id }) { tx ->
                TxRow(
                    tx = tx,
                    pocket = tx.pocketId?.let(s.pocketsById::get),
                    toPocket = tx.toPocketId?.let(s.pocketsById::get),
                    pocketColor = s.pocketColor(tx.pocketId, c),
                    onClick = { onOpenTx(tx.id) },
                )
            }
        }
        item { Spacer(Modifier.height(24.dp)) }
    }
}

private fun HomeState.pocketColor(id: Long?, c: id.cukup.ui.theme.CukupColors): Color {
    val p = id?.let(pocketsById::get) ?: return c.faint
    return c.of(p, pocketIndex[id] ?: 0)
}

@Composable
private fun Hero(
    name: String,
    summary: Summary,
    periodName: String,
    pendingCount: Int,
    selected: Long?,
    onSelect: (Long) -> Unit,
    onOpenPocket: (Long) -> Unit,
    onOpenInbox: () -> Unit,
) {
    val c = colors
    val white = Color.White
    val soft = Color.White.copy(alpha = 0.7f)
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(bottomStart = 36.dp, bottomEnd = 36.dp))
            .background(c.brandBrush)
            .windowInsetsPadding(WindowInsets.statusBars)
            .padding(bottom = 46.dp),
    ) {
        // Baris atas: sapaan dan lonceng.
        Row(Modifier.fillMaxWidth().padding(horizontal = Gutter, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(40.dp).clip(CircleShape).background(white.copy(alpha = 0.18f)),
                contentAlignment = Alignment.Center,
            ) { Text(name.firstOrNull()?.uppercase() ?: "☺", style = Type.strong, color = white) }
            Spacer(Modifier.width(10.dp))
            Text(if (name.isBlank()) "Hai!" else "Hai, $name", style = Type.strong, color = white, modifier = Modifier.weight(1f))
            Box {
                Box(
                    Modifier.size(40.dp).clip(CircleShape).background(white.copy(alpha = 0.18f))
                        .clickable(role = Role.Button, onClick = onOpenInbox),
                    contentAlignment = Alignment.Center,
                ) { Icon(Icons.Rounded.Notifications, "Perlu dicek", tint = white, modifier = Modifier.size(20.dp)) }
                if (pendingCount > 0) {
                    Box(
                        Modifier.align(Alignment.TopEnd).size(18.dp).clip(CircleShape).background(c.over),
                        contentAlignment = Alignment.Center,
                    ) { Text("$pendingCount", style = Type.label.copy(fontSize = Type.label.fontSize * 0.85f), color = white) }
                }
            }
        }

        // Angka utama.
        Spacer(Modifier.height(8.dp))
        Text("Sisa jatah $periodName", style = Type.bodySmall, color = soft, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
        Spacer(Modifier.height(4.dp))
        val left = summary.budgetLeft
        Text(
            buildAnnotatedString {
                if (left < 0) withStyle(SpanStyle(color = soft)) { append("−") }
                val text = Rupiah.format(kotlin.math.abs(left))
                withStyle(SpanStyle(color = soft, fontSize = Type.hero.fontSize * 0.55f)) { append("Rp") }
                append(text.removePrefix("Rp"))
            },
            style = Type.hero, color = white,
            modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center, maxLines = 1,
        )
        Spacer(Modifier.height(10.dp))
        StatusChip(summary, Modifier.align(Alignment.CenterHorizontally))

        // Gelembung kantong.
        val pockets = summary.pockets
        val bubbles = pockets.mapIndexed { i, pb -> pb.toBubble(c.of(pb.pocket, i)) }
        BubbleChart(
            bubbles = bubbles,
            selected = selected,
            onSelect = onSelect,
            modifier = Modifier.fillMaxWidth().height(250.dp).padding(horizontal = 28.dp, vertical = 6.dp),
        )
        val pick = pockets.firstOrNull { it.pocket.id == selected }
        AnimatedContent(pick, transitionSpec = { fadeIn() togetherWith fadeOut() }, label = "pick") { pb ->
            if (pb == null) {
                Text(
                    "Ketuk gelembung untuk lihat isinya",
                    style = Type.bodySmall, color = soft,
                    modifier = Modifier.fillMaxWidth().height(48.dp).padding(top = 16.dp), textAlign = TextAlign.Center,
                )
            } else {
                Row(
                    Modifier
                        .padding(horizontal = Gutter)
                        .fillMaxWidth()
                        .height(48.dp)
                        .clip(Pill)
                        .background(white.copy(alpha = 0.16f))
                        .clickable { onOpenPocket(pb.pocket.id) }
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("${pb.pocket.emoji}  ${pb.pocket.name}", style = Type.strong, color = white, modifier = Modifier.weight(1f), maxLines = 1)
                    Text(pocketLine(pb), style = Type.bodySmall, color = soft)
                    Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, null, tint = white)
                }
            }
        }
    }
}

private fun PocketBalance.toBubble(color: Color): Bubble {
    val available = balance + spentThisCycle
    val fill = when {
        pocket.kind != PocketKind.SPEND -> -1f
        available <= 0 -> 0f
        else -> balance.coerceAtLeast(0).toFloat() / available
    }
    return Bubble(
        key = pocket.id,
        emoji = pocket.emoji,
        label = pocket.name,
        value = Rupiah.short(balance),
        weight = balance.coerceAtLeast(0).toFloat().coerceAtLeast(1f),
        fill = fill,
        color = color,
    )
}

private fun pocketLine(pb: PocketBalance): String = when (pb.pocket.kind) {
    PocketKind.SPEND -> "sisa ${Rupiah.short(pb.balance)}"
    PocketKind.SAVE -> "tabungan ${Rupiah.short(pb.balance)}"
    PocketKind.DEBT -> "${Rupiah.short(pb.balance)} buat cicilan"
} + "  "

@Composable
private fun StatusChip(summary: Summary, modifier: Modifier = Modifier) {
    val white = Color.White
    val (icon, text) = when {
        summary.budget <= 0 && summary.budgetUsed == 0L -> Icons.Rounded.Lightbulb to "Catat uang masuk dulu"
        summary.warning == Warning.OVER -> Icons.Rounded.WarningAmber to "Jatah sudah lewat"
        else -> Icons.Rounded.TrendingUp to "${(summary.budgetRatio * 100).toInt()}% kepakai · gajian ${summary.daysLeft} hari lagi"
    }
    Row(
        modifier.clip(Pill).background(white.copy(alpha = 0.16f)).padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, null, tint = white, modifier = Modifier.size(14.dp))
        Spacer(Modifier.width(6.dp))
        Text(text, style = Type.label, color = white)
    }
}

@Composable
private fun ActionPill(text: String, icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit, modifier: Modifier) {
    val c = colors
    Row(
        modifier.height(52.dp).clip(Pill).background(c.card).clickable(role = Role.Button, onClick = onClick),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, null, tint = c.ink, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(8.dp))
        Text(text, style = Type.strong, color = c.ink)
    }
}

/** Catatan penasehat yang bisa digeser satu per satu. */
@Composable
private fun InsightPager(insights: List<Insight>) {
    val c = colors
    val pager = rememberPagerState { insights.size }
    Column(Modifier.offset(y = (-10).dp)) {
        HorizontalPager(
            state = pager,
            contentPadding = PaddingValues(horizontal = Gutter),
            pageSpacing = 10.dp,
        ) { page ->
            val insight = insights[page]
            val tone = when (insight.tone) {
                Insight.Tone.GOOD -> c.good
                Insight.Tone.INFO -> c.accent
                Insight.Tone.WARN -> c.caution
            }
            Row(
                Modifier.fillMaxWidth().height(96.dp).clip(CardShape).background(c.card).padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    Modifier.size(40.dp).clip(CircleShape).background(tone.copy(alpha = 0.14f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        when (insight.tone) {
                            Insight.Tone.GOOD -> Icons.Rounded.TrendingUp
                            Insight.Tone.INFO -> Icons.Rounded.Lightbulb
                            Insight.Tone.WARN -> Icons.Rounded.WarningAmber
                        },
                        null, tint = tone, modifier = Modifier.size(20.dp),
                    )
                }
                Spacer(Modifier.width(14.dp))
                Text(insight.text, style = Type.bodySmall, color = c.ink, maxLines = 4)
            }
        }
        if (insights.size > 1) {
            Row(Modifier.fillMaxWidth().padding(top = 10.dp), horizontalArrangement = Arrangement.Center) {
                repeat(insights.size) { i ->
                    val w by animateFloatAsState(if (pager.currentPage == i) 18f else 6f, tween(250), label = "dot")
                    Box(
                        Modifier.padding(horizontal = 3.dp).height(6.dp).width(w.dp).clip(Pill)
                            .background(if (pager.currentPage == i) c.ink else c.line),
                    )
                }
            }
        }
    }
}

/** Baris kantong untuk layar lain (daftar). */
@Composable
internal fun PocketRow(pb: PocketBalance, index: Int, onClick: () -> Unit) {
    val c = colors
    val color = c.of(pb.pocket, index)
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = Gutter, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        PocketBadge(pb.pocket.emoji, color)
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(pb.pocket.name, style = Type.strong, color = c.ink, modifier = Modifier.weight(1f), maxLines = 1)
                Text(Rupiah.format(pb.balance), style = Type.amount, color = if (pb.balance < 0) c.over else c.ink)
            }
            Spacer(Modifier.height(6.dp))
            if (pb.pocket.kind == PocketKind.SPEND) {
                UsageBar(pb.usedRatio, color)
            } else {
                Text("${pb.pocket.percent}%", style = Type.bodySmall, color = c.faint)
            }
        }
    }
}
