package id.cukup.ui.home

import id.cukup.ui.components.AccountIcon
import id.cukup.ui.components.GlassIcon
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.BubbleChart
import androidx.compose.material.icons.rounded.DonutLarge
import androidx.compose.material.icons.rounded.Lightbulb
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.SwapHoriz
import androidx.compose.material.icons.rounded.TrendingUp
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import id.cukup.data.Overview
import id.cukup.domain.AccountBalance
import id.cukup.domain.AccountKind
import id.cukup.domain.Advisor
import id.cukup.domain.CategoryAmount
import id.cukup.domain.ChartReader
import id.cukup.domain.Insight
import id.cukup.domain.Ledger
import id.cukup.domain.Rupiah
import id.cukup.domain.TxType
import id.cukup.domain.Warning
import id.cukup.ui.AppViewModel
import id.cukup.ui.components.Avatar
import id.cukup.ui.components.Bubble
import id.cukup.ui.components.BubbleChart
import id.cukup.ui.components.CardShape
import id.cukup.ui.components.ChartSwitch
import id.cukup.ui.components.DonutChart
import id.cukup.ui.components.Gutter
import id.cukup.ui.components.HBarChart
import id.cukup.ui.components.Pill
import id.cukup.ui.components.SectionHeader
import id.cukup.ui.components.Slice
import id.cukup.ui.components.TextAction
import id.cukup.ui.components.TxRow
import id.cukup.ui.components.UsageBar
import id.cukup.ui.components.colorOf
import id.cukup.ui.theme.Type
import id.cukup.ui.theme.colors
import java.time.LocalDate
import java.time.format.DateTimeFormatter

private val rangeFmt = DateTimeFormatter.ofPattern("d MMM", id.cukup.ui.components.Id)

/**
 * CATATAN. Menjawab tiga pertanyaan, berurutan:
 * 1. Uangku sekarang berapa? (jumlah saldo semua dompet)
 * 2. Periode ini masuk berapa, keluar berapa?
 * 3. Keluarnya ke mana? (grafik per kategori)
 */
