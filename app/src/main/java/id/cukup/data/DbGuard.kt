package id.cukup.data

import android.content.Context
import android.os.Build
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * Pengaman database. Dipanggil sebelum Room membuka database:
 * kalau versi aplikasi berubah (update), file database disalin apa adanya dulu,
 * jadi migrasi yang gagal tidak pernah menghilangkan data.
 */
object DbGuard {
    const val DB_NAME = "cukup.db"
    private const val KEEP_PRE_UPDATE = 3
    private val stamp = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss")

    fun backupDir(context: Context) = File(context.filesDir, "backups").apply { mkdirs() }

    fun beforeOpen(context: Context) {
        runCatching {
            val prefs = context.getSharedPreferences("db_guard", Context.MODE_PRIVATE)
            val current = versionCode(context)
            val last = prefs.getLong("version_code", -1)
            val db = context.getDatabasePath(DB_NAME)
            if (last != current && db.exists()) {
                val dir = File(backupDir(context), "pre-update-$last-to-$current-${LocalDateTime.now().format(stamp)}").apply { mkdirs() }
                listOf("", "-wal", "-shm").forEach { suffix ->
                    val f = File(db.path + suffix)
                    if (f.exists()) f.copyTo(File(dir, f.name), overwrite = true)
                }
                // Simpan beberapa salinan terakhir saja.
                backupDir(context).listFiles { f -> f.isDirectory && f.name.startsWith("pre-update-") }
                    ?.sortedByDescending { it.lastModified() }
                    ?.drop(KEEP_PRE_UPDATE)
                    ?.forEach { it.deleteRecursively() }
            }
            prefs.edit().putLong("version_code", current).apply()
        }
    }

    private fun versionCode(context: Context): Long {
        val info = context.packageManager.getPackageInfo(context.packageName, 0)
        return if (Build.VERSION.SDK_INT >= 28) info.longVersionCode else @Suppress("DEPRECATION") info.versionCode.toLong()
    }
}
