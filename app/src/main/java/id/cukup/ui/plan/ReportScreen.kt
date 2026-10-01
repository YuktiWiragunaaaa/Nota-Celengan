package id.cukup.ui.plan

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import id.cukup.data.previousReport
import id.cukup.domain.Insight
import id.cukup.domain.Rupiah
import id.cukup.ui.AppViewModel
import id.cukup.ui.components.CardShape
import id.cukup.ui.components.GlassIcon
import id.cukup.ui.components.Gutter
import id.cukup.ui.components.LineButton
import id.cukup.ui.components.Pill
import id.cukup.ui.components.SectionHeader
import id.cukup.ui.components.TopBar
import id.cukup.ui.components.colorOf
import id.cukup.ui.theme.Type
import id.cukup.ui.theme.colors
import java.time.format.DateTimeFormatter
import java.util.Locale

private val dayMonth = DateTimeFormatter.ofPattern("d MMM", Locale.forLanguageTag("id-ID"))

/** Laporan periode gajian yang baru selesai: angka, pengeluaran per kategori, kesimpulan, saran. */
@Composable
fun ReportScreen(onBack: () -> Unit, onOpenPlan: () -> Unit, vm: AppViewModel = hiltViewModel()) {
    val o by vm.overview.collectAsStateWithLifecycle()
    val c = colors
    val data = o ?: return
    val report = remember(data) { data.previousReport() }
    val income = report.totals.income
    val spent = report.totals.expense

    Column(Modifier.fillMaxSize().background(c.paper).systemBarsPadding().verticalScroll(rememberScrollState())) {
        TopBar("Laporan periode lalu", onBack)
        Text(
            "${data.previous.start.format(dayMonth)} – ${data.previous.nextPayday.minusDays(1).format(dayMonth)}",
            style = Type.bodySmall, color = c.mute, modifier = Modifier.padding(horizontal = Gutter),
        )
        if (report.empty) {
            Text(
                "Belum ada catatan di periode itu. Laporan pertama muncul setelah satu periode gajian terlewati dengan catatan.",
                style = Type.body, color = c.mute, modifier = Modifier.padding(Gutter),
            )
            return@Column
        }

        Row(Modifier.padding(horizontal = Gutter).padding(top = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Figure("Masuk", Rupiah.short(income), Modifier.weight(1f))
            Spacer(Modifier.width(8.dp))
            Figure("Keluar", Rupiah.short(spent), Modifier.weight(1f))
            Spacer(Modifier.width(8.dp))
            Figure(if (income >= spent) "Sisa" else "Tekor", Rupiah.short(kotlin.math.abs(income - spent)), Modifier.weight(1f), warn = spent > income)
        }

        SectionHeader("Keluar ke mana")
        val base = if (income > 0) income else spent.coerceAtLeast(1)
        Column(Modifier.padding(horizontal = Gutter).fillMaxWidth().clip(CardShape).background(c.card).padding(16.dp)) {
            Text(
                if (income > 0) "Persen dihitung dari uang masuk ${Rupiah.short(income)}." else "Tidak ada uang masuk tercatat, jadi persen dihitung dari total keluar.",
                style = Type.bodySmall, color = c.faint,
            )
            report.parts.forEach { p ->
                val share = (p.amount.toFloat() / base).coerceIn(0f, 1f)
                Row(Modifier.padding(top = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                    GlassIcon(p.category?.emoji ?: "🧾", colorOf(p.category), size = 34.dp)
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Row {
                            Text(p.category?.name ?: "Tanpa kategori", style = Type.strong, color = c.ink, modifier = Modifier.weight(1f))
                            Text("${Rupiah.short(p.amount)} · ${p.amount * 100 / base}%", style = Type.strong, color = c.ink)
                        }
                        Row(Modifier.padding(top = 6.dp).fillMaxWidth().height(5.dp).clip(Pill).background(c.line)) {
                            Spacer(Modifier.fillMaxWidth(share).height(5.dp).clip(Pill).background(colorOf(p.category)))
                        }
                    }
                }
            }
        }

        if (report.findings.isNotEmpty()) {
            SectionHeader("Kesimpulan")
            report.findings.forEach { f ->
                Row(
                    Modifier.padding(horizontal = Gutter, vertical = 4.dp).fillMaxWidth().clip(CardShape).background(c.card).padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Spacer(
                        Modifier.size(10.dp).clip(CircleShape).background(
                            when (f.tone) {
                                Insight.Tone.GOOD -> c.good
                                Insight.Tone.WARN -> c.caution
                                Insight.Tone.INFO -> c.accent
                            },
                        ),
                    )
                    Spacer(Modifier.width(12.dp))
                    Text(f.text, style = Type.body, color = c.ink)
                }
            }
        }

        if (report.advice.isNotEmpty()) {
            SectionHeader("Saran untuk periode ini")
            report.advice.forEachIndexed { i, a ->
                Row(Modifier.padding(horizontal = Gutter, vertical = 4.dp).fillMaxWidth().clip(CardShape).background(c.accent.copy(alpha = 0.10f)).padding(14.dp)) {
                    Text("${i + 1}.", style = Type.strong, color = c.accent, modifier = Modifier.width(24.dp))
                    Text(a, style = Type.body, color = c.ink)
                }
            }
            LineButton("Pasang batasnya di Rencana", onOpenPlan, Modifier.padding(horizontal = Gutter, vertical = 12.dp).fillMaxWidth(), height = 46.dp)
        }
        Text(
            "Kesimpulan dan saran dihitung di HP ini dari catatanmu sendiri dengan aturan tetap, tanpa internet. Bukan nasihat keuangan profesional.",
            style = Type.bodySmall, color = c.faint, modifier = Modifier.padding(horizontal = Gutter).padding(top = 4.dp, bottom = 28.dp),
        )
    }
}

@Composable
private fun Figure(label: String, value: String, modifier: Modifier, warn: Boolean = false) {
    val c = colors
    Column(modifier.clip(CardShape).background(c.card).padding(14.dp)) {
        Text(label, style = Type.bodySmall, color = c.mute)
        Text(value, style = Type.amount, color = if (warn) c.over else c.ink)
    }
}
