package id.cukup.widget

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.LinearProgressIndicator
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.action.actionStartActivity
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.updateAll
import androidx.glance.background
import androidx.glance.color.ColorProvider
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import id.cukup.MainActivity
import id.cukup.data.MoneyRepository
import id.cukup.domain.Rupiah
import id.cukup.domain.PlanStatus
import id.cukup.domain.Warning
import id.cukup.domain.Transaction
import id.cukup.domain.TxType
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import androidx.glance.LocalSize
import androidx.glance.appwidget.SizeMode
import androidx.glance.layout.fillMaxHeight
import androidx.glance.layout.width
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton
import android.net.Uri
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import android.widget.Toast
import androidx.glance.action.Action
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionParametersOf
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import id.cukup.data.Overview
import id.cukup.domain.QuickPicks
import kotlinx.coroutines.withContext

@EntryPoint
@InstallIn(SingletonComponent::class)
interface WidgetEntryPoint {
    fun repository(): MoneyRepository
}

class CukupWidget : GlanceAppWidget() {

    override val sizeMode: SizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val repo = EntryPointAccessors.fromApplication(context, WidgetEntryPoint::class.java).repository()
        val first = runCatching { repo.current() }.getOrNull()
        provideContent {
            // Ikuti aliran data: sesi Glance bisa hidup lama, jadi nilai sekali-muat akan basi.
            val state = repo.overview.collectAsState(initial = first)
            val o = state.value
            val chips = remember(o) { o?.let { quickChips(context, it) } ?: emptyList() }
            widgetTheme = o?.settings?.theme ?: "DARK"
            // Kunci PIN aktif = saldo tidak ditampilkan di layar utama (bisa dimatikan di Setelan).
            val hidden = o?.settings?.let { it.biometricLock && it.pinHash.isNotEmpty() && it.widgetHide } ?: false
            GlanceTheme {
                WidgetBody(
                    ready = o != null && o.settings.onboarded && o.accounts.isNotEmpty(),
                    total = o?.netWorth ?: 0,
                    spent = o?.totals?.expense ?: 0,
                    period = o?.periodName ?: "ini",
                    plan = o?.planStatus?.takeIf { it.active },
                    pending = o?.pending?.size ?: 0,
                    week = o?.let { lastSevenDays(it.confirmed) } ?: emptyList(),
                    chips = chips,
                    hidden = hidden,
                    addIntent = Intent(context, MainActivity::class.java).putExtra(MainActivity.EXTRA_ADD, true),
                )
            }
        }
    }
}

/** Tombol di widget: [instant] = langsung tercatat, selain itu membuka layar catat. */
private class Chip(val label: String, val action: Action, val instant: Boolean)

private val CategoryKey = ActionParameters.Key<Long>("category")
private val AmountKey = ActionParameters.Key<Long>("amount")
private val AccountKey = ActionParameters.Key<Long>("account")
private val MerchantKey = ActionParameters.Key<String>("merchant")

/**
 * Belanja yang sering diulang jadi tombol sekali ketuk ("☕ 18 rb").
 * Kalau kurang dari tiga, sisanya kategori favorit yang membuka layar catat dengan kategori terpilih.
 */
private fun quickChips(context: Context, o: Overview): List<Chip> {
    val byId = o.categoryById
    val picks = QuickPicks.top(o.transactions, System.currentTimeMillis())
        .filter { p -> o.categories.any { it.id == p.categoryId } }
        .map { p ->
            Chip(
                "${byId[p.categoryId]?.emoji.orEmpty()} ${Rupiah.short(p.amount)}",
                actionRunCallback<QuickLogAction>(
                    actionParametersOf(
                        CategoryKey to p.categoryId,
                        AmountKey to p.amount,
                        AccountKey to (p.accountId ?: 0L),
                        MerchantKey to p.merchant,
                    ),
                ),
                instant = true,
            )
        }
    val used = o.confirmed.filter { it.type == TxType.EXPENSE }.groupingBy { it.categoryId }.eachCount()
    val favorites = o.expenseCategories().sortedByDescending { used[it.id] ?: 0 }
        .filter { c -> picks.none { it.label.startsWith(c.emoji) } }
        .map { c ->
            val intent = Intent(context, MainActivity::class.java)
                .setData(Uri.parse("cukup://add/${c.id}"))
                .putExtra(MainActivity.EXTRA_ADD, true)
                .putExtra(MainActivity.EXTRA_CATEGORY, c.id)
            Chip("${c.emoji} ${c.name}", actionStartActivity(intent), instant = false)
        }
    return (picks + favorites).take(3)
}

