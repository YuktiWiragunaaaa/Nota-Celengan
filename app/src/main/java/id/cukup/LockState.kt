package id.cukup

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * Status kunci aplikasi. Terkunci saat aplikasi baru dibuka (proses baru) dan saat layar HP mati,
 * seperti aplikasi bank. Pindah ke aplikasi lain sebentar tidak mengunci.
 */
object LockState {
    var locked by mutableStateOf(true)

    fun watchScreen(context: Context) {
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