@Composable
fun HomeScreen(
    contentPadding: PaddingValues,
    onAdd: (TxType) -> Unit,
    onQuickAdd: (Long) -> Unit,
    onOpenAccount: (Long) -> Unit,
    onNewAccount: () -> Unit,
    onOpenInbox: () -> Unit,
    onOpenTx: (Long) -> Unit,
    onOpenHistory: () -> Unit,
    onOpenProfile: () -> Unit,
    onOpenPlan: () -> Unit,
    onOpenHelp: () -> Unit,
    vm: AppViewModel = hiltViewModel(),
) {
    val o by vm.overview.collectAsStateWithLifecycle()
    val c = colors
    val data = o
    if (data == null) {
        Box(Modifier.fillMaxSize().background(c.brandBrush))
        return
    }
    val insights = remember(data) {
        Advisor.insights(data.planStatus, data.transactions, data.debt, data.cycle, data.previous, LocalDate.now())
    }
    val recent = remember(data) { data.confirmed.take(6) }

    LazyColumn(
        Modifier.fillMaxSize().background(c.paper),
        contentPadding = PaddingValues(bottom = contentPadding.calculateBottomPadding()),
    ) {
        item { Hero(data, vm::chart, onOpenInbox, onOpenProfile, onOpenHelp) }
        item {
            Row(
                Modifier.padding(horizontal = Gutter).offset(y = (-26).dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                ActionPill("Masuk", Icons.Rounded.ArrowDownward, { onAdd(TxType.INCOME) }, Modifier.weight(1f))
                Box(
                    Modifier.size(52.dp).clip(CircleShape).background(c.card).clickable(role = Role.Button) { onAdd(TxType.TRANSFER) },
                    contentAlignment = Alignment.Center,
                ) { Icon(Icons.Rounded.SwapHoriz, "Pindah antar dompet", tint = c.ink) }
                ActionPill("Keluar", Icons.Rounded.ArrowUpward, { onAdd(TxType.EXPENSE) }, Modifier.weight(1f))
            }
        }
        item { QuickAdd(data, onQuickAdd) }
        item { Accounts(data.accounts, onOpenAccount, onNewAccount) }
        item { PlanCard(data, onOpenPlan) }
        if (insights.isNotEmpty()) item { InsightPager(insights) }
        item {
            SectionHeader("Catatan terakhir", trailing = {
                if (recent.isNotEmpty()) TextAction("Lihat semua", onOpenHistory)
            })
        }
        if (recent.isEmpty()) {
            item {
                Text(
                    "Belum ada catatan. Tekan Keluar setiap kali belanja (termasuk tunai), dan Masuk setiap dapat uang.",
                    style = Type.body, color = c.faint,
                    modifier = Modifier.padding(horizontal = Gutter, vertical = 8.dp),
                )
            }
        } else {
            items(recent, key = { "t" + it.id }) { tx ->
                val cat = tx.categoryId?.let(data.categoryById::get)
                val accounts = data.accountById
                TxRow(
                    tx = tx,
                    category = cat,
                    account = tx.accountId?.let(accounts::get),
                    toAccount = tx.toAccountId?.let(accounts::get),
                    color = colorOf(cat),
                    onClick = { onOpenTx(tx.id) },
                )
            }
        }
        item { Spacer(Modifier.height(24.dp)) }
    }
}

@Composable
private fun Hero(
    o: Overview,
    onChart: (String) -> Unit,
    onOpenInbox: () -> Unit,
    onOpenProfile: () -> Unit,
    onOpenHelp: () -> Unit,
) {
    val c = colors
    val white = Color.White
    val soft = Color.White.copy(alpha = 0.72f)
    var selected by rememberSaveable { mutableStateOf<Long?>(null) }
    val parts = remember(o) { Ledger.byCategory(o.transactions, o.categoryById.values.toList(), TxType.EXPENSE, o.cycleStart, o.cycleEnd) }
    val reading = remember(o) { ChartReader.readSpending(parts, o.totals, o.periodName) }

    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(bottomStart = 36.dp, bottomEnd = 36.dp))
            .background(c.brandBrush)
            .windowInsetsPadding(WindowInsets.statusBars)
            .padding(bottom = 46.dp),
    ) {
        Row(Modifier.fillMaxWidth().padding(horizontal = Gutter, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Row(Modifier.weight(1f).clip(Pill).clickable(onClick = onOpenProfile), verticalAlignment = Alignment.CenterVertically) {
                Avatar(o.settings.name, o.settings.avatarVersion)
                Spacer(Modifier.width(10.dp))
                Text(
                    if (o.settings.name.isBlank()) "Hai!" else "Hai, ${o.settings.name}",
                    style = Type.strong, color = white, modifier = Modifier.padding(end = 12.dp),
                )
            }
            Box(
                Modifier.size(40.dp).clip(CircleShape).background(white.copy(alpha = 0.18f)).clickable(onClick = onOpenHelp),
                contentAlignment = Alignment.Center,
            ) { Text("?", style = Type.strong, color = white) }
            Spacer(Modifier.width(8.dp))
            Box {
                Box(
                    Modifier.size(40.dp).clip(CircleShape).background(white.copy(alpha = 0.18f)).clickable(role = Role.Button, onClick = onOpenInbox),
                    contentAlignment = Alignment.Center,
                ) { Icon(Icons.Rounded.Notifications, "Perlu dicek", tint = white, modifier = Modifier.size(20.dp)) }
                val pending = o.pending.size
                if (pending > 0) {
                    Box(
                        Modifier.align(Alignment.TopEnd).size(18.dp).clip(CircleShape).background(c.over),
                        contentAlignment = Alignment.Center,
                    ) { Text("$pending", style = Type.label.copy(fontSize = 10.sp), color = white) }
                }
            }
        }

        // 1. Uangku sekarang.
        Spacer(Modifier.height(6.dp))
        Text("Uangmu sekarang", style = Type.bodySmall, color = soft, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
        Spacer(Modifier.height(2.dp))
        val total = o.netWorth
        val digits = Rupiah.format(kotlin.math.abs(total)).removePrefix("Rp")
        // Angka panjang (ratusan juta ke atas) dikecilkan supaya tidak terpotong.
        val heroSize = Type.hero.fontSize * (11f / digits.length).coerceAtMost(1f)
        Text(
            buildAnnotatedString {
                if (total < 0) withStyle(SpanStyle(color = soft)) { append("−") }
                withStyle(SpanStyle(color = soft, fontSize = heroSize * 0.55f)) { append("Rp") }
                append(digits)
            },
            style = Type.hero.copy(fontSize = heroSize), color = white,
            modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center, maxLines = 1,
        )
        val sub = buildList {
            add("jumlah ${o.accounts.count { it.account.kind != AccountKind.PAYLATER }} dompet")
            if (o.debt > 0) add("hutang ${Rupiah.short(o.debt)}")
        }.joinToString(" · ")
        Text(sub, style = Type.label, color = soft, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)

        // 2. Masuk & keluar periode ini.
        Spacer(Modifier.height(16.dp))
        Row(Modifier.padding(horizontal = Gutter).fillMaxWidth().clip(CardShape).background(white.copy(alpha = 0.12f)).padding(vertical = 12.dp)) {
            Flow("Masuk", o.totals.income, "+", Modifier.weight(1f))
            Box(Modifier.width(1.dp).height(36.dp).background(white.copy(alpha = 0.2f)))
            Flow("Keluar", o.totals.expense, "−", Modifier.weight(1f))
            Box(Modifier.width(1.dp).height(36.dp).background(white.copy(alpha = 0.2f)))
            Flow("Selisih", o.totals.net, if (o.totals.net >= 0) "+" else "−", Modifier.weight(1f))
        }
        Text(
            "${o.periodName.replaceFirstChar { it.uppercase() }} · ${o.cycle.start.format(rangeFmt)} – ${o.cycle.nextPayday.minusDays(1).format(rangeFmt)}",
            style = Type.label, color = soft,
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp), textAlign = TextAlign.Center,
        )

        // 3. Keluar ke mana?
        Spacer(Modifier.height(18.dp))
        Row(Modifier.fillMaxWidth().padding(horizontal = Gutter), verticalAlignment = Alignment.CenterVertically) {
            Text("Keluar ke mana?", style = Type.strong, color = white, modifier = Modifier.weight(1f))
            ChartSwitch(
                listOf("DONUT" to Icons.Rounded.DonutLarge, "BAR" to Icons.Rounded.BarChart, "BUBBLE" to Icons.Rounded.BubbleChart),
                current = o.settings.chart, onPick = onChart,
            )
        }
        if (parts.isEmpty()) {
            Text(
                reading.firstOrNull() ?: "",
                style = Type.body, color = soft, textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 28.dp, vertical = 36.dp),
            )
        } else {
            SpendingChart(o, parts, o.settings.chart, selected) { selected = if (selected == it) null else it }
            val pick = parts.firstOrNull { (it.category?.id ?: -1L) == selected }
            AnimatedContent(pick, transitionSpec = { fadeIn() togetherWith fadeOut() }, label = "pick") { p ->
                Column(Modifier.fillMaxWidth().padding(horizontal = 28.dp).height(44.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    if (p == null) {
                        reading.forEach { line -> Text(line, style = Type.bodySmall, color = soft, textAlign = TextAlign.Center, maxLines = 1, overflow = TextOverflow.Ellipsis) }
                    } else {
                        val share = p.amount * 100 / parts.sumOf { it.amount }.coerceAtLeast(1)
                        Text("${p.category?.emoji ?: "🧾"} ${p.category?.name ?: "Tanpa kategori"}", style = Type.strong, color = white)
                        Text("${Rupiah.format(p.amount)} · $share% dari semua pengeluaran", style = Type.bodySmall, color = soft)
                    }
                }
            }
        }
    }
}

