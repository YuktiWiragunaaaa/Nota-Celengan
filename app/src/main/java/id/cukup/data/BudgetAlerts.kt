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
import id.cukup.domain.Rupiah
import id.cukup.domain.Summary
import id.cukup.domain.Warning
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Mengirim notifikasi saat jatah belanja periode ini mencapai 80% dan saat terlewati.
 * Tiap tingkat hanya dikirim sekali per periode.
 */
@Singleton
class BudgetAlerts @Inject constructor(
    @ApplicationContext private val context: Context,
    private val settings: SettingsStore,
) {
    suspend fun check(summary: Summary) {
        val s = settings.current()
        if (!s.budgetAlerts || !s.onboarded || summary.budget <= 0) return
        val level = summary.warning
        if (level == Warning.CALM) return

        val period = PayCycle.of(LocalDate.now(), s.schedule).start.toString()
        val key = "$period:${level.name}"
        // Sudah dikirim untuk tingkat ini (atau lebih tinggi) di periode yang sama.
        if (s.lastAlert == key || (level == Warning.NEAR && s.lastAlert == "$period:${Warning.OVER.name}")) return
        settings.update { it.copy(lastAlert = key) }

        val name = s.schedule.periodName
        val (title, text) = when (level) {
            Warning.NEAR -> "Jatah $name tinggal sedikit" to
                "Sudah terpakai ${Rupiah.format(summary.budgetUsed)} dari ${Rupiah.format(summary.budget)}. Sisa ${Rupiah.format(summary.budgetLeft)}."
            else -> "Jatah $name sudah lewat" to
                "Kamu belanja ${Rupiah.format(-summary.budgetLeft)} lebih dari jatah ${Rupiah.format(summary.budget)}."
        }
        post(title, text)
    }

    private fun post(title: String, text: String) {
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) return
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL, "Peringatan jatah", NotificationManager.IMPORTANCE_DEFAULT).apply {
                description = "Saat belanja hampir atau sudah melewati jatah."
            },
        )
        val open = PendingIntent.getActivity(
            context, 0, Intent(context, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE,
        )
        val n = NotificationCompat.Builder(context, CHANNEL)
            .setSmallIcon(R.drawable.ic_notif)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setContentIntent(open)
            .setAutoCancel(true)
            .build()
        runCatching { NotificationManagerCompat.from(context).notify(ID, n) }
    }

    private companion object {
        const val CHANNEL = "budget"
        const val ID = 1
    }
}
