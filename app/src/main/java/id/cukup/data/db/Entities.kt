package id.cukup.data.db

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import id.cukup.domain.Allocation
import id.cukup.domain.Pocket
import id.cukup.domain.PocketKind
import id.cukup.domain.PocketTag
import id.cukup.domain.Transaction
import id.cukup.domain.TxSource
import id.cukup.domain.TxStatus
import id.cukup.domain.TxType

@Entity(tableName = "pockets")
data class PocketEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val emoji: String,
    val percent: Int,
    val kind: String,
    val tag: String,
    val sortOrder: Int,
    val archived: Boolean = false,
    val color: Int? = null,
) {
    fun toDomain() = Pocket(id, name, emoji, percent, PocketKind.valueOf(kind), PocketTag.valueOf(tag), sortOrder, color)

    companion object {
        fun from(p: Pocket) = PocketEntity(p.id, p.name, p.emoji, p.percent, p.kind.name, p.tag.name, p.sortOrder, color = p.color)
    }
}

@Entity(
    tableName = "transactions",
    indices = [Index("occurredAt"), Index("status"), Index(value = ["fingerprint"], unique = true)],
)
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val type: String,
    val amount: Long,
    val pocketId: Long?,
    val toPocketId: Long?,
    val merchant: String,
    val note: String,
    val occurredAt: Long,
    val source: String,
    val sourceApp: String?,
    val status: String,
    val isPaylater: Boolean,
    val fingerprint: String?,
) {
    fun toDomain() = Transaction(
        id, TxType.valueOf(type), amount, pocketId, toPocketId, merchant, note, occurredAt,
        TxSource.valueOf(source), sourceApp, TxStatus.valueOf(status), isPaylater, fingerprint,
    )

    companion object {
        fun from(t: Transaction) = TransactionEntity(
            t.id, t.type.name, t.amount, t.pocketId, t.toPocketId, t.merchant, t.note, t.occurredAt,
            t.source.name, t.sourceApp, t.status.name, t.isPaylater, t.fingerprint,
        )
    }
}

@Entity(
    tableName = "allocations",
    foreignKeys = [
        ForeignKey(
            entity = TransactionEntity::class,
            parentColumns = ["id"],
            childColumns = ["transactionId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("transactionId"), Index("pocketId")],
)
data class AllocationEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val transactionId: Long,
    val pocketId: Long,
    val amount: Long,
    val percentAtTime: Int,
) {
    fun toDomain() = Allocation(transactionId, pocketId, amount, percentAtTime)
}

@Entity(tableName = "merchant_rules")
data class MerchantRuleEntity(
    @PrimaryKey val merchantKey: String,
    val pocketId: Long,
    val updatedAt: Long,
)
