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
        }
    }
}
