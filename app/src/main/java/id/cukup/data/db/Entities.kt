package id.cukup.data.db

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import id.cukup.domain.Account
import id.cukup.domain.AccountKind
import id.cukup.domain.Category
import id.cukup.domain.CategoryKind
import id.cukup.domain.Goal
import id.cukup.domain.PlanKind
import id.cukup.domain.PlanPos
import id.cukup.domain.Tag
import id.cukup.domain.Transaction
import id.cukup.domain.TxSource
import id.cukup.domain.TxStatus
import id.cukup.domain.TxType

/** Dompet: tempat uang sungguhan (tunai, rekening, e-wallet). */
@Entity(tableName = "accounts")
data class AccountEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val emoji: String,
    val kind: String,
    val initialBalance: Long,
    val sortOrder: Int,
    val color: Int? = null,
    val archived: Boolean = false,
) {
    fun toDomain() = Account(id, name, emoji, AccountKind.valueOf(kind), initialBalance, sortOrder, color)

    companion object {
        fun from(a: Account) = AccountEntity(a.id, a.name, a.emoji, a.kind.name, a.initialBalance, a.sortOrder, a.color)
    }
}

@Entity(tableName = "categories")
data class CategoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val emoji: String,
    val kind: String,
    val tag: String,
    val sortOrder: Int,
    val color: Int? = null,
    val planId: Long? = null,
    val archived: Boolean = false,
) {
    fun toDomain() = Category(id, name, emoji, CategoryKind.valueOf(kind), Tag.valueOf(tag), sortOrder, color, planId)

    companion object {
        fun from(c: Category) = CategoryEntity(c.id, c.name, c.emoji, c.kind.name, c.tag.name, c.sortOrder, c.color, c.planId)
    }
}

@Entity(
    tableName = "transactions",
    indices = [Index("occurredAt"), Index("status"), Index("accountId"), Index(value = ["fingerprint"], unique = true)],
)
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val type: String,
    val amount: Long,
    val accountId: Long?,
    val toAccountId: Long?,
    val categoryId: Long?,
    val merchant: String,
    val note: String,
    val occurredAt: Long,
    val source: String,
    val sourceApp: String?,
    val status: String,
    val fingerprint: String?,
) {
    fun toDomain() = Transaction(
        id, TxType.valueOf(type), amount, accountId, toAccountId, categoryId, merchant, note, occurredAt,
        TxSource.valueOf(source), sourceApp, TxStatus.valueOf(status), fingerprint,
    )

    companion object {
        fun from(t: Transaction) = TransactionEntity(
            t.id, t.type.name, t.amount, t.accountId, t.toAccountId, t.categoryId, t.merchant, t.note, t.occurredAt,
            t.source.name, t.sourceApp, t.status.name, t.fingerprint,
        )
    }
}

/** Pos rencana (mis. Kebutuhan 50%). Hanya rencana — tidak memegang uang. */
@Entity(tableName = "plan")
data class PlanPosEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val emoji: String,
    val percent: Int,
    val kind: String,
    val sortOrder: Int,
    val color: Int? = null,
) {
    fun toDomain() = PlanPos(id, name, emoji, percent, PlanKind.valueOf(kind), sortOrder, color)

    companion object {
        fun from(p: PlanPos) = PlanPosEntity(p.id, p.name, p.emoji, p.percent, p.kind.name, p.sortOrder, p.color)
    }
}

@Entity(tableName = "goals")
data class GoalEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val emoji: String,
    val target: Long,
    val saved: Long,
    val accountId: Long?,
    val color: Int?,
    val sortOrder: Int,
) {
    fun toDomain() = Goal(id, name, emoji, target, saved, accountId, color, sortOrder)

    companion object {
        fun from(g: Goal) = GoalEntity(g.id, g.name, g.emoji, g.target, g.saved, g.accountId, g.color, g.sortOrder)
    }
}

@Entity(tableName = "merchant_rules")
data class MerchantRuleEntity(
    @PrimaryKey val merchantKey: String,
    val categoryId: Long,
    val updatedAt: Long,
)
