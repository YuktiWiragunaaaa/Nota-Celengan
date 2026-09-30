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
import id.cukup.domain.PlanStatus
import id.cukup.domain.Warning
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Notifikasi pop-up (heads-up) soal rencana belanja:
 * - 80% batas rencana terpakai, dan saat batas terlewati (masing-masing sekali per periode);
 * - satu kali belanja di atas batas yang diatur pengguna.
 */
@Singleton
class BudgetAlerts @Inject constructor(
    @ApplicationContext private val context: Context,
    private val settings: SettingsStore,
) {
    suspend fun check(plan: PlanStatus) {
        val s = settings.current()
        if (!s.budgetAlerts || !s.onboarded || !plan.active || plan.spendLimit <= 0) return
        val level = plan.warning
        if (level == Warning.CALM) return

        val period = PayCycle.of(LocalDate.now(), s.schedule).start.toString()
        val key = "$period:${level.name}"
        if (s.lastAlert == key || (level == Warning.NEAR && s.lastAlert == "$period:${Warning.OVER.name}")) return
        settings.update { it.copy(lastAlert = key) }

        val period_ = s.schedule.periodName
        when (level) {
            Warning.NEAR -> post(
                ID_BUDGET,
                "Batas belanja $period_ tinggal ${Rupiah.format(plan.spendLeft)}",
                "Sudah kepakai ${Rupiah.format(plan.spendUsed)} dari rencana ${Rupiah.format(plan.spendLimit)}. Pelan-pelan dulu ya.",
            )
            else -> post(
                ID_BUDGET,
                "Belanja $period_ sudah lewat rencana",
                "Kelebihan ${Rupiah.format(-plan.spendLeft)}. Kalau bisa, tahan dulu sampai gajian.",
            )
        }
    }

    /** Dipanggil untuk belanja yang tercatat otomatis dari notifikasi. */
    suspend fun checkSingle(amount: Long, merchant: String) {
        val s = settings.current()
        if (!s.budgetAlerts || s.singleLimit <= 0 || amount <= s.singleLimit) return
        val where = if (merchant.isBlank()) "" else " di $merchant"
        post(
            ID_SINGLE,
            "Belanja besar: ${Rupiah.format(amount)}$where",
            "Di atas batas sekali belanjamu (${Rupiah.format(s.singleLimit)}). Cuma mengingatkan.",
        )
    }

    private fun post(id: Int, title: String, text: String) {
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) return
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.deleteNotificationChannel(OLD_CHANNEL)
        manager.createNotificationChannel(
            // IMPORTANCE_HIGH = muncul sebagai pop-up di atas layar.
            NotificationChannel(CHANNEL, "Peringatan belanja", NotificationManager.IMPORTANCE_HIGH).apply {
                description = "Muncul saat jatah hampir habis, sudah habis, atau ada belanja besar."
            },
        )
        val open = PendingIntent.getActivity(
            context, id, Intent(context, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE,
        )
        val n = NotificationCompat.Builder(context, CHANNEL)
            .setSmallIcon(R.drawable.ic_notif)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setContentIntent(open)
            .setAutoCancel(true)
            .build()
        runCatching { NotificationManagerCompat.from(context).notify(id, n) }
    }

    private companion object {
        const val OLD_CHANNEL = "budget"
        const val CHANNEL = "budget_alerts"
        const val ID_BUDGET = 1
        const val ID_SINGLE = 2
    }
}