/** Mencatat belanja berulang langsung dari widget, tanpa membuka aplikasi. */
class QuickLogAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        val amount = parameters[AmountKey] ?: return
        val category = parameters[CategoryKey] ?: return
        val repo = EntryPointAccessors.fromApplication(context, WidgetEntryPoint::class.java).repository()
        repo.save(
            Transaction(
                type = TxType.EXPENSE,
                amount = amount,
                accountId = parameters[AccountKey]?.takeIf { it > 0 },
                categoryId = category,
                merchant = parameters[MerchantKey].orEmpty(),
                occurredAt = System.currentTimeMillis(),
            ),
        )
        withContext(Dispatchers.Main) {
            Toast.makeText(context, "Tercatat ${Rupiah.format(amount)}", Toast.LENGTH_SHORT).show()
        }
    }
}

/** Tema widget mengikuti setelan aplikasi (DARK bawaan, LIGHT, atau SYSTEM = ikut HP). */
@Volatile private var widgetTheme = "DARK"

private fun pal(day: Long, night: Long) = when (widgetTheme) {
    "LIGHT" -> ColorProvider(day = Color(day), night = Color(day))
    "SYSTEM" -> ColorProvider(day = Color(day), night = Color(night))
    else -> ColorProvider(day = Color(night), night = Color(night))
}

private val paper get() = pal(0xFFF4F1EE, 0xFF16141B)
private val ink get() = pal(0xFF17110E, 0xFFF4F1F6)
private val mute get() = pal(0xFF6F6560, 0xFFA8A2AE)
private val line get() = pal(0xFFEAE4DF, 0xFF29252F)
private val caution get() = pal(0xFFE08A1E, 0xFFD1A263)
private val over get() = pal(0xFFD9392B, 0xFFD9826F)
private val inkInverse get() = pal(0xFFF4F1EE, 0xFF16141B)
private val accent get() = pal(0xFFFFE3D1, 0xFF3A2218)
private val brand get() = pal(0xFFD9481A, 0xFFF2782E)

@Composable
private fun WidgetBody(
    ready: Boolean,
    total: Long,
    spent: Long,
    period: String,
    plan: PlanStatus?,
    pending: Int,
    week: List<Long>,
    chips: List<Chip>,
    hidden: Boolean,
    addIntent: Intent,
) {
    val size = LocalSize.current
    val wide = size.width >= 260.dp && ready
    // Perkiraan lebar tombol dari panjang label (±7dp per huruf + padding); sisakan 44dp untuk tombol +.
    val chipCount = if (!ready) 0 else {
        var room = size.width.value - 32 - 44
        chips.takeWhile { chip -> room -= 30 + 7 * chip.label.length; room >= 0 }.size
    }
    val tall = size.height >= 150.dp
    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(paper)
            .cornerRadius(20.dp)
            .padding(16.dp)
            .clickable(actionStartActivity<MainActivity>()),
    ) {
        Row(modifier = GlanceModifier.fillMaxWidth().defaultWeight()) {
            Summary(ready, total, spent, period, plan.takeIf { (tall || chipCount == 0) && !hidden }, pending, hidden, GlanceModifier.defaultWeight().fillMaxHeight())
            if (wide) {
                Spacer(GlanceModifier.width(16.dp))
                WeekChart(week, hidden, GlanceModifier.defaultWeight().fillMaxHeight())
            }
        }
        Spacer(GlanceModifier.height(8.dp))
        Row(modifier = GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            chips.take(chipCount).forEach { chip ->
                Box(
                    modifier = GlanceModifier
                        .height(36.dp)
                        .background(if (chip.instant) accent else line)
                        .cornerRadius(18.dp)
                        .padding(horizontal = 12.dp)
                        .clickable(chip.action),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(chip.label, style = TextStyle(color = ink, fontSize = 12.sp), maxLines = 1)
                }
                Spacer(GlanceModifier.width(6.dp))
            }
            Spacer(GlanceModifier.defaultWeight())
            Box(
                modifier = GlanceModifier
                    .size(36.dp)
                    .background(ink)
                    .cornerRadius(18.dp)
                    .clickable(actionStartActivity(addIntent)),
                contentAlignment = Alignment.Center,
            ) {
                Text("+", style = TextStyle(color = inkInverse, fontSize = 20.sp))
            }
        }
    }
}

