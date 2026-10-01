package id.cukup.notif

import android.app.Notification
import android.content.ComponentName
import android.content.Context
import android.os.SystemClock
import android.provider.Settings
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import dagger.hilt.android.AndroidEntryPoint
import id.cukup.data.MoneyRepository
import id.cukup.data.NoticeLog
import id.cukup.domain.NotificationParser
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Membaca notifikasi dari daftar aplikasi keuangan yang dikenal saja.
 * Yang disimpan: hasil bacaannya, plus teks [NoticeLog.KEEP] notifikasi terakhir untuk layar "Catat otomatis".
 */
@AndroidEntryPoint
class MoneyNotificationListener : NotificationListenerService() {

    @Inject lateinit var repository: MoneyRepository
    @Inject lateinit var log: NoticeLog
    @Inject lateinit var paydayReport: id.cukup.data.PaydayReport

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var beat: Job? = null
    private var lastBeat = 0L
    /** Sedang diproses, supaya notifikasi yang diperbarui beruntun tidak dibaca dua kali. */
    private val working = java.util.concurrent.ConcurrentHashMap.newKeySet<String>()

    override fun onListenerConnected() {
        val now = System.currentTimeMillis()
        log.connected(now, freshProcess = !connectedBefore, bootedAt = now - SystemClock.elapsedRealtime())
        connectedBefore = true
        beat?.cancel()
        beat = scope.launch {
            while (isActive) {
                delay(NoticeLog.BEAT_MS)
                log.beat(System.currentTimeMillis())
                // Selagi pembaca hidup, sekalian periksa apakah sudah tanggal gajian.
                runCatching { paydayReport.check() }
            }
        }
        // Susul notifikasi yang datang selagi Cukup dimatikan sistem dan masih ada di panel notifikasi.
        val waiting = runCatching { activeNotifications }.getOrNull().orEmpty().sortedBy { it.postTime }
        waiting.forEach(::read)
    }

    override fun onListenerDisconnected() {
        beat?.cancel()
        log.disconnected()
        // Minta disambung lagi; tanpa ini sebagian HP membiarkannya putus sampai aplikasi dibuka.
        runCatching { requestRebind(ComponentName(this, MoneyNotificationListener::class.java)) }
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        // Notifikasi apa pun membuktikan pembaca masih hidup, juga saat HP tidur dan denyut berkala tertunda.
        val now = System.currentTimeMillis()
        if (now - lastBeat > 60_000) {
            lastBeat = now
            log.beat(now)
        }
        read(sbn)
    }

    private fun read(sbn: StatusBarNotification) {
        if (!NotificationParser.isSupported(sbn.packageName)) return
        val extras = sbn.notification.extras
        // Ringkasan grup tidak berisi transaksi.
        if (sbn.notification.flags and Notification.FLAG_GROUP_SUMMARY != 0) return
        val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString()
        val text = extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString()
            ?: extras.getCharSequence(Notification.EXTRA_TEXT)?.toString()
        // Notifikasi yang sama bisa datang lagi (diperbarui, atau saat menyusul): proses sekali saja.
        val key = "${sbn.key}|${sbn.postTime}|${text.hashCode()}"
        val packageName = sbn.packageName
        val postedAt = sbn.postTime
        scope.launch {
            if (log.seen(key) || !working.add(key)) return@launch
            runCatching { repository.ingest(packageName, title, text, postedAt) }
                // Baru ditandai selesai kalau memang tersimpan/terbaca; kalau gagal, dicoba lagi saat menyusul.
                .onSuccess { log.markSeen(key) }
                .onFailure {
                    log.add(NoticeLog.Entry(postedAt, packageName, listOfNotNull(title, text).joinToString(" · ").take(240), NoticeLog.Result.SKIPPED, "Gagal disimpan: ${it.message ?: it.javaClass.simpleName}"))
                }
            working.remove(key)
        }
    }

    override fun onDestroy() {
        log.disconnected()
        scope.cancel()
        super.onDestroy()
    }

    companion object {
        /** False sampai pembaca pertama kali tersambung di proses ini. */
        @Volatile private var connectedBefore = false

        fun isEnabled(context: Context): Boolean {
            val flat = Settings.Secure.getString(context.contentResolver, "enabled_notification_listeners") ?: return false
            val me = ComponentName(context, MoneyNotificationListener::class.java)
            return flat.split(':').any { ComponentName.unflattenFromString(it) == me }
        }
    }
}
