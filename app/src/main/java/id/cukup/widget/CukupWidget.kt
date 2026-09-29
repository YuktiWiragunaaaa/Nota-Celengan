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
import id.cukup.domain.Warning
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@EntryPoint
@InstallIn(SingletonComponent::class)
interface WidgetEntryPoint {
    fun repository(): MoneyRepository
}

class CukupWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val repo = EntryPointAccessors.fromApplication(context, WidgetEntryPoint::class.java).repository()
        val summary = runCatching { repo.currentSummary() }.getOrNull()
        val pending = runCatching { repo.pendingCount() }.getOrDefault(0)
        val period = runCatching { repo.settings.first().schedule.periodName }.getOrDefault("ini")
        provideContent {
            GlanceTheme {
                WidgetBody(
                    left = summary?.budgetLeft ?: 0,
                    used = summary?.budgetUsed ?: 0,
                    budget = summary?.budget ?: 0,
                    period = period,
                    daysLeft = summary?.daysLeft ?: 0,
                    pending = pending,
                    ready = summary != null && summary.pockets.isNotEmpty(),
                    addIntent = Intent(context, MainActivity::class.java).putExtra(MainActivity.EXTRA_ADD, true),
                )
            }
        }
    }
}

private val paper = ColorProvider(day = Color(0xFFFBFAF7), night = Color(0xFF1F1E1C))
private val ink = ColorProvider(day = Color(0xFF1F1E1C), night = Color(0xFFF3F1EC))
private val mute = ColorProvider(day = Color(0xFF77756F), night = Color(0xFFB9B6AE))
private val line = ColorProvider(day = Color(0xFFE6E4DE), night = Color(0xFF34322E))
private val caution = ColorProvider(day = Color(0xFF9A6B2F), night = Color(0xFFD1A263))
private val over = ColorProvider(day = Color(0xFFA3402F), night = Color(0xFFD9826F))
private val inkInverse = ColorProvider(day = Color(0xFFFBFAF7), night = Color(0xFF1F1E1C))

@Composable
private fun WidgetBody(
    left: Long,
    used: Long,
    budget: Long,
    period: String,
    daysLeft: Int,
    pending: Int,
    ready: Boolean,
    addIntent: Intent,
) {
    val warning = Warning.of(used, budget)
    val tone = when (warning) {
        Warning.CALM -> ink
        Warning.NEAR -> caution
        Warning.OVER -> over
    }
    val ratio = if (budget > 0) (used.toFloat() / budget).coerceIn(0f, 1f) else 0f
    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(paper)
            .cornerRadius(20.dp)
            .padding(16.dp)
            .clickable(actionStartActivity<MainActivity>()),
    ) {
        Text("JATAH ${period.uppercase()}", style = TextStyle(color = mute, fontSize = 10.sp, fontWeight = FontWeight.Medium))
        Spacer(GlanceModifier.height(6.dp))
        Text(
            when {
                !ready -> "Belum diatur"
                budget <= 0 && used == 0L -> "Belum ada uang masuk"
                left < 0 -> "Lewat ${Rupiah.format(-left)}"
                else -> "${Rupiah.format(left)} lagi"
            },
            style = TextStyle(color = tone, fontSize = if (budget > 0 || used > 0) 24.sp else 16.sp),
            maxLines = 1,
        )
        if (budget > 0) {
            Spacer(GlanceModifier.height(10.dp))
            LinearProgressIndicator(
                progress = ratio,
                modifier = GlanceModifier.fillMaxWidth().height(6.dp),
                color = tone,
                backgroundColor = line,
            )
            Spacer(GlanceModifier.height(6.dp))
            Text(
                "Kepakai ${Rupiah.short(used)} dari ${Rupiah.short(budget)}",
                style = TextStyle(color = mute, fontSize = 11.sp),
            )
        }
        Spacer(GlanceModifier.defaultWeight())
        Row(modifier = GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                when {
                    pending > 0 -> "$pending transaksi perlu dicek"
                    !ready -> "Buka Cukup dulu"
                    daysLeft == 1 -> "Besok gajian"
                    else -> "Gajian $daysLeft hari lagi"
                },
                style = TextStyle(color = if (pending > 0) caution else mute, fontSize = 12.sp),
                modifier = GlanceModifier.defaultWeight(),
            )
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

class CukupWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = CukupWidget()
}

/** Memperbarui semua widget setelah data berubah. */
@Singleton
class WidgetRefresher @Inject constructor(@ApplicationContext private val context: Context) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    fun refresh() {
        scope.launch { runCatching { CukupWidget().updateAll(context) } }
    }
}
