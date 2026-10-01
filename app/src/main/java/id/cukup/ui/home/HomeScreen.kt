package id.cukup.ui.home

import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.verticalScroll
import id.cukup.ui.components.ScheduleEditor
import androidx.compose.material3.TextButton
import androidx.compose.material3.AlertDialog
import androidx.compose.animation.core.spring
import androidx.compose.animation.SizeTransform
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
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.repeatOnLifecycle
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
import id.cukup.notif.MoneyNotificationListener
import id.cukup.ui.settings.span
import id.cukup.ui.components.AmountDialog
import id.cukup.ui.components.CardShape
import id.cukup.ui.components.OTHER_KEY
import id.cukup.ui.components.merged
import id.cukup.ui.components.CategoryLookDialog
import id.cukup.ui.components.LineButton
import id.cukup.ui.components.Hairline
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
    onOpenAuto: () -> Unit,
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
        item {
            Hero(
                data, vm::chart, onOpenInbox, onOpenProfile, onOpenHelp,
                onOpenAccount = onOpenAccount, onOpenTx = onOpenTx,
                onSchedule = { sch -> vm.settings { it.copy(schedule = sch) } },
                onTransfer = { onAdd(TxType.TRANSFER) },
                onSetBalance = { accountId, v -> vm.setAccountBalance(accountId, v) },
                onSaveCategory = vm::saveCategory,
            )
        }
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
        item { AutoHealth(vm, onOpenAuto) }
        item {
            // Data, bukan tombol: diberi kartu sendiri supaya terbaca terpisah dari fitur di atasnya.
            Column(Modifier.padding(horizontal = Gutter).padding(top = 28.dp).fillMaxWidth().clip(CardShape).background(c.card)) {
                Row(
                    Modifier.fillMaxWidth().padding(start = Gutter, end = 8.dp, top = 14.dp, bottom = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text("Catatan terakhir", style = Type.title, color = c.ink)
                        if (recent.isNotEmpty()) Text("${recent.size} terbaru · ketuk untuk rincian", style = Type.bodySmall, color = c.faint)
                    }
                    if (recent.isNotEmpty()) TextAction("Lihat semua", onOpenHistory)
                }
                if (recent.isEmpty()) {
                    Text(
                        "Belum ada catatan. Tekan Keluar setiap kali belanja (termasuk tunai), dan Masuk setiap dapat uang.",
                        style = Type.body, color = c.faint,
                        modifier = Modifier.padding(horizontal = Gutter).padding(top = 4.dp, bottom = 16.dp),
                    )
                } else {
                    val accounts = data.accountById
                    recent.forEachIndexed { i, tx ->
                        if (i > 0) Hairline(Modifier.padding(horizontal = Gutter))
                        val cat = tx.categoryId?.let(data.categoryById::get)
                        TxRow(
                            tx = tx,
                            category = cat,
                            account = tx.accountId?.let(accounts::get),
                            toAccount = tx.toAccountId?.let(accounts::get),
                            color = colorOf(cat),
                            onClick = { onOpenTx(tx.id) },
                        )
                    }
                    Spacer(Modifier.height(6.dp))
                }
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
    onOpenAccount: (Long) -> Unit,
    onOpenTx: (Long) -> Unit,
    onSchedule: (id.cukup.domain.Schedule) -> Unit,
    onTransfer: () -> Unit,
    onSetBalance: (Long, Long) -> Unit,
    onSaveCategory: (id.cukup.domain.Category) -> Unit,
) {
    val c = colors
    val white = Color.White
    // Hampir semua angka di bagian atas bisa diketuk untuk melihat rinciannya.
    var sheet by remember { mutableStateOf<HeroSheet?>(null) }
    var editSchedule by remember { mutableStateOf(false) }
    sheet?.let {
        HeroDetails(
            it, o, onDismiss = { sheet = null }, onOpenAccount = { id -> sheet = null; onOpenAccount(id) }, onOpenTx = { id -> sheet = null; onOpenTx(id) },
            onTransfer = { sheet = null; onTransfer() }, onSetBalance = onSetBalance,
        )
    }
    if (editSchedule) {
        var draft by remember { mutableStateOf(o.settings.schedule) }
        AlertDialog(
            onDismissRequest = { editSchedule = false },
            title = { Text("Jadwal gajian", style = Type.title) },
            text = { Column(Modifier.verticalScroll(rememberScrollState())) { ScheduleEditor(draft, { draft = it }, Modifier.padding(horizontal = 0.dp)) } },
            confirmButton = { TextButton({ onSchedule(draft); editSchedule = false }) { Text("Simpan", color = c.ink) } },
            dismissButton = { TextButton({ editSchedule = false }) { Text("Batal", color = c.mute) } },
            containerColor = c.card,
        )
    }
    val soft = Color.White.copy(alpha = 0.72f)
    var selected by rememberSaveable { mutableStateOf<Long?>(null) }
    // Grafik bisa menampilkan uang keluar atau uang masuk per kategori.
    var incomeView by rememberSaveable { mutableStateOf(false) }
    val parts = remember(o, incomeView) {
        Ledger.byCategory(o.transactions, o.categoryById.values.toList(), if (incomeView) TxType.INCOME else TxType.EXPENSE, o.cycleStart, o.cycleEnd)
    }
    val reading = remember(o, incomeView) {
        if (incomeView) ChartReader.readIncome(parts, o.periodName) else ChartReader.readSpending(parts, o.totals, o.periodName)
    }

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
            modifier = Modifier.fillMaxWidth().padding(horizontal = Gutter).clip(CardShape).clickable { sheet = HeroSheet.WALLETS },
            textAlign = TextAlign.Center, maxLines = 1,
        )
        val sub = buildList {
            add("jumlah ${o.accounts.count { it.account.kind != AccountKind.PAYLATER }} dompet")
            if (o.debt > 0) add("hutang ${Rupiah.short(o.debt)}")
        }.joinToString(" · ")
        Text(sub, style = Type.label, color = soft, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)

        // 2. Masuk & keluar periode ini.
        Spacer(Modifier.height(16.dp))
        Row(Modifier.padding(horizontal = Gutter).fillMaxWidth().clip(CardShape).background(white.copy(alpha = 0.12f)).padding(vertical = 12.dp)) {
            Flow("Masuk", o.totals.income, "+", Modifier.weight(1f).clickable { sheet = HeroSheet.INCOME })
            Box(Modifier.width(1.dp).height(36.dp).background(white.copy(alpha = 0.2f)))
            Flow("Keluar", o.totals.expense, "−", Modifier.weight(1f).clickable { sheet = HeroSheet.EXPENSE })
            Box(Modifier.width(1.dp).height(36.dp).background(white.copy(alpha = 0.2f)))
            Flow("Selisih", o.totals.net, if (o.totals.net >= 0) "+" else "−", Modifier.weight(1f).clickable { sheet = HeroSheet.NET })
        }
        Row(Modifier.fillMaxWidth().padding(top = 4.dp), horizontalArrangement = Arrangement.Center) {
            Text(
                "${o.periodName.replaceFirstChar { it.uppercase() }} · ${o.cycle.start.format(rangeFmt)} – ${o.cycle.nextPayday.minusDays(1).format(rangeFmt)}  ›",
                style = Type.label, color = soft,
                modifier = Modifier.clip(Pill).clickable { editSchedule = true }.padding(horizontal = 12.dp, vertical = 6.dp),
            )
        }

        // 3. Keluar ke mana?
        // Jenis grafik diganti langsung di layar; menyimpannya ke setelan menyusul di belakang,
        // supaya tidak menunggu tulis setelan + hitung ulang semua data.
        var chart by rememberSaveable { mutableStateOf(o.settings.chart) }
        Spacer(Modifier.height(18.dp))
        Row(Modifier.fillMaxWidth().padding(horizontal = Gutter), verticalAlignment = Alignment.CenterVertically) {
            Row(Modifier.weight(1f)) {
                FlowToggle(incomeView) { incomeView = it; selected = null }
            }
            ChartSwitch(
                listOf("DONUT" to Icons.Rounded.DonutLarge, "BAR" to Icons.Rounded.BarChart, "BUBBLE" to Icons.Rounded.BubbleChart),
                current = chart, onPick = { chart = it; onChart(it) },
            )
        }
        if (parts.isEmpty()) {
            Text(
                reading.firstOrNull() ?: "",
                style = Type.body, color = soft, textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 28.dp, vertical = 36.dp),
            )
        } else {
            var recolor by remember { mutableStateOf<Long?>(null) }
            recolor?.let(o.categoryById::get)?.let { cat ->
                CategoryLookDialog(cat, onDismiss = { recolor = null }) { onSaveCategory(it); recolor = null }
            }
            SpendingChart(o, parts, chart, selected, incomeView, onRecolor = { recolor = it }) { selected = if (selected == it) null else it }
            val pick = parts.firstOrNull { (it.category?.id ?: -1L) == selected }
            AnimatedContent(pick, transitionSpec = { fadeIn() togetherWith fadeOut() }, label = "pick") { p ->
                Column(Modifier.fillMaxWidth().padding(horizontal = 28.dp).height(44.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    if (p == null) {
                        reading.forEach { line -> Text(line, style = Type.bodySmall, color = soft, textAlign = TextAlign.Center, maxLines = 1, overflow = TextOverflow.Ellipsis) }
                    } else {
                        val share = p.amount * 100 / parts.sumOf { it.amount }.coerceAtLeast(1)
                        Text(p.category?.name ?: "Tanpa kategori", style = Type.strong, color = white)
                        Text("${Rupiah.format(p.amount)} · $share% dari semua ${if (incomeView) "uang masuk" else "pengeluaran"}", style = Type.bodySmall, color = soft)
                    }
                }
            }
        }
    }
}

