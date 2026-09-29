package id.cukup.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

data class Settings(
    val name: String = "",
    val payday: Int = 25,
    val onboarded: Boolean = false,
    val biometricLock: Boolean = false,
    /** Transaksi dari notifikasi langsung tercatat tanpa perlu dicek. */
    val autoConfirm: Boolean = false,
)

private val Context.dataStore by preferencesDataStore("settings")

@Singleton
class SettingsStore @Inject constructor(@ApplicationContext private val context: Context) {

    private object K {
        val name = stringPreferencesKey("name")
        val payday = intPreferencesKey("payday")
        val onboarded = booleanPreferencesKey("onboarded")
        val biometric = booleanPreferencesKey("biometric")
        val autoConfirm = booleanPreferencesKey("auto_confirm")
    }

    val settings: Flow<Settings> = context.dataStore.data.map { p ->
        Settings(
            name = p[K.name] ?: "",
            payday = p[K.payday] ?: 25,
            onboarded = p[K.onboarded] ?: false,
            biometricLock = p[K.biometric] ?: false,
            autoConfirm = p[K.autoConfirm] ?: false,
        )
    }

    suspend fun current(): Settings = settings.first()

    suspend fun update(transform: (Settings) -> Settings) {
        context.dataStore.edit { p ->
            val cur = Settings(
                name = p[K.name] ?: "",
                payday = p[K.payday] ?: 25,
                onboarded = p[K.onboarded] ?: false,
                biometricLock = p[K.biometric] ?: false,
                autoConfirm = p[K.autoConfirm] ?: false,
            )
            val next = transform(cur)
            p[K.name] = next.name
            p[K.payday] = next.payday
            p[K.onboarded] = next.onboarded
            p[K.biometric] = next.biometricLock
            p[K.autoConfirm] = next.autoConfirm
        }
    }
}
