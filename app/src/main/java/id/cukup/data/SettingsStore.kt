package id.cukup.data

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import id.cukup.domain.BudgetRule
import id.cukup.domain.Frequency
import id.cukup.domain.Schedule
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

data class Settings(
    val name: String = "",
    val schedule: Schedule = Schedule(),
    val onboarded: Boolean = false,
    val biometricLock: Boolean = false,
    /** Transaksi dari notifikasi langsung tercatat tanpa perlu dicek. */
    val autoConfirm: Boolean = false,
    /** Kirim notifikasi saat jatah belanja hampir / sudah habis. */
    val budgetAlerts: Boolean = true,
    /** Tanya dulu kalau satu kali belanja di atas nominal ini. 0 = mati. */
    val singleLimit: Long = 0,
    /** Cara hitung jatah belanja. */
    val budgetRule: BudgetRule = BudgetRule(),
    /** Grafik di beranda: DONUT, BAR, atau BUBBLE. */
    val chart: String = "DONUT",
    /** Versi foto profil (0 = belum ada). Dipakai agar gambar dimuat ulang saat diganti. */
    val avatarVersion: Long = 0,
    /** Peringatan terakhir yang sudah dikirim: "<awal periode>:<tingkat>", agar tidak berulang. */
    val lastAlert: String = "",
)

private val Context.dataStore by preferencesDataStore("settings")

@Singleton
class SettingsStore @Inject constructor(@ApplicationContext private val context: Context) {

    private object K {
        val name = stringPreferencesKey("name")
        val payday = intPreferencesKey("payday")
        val frequency = stringPreferencesKey("frequency")
        val weekday = intPreferencesKey("weekday")
        val anchor = longPreferencesKey("anchor")
        val onboarded = booleanPreferencesKey("onboarded")
        val biometric = booleanPreferencesKey("biometric")
        val autoConfirm = booleanPreferencesKey("auto_confirm")
        val budgetAlerts = booleanPreferencesKey("budget_alerts")
        val lastAlert = stringPreferencesKey("last_alert")
        val singleLimit = longPreferencesKey("single_limit")
        val budgetMode = stringPreferencesKey("budget_mode")
        val budgetPercent = intPreferencesKey("budget_percent")
        val chart = stringPreferencesKey("chart")
        val avatarVersion = longPreferencesKey("avatar_version")
    }

    private fun read(p: Preferences) = Settings(
        name = p[K.name] ?: "",
        schedule = Schedule(
            frequency = p[K.frequency]?.let { runCatching { Frequency.valueOf(it) }.getOrNull() } ?: Frequency.MONTHLY,
            monthDay = p[K.payday] ?: 25,
            weekday = p[K.weekday] ?: 5,
            anchor = p[K.anchor] ?: 0,
        ),
        onboarded = p[K.onboarded] ?: false,
        biometricLock = p[K.biometric] ?: false,
        autoConfirm = p[K.autoConfirm] ?: false,
        budgetAlerts = p[K.budgetAlerts] ?: true,
        lastAlert = p[K.lastAlert] ?: "",
        singleLimit = p[K.singleLimit] ?: 0,
        budgetRule = BudgetRule(
            mode = p[K.budgetMode]?.let { runCatching { BudgetRule.Mode.valueOf(it) }.getOrNull() } ?: BudgetRule.Mode.POCKETS,
            percent = p[K.budgetPercent] ?: 50,
        ),
        chart = p[K.chart] ?: "DONUT",
        avatarVersion = p[K.avatarVersion] ?: 0,
    )

    val settings: Flow<Settings> = context.dataStore.data.map(::read)

    suspend fun current(): Settings = settings.first()

    suspend fun update(transform: (Settings) -> Settings) {
        context.dataStore.edit { p ->
            val next = transform(read(p))
            p[K.name] = next.name
            p[K.frequency] = next.schedule.frequency.name
            p[K.payday] = next.schedule.monthDay
            p[K.weekday] = next.schedule.weekday
            p[K.anchor] = next.schedule.anchor
            p[K.onboarded] = next.onboarded
            p[K.biometric] = next.biometricLock
            p[K.autoConfirm] = next.autoConfirm
            p[K.budgetAlerts] = next.budgetAlerts
            p[K.lastAlert] = next.lastAlert
            p[K.singleLimit] = next.singleLimit
            p[K.budgetMode] = next.budgetRule.mode.name
            p[K.budgetPercent] = next.budgetRule.percent
            p[K.chart] = next.chart
            p[K.avatarVersion] = next.avatarVersion
        }
    }
}
