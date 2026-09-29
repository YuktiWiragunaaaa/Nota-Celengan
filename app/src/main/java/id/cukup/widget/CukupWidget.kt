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
import androidx.glance.text.FontStyle
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
        val summary = runCatching { repo.currentSummary() }.getOrNull()
        val pending = runCatching { repo.pendingCount() }.getOrDefault(0)
        provideContent {
            GlanceTheme {
                WidgetBody(
                    safe = summary?.safeToSpendToday ?: 0,
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
private val inkInverse = ColorProvider(day = Color(0xFFFBFAF7), night = Color(0xFF1F1E1C))

@Composable
private fun WidgetBody(safe: Long, daysLeft: Int, pending: Int, ready: Boolean, addIntent: Intent) {
    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(paper)
            .cornerRadius(20.dp)
            .padding(16.dp)
            .clickable(actionStartActivity<MainActivity>()),
    ) {
        Text("AMAN DIPAKAI HARI INI", style = TextStyle(color = mute, fontSize = 10.sp, fontWeight = FontWeight.Medium))
        Spacer(GlanceModifier.height(6.dp))
        Text(
            if (ready) Rupiah.format(safe) else "Belum diatur",
            style = TextStyle(color = ink, fontSize = 26.sp),
        )
        Spacer(GlanceModifier.defaultWeight())
        Row(modifier = GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                when {
                    pending > 0 -> "$pending perlu dicek"
                    ready -> "$daysLeft hari lagi gajian"
                    else -> "Buka Cukup"
                },
                style = TextStyle(color = mute, fontSize = 12.sp, fontStyle = if (pending > 0) FontStyle.Italic else FontStyle.Normal),
                modifier = GlanceModifier.defaultWeight(),
            )
            Box(
                modifier = GlanceModifier
                    .size(36.dp)
                    .background(ink)
                    .cornerRadius(18.dp)
                    .clickable(
                        actionStartActivity(addIntent),
                    ),
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
