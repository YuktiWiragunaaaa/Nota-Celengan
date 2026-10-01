package id.cukup.data.db

import androidx.room.Database
import androidx.room.RoomDatabase

/**
 * v4 (Cukup 0.6): catatan (dompet, kategori, transaksi) dipisah dari rencana (pos, target).
 * Struktur v1–v3 (kantong berbasis persen) tidak bisa diterjemahkan dengan jujur ke saldo dompet,
 * jadi database lama dikosongkan dan pengguna memulai lagi dari pengisian saldo.
 */
@Database(
    entities = [
        AccountEntity::class, CategoryEntity::class, TransactionEntity::class,
        PlanPosEntity::class, GoalEntity::class, MerchantRuleEntity::class,
    ],
    version = 6,
    exportSchema = true,
)
abstract class CukupDatabase : RoomDatabase() {
    abstract fun dao(): CukupDao

    companion object {
        /** v5: pos rencana bisa bernominal tetap dengan jangka sendiri (per gajian / minggu / hari). */
        private val MIGRATION_4_5 = object : androidx.room.migration.Migration(4, 5) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE plan ADD COLUMN amount INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE plan ADD COLUMN period TEXT NOT NULL DEFAULT 'CYCLE'")
            }
        }

        /** v6: dompet bisa dikelompokkan ke pos rencana. */
        private val MIGRATION_5_6 = object : androidx.room.migration.Migration(5, 6) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE accounts ADD COLUMN planId INTEGER")
            }
        }

        /**
         * Setiap kenaikan versi WAJIB punya migrasi di sini (mis. MIGRATION_4_5), dan skema barunya
         * ikut di-commit di app/schemas. Tanpa migrasi, aplikasi berhenti dan data tidak disentuh.
         */
        val MIGRATIONS: Array<androidx.room.migration.Migration> = arrayOf(MIGRATION_4_5, MIGRATION_5_6)
    }
}
