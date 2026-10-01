package id.cukup

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import id.cukup.data.Backup
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltAndroidApp
class CukupApp : Application() {

    @Inject lateinit var backup: Backup
    @Inject lateinit var paydayReport: id.cukup.data.PaydayReport
    @Inject lateinit var repository: id.cukup.data.MoneyRepository

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        LockState.init(this)
        // Cadangan harian otomatis, sedikit ditunda supaya tidak memperlambat saat aplikasi dibuka.
        scope.launch {
            delay(5_000)
            runCatching { backup.autoBackup() }
                .onFailure { android.util.Log.w("Cukup", "Cadangan otomatis gagal", it) }
            runCatching { paydayReport.check() }
            // Perapian satu kali: catatan lama dari notifikasi dipindah ke dompet bank/e-wallet masing-masing.
            runCatching {
                val prefs = getSharedPreferences("one_time", MODE_PRIVATE)
                if (!prefs.getBoolean("brand_wallets_v1", false)) {
                    backup.safetyCopy("rapikan-dompet")
                    val moved = repository.tidyBrandWallets()
                    prefs.edit().putBoolean("brand_wallets_v1", true).apply()
                    android.util.Log.i("Cukup", "Perapian dompet: $moved catatan dipindah")
                }
            }.onFailure { android.util.Log.w("Cukup", "Perapian dompet gagal", it) }
        }
    }
}
