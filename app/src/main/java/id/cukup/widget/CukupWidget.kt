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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
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
        val o = runCatching { repo.current() }.getOrNull()
        provideContent {
            GlanceTheme {
                WidgetBody(
                    ready = o != null && o.settings.onboarded && o.accounts.isNotEmpty(),
                    total = o?.netWorth ?: 0,
                    spent = o?.totals?.expense ?: 0,
                    period = o?.periodName ?: "ini",
                    plan = o?.planStatus?.takeIf { it.active },
                    pending = o?.pending?.size ?: 0,
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
    ready: Boolean,
    total: Long,
    spent: Long,
    period: String,
    plan: PlanStatus?,
    pending: Int,
    addIntent: Intent,
) {
    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(paper)
            .cornerRadius(20.dp)
            .padding(16.dp)
            .clickable(actionStartActivity<MainActivity>()),
    ) {
        Text("UANGMU", style = TextStyle(color = mute, fontSize = 10.sp, fontWeight = FontWeight.Medium))
        Spacer(GlanceModifier.height(4.dp))
        Text(
            if (ready) Rupiah.format(total) else "Buka Cukup dulu",
            style = TextStyle(color = ink, fontSize = if (ready) 24.sp else 16.sp),
            maxLines = 1,
        )
        Spacer(GlanceModifier.height(4.dp))
        Text("Keluar $period: ${Rupiah.short(spent)}", style = TextStyle(color = mute, fontSize = 11.sp))
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
            )
        }
        Spacer(GlanceModifier.defaultWeight())
        Row(modifier = GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                if (pending > 0) "$pending perlu dicek" else "Catat",
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
