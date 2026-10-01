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
        }
    }
}
