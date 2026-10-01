package id.cukup.data

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import id.cukup.MainActivity
import id.cukup.R
import id.cukup.domain.PayCycle
import id.cukup.domain.PeriodReport
import id.cukup.domain.Reporter
import id.cukup.domain.Rupiah
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton

/** Laporan periode yang baru selesai, dihitung dari catatan. */
fun Overview.previousReport(zone: ZoneId = ZoneId.systemDefault()): PeriodReport {
    val before = PayCycle.of(previous.start.minusDays(1), settings.schedule)
    return Reporter.of(
        transactions, categoryById.values.toList(),
        from = previousStart, to = cycleStart,
        prevFrom = before.start.atStartOfDay(zone).toInstant().toEpochMilli(),
        days = previous.length.coerceAtLeast(1),
    )
}

/**
 * Tiap tanggal gajian: notifikasi ringan berisi pengeluaran terbanyak periode yang baru selesai.
 * Rinciannya (kesimpulan dan saran) ada di layar Laporan. Sekali per periode.
 */
@Singleton
class PaydayReport @Inject constructor(
    @ApplicationContext private val context: Context,
    private val settings: SettingsStore,
    private val repository: MoneyRepository,
) {
    suspend fun check() {
        val s = settings.current()
        if (!s.onboarded) return
        val key = PayCycle.of(LocalDate.now(), s.schedule).start.toString()
        if (s.lastReport == key) return
        settings.update { it.copy(lastReport = key) }
        // Pertama kali dipasang: belum ada periode yang "baru selesai" untuk dilaporkan.
        if (s.lastReport.isEmpty()) return
        val o = repository.current()
        val report = o.previousReport()
        if (report.empty || report.notice.isBlank()) return
        post("Laporan gajian: periode lalu keluar ${Rupiah.short(report.totals.expense)}", report.notice)
    }

    private fun post(title: String, text: String) {
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) return
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(
            NotificationChannel(CHANNEL, "Laporan gajian", NotificationManager.IMPORTANCE_DEFAULT).apply {
                description = "Ringkasan pengeluaran tiap tanggal gajian."
            },
        )
        val open = PendingIntent.getActivity(
            context, ID,
            Intent(context, MainActivity::class.java).putExtra(MainActivity.EXTRA_REPORT, true)
                .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val n = NotificationCompat.Builder(context, CHANNEL)
            .setSmallIcon(R.drawable.ic_notif)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText("$text Ketuk untuk kesimpulan dan saran."))
            .setContentIntent(open)
            .setAutoCancel(true)
            .build()
        runCatching { NotificationManagerCompat.from(context).notify(ID, n) }
    }

    private companion object {
        const val CHANNEL = "payday_report"
        const val ID = 3
    }
}