@Composable
private fun SpendingChart(o: Overview, parts: List<CategoryAmount>, type: String, selected: Long?, onSelect: (Long) -> Unit) {
    val slices = parts.map { p ->
        Slice(p.category?.id ?: -1L, p.category?.name ?: "Tanpa kategori", p.category?.emoji ?: "🧾", p.amount.toFloat(), colorOf(p.category), Rupiah.short(p.amount))
    }
    val pick: (Long?) -> Unit = { id -> if (id != null) onSelect(id) else if (selected != null) onSelect(selected) }
    AnimatedContent(type, transitionSpec = { fadeIn() togetherWith fadeOut() }, label = "chart") { t ->
        when (t) {
            "BAR" -> HBarChart(slices, selected, pick, Modifier.fillMaxWidth().padding(horizontal = 28.dp, vertical = 18.dp))
            "BUBBLE" -> BubbleChart(
                bubbles = slices.map { Bubble(it.key, it.emoji, it.label, it.valueText, it.value.coerceAtLeast(1f), -1f, it.color) },
                selected = selected,
                onSelect = onSelect,
                modifier = Modifier.fillMaxWidth().height(240.dp).padding(horizontal = 28.dp, vertical = 8.dp),
            )
            else -> DonutChart(slices, selected, pick, Modifier.fillMaxWidth().height(220.dp).padding(vertical = 16.dp)) {
                val p = parts.firstOrNull { (it.category?.id ?: -1L) == selected }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    GlassIcon(p?.category?.emoji ?: "🧾", Color.White, size = 36.dp, onDark = true)
                    Text(p?.category?.name ?: "Total keluar", style = Type.label, color = Color.White.copy(alpha = 0.72f))
                    Text(Rupiah.short(p?.amount ?: o.totals.expense), style = Type.title, color = Color.White)
                }
            }
        }
    }
}

