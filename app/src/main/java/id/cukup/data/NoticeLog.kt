package id.cukup.data

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Catatan kerja "Catat otomatis": notifikasi keuangan terakhir beserta hasil bacaannya,
 * dan kapan pembaca notifikasi sempat dimatikan sistem. Hanya di perangkat, tidak ikut cadangan,
 * dan hanya [KEEP] notifikasi terakhir yang disimpan.
 */
@Singleton
class NoticeLog @Inject constructor(@ApplicationContext context: Context) {

    enum class Result { RECORDED, PENDING, SKIPPED }

    data class Entry(val at: Long, val app: String, val text: String, val result: Result, val detail: String)

    /** Pembaca notifikasi mati dari [from] sampai [to]; notifikasi di antaranya bisa terlewat. */
    data class Gap(val from: Long, val to: Long)

    data class State(val entries: List<Entry> = emptyList(), val gap: Gap? = null, val connected: Boolean = false)

    private val file = File(context.filesDir, "notice-log.json")
    private val prefs = context.getSharedPreferences("notice_health", Context.MODE_PRIVATE)
    private val _state = MutableStateFlow(State(load(), loadGap()))
    val state: StateFlow<State> = _state

    @Synchronized
    fun add(entry: Entry) {
        var skipped = 0
        // "Dilewati" (promo dsb.) dibatasi supaya tidak mengusir hasil yang penting dari daftar.
        val next = (listOf(entry) + _state.value.entries)
            .filter { it.result != Result.SKIPPED || ++skipped <= KEEP_SKIPPED }
            .take(KEEP)
        _state.value = _state.value.copy(entries = next)
        runCatching {
            file.writeText(
                JSONArray(next.map {
                    JSONObject().put("at", it.at).put("app", it.app).put("text", it.text).put("result", it.result.name).put("detail", it.detail)
                }).toString(),
            )
        }
    }

    @Synchronized
    fun clear() {
        file.delete()
        _state.value = _state.value.copy(entries = emptyList())
    }

    /** True bila notifikasi ini sudah selesai diproses (dipakai saat menyusul notifikasi yang masih tampil). */
    @Synchronized
    fun seen(key: String): Boolean = key in seenKeys()

    @Synchronized
    fun markSeen(key: String) {
        val seen = seenKeys()
        if (key !in seen) prefs.edit().putString("seen", (seen + key).takeLast(SEEN).joinToString("\n")).apply()
    }

    private fun seenKeys() = prefs.getString("seen", "").orEmpty().split('\n').filter { it.isNotEmpty() }

    /** Dipanggil berkala selama pembaca notifikasi hidup. */
    fun beat(now: Long) = prefs.edit().putLong("alive", now).apply()

    /**
     * Pembaca tersambung. Selang mati hanya dicatat saat proses Cukup baru hidup lagi ([freshProcess]) dan denyut
     * terakhirnya sudah lama; sambung ulang di proses yang sama (mis. setelah HP tidur) bukan berarti sempat mati.
     * [bootedAt] = kapan HP dinyalakan: selagi HP mati tidak ada notifikasi yang terlewat.
     */
    @Synchronized
    fun connected(now: Long, freshProcess: Boolean, bootedAt: Long) {
        val last = prefs.getLong("alive", 0)
        val from = maxOf(last, bootedAt)
        if (freshProcess && last > 0 && now - from > GAP_MS) {
            // Selang yang belum kamu cek tidak ditimpa: awalnya tetap yang paling lama.
            val earlier = prefs.getLong("gapFrom", 0).takeIf { it > 0 }
            prefs.edit().putLong("gapFrom", minOf(earlier ?: from, from)).putLong("gapTo", now).apply()
        }
        beat(now)
        _state.value = _state.value.copy(gap = loadGap(), connected = true)
    }

    @Synchronized
    fun disconnected() {
        _state.value = _state.value.copy(connected = false)
    }

    @Synchronized
    fun dismissGap() {
        prefs.edit().remove("gapFrom").remove("gapTo").apply()
        _state.value = _state.value.copy(gap = null)
    }

    private fun loadGap(): Gap? {
        val from = prefs.getLong("gapFrom", 0)
        val to = prefs.getLong("gapTo", 0)
        return if (from > 0 && to > from) Gap(from, to) else null
    }

    private fun load(): List<Entry> = runCatching {
        val a = JSONArray(file.readText())
        List(a.length()) { i ->
            val o = a.getJSONObject(i)
            Entry(o.getLong("at"), o.getString("app"), o.getString("text"), Result.valueOf(o.getString("result")), o.getString("detail"))
        }
    }.getOrDefault(emptyList())

    companion object {
        const val KEEP = 30
        private const val KEEP_SKIPPED = 12
        private const val SEEN = 200
        /** Denyut tiap 5 menit; lebih dari 20 menit tanpa denyut berarti prosesnya sempat dimatikan. */
        const val BEAT_MS = 5 * 60_000L
        private const val GAP_MS = 20 * 60_000L
    }
}
