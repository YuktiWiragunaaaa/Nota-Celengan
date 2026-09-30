package id.cukup

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import id.cukup.domain.PinCode

/**
 * Status kunci aplikasi. Terkunci saat aplikasi baru dibuka (proses baru) dan saat layar HP mati,
 * seperti aplikasi bank. Pindah ke aplikasi lain sebentar tidak mengunci.
 * Jumlah salah & waktu tunggu disimpan, jadi menutup paksa aplikasi tidak mereset batas percobaan.
 */
object LockState {
    var locked by mutableStateOf(true)
    /** PIN salah berturut-turut. */
    var wrong by mutableStateOf(0)
        private set
    /** Waktu (ms) sampai boleh coba lagi setelah terlalu sering salah. */
    var waitUntil by mutableStateOf(0L)
        private set

    private var prefs: SharedPreferences? = null

    fun unlock() {
        locked = false
        wrong = 0
        save()
    }

    fun fail() {
        wrong++
        if (wrong >= PinCode.MAX_TRIES) {
            wrong = 0
            waitUntil = System.currentTimeMillis() + PinCode.COOLDOWN_MS
        }
        save()
    }

    private fun save() {
        prefs?.edit()?.putInt("wrong", wrong)?.putLong("wait_until", waitUntil)?.apply()
    }

    fun init(context: Context) {
        val p = context.getSharedPreferences("lock", Context.MODE_PRIVATE)
        prefs = p
        wrong = p.getInt("wrong", 0)
        waitUntil = p.getLong("wait_until", 0L)
        androidx.core.content.ContextCompat.registerReceiver(
            context,
            object : BroadcastReceiver() {
                override fun onReceive(c: Context, intent: Intent) {
                    if (intent.action == Intent.ACTION_SCREEN_OFF) locked = true
                }
            },
            IntentFilter(Intent.ACTION_SCREEN_OFF),
            androidx.core.content.ContextCompat.RECEIVER_NOT_EXPORTED,
        )
    }
}
