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
import id.cukup.domain.PlanBasis
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
    /** "garam:hash" PIN aplikasi; kosong = belum diatur. */
    val pinHash: String = "",
    /** Boleh buka pakai sidik jari selain PIN. */
    val fingerprint: Boolean = true,
    /** Transaksi dari notifikasi langsung tercatat tanpa perlu dicek. */
    val autoConfirm: Boolean = true,
    /** Aplikasi notifikasi → dompet, dipelajari dari koreksi pengguna. */
    val appLinks: Map<String, Long> = emptyMap(),
    /** Kirim notifikasi saat jatah belanja hampir / sudah habis. */
    val budgetAlerts: Boolean = true,
    /** Tanya dulu kalau satu kali belanja di atas nominal ini. 0 = mati. */
    val singleLimit: Long = 0,
    /** Dari mana rencana menghitung uang masuk yang dibagi ke pos. */
    val planBasis: PlanBasis = PlanBasis(),
    /** Dompet yang dipilih otomatis saat mencatat. */
    val defaultAccountId: Long = 0,
    /** Grafik di beranda: DONUT, BAR, atau BUBBLE. */
    val chart: String = "DONUT",
    /** Tema: DARK (bawaan), LIGHT, atau SYSTEM. */
    val theme: String = "DARK",
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
        val pinHash = stringPreferencesKey("pin_hash")
        val fingerprint = booleanPreferencesKey("fingerprint")
        // v0.8: catat otomatis jadi bawaan; kunci baru supaya nilai lama (mati) tidak terbawa.
        val autoConfirm = booleanPreferencesKey("auto_confirm_v2")
        val appLinks = stringPreferencesKey("app_links")
        val budgetAlerts = booleanPreferencesKey("budget_alerts")
        val lastAlert = stringPreferencesKey("last_alert")
        val singleLimit = longPreferencesKey("single_limit")
        val basisMode = stringPreferencesKey("plan_basis_mode")
        val basisAmount = longPreferencesKey("plan_basis_amount")
        val defaultAccount = longPreferencesKey("default_account")
        val dataVersion = intPreferencesKey("data_version")
        val chart = stringPreferencesKey("chart")
        val theme = stringPreferencesKey("theme")
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
        // Data sebelum v0.6 dikosongkan (lihat CukupDatabase), jadi pengenalan diulang.
        onboarded = (p[K.onboarded] ?: false) && (p[K.dataVersion] ?: 0) >= DATA_VERSION,
        biometricLock = p[K.biometric] ?: false,
        pinHash = p[K.pinHash] ?: "",
        fingerprint = p[K.fingerprint] ?: true,
        autoConfirm = p[K.autoConfirm] ?: true,
        appLinks = p[K.appLinks].orEmpty().split(';').mapNotNull { e ->
            val (pkg, id) = e.split('=').takeIf { it.size == 2 } ?: return@mapNotNull null
            id.toLongOrNull()?.let { pkg to it }
        }.toMap(),
        budgetAlerts = p[K.budgetAlerts] ?: true,
        lastAlert = p[K.lastAlert] ?: "",
        singleLimit = p[K.singleLimit] ?: 0,
        planBasis = PlanBasis(
            mode = p[K.basisMode]?.let { runCatching { PlanBasis.Mode.valueOf(it) }.getOrNull() } ?: PlanBasis.Mode.FIXED,
            fixedAmount = p[K.basisAmount] ?: 0,
        ),
        defaultAccountId = p[K.defaultAccount] ?: 0,
        chart = p[K.chart] ?: "DONUT",
        theme = p[K.theme] ?: "DARK",
        avatarVersion = p[K.avatarVersion] ?: 0,
    )

    private companion object {
        const val DATA_VERSION = 4
    }

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
            p[K.pinHash] = next.pinHash
            p[K.fingerprint] = next.fingerprint
            p[K.autoConfirm] = next.autoConfirm
            p[K.appLinks] = next.appLinks.entries.joinToString(";") { "${it.key}=${it.value}" }
            p[K.budgetAlerts] = next.budgetAlerts
            p[K.lastAlert] = next.lastAlert
            p[K.singleLimit] = next.singleLimit
            p[K.basisMode] = next.planBasis.mode.name
            p[K.basisAmount] = next.planBasis.fixedAmount
            p[K.defaultAccount] = next.defaultAccountId
            p[K.dataVersion] = DATA_VERSION
            p[K.chart] = next.chart
            p[K.theme] = next.theme
            p[K.avatarVersion] = next.avatarVersion
        }
    }
}