@Composable
private fun Summary(
    ready: Boolean,
    total: Long,
    spent: Long,
    period: String,
    plan: PlanStatus?,
    pending: Int,
    hidden: Boolean,
    modifier: GlanceModifier,
) {
    Column(modifier = modifier) {
        Text("UANGMU", style = TextStyle(color = mute, fontSize = 10.sp, fontWeight = FontWeight.Medium))
        Spacer(GlanceModifier.height(4.dp))
        Text(
            when {
                !ready -> "Buka Cukup dulu"
                hidden -> "Rp•••••"
                kotlin.math.abs(total) >= 100_000_000 -> "Rp" + Rupiah.short(total)
                else -> Rupiah.format(total)
            },
            style = TextStyle(color = ink, fontSize = if (ready) 24.sp else 16.sp),
            maxLines = 1,
        )
        Spacer(GlanceModifier.height(2.dp))
        Text(
            when {
                hidden -> "Buka Cukup untuk lihat saldo"
                pending > 0 -> "$pending perlu dicek · keluar ${Rupiah.short(spent)}"
                else -> "Keluar $period: ${Rupiah.short(spent)}"
            },
            style = TextStyle(color = if (pending > 0) caution else mute, fontSize = 11.sp),
            maxLines = 1,
        )
        if (plan != null) {
            val tone = when (plan.warning) {
                Warning.CALM -> ink
                Warning.NEAR -> caution
                Warning.OVER -> over
            }
            Spacer(GlanceModifier.height(8.dp))
            LinearProgressIndicator(
                progress = (plan.spendUsed.toFloat() / plan.spendLimit.coerceAtLeast(1)).coerceIn(0f, 1f),
                modifier = GlanceModifier.fillMaxWidth().height(6.dp),
                color = tone,
                backgroundColor = line,
            )
            Spacer(GlanceModifier.height(4.dp))
            Text(
                if (plan.spendLeft < 0) "Lewat rencana ${Rupiah.short(-plan.spendLeft)}"
                else "Rencana: aman ${Rupiah.short(plan.perDay)}/hari",
                style = TextStyle(color = tone, fontSize = 11.sp),
                maxLines = 1,
            )
        }
    }
}

/** Total pengeluaran per hari, 7 hari terakhir (paling lama dulu). */
private fun lastSevenDays(txs: List<Transaction>): List<Long> {
    val zone = ZoneId.systemDefault()
    val today = LocalDate.now(zone)
    val sums = LongArray(7)
    txs.filter { it.type == TxType.EXPENSE }.forEach { t ->
        val d = Instant.ofEpochMilli(t.occurredAt).atZone(zone).toLocalDate()
        val back = ChronoUnit.DAYS.between(d, today).toInt()
        if (back in 0..6) sums[6 - back] += t.amount
    }
    return sums.toList()
}

@Composable
private fun WeekChart(week: List<Long>, hidden: Boolean, modifier: GlanceModifier) {
    val max = (week.maxOrNull() ?: 0L).coerceAtLeast(1L)
    val names = listOf("M", "S", "S", "R", "K", "J", "S")
    val today = LocalDate.now()
    Column(modifier = modifier) {
        Text("7 HARI", style = TextStyle(color = mute, fontSize = 10.sp, fontWeight = FontWeight.Medium))
        Text(if (hidden) "Keluar •••" else "Keluar ${Rupiah.short(week.sum())}", style = TextStyle(color = ink, fontSize = 12.sp), maxLines = 1)
        Spacer(GlanceModifier.height(6.dp))
        Row(modifier = GlanceModifier.fillMaxWidth().defaultWeight(), verticalAlignment = Alignment.Bottom) {
            week.forEachIndexed { i, v ->
                Column(modifier = GlanceModifier.defaultWeight().fillMaxHeight(), horizontalAlignment = Alignment.CenterHorizontally, verticalAlignment = Alignment.Bottom) {
                    val h = if (v <= 0) 2 else (4 + 60 * v / max).toInt()
                    Box(modifier = GlanceModifier.width(10.dp).height(h.dp).background(if (i == 6) brand else line).cornerRadius(3.dp)) {}
                    Spacer(GlanceModifier.height(3.dp))
                    Text(names[today.minusDays((6 - i).toLong()).dayOfWeek.value % 7], style = TextStyle(color = mute, fontSize = 9.sp))
                }
            }
        }
    }
}

class CukupWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = CukupWidget()
}

/** Memperbarui semua widget setelah data berubah. */
@Singleton
class WidgetRefresher @Inject constructor(@ApplicationContext private val context: Context) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    fun refresh() {
        scope.launch {
            runCatching { CukupWidget().updateAll(context) }
                .onFailure { android.util.Log.w("CukupWidget", "Gagal memperbarui widget", it) }
        }
    }
}
