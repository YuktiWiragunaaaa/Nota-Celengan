package id.cukup.ui.history

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.patrykandpatrick.vico.compose.cartesian.CartesianChartHost
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberBottom
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberColumnCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.rememberCartesianChart
import com.patrykandpatrick.vico.compose.common.component.rememberLineComponent
import com.patrykandpatrick.vico.compose.common.component.rememberTextComponent
import com.patrykandpatrick.vico.compose.common.fill
import com.patrykandpatrick.vico.core.cartesian.axis.HorizontalAxis
import com.patrykandpatrick.vico.core.cartesian.data.CartesianChartModelProducer
import com.patrykandpatrick.vico.core.cartesian.data.CartesianValueFormatter
import com.patrykandpatrick.vico.core.cartesian.data.columnSeries
import com.patrykandpatrick.vico.core.cartesian.layer.ColumnCartesianLayer
import dagger.hilt.android.lifecycle.HiltViewModel
import id.cukup.data.MoneyRepository
import id.cukup.domain.Pocket
import id.cukup.domain.Rupiah
import id.cukup.domain.Transaction
import id.cukup.domain.TxStatus
import id.cukup.domain.TxType
import id.cukup.ui.components.Choice
import id.cukup.ui.components.Eyebrow
import id.cukup.ui.components.Gutter
import id.cukup.ui.components.Hairline
import id.cukup.ui.components.TxRow
import id.cukup.ui.components.dayLabel
import id.cukup.ui.components.localDate
import id.cukup.ui.onboarding.italicize
import id.cukup.ui.theme.Type
import id.cukup.ui.theme.colors
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import javax.inject.Inject

data class HistoryState(
    val loaded: Boolean = false,
    val pockets: List<Pocket> = emptyList(),
    val filter: Long? = null,
    /** Pengeluaran per hari sepanjang siklus berjalan (indeks 0 = hari pertama siklus). */
    val daily: List<Long> = emptyList(),
    val cycleStart: LocalDate = LocalDate.now(),
    val groups: List<Pair<LocalDate, List<Transaction>>> = emptyList(),
    val spent: Long = 0,
    val average: Long = 0,
)

@HiltViewModel
class HistoryViewModel @Inject constructor(repository: MoneyRepository) : ViewModel() {
    private val filter = MutableStateFlow<Long?>(null)

    val state: StateFlow<HistoryState> = combine(repository.pockets, repository.transactions, repository.settings, filter) { pockets, txs, settings, f ->
        val today = LocalDate.now()
        val cycle = repository.cycle(settings.payday, today)
        val shown = txs.filter { it.status == TxStatus.CONFIRMED && (f == null || it.pocketId == f || it.toPocketId == f) }
        val days = (ChronoUnit.DAYS.between(cycle.start, today).toInt() + 1).coerceAtLeast(1)
        val daily = LongArray(days)
        shown.filter { it.type == TxType.EXPENSE && !it.isPaylater }.forEach { t ->
            val d = ChronoUnit.DAYS.between(cycle.start, localDate(t.occurredAt)).toInt()
            if (d in 0 until days) daily[d] += t.amount
        }
        HistoryState(
            loaded = true,
            pockets = pockets,
            filter = f,
            daily = daily.toList(),
            cycleStart = cycle.start,
            groups = shown.groupBy { localDate(it.occurredAt) }.toList().sortedByDescending { it.first },
            spent = daily.sum(),
            average = daily.sum() / days,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HistoryState())

    fun setFilter(id: Long?) {
        filter.value = id
    }
}

@Composable
fun HistoryScreen(contentPadding: PaddingValues, onOpenTx: (Long) -> Unit, vm: HistoryViewModel = hiltViewModel()) {
    val s by vm.state.collectAsStateWithLifecycle()
    val c = colors
    val byId = s.pockets.associateBy { it.id }
    val index = s.pockets.mapIndexed { i, p -> p.id to i }.toMap()

    LazyColumn(Modifier.fillMaxSize().background(c.paper), contentPadding = contentPadding) {
        item {
            Column(Modifier.padding(horizontal = Gutter).padding(top = 24.dp)) {
                Text(italicize("Ke mana <i>uangnya?</i>"), style = Type.display, color = c.ink)
                Spacer(Modifier.height(16.dp))
                Row {
                    Column(Modifier.weight(1f)) {
                        Eyebrow("Keluar siklus ini")
                        Text(Rupiah.format(s.spent), style = Type.title, color = c.ink)
                    }
                    Column(Modifier.weight(1f)) {
                        Eyebrow("Rata-rata per hari")
                        Text(Rupiah.format(s.average), style = Type.title, color = c.ink)
                    }
                }
            }
        }
        item {
            DailyChart(s.daily, s.cycleStart, Modifier.fillMaxWidth().height(180.dp).padding(horizontal = 12.dp, vertical = 16.dp))
        }
        item {
            androidx.compose.foundation.lazy.LazyRow(
                contentPadding = PaddingValues(horizontal = Gutter),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(bottom = 8.dp),
            ) {
                item { Choice("Semua", s.filter == null, { vm.setFilter(null) }) }
                items(s.pockets, key = { it.id }) { p ->
                    Choice("${p.emoji} ${p.name}", s.filter == p.id, { vm.setFilter(p.id) })
                }
            }
        }
        if (s.loaded && s.groups.isEmpty()) {
            item {
                Text("Belum ada transaksi di sini.", style = Type.body, color = c.faint, modifier = Modifier.padding(Gutter))
            }
        }
        s.groups.forEach { (date, txs) ->
            item(key = "d$date") {
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = Gutter).padding(top = 20.dp, bottom = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Eyebrow(dayLabel(date), Modifier.weight(1f))
                    val out = txs.filter { it.type == TxType.EXPENSE && !it.isPaylater }.sumOf { it.amount }
                    if (out > 0) Text("−" + Rupiah.format(out), style = Type.bodySmall, color = c.mute)
                }
                Hairline()
            }
            items(txs, key = { it.id }) { tx ->
                TxRow(
                    tx, tx.pocketId?.let(byId::get), tx.toPocketId?.let(byId::get),
                    c.pocket(index[tx.pocketId] ?: 0), onClick = { onOpenTx(tx.id) },
                )
            }
        }
        item { Spacer(Modifier.height(24.dp)) }
    }
}

@Composable
private fun DailyChart(daily: List<Long>, start: LocalDate, modifier: Modifier) {
    val c = colors
    val producer = remember { CartesianChartModelProducer() }
    LaunchedEffect(daily) {
        val values = daily.ifEmpty { listOf(0L) }
        producer.runTransaction { columnSeries { series(values.map { it.toDouble() }) } }
    }
    val dayFormatter = remember(start) {
        CartesianValueFormatter { _, x, _ -> start.plusDays(x.toLong()).dayOfMonth.toString() }
    }
    CartesianChartHost(
        chart = rememberCartesianChart(
            rememberColumnCartesianLayer(
                ColumnCartesianLayer.ColumnProvider.series(
                    rememberLineComponent(fill = fill(c.ink), thickness = 8.dp),
                ),
            ),
            bottomAxis = HorizontalAxis.rememberBottom(
                valueFormatter = dayFormatter,
                label = rememberTextComponent(color = c.faint),
                line = null,
                tick = null,
                guideline = null,
            ),
        ),
        modelProducer = producer,
        modifier = modifier,
    )
}
