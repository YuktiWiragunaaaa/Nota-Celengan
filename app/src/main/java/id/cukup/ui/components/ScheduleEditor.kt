package id.cukup.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import id.cukup.domain.Frequency
import id.cukup.domain.PayCycle
import id.cukup.domain.Schedule
import id.cukup.ui.theme.Type
import id.cukup.ui.theme.colors
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle

private val dayFmt = DateTimeFormatter.ofPattern("d MMM", Id)
private val monthFmt = DateTimeFormatter.ofPattern("MMMM yyyy", Id)

fun weekdayName(day: Int): String =
    DayOfWeek.of(day.coerceIn(1, 7)).getDisplayName(TextStyle.FULL, Id).replaceFirstChar { it.titlecase(Id) }

/** Kalimat ringkas jadwal: "Tiap Jumat", "Tiap 2 minggu, Jumat", "Tiap tanggal 25", "Tiap 10 hari". */
fun Schedule.describe(): String = when (frequency) {
    Frequency.WEEKLY -> "Tiap ${weekdayName(weekday)}"
    Frequency.BIWEEKLY -> "Tiap 2 minggu, hari ${weekdayName(weekday)}"
    Frequency.MONTHLY -> "Tiap tanggal $monthDay"
    Frequency.DAYS -> "Tiap $days hari"
}

/** Pilih periode gajian sebagai blok tanggal (mulai sampai akhir) di kalender; blok itu berulang. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ScheduleEditor(schedule: Schedule, onChange: (Schedule) -> Unit, modifier: Modifier = Modifier) {
    val c = colors
    val cycle = PayCycle.of(LocalDate.now(), schedule)
    val blockEnd = cycle.nextPayday.minusDays(1)
    var pending by remember { mutableStateOf<LocalDate?>(null) }
    var month by remember { mutableStateOf(YearMonth.from(cycle.start)) }

    fun pick(d: LocalDate) {
        val p = pending
        if (p == null || !d.isAfter(p)) pending = d
        else {
            pending = null
            onChange(Schedule.fromRange(p, d))
        }
    }

    fun shortcut(end: (LocalDate) -> LocalDate) {
        val start = pending ?: cycle.start
        pending = null
        onChange(Schedule.fromRange(start, end(start)))
    }

    Column(modifier.padding(horizontal = Gutter)) {
        Eyebrow("Periode gajian")
        Spacer(Modifier.height(6.dp))
        Text(
            if (pending == null) "Ketuk tanggal mulai, lalu tanggal akhir." else "Sekarang ketuk tanggal akhir.",
            style = Type.bodySmall, color = c.mute,
        )
        Spacer(Modifier.height(12.dp))
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(44.dp).clickable(role = Role.Button) { month = month.minusMonths(1) },
                contentAlignment = Alignment.Center,
            ) { Text("‹", style = Type.strong, color = c.ink) }
            Text(
                month.format(monthFmt).replaceFirstChar { it.titlecase(Id) },
                style = Type.strong, color = c.ink,
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center,
            )
            Box(
                Modifier.size(44.dp).clickable(role = Role.Button) { month = month.plusMonths(1) },
                contentAlignment = Alignment.Center,
            ) { Text("›", style = Type.strong, color = c.ink) }
        }
        Spacer(Modifier.height(6.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            for (d in 1..7) {
                Text(
                    DayOfWeek.of(d).getDisplayName(TextStyle.NARROW, Id).uppercase(Id),
                    style = Type.label, color = c.faint,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                )
            }
        }
        Spacer(Modifier.height(4.dp))
        val lead = month.atDay(1).dayOfWeek.value - 1
        val rows = (lead + month.lengthOfMonth() + 6) / 7
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            for (r in 0 until rows) {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    for (col in 0..6) {
                        val n = r * 7 + col - lead + 1
                        if (n < 1 || n > month.lengthOfMonth()) {
                            Spacer(Modifier.weight(1f).height(40.dp))
                        } else {
                            val date = month.atDay(n)
                            val p = pending
                            val edge = if (p != null) date == p else date == cycle.start || date == blockEnd
                            val inside = p == null && date.isAfter(cycle.start) && date.isBefore(blockEnd)
                            Cell("$n", edge, inside, Modifier.weight(1f)) { pick(date) }
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(16.dp))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Choice("1 minggu", schedule.frequency == Frequency.WEEKLY, { shortcut { it.plusDays(6) } })
            Choice("2 minggu", schedule.frequency == Frequency.BIWEEKLY, { shortcut { it.plusDays(13) } })
            Choice("1 bulan", schedule.frequency == Frequency.MONTHLY, { shortcut { it.plusMonths(1).minusDays(1) } })
        }
        Spacer(Modifier.height(16.dp))
        val every = when (schedule.frequency) {
            Frequency.WEEKLY -> "tiap minggu"
            Frequency.BIWEEKLY -> "tiap 2 minggu"
            Frequency.MONTHLY -> "tiap bulan"
            Frequency.DAYS -> "tiap ${schedule.days} hari"
        }
        Text(
            "${cycle.start.format(dayFmt)} – ${blockEnd.format(dayFmt)}, lalu berulang $every",
            style = Type.bodySmall, color = c.mute,
        )
    }
}

/** Sel tanggal: ujung blok terisi penuh, isi blok lebih pucat. */
@Composable
private fun Cell(text: String, edge: Boolean, inside: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val c = colors
    val bg = when {
        edge -> c.accent
        inside -> c.accent.copy(alpha = 0.18f)
        else -> c.surface
    }
    val shape = RoundedCornerShape(10.dp)
    Box(
        modifier
            .height(40.dp)
            .clip(shape)
            .background(bg)
            .border(1.dp, if (edge) c.accent else if (inside) bg else c.line, shape)
            .clickable(role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, style = Type.bodySmall, color = if (edge) c.onAccent else c.ink)
    }
}
