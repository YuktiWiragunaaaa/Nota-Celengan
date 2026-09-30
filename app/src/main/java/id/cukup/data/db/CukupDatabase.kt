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
    version = 4,
    exportSchema = true,
)
abstract class CukupDatabase : RoomDatabase() {
    abstract fun dao(): CukupDao
}
