package id.cukup.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface CukupDao {

    // Dompet
    @Query("SELECT * FROM accounts WHERE archived = 0 ORDER BY sortOrder")
    fun observeAccounts(): Flow<List<AccountEntity>>

    @Query("SELECT * FROM accounts WHERE archived = 0 ORDER BY sortOrder")
    suspend fun accounts(): List<AccountEntity>

    @Query("SELECT * FROM accounts WHERE id = :id")
    suspend fun account(id: Long): AccountEntity?

    @Upsert
    suspend fun upsertAccount(a: AccountEntity): Long

    @Query("UPDATE accounts SET archived = 1 WHERE id = :id")
    suspend fun archiveAccount(id: Long)

    // Kategori
    @Query("SELECT * FROM categories WHERE archived = 0 ORDER BY sortOrder")
    fun observeCategories(): Flow<List<CategoryEntity>>

    @Query("SELECT * FROM categories WHERE archived = 0 ORDER BY sortOrder")
    suspend fun categories(): List<CategoryEntity>

    /** Termasuk yang diarsipkan, agar transaksi lama tetap punya nama kategori. */
    @Query("SELECT * FROM categories")
    fun observeAllCategories(): Flow<List<CategoryEntity>>

    @Upsert
    suspend fun upsertCategory(c: CategoryEntity): Long

    @Upsert
    suspend fun upsertCategories(c: List<CategoryEntity>)

    @Query("UPDATE categories SET archived = 1 WHERE id = :id")
    suspend fun archiveCategory(id: Long)

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

    /** Notifikasi lawan (masuk vs keluar) bernominal sama, untuk digabung jadi Pindah. */
    @Query(
        "SELECT * FROM transactions WHERE amount = :amount AND type = :type AND source = 'NOTIFICATION' " +
            "AND occurredAt BETWEEN :from AND :to AND status != 'DISMISSED' ORDER BY occurredAt DESC",
    )
    suspend fun findNotified(amount: Long, type: String, from: Long, to: Long): List<TransactionEntity>

    /** Duplikat sungguhan: nominal, jenis, dan dompet sama dalam rentang waktu. */
    @Query(
        "SELECT COUNT(*) FROM transactions WHERE amount = :amount AND type = :type AND accountId IS :accountId " +
            "AND occurredAt BETWEEN :from AND :to AND status != 'DISMISSED'",
    )
    suspend fun countSame(amount: Long, type: String, accountId: Long?, from: Long, to: Long): Int

    // Rencana
    @Query("SELECT * FROM plan ORDER BY sortOrder")
    fun observePlan(): Flow<List<PlanPosEntity>>

    @Query("SELECT * FROM plan ORDER BY sortOrder")
    suspend fun plan(): List<PlanPosEntity>

    @Upsert
    suspend fun upsertPlan(p: List<PlanPosEntity>): List<Long>

    @Query("DELETE FROM plan WHERE id IN (:ids)")
    suspend fun deletePlan(ids: List<Long>)

    @Query("UPDATE categories SET planId = NULL WHERE planId IN (:ids)")
    suspend fun unlinkPlan(ids: List<Long>)

    // Target
    @Query("SELECT * FROM goals ORDER BY sortOrder")
    fun observeGoals(): Flow<List<GoalEntity>>

    @Query("SELECT * FROM goals WHERE id = :id")
    suspend fun goal(id: Long): GoalEntity?

    @Upsert
    suspend fun upsertGoal(g: GoalEntity): Long

    @Query("DELETE FROM goals WHERE id = :id")
    suspend fun deleteGoal(id: Long)

    // Aturan merchant
    @Query("SELECT * FROM merchant_rules")
    suspend fun merchantRules(): List<MerchantRuleEntity>

    @Upsert
    suspend fun upsertRule(rule: MerchantRuleEntity)

    // Cadangan: semua baris termasuk yang diarsipkan/diabaikan.
    @Query("SELECT * FROM accounts") suspend fun allAccounts(): List<AccountEntity>
    @Query("SELECT * FROM categories") suspend fun allCategories(): List<CategoryEntity>
    @Query("SELECT * FROM transactions") suspend fun allTransactions(): List<TransactionEntity>
    @Query("SELECT * FROM goals") suspend fun allGoals(): List<GoalEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun restoreAccounts(x: List<AccountEntity>)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun restoreCategories(x: List<CategoryEntity>)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun restoreTransactions(x: List<TransactionEntity>)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun restorePlan(x: List<PlanPosEntity>)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun restoreGoals(x: List<GoalEntity>)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun restoreRules(x: List<MerchantRuleEntity>)
}
