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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import id.cukup.domain.Frequency
import id.cukup.domain.PayCycle
import id.cukup.domain.Schedule
import id.cukup.ui.theme.Type
import id.cukup.ui.theme.colors
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.time.temporal.TemporalAdjusters

private val nextFmt = DateTimeFormatter.ofPattern("EEEE, d MMMM", Id)

fun weekdayName(day: Int): String =
    DayOfWeek.of(day.coerceIn(1, 7)).getDisplayName(TextStyle.FULL, Id).replaceFirstChar { it.titlecase(Id) }

/** Kalimat ringkas jadwal: "Tiap Jumat", "Tiap 2 minggu, Jumat", "Tiap tanggal 25". */
fun Schedule.describe(): String = when (frequency) {
    Frequency.WEEKLY -> "Tiap ${weekdayName(weekday)}"
    Frequency.BIWEEKLY -> "Tiap 2 minggu, hari ${weekdayName(weekday)}"
    Frequency.MONTHLY -> "Tiap tanggal $monthDay"
}

/** Pilih seberapa sering menerima uang, lalu harinya. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ScheduleEditor(schedule: Schedule, onChange: (Schedule) -> Unit, modifier: Modifier = Modifier) {
    val c = colors
    Column(modifier.padding(horizontal = Gutter)) {
        Eyebrow("Kamu terima uang")
        Spacer(Modifier.height(10.dp))
        // FlowRow: di dialog yang sempit, pilihan turun ke baris baru alih-alih terpotong.
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Choice("Tiap minggu", schedule.frequency == Frequency.WEEKLY, { onChange(schedule.copy(frequency = Frequency.WEEKLY)) })
            Choice("Tiap 2 minggu", schedule.frequency == Frequency.BIWEEKLY, {
                onChange(schedule.copy(frequency = Frequency.BIWEEKLY, anchor = thisWeekPayday(schedule.weekday)))
            })
            Choice("Tiap bulan", schedule.frequency == Frequency.MONTHLY, { onChange(schedule.copy(frequency = Frequency.MONTHLY)) })
        }
        Spacer(Modifier.height(24.dp))
        if (schedule.frequency == Frequency.MONTHLY) {
            Eyebrow("Tanggal berapa?")
            Spacer(Modifier.height(10.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
                maxItemsInEachRow = 7,
            ) {
                for (d in 1..31) {
                    Cell("$d", d == schedule.monthDay, Modifier.weight(1f)) { onChange(schedule.copy(monthDay = d)) }
                }
                repeat(4) { Spacer(Modifier.weight(1f).height(44.dp)) }
            }
        } else {
            Eyebrow("Hari apa?")
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                for (d in 1..7) {
                    val short = DayOfWeek.of(d).getDisplayName(TextStyle.SHORT, Id).take(3)
                    Cell(short, d == schedule.weekday, Modifier.weight(1f)) {
                        onChange(schedule.copy(weekday = d, anchor = thisWeekPayday(d)))
                    }
                }
            }
        }
        Spacer(Modifier.height(16.dp))
        val next = PayCycle.of(LocalDate.now(), schedule).nextPayday
        Text(
            "Gajian berikutnya: ${next.format(nextFmt)}",
            style = Type.bodySmall, color = c.mute,
        )
    }
}

/** Hari gajian terdekat yang sudah lewat — dipakai sebagai patokan jadwal 2 mingguan. */
private fun thisWeekPayday(weekday: Int): Long =
    LocalDate.now().with(TemporalAdjusters.previousOrSame(DayOfWeek.of(weekday.coerceIn(1, 7)))).toEpochDay()

@Composable
private fun Cell(text: String, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val c = colors
    Box(
        modifier
            .height(44.dp)
            .clip(RoundedCornerShape(2.dp))
            .background(if (selected) c.ink else c.paper)
            .border(1.dp, if (selected) c.ink else c.line, RoundedCornerShape(2.dp))
            .clickable(role = Role.RadioButton, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, style = Type.bodySmall, color = if (selected) c.paper else c.ink)
    }
}
