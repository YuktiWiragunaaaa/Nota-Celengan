package id.cukup.ui.history

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.DonutLarge
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import id.cukup.data.MoneyRepository
import id.cukup.domain.ChartReader
import id.cukup.data.Overview
import id.cukup.domain.CategoryAmount
import id.cukup.domain.Ledger
import id.cukup.domain.Totals
import id.cukup.ui.components.colorOf
import id.cukup.domain.Rupiah
import id.cukup.domain.Transaction
import id.cukup.domain.TxStatus
import id.cukup.domain.TxType
import id.cukup.ui.components.CardShape
import id.cukup.ui.components.Choice
import id.cukup.ui.components.DayBarChart
import id.cukup.ui.components.DonutChart
import id.cukup.ui.components.Gutter
import id.cukup.ui.components.HBarChart
import id.cukup.ui.components.Id
import id.cukup.ui.components.Pill
import id.cukup.ui.components.RoundIcon
import id.cukup.ui.components.Slice
import id.cukup.ui.components.TxRow
import id.cukup.ui.components.dayLabel
import id.cukup.ui.components.localDate
import id.cukup.ui.theme.Type
import id.cukup.ui.theme.colors
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate
import java.time.format.TextStyle
import java.time.temporal.ChronoUnit
import javax.inject.Inject

enum class Range(val label: String) { THIS("Periode ini"), LAST("Periode lalu"), DAYS30("30 hari") }

data class HistoryState(
    val loaded: Boolean = false,
    val range: Range = Range.THIS,
    val overview: Overview? = null,
    val days: List<LocalDate> = emptyList(),
    val daily: List<Long> = emptyList(),
    val txs: List<Transaction> = emptyList(),
    val spent: Long = 0,
    val income: Long = 0,
    val byCategory: List<CategoryAmount> = emptyList(),
    val reading: String = "",
    val limitPerDay: Long = 0,
)

@HiltViewModel
class HistoryViewModel @Inject constructor(repository: MoneyRepository) : ViewModel() {
    private val range = MutableStateFlow(Range.THIS)

    val state: StateFlow<HistoryState> = combine(repository.overview, range) { o, r ->
        val today = LocalDate.now()
        val (from, toExclusive) = when (r) {
            Range.THIS -> o.cycle.start to o.cycle.nextPayday
            Range.LAST -> o.previous.start to o.cycle.start
            Range.DAYS30 -> today.minusDays(29) to today.plusDays(1)
        }
        val shownEnd = if (toExclusive.isAfter(today.plusDays(1))) today.plusDays(1) else toExclusive
        val n = ChronoUnit.DAYS.between(from, shownEnd).toInt().coerceIn(1, 62)
        val days = (0 until n).map { from.plusDays(it.toLong()) }
        val inRange = o.confirmed.filter { localDate(it.occurredAt).let { d -> !d.isBefore(from) && d.isBefore(toExclusive) } }
        val daily = LongArray(n)
        inRange.filter { it.type == TxType.EXPENSE }.forEach { t ->
            val i = ChronoUnit.DAYS.between(from, localDate(t.occurredAt)).toInt()
            if (i in 0 until n) daily[i] += t.amount
        }
        val byCategory = Ledger.byCategory(inRange, o.categoryById.values.toList(), TxType.EXPENSE)
        val income = inRange.filter { it.type == TxType.INCOME }.sumOf { it.amount }
        HistoryState(
            loaded = true,
            range = r,
            overview = o,
            days = days,
            daily = daily.toList(),
            txs = inRange,
            spent = daily.sum(),
            income = income,
            byCategory = byCategory,
            reading = ChartReader.readSpending(byCategory, Totals(income, daily.sum()), r.label.lowercase()).firstOrNull() ?: "",
            limitPerDay = if (r == Range.THIS && o.planStatus.active) o.planStatus.spendLimit / o.cycle.length.coerceAtLeast(1) else 0,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HistoryState())

    fun setRange(r: Range) {
        range.value = r
    }
}

@Composable
fun HistoryScreen(contentPadding: PaddingValues, onOpenTx: (Long) -> Unit, vm: HistoryViewModel = hiltViewModel()) {
    val s by vm.state.collectAsStateWithLifecycle()
    val c = colors
    var day by rememberSaveable(s.range) { mutableStateOf<Int?>(null) }
    var picked by rememberSaveable(s.range) { mutableStateOf<Long?>(null) }
    var breakdown by rememberSaveable { mutableStateOf("DONUT") }
    var account by rememberSaveable { mutableStateOf<Long?>(null) }
    val o = s.overview

    val selectedDay = day?.let { s.days.getOrNull(it) }
    val list = s.txs
        .filter { selectedDay == null || localDate(it.occurredAt) == selectedDay }
        .filter { picked == null || (it.categoryId ?: -1L) == picked }
        .filter { account == null || it.accountId == account || it.toAccountId == account }
    val groups = list.groupBy { localDate(it.occurredAt) }.toList().sortedByDescending { it.first }

    LazyColumn(Modifier.fillMaxSize().background(c.paper), contentPadding = contentPadding) {
        item {
            Text("Riwayat", style = Type.display, color = c.ink, modifier = Modifier.padding(horizontal = Gutter).padding(top = 16.dp))
            LazyRow(contentPadding = PaddingValues(horizontal = Gutter), horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 12.dp)) {
                items(Range.entries) { r -> Choice(r.label, s.range == r, { vm.setRange(r) }) }
            }
            if (o != null && o.accounts.size > 1) {
                LazyRow(contentPadding = PaddingValues(horizontal = Gutter), horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 8.dp)) {
                    item { Choice("Semua dompet", account == null, { account = null }) }
                    items(o.accounts) { ab -> Choice(ab.account.name, account == ab.account.id, { account = ab.account.id }) }
                }
            }
        }

