package id.cukup.data

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.android.qualifiers.ApplicationContext
import id.cukup.MainActivity
import id.cukup.R
import id.cukup.widget.WidgetEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Notifikasi kecil setelah Cukup mencatat otomatis dari notifikasi bank/e-wallet,
 * dengan tombol Batalkan (atau "Bukan pindah" untuk Pindah yang digabung otomatis).
 */
@Singleton
class RecordedNotice @Inject constructor(@ApplicationContext private val context: Context) {

    enum class Undo { DELETE, SPLIT }

    fun post(txId: Long, title: String, text: String, undo: Undo) {
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) return
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(
            NotificationChannel(CHANNEL, "Tercatat otomatis", NotificationManager.IMPORTANCE_LOW).apply {
                description = "Konfirmasi singkat tiap kali notifikasi bank/e-wallet dicatat, dengan tombol Batalkan."
            },
        )
        val id = notificationId(txId)
        val open = PendingIntent.getActivity(context, id, Intent(context, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE)
        val undoIntent = PendingIntent.getBroadcast(
            context, id,
            Intent(context, UndoReceiver::class.java).putExtra(EXTRA_TX, txId).putExtra(EXTRA_MODE, undo.name),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val n = NotificationCompat.Builder(context, CHANNEL)
            .setSmallIcon(R.drawable.ic_notif)
            .setContentTitle(title)
            .setContentText(text)
            .setContentIntent(open)
            .addAction(0, if (undo == Undo.SPLIT) "Bukan pindah" else "Batalkan", undoIntent)
            .setAutoCancel(true)
            .setTimeoutAfter(TIMEOUT_MS)
            .setOnlyAlertOnce(true)
            .build()
        runCatching { NotificationManagerCompat.from(context).notify(id, n) }
    }

    companion object {
        private const val CHANNEL = "recorded"
        private const val TIMEOUT_MS = 30 * 60_000L
        const val EXTRA_TX = "tx"
        const val EXTRA_MODE = "mode"
        fun notificationId(txId: Long) = 10_000 + (txId % 100_000).toInt()
    }
}

/** Menjalankan tombol Batalkan / Bukan pindah dari notifikasi "Tercatat". */
class UndoReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val tx = intent.getLongExtra(RecordedNotice.EXTRA_TX, 0).takeIf { it > 0 } ?: return
        val mode = runCatching { RecordedNotice.Undo.valueOf(intent.getStringExtra(RecordedNotice.EXTRA_MODE).orEmpty()) }
            .getOrDefault(RecordedNotice.Undo.DELETE)
        val repo = EntryPointAccessors.fromApplication(context, WidgetEntryPoint::class.java).repository()
        val pending = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                if (mode == RecordedNotice.Undo.SPLIT) repo.splitTransfer(tx) else repo.dismiss(tx)
                NotificationManagerCompat.from(context).cancel(RecordedNotice.notificationId(tx))
            } finally {
                pending.finish()
            }
        }
    }
}
