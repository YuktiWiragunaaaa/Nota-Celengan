package id.cukup.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface CukupDao {

    // Pos
    @Query("SELECT * FROM pockets WHERE archived = 0 ORDER BY sortOrder")
    fun observePockets(): Flow<List<PocketEntity>>

    @Query("SELECT * FROM pockets WHERE archived = 0 ORDER BY sortOrder")
    suspend fun pockets(): List<PocketEntity>

    @Upsert
    suspend fun upsertPockets(pockets: List<PocketEntity>): List<Long>

    @Query("UPDATE pockets SET archived = 1, percent = 0 WHERE id IN (:ids)")
    suspend fun archivePockets(ids: List<Long>)

    @Query("SELECT COUNT(*) FROM pockets WHERE archived = 0")
    suspend fun pocketCount(): Int

    // Transaksi
    @Query("SELECT * FROM transactions WHERE status != 'DISMISSED' ORDER BY occurredAt DESC")
    fun observeTransactions(): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE id = :id")
    suspend fun transaction(id: Long): TransactionEntity?

    @Query("SELECT * FROM transactions WHERE status = 'PENDING' ORDER BY occurredAt DESC")
    fun observePending(): Flow<List<TransactionEntity>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertTransaction(t: TransactionEntity): Long

    @Upsert
    suspend fun upsertTransaction(t: TransactionEntity): Long

    @Query("DELETE FROM transactions WHERE id = :id")
    suspend fun deleteTransaction(id: Long)

    /** Transaksi dengan nominal sama dalam rentang waktu — untuk menandai kemungkinan duplikat. */
    @Query(
        "SELECT COUNT(*) FROM transactions WHERE amount = :amount AND type = :type " +
            "AND occurredAt BETWEEN :from AND :to AND status != 'DISMISSED'",
    )
    suspend fun countSimilar(amount: Long, type: String, from: Long, to: Long): Int

    // Alokasi
    @Query("SELECT * FROM allocations")
    fun observeAllocations(): Flow<List<AllocationEntity>>

    @Insert
    suspend fun insertAllocations(a: List<AllocationEntity>)

    @Query("DELETE FROM allocations WHERE transactionId = :txId")
    suspend fun deleteAllocations(txId: Long)

    // Aturan merchant
    @Query("SELECT * FROM merchant_rules")
    suspend fun merchantRules(): List<MerchantRuleEntity>

    @Upsert
    suspend fun upsertRule(rule: MerchantRuleEntity)
}