        // Grafik harian: sentuh batang untuk memilih hari.
        item {
            Column(Modifier.padding(Gutter).fillMaxWidth().clip(CardShape).background(c.card).padding(18.dp)) {
                AnimatedContent(selectedDay, transitionSpec = { fadeIn() togetherWith fadeOut() }, label = "day") { d ->
                    Row(verticalAlignment = Alignment.Bottom) {
                        Column(Modifier.weight(1f)) {
                            Text(if (d == null) "Total keluar" else dayLabel(d), style = Type.bodySmall, color = c.mute)
                            Text(
                                Rupiah.format(if (d == null) s.spent else s.daily.getOrElse(day ?: 0) { 0 }),
                                style = Type.number, color = c.ink,
                            )
                        }
                        if (d == null && s.income > 0) {
                            Column(horizontalAlignment = Alignment.End) {
                                Text("Masuk", style = Type.bodySmall, color = c.mute)
                                Text("+" + Rupiah.short(s.income), style = Type.amount, color = c.good)
                            }
                        }
                    }
                }
                Spacer(Modifier.height(14.dp))
                val labels = s.days.map { d ->
                    when {
                        s.days.size <= 8 -> d.dayOfWeek.getDisplayName(TextStyle.SHORT, Id).take(3)
                        d.dayOfMonth % 5 == 1 -> "${d.dayOfMonth}"
                        else -> ""
                    }
                }
                DayBarChart(
                    values = s.daily,
                    labels = labels,
                    selected = day,
                    onSelect = { day = it },
                    barColor = c.accent,
                    faint = c.faint,
                    limitPerDay = s.limitPerDay,
                    limitColor = c.caution,
                    modifier = Modifier.fillMaxWidth().height(170.dp),
                )
                if (s.limitPerDay > 0) {
                    Text(
                        "Garis putus-putus = batas rencana per hari (${Rupiah.short(s.limitPerDay)}). Sentuh batang untuk lihat harinya.",
                        style = Type.label, color = c.faint, modifier = Modifier.padding(top = 10.dp),
                    )
                }
            }
        }

        // Pengeluaran per kategori.
        if (s.byCategory.isNotEmpty()) {
            item {
                Column(Modifier.padding(horizontal = Gutter).fillMaxWidth().clip(CardShape).background(c.card).padding(18.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Ke mana saja", style = Type.title, color = c.ink, modifier = Modifier.weight(1f))
                        RoundIcon(
                            if (breakdown == "DONUT") Icons.Rounded.BarChart else Icons.Rounded.DonutLarge,
                            "Ganti grafik", { breakdown = if (breakdown == "DONUT") "BAR" else "DONUT" },
                            background = c.surface, size = 38.dp,
                        )
                    }
                    Text(s.reading, style = Type.bodySmall, color = c.mute, modifier = Modifier.padding(top = 4.dp, bottom = 12.dp))
                    val slices = s.byCategory.map { p -> Slice(p.category?.id ?: -1L, p.category?.name ?: "Tanpa kategori", p.category?.emoji ?: "🧾", p.amount.toFloat(), colorOf(p.category), Rupiah.short(p.amount)) }
                    AnimatedContent(breakdown, transitionSpec = { fadeIn() togetherWith fadeOut() }, label = "bd") { type ->
                        if (type == "BAR") {
                            HBarChart(slices, picked, { picked = it }, textColor = c.ink, track = c.line)
                        } else {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                DonutChart(slices, picked, { picked = it }, Modifier.height(150.dp).weight(1f), track = c.line) {
                                    val p = s.byCategory.firstOrNull { (it.category?.id ?: -1L) == picked }
                                    val total = s.byCategory.sumOf { it.amount }.coerceAtLeast(1)
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text(if (p == null) "100%" else "${p.amount * 100 / total}%", style = Type.amount, color = c.ink)
                                        Text(p?.category?.name ?: "semua", style = Type.label, color = c.faint, textAlign = TextAlign.Center)
                                    }
                                }
                                Column(Modifier.weight(1f).padding(start = 12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    s.byCategory.take(5).forEach { p ->
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Box(Modifier.padding(end = 8.dp).size(10.dp).clip(Pill).background(colorOf(p.category)))
                                            Text(p.category?.name ?: "Tanpa kategori", style = Type.label, color = c.ink, modifier = Modifier.weight(1f), maxLines = 1)
                                            Text(Rupiah.short(p.amount), style = Type.label, color = c.mute)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Daftar transaksi (ikut tersaring oleh hari / kantong yang dipilih).
        item {
            Row(Modifier.padding(horizontal = Gutter).padding(top = 24.dp, bottom = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("Transaksi", style = Type.title, color = c.ink, modifier = Modifier.weight(1f))
                if (day != null || picked != null || account != null) Choice("Tampilkan semua", false, { day = null; picked = null; account = null })
            }
        }
        if (s.loaded && groups.isEmpty()) {
            item { Text("Belum ada yang dicatat di sini.", style = Type.body, color = c.faint, modifier = Modifier.padding(Gutter)) }
        }
        groups.forEach { (date, txs) ->
            item(key = "d$date") {
                Text(dayLabel(date), style = Type.label, color = c.faint, modifier = Modifier.padding(horizontal = Gutter).padding(top = 14.dp, bottom = 2.dp))
            }
            items(txs, key = { it.id }) { tx ->
                val cat = tx.categoryId?.let { o?.categoryById?.get(it) }
                TxRow(tx, cat, tx.accountId?.let { o?.accountById?.get(it) }, tx.toAccountId?.let { o?.accountById?.get(it) }, colorOf(cat), onClick = { onOpenTx(tx.id) })
            }
        }
        item { Spacer(Modifier.height(24.dp)) }
    }
}