@Composable
private fun Flow(label: String, amount: Long, sign: String, modifier: Modifier) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, style = Type.label, color = Color.White.copy(alpha = 0.72f))
        Text(
            (if (amount == 0L) "" else sign) + Rupiah.short(kotlin.math.abs(amount)),
            style = Type.amount, color = Color.White, maxLines = 1,
        )
    }
}

/** Kategori yang paling sering dipakai 60 hari terakhir: ketuk, isi nominal, simpan. */
@Composable
private fun QuickAdd(data: id.cukup.data.Overview, onPick: (Long) -> Unit) {
    val c = colors
    val cats = remember(data) {
        val since = System.currentTimeMillis() - 60L * 24 * 60 * 60 * 1000
        val uses = data.confirmed.filter { it.type == TxType.EXPENSE && it.occurredAt >= since }
            .groupingBy { it.categoryId }.eachCount()
        data.expenseCategories().sortedByDescending { uses[it.id] ?: 0 }.take(8)
    }
    if (cats.isEmpty()) return
    Column(Modifier.offset(y = (-18).dp)) {
        SectionHeader("Catat cepat")
        cats.chunked(4).forEach { row ->
            Row(Modifier.fillMaxWidth().padding(horizontal = Gutter, vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { cat ->
                    val color = colorOf(cat)
                    Column(
                        Modifier.weight(1f).clip(CardShape).clickable(role = Role.Button) { onPick(cat.id) }.padding(vertical = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        GlassIcon(cat.emoji, color, size = 54.dp)
                        Spacer(Modifier.height(6.dp))
                        Text(cat.name, style = Type.label, color = c.ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
                repeat(4 - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

/** Kartu dompet bergulir: di sinilah uang sungguhan berada. */
@Composable
private fun Accounts(accounts: List<AccountBalance>, onOpen: (Long) -> Unit, onNew: () -> Unit) {
    val c = colors
    Column(Modifier.offset(y = (-18).dp)) {
    SectionHeader("Dompet")
    Row(
        Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = Gutter),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        accounts.forEach { ab ->
            val color = colorOf(ab.account)
            Column(
                Modifier.width(150.dp).clip(CardShape).background(c.card).clickable { onOpen(ab.account.id) }.padding(14.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    AccountIcon(ab.account, size = 34.dp)
                    Spacer(Modifier.width(8.dp))
                    Text(ab.account.name, style = Type.bodySmall, color = c.mute, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                Spacer(Modifier.height(10.dp))
                Text(
                    // Di atas 99 juta tidak muat di kartu; pakai bentuk ringkas daripada terpotong.
                    if (kotlin.math.abs(ab.balance) >= 100_000_000) (if (ab.balance < 0) "−Rp" else "Rp") + Rupiah.short(kotlin.math.abs(ab.balance)) else Rupiah.format(ab.balance),
                    style = Type.amount,
                    color = if (ab.balance < 0) c.over else c.ink, maxLines = 1,
                )
            }
        }
        Column(
            Modifier.width(110.dp).height(92.dp).clip(CardShape).border(1.dp, c.line, CardShape).clickable(onClick = onNew).padding(14.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(Icons.Rounded.Add, null, tint = c.mute)
            Text("Dompet", style = Type.bodySmall, color = c.mute)
        }
    }
    }
}

/** Ringkasan rencana — jelas ditandai sebagai rencana, bukan uang sungguhan. */
@Composable
private fun PlanCard(o: Overview, onOpen: () -> Unit) {
    val c = colors
    val s = o.planStatus
    Column(
        Modifier.padding(horizontal = Gutter).fillMaxWidth().clip(CardShape).background(c.card).clickable(onClick = onOpen).padding(16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("RENCANA", style = Type.label, color = c.accent, modifier = Modifier.weight(1f))
            Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, null, tint = c.faint)
        }
        if (!s.active) {
            Text("Belum ada rencana belanja", style = Type.strong, color = c.ink)
            Text(
                "Opsional. Tetapkan batas (mis. 50/30/20) dan Cukup akan membandingkannya dengan catatanmu.",
                style = Type.bodySmall, color = c.mute,
            )
            return@Column
        }
        val left = s.spendLeft
        Text(
            if (left >= 0) "Batas belanja tersisa ${Rupiah.format(left)}" else "Belanja lewat rencana ${Rupiah.format(-left)}",
            style = Type.strong, color = if (left < 0) c.over else c.ink,
        )
        Spacer(Modifier.height(8.dp))
        UsageBar(s.spendUsed.toFloat() / s.spendLimit.coerceAtLeast(1), c.good)
        Spacer(Modifier.height(6.dp))
        Text(
            when (s.warning) {
                Warning.OVER -> "Sudah ${Rupiah.short(s.spendUsed)} dari rencana ${Rupiah.short(s.spendLimit)}."
                else -> "Aman ${Rupiah.short(s.perDay)}/hari · ${s.daysLeft} hari lagi sampai gajian"
            },
            style = Type.bodySmall, color = c.mute,
        )
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
    Column(Modifier.padding(top = 16.dp)) {
        HorizontalPager(state = pager, contentPadding = PaddingValues(horizontal = Gutter), pageSpacing = 10.dp) { page ->
            val insight = insights[page]
            val tone = when (insight.tone) {
                Insight.Tone.GOOD -> c.good
                Insight.Tone.INFO -> c.accent
                Insight.Tone.WARN -> c.caution
            }
            Row(
                Modifier.fillMaxWidth().height(92.dp).clip(CardShape).background(c.card).padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(Modifier.size(40.dp).clip(CircleShape).background(tone.copy(alpha = 0.14f)), contentAlignment = Alignment.Center) {
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
