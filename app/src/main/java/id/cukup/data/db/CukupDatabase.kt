package id.cukup.data.db

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [PocketEntity::class, TransactionEntity::class, AllocationEntity::class, MerchantRuleEntity::class],
    version = 3,
    exportSchema = true,
)
abstract class CukupDatabase : RoomDatabase() {
    abstract fun dao(): CukupDao
}

/** v2: kantong punya warna pilihan sendiri. */
val MIGRATION_1_2 = object : androidx.room.migration.Migration(1, 2) {
    override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE pockets ADD COLUMN color INTEGER")
    }
}

/** v3: kantong bisa punya target tabungan. */
val MIGRATION_2_3 = object : androidx.room.migration.Migration(2, 3) {
    override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE pockets ADD COLUMN target INTEGER")
    }
}