@Composable
private fun SpendingChart(
    o: Overview, parts: List<CategoryAmount>, type: String, selected: Long?, income: Boolean,
    onRecolor: (Long) -> Unit, onSelect: (Long) -> Unit,
) {
    val colorsOf = parts.map { colorOf(it.category) }
    val slices = remember(parts, colorsOf) {
        parts.mapIndexed { i, p -> Slice(p.category?.id ?: -1L, p.category?.name ?: "Tanpa kategori", p.category?.emoji ?: "🧾", p.amount.toFloat(), colorsOf[i], Rupiah.short(p.amount)) }
    }
    val pick: (Long?) -> Unit = { id -> if (id != null) onSelect(id) else if (selected != null) onSelect(selected) }
    // Tinggi tetap: saat ganti grafik hanya isinya yang berganti (fade = murah), bagian atas Beranda
    // tidak perlu dihitung ulang tata letaknya di setiap frame animasi.
    Crossfade(type, Modifier.fillMaxWidth().height(240.dp), animationSpec = tween(200), label = "chart") { t ->
        when (t) {
            "BAR" -> HBarChart(
                slices.take(5), selected, pick,
                Modifier.fillMaxSize().padding(horizontal = 28.dp, vertical = 12.dp).wrapContentHeight(Alignment.CenterVertically),
                onLongPress = onRecolor,
            )
            "BUBBLE" -> BubbleChart(
                bubbles = slices.map { Bubble(it.key, it.emoji, it.label, it.valueText, it.value.coerceAtLeast(1f), -1f, it.color) },
                selected = selected,
                onSelect = onSelect,
                modifier = Modifier.fillMaxSize().padding(horizontal = 28.dp, vertical = 8.dp),
                onLongPress = onRecolor,
            )
            else -> {
                // Kategori kecil-kecil digabung supaya donat tidak pecah jadi serpihan.
                val donut = remember(slices) { slices.merged(color = Color.White.copy(alpha = 0.38f)) { Rupiah.short(it.toLong()) } }
                val other = donut.lastOrNull()?.takeIf { it.key == OTHER_KEY && selected == OTHER_KEY }
                DonutChart(donut, selected, pick, Modifier.fillMaxSize().padding(vertical = 16.dp), onLongPress = onRecolor) {
                val p = parts.firstOrNull { (it.category?.id ?: -1L) == selected }
                if (other != null) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(other.label, style = Type.label, color = Color.White.copy(alpha = 0.72f))
                        Text(other.valueText, style = Type.title, color = Color.White)
                        Text("lihat di grafik batang", style = Type.label, color = Color.White.copy(alpha = 0.6f))
                    }
                } else
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    GlassIcon(p?.category?.emoji ?: "🧾", Color.White, size = 36.dp, onDark = true)
                    Text(p?.category?.name ?: if (income) "Total masuk" else "Total keluar", style = Type.label, color = Color.White.copy(alpha = 0.72f))
                    Text(Rupiah.short(p?.amount ?: if (income) o.totals.income else o.totals.expense), style = Type.title, color = Color.White)
                }
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
                        Text(cat.name, style = Type.label, color = c.ink, maxLines = 2, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center)
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
                Modifier.width(172.dp).clip(CardShape).background(c.card).clickable { onOpen(ab.account.id) }.padding(14.dp),
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

private enum class HeroSheet { WALLETS, INCOME, EXPENSE, NET }

/** Rincian angka di bagian atas Beranda: dompet, uang masuk, uang keluar, atau selisih periode ini. */
@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
private fun HeroDetails(
    kind: HeroSheet, o: Overview, onDismiss: () -> Unit, onOpenAccount: (Long) -> Unit, onOpenTx: (Long) -> Unit,
    onTransfer: () -> Unit, onSetBalance: (Long, Long) -> Unit,
) {
    val c = colors
    var fixing by remember { mutableStateOf<AccountBalance?>(null) }
    fixing?.let { ab ->
        AmountDialog(
            "Samakan saldo ${ab.account.name}",
            "Isi saldo yang benar sekarang (lihat di aplikasi bank/e-wallet atau hitung uang tunai). Selisihnya dicatat sebagai penyesuaian; riwayat lain tidak berubah.",
            ab.balance, onDismiss = { fixing = null }, allowNegative = true,
        ) { v -> onSetBalance(ab.account.id, v); fixing = null }
    }
    androidx.compose.material3.ModalBottomSheet(onDismissRequest = onDismiss, containerColor = c.card) {
        androidx.compose.foundation.lazy.LazyColumn(Modifier.fillMaxWidth().padding(bottom = 24.dp)) {
            when (kind) {
                HeroSheet.WALLETS -> {
                    val wallets = o.accounts.filter { it.account.kind != AccountKind.PAYLATER }
                    val total = wallets.sumOf { it.balance.coerceAtLeast(0) }.coerceAtLeast(1)
                    item { SheetTitle("Uangmu di mana", Rupiah.format(o.netWorth), "${wallets.size} dompet" + if (o.debt > 0) " · hutang ${Rupiah.short(o.debt)}" else "") }
                    item {
                        // Dua cara membetulkan angka: uangnya memang pindah dompet, atau catatannya yang meleset.
                        Column(Modifier.padding(horizontal = Gutter).padding(bottom = 8.dp)) {
                            LineButton("Pindah uang antar dompet", onClick = onTransfer, modifier = Modifier.fillMaxWidth(), height = 44.dp)
                            Text(
                                "Angkanya beda dengan bank atau isi dompet? Ketuk \"Samakan\" di dompet itu.",
                                style = Type.bodySmall, color = c.mute, modifier = Modifier.padding(top = 8.dp),
                            )
                        }
                    }
                    items(o.accounts.sortedByDescending { it.balance }, key = { "a" + it.account.id }) { ab ->
                        val share = (ab.balance.coerceAtLeast(0).toFloat() / total).coerceIn(0f, 1f)
                        Row(
                            Modifier.fillMaxWidth().clickable { onOpenAccount(ab.account.id) }.padding(horizontal = Gutter, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            AccountIcon(ab.account, size = 40.dp)
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Row {
                                    Text(ab.account.name, style = Type.strong, color = c.ink, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Text(Rupiah.format(ab.balance), style = Type.amount, color = if (ab.balance < 0) c.over else c.ink)
                                }
                                if (ab.account.kind != AccountKind.PAYLATER) {
                                    Box(Modifier.padding(top = 6.dp).fillMaxWidth().height(4.dp).clip(Pill).background(c.line)) {
                                        Box(Modifier.fillMaxWidth(share).height(4.dp).clip(Pill).background(colorOf(ab.account)))
                                    }
                                }
                            }
                            Spacer(Modifier.width(8.dp))
                            TextAction("Samakan", { fixing = ab })
                        }
                    }
                }
                else -> {
                    val txs = o.confirmed.filter { it.occurredAt >= o.cycleStart && it.occurredAt < o.cycleEnd }.filter {
                        when (kind) {
                            HeroSheet.INCOME -> it.type == TxType.INCOME
                            HeroSheet.EXPENSE -> it.type == TxType.EXPENSE
                            else -> it.type != TxType.TRANSFER
                        }
                    }
                    val title = when (kind) {
                        HeroSheet.INCOME -> "Uang masuk ${o.periodName}"
                        HeroSheet.EXPENSE -> "Uang keluar ${o.periodName}"
                        else -> "Selisih ${o.periodName}"
                    }
                    val amount = when (kind) {
                        HeroSheet.INCOME -> o.totals.income
                        HeroSheet.EXPENSE -> o.totals.expense
                        else -> o.totals.net
                    }
                    val sub = if (kind == HeroSheet.NET) "Masuk ${Rupiah.short(o.totals.income)} − keluar ${Rupiah.short(o.totals.expense)}" else "${txs.size} catatan"
                    item { SheetTitle(title, (if (kind == HeroSheet.NET && amount < 0) "−" else "") + Rupiah.format(kotlin.math.abs(amount)), sub) }
                    if (txs.isEmpty()) {
                        item { Text("Belum ada catatan.", style = Type.body, color = c.faint, modifier = Modifier.padding(horizontal = Gutter, vertical = 12.dp)) }
                    }
                    items(txs, key = { "t" + it.id }) { tx ->
                        val cat = tx.categoryId?.let(o.categoryById::get)
                        TxRow(tx, cat, o.accountById[tx.accountId], o.accountById[tx.toAccountId], colorOf(cat), { onOpenTx(tx.id) })
                    }
                }
            }
        }
    }
}

@Composable
private fun SheetTitle(title: String, amount: String, sub: String) {
    val c = colors
    Column(Modifier.padding(horizontal = Gutter).padding(bottom = 12.dp)) {
        Text(title, style = Type.bodySmall, color = c.mute)
        Text(amount, style = Type.number, color = c.ink)
        Text(sub, style = Type.bodySmall, color = c.faint)
    }
}

/** Pengalih kecil Keluar | Masuk di atas grafik. */
@Composable
private fun FlowToggle(income: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.clip(Pill).background(Color.White.copy(alpha = 0.14f)).padding(3.dp)) {
        listOf(false to "Keluar", true to "Masuk").forEach { (value, label) ->
            val on = value == income
            Text(
                label,
                style = Type.strong,
                color = if (on) colors.accent else Color.White,
                modifier = Modifier.clip(Pill).background(if (on) Color.White else Color.Transparent)
                    .clickable { onChange(value) }.padding(horizontal = 14.dp, vertical = 6.dp),
            )
        }
    }
}

/** Peringatan kalau catat otomatis belum nyala atau sempat dimatikan sistem; diam kalau sehat. */
@Composable
private fun AutoHealth(vm: AppViewModel, onOpen: () -> Unit) {
    val c = colors
    val context = androidx.compose.ui.platform.LocalContext.current
    val n by vm.notices.collectAsStateWithLifecycle()
    val lifecycle = androidx.lifecycle.compose.LocalLifecycleOwner.current.lifecycle
    var on by remember { mutableStateOf(true) }
    LaunchedEffect(lifecycle) {
        lifecycle.repeatOnLifecycle(androidx.lifecycle.Lifecycle.State.RESUMED) { on = MoneyNotificationListener.isEnabled(context) }
    }
    val gap = n.gap
    val (title, body) = when {
        !on -> "Catat otomatis belum nyala" to "Transfer dan pembayaran belum tercatat sendiri. Ketuk untuk menyalakan."
        gap != null -> "Catat otomatis sempat mati ${span(gap.to - gap.from)}" to
            "Notifikasi bank selama itu mungkin terlewat. Ketuk untuk lihat dan cegah terulang."
        else -> return
    }
    Row(
        Modifier.padding(horizontal = Gutter).padding(top = 4.dp).fillMaxWidth().clip(CardShape)
            .background(c.caution.copy(alpha = 0.14f)).clickable(onClick = onOpen).padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Rounded.Notifications, null, tint = c.caution, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = Type.strong, color = c.ink)
            Text(body, style = Type.bodySmall, color = c.mute)
        }
    }
}
