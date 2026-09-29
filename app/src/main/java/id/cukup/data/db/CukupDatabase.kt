package id.cukup.data.db

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [PocketEntity::class, TransactionEntity::class, AllocationEntity::class, MerchantRuleEntity::class],
    version = 1,
    exportSchema = true,
)
abstract class CukupDatabase : RoomDatabase() {
    abstract fun dao(): CukupDao
}
