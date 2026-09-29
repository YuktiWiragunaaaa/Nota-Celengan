package id.cukup.notif

import android.app.Notification
import android.content.ComponentName
import android.content.Context
import android.provider.Settings
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import dagger.hilt.android.AndroidEntryPoint
import id.cukup.data.MoneyRepository
import id.cukup.domain.NotificationParser
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Membaca notifikasi dari daftar aplikasi keuangan yang dikenal saja.
 * Teks notifikasi tidak disimpan — hanya hasil bacaannya (nominal, merchant, jenis).
 */
@AndroidEntryPoint
class MoneyNotificationListener : NotificationListenerService() {

    @Inject lateinit var repository: MoneyRepository

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        if (!NotificationParser.isSupported(sbn.packageName)) return
        val extras = sbn.notification.extras
        // Ringkasan grup tidak berisi transaksi.
        if (sbn.notification.flags and Notification.FLAG_GROUP_SUMMARY != 0) return
        val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString()
        val text = extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString()
            ?: extras.getCharSequence(Notification.EXTRA_TEXT)?.toString()
        scope.launch {
            runCatching { repository.ingest(sbn.packageName, title, text, sbn.postTime) }
        }
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    companion object {
        fun isEnabled(context: Context): Boolean {
            val flat = Settings.Secure.getString(context.contentResolver, "enabled_notification_listeners") ?: return false
            val me = ComponentName(context, MoneyNotificationListener::class.java)
            return flat.split(':').any { ComponentName.unflattenFromString(it) == me }
        }
    }
}
