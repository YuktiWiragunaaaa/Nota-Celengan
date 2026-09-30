package id.cukup.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class LedgerTest {

    private val cash = Account(1, "Tunai", "💵", AccountKind.CASH, initialBalance = 100_000)
    private val gopay = Account(2, "GoPay", "🟢", AccountKind.EWALLET, initialBalance = 50_000, sortOrder = 1)
    private val paylater = Account(3, "Kredivo", "🧾", AccountKind.PAYLATER, sortOrder = 2)
    private val food = Category(1, "Makan", "🍜", CategoryKind.EXPENSE, Tag.FOOD)
    private val salary = Category(2, "Gaji", "💼", CategoryKind.INCOME, Tag.SALARY)

    private val txs = listOf(
        Transaction(1, TxType.INCOME, 1_000_000, accountId = 1, categoryId = 2, occurredAt = 10),
        Transaction(2, TxType.EXPENSE, 30_000, accountId = 2, categoryId = 1, occurredAt = 20),
        Transaction(3, TxType.TRANSFER, 200_000, accountId = 1, toAccountId = 2, occurredAt = 30),
        Transaction(4, TxType.EXPENSE, 150_000, accountId = 3, categoryId = 1, occurredAt = 40),
        Transaction(5, TxType.EXPENSE, 999_000, accountId = 1, status = TxStatus.PENDING, occurredAt = 50),
    )

    @Test
    fun `balances follow real money`() {
        val b = Ledger.balances(listOf(cash, gopay, paylater), txs).associate { it.account.id to it.balance }
        assertEquals(900_000L, b[1]) // 100 rb + 1 jt − 200 rb pindah
        assertEquals(220_000L, b[2]) // 50 rb − 30 rb + 200 rb
        assertEquals(-150_000L, b[3]) // hutang
    }

    @Test
    fun `net worth excludes paylater and debt is positive`() {
        val b = Ledger.balances(listOf(cash, gopay, paylater), txs)
        assertEquals(1_120_000L, Ledger.netWorth(b))
        assertEquals(150_000L, Ledger.debt(b))
    }

    @Test
    fun `transfers are neither income nor expense`() {
        val t = Ledger.totals(txs, 0, 100)
        assertEquals(1_000_000L, t.income)
        assertEquals(180_000L, t.expense)
        assertEquals(820_000L, t.net)
    }

    @Test
    fun `by category groups and sorts`() {
        val parts = Ledger.byCategory(txs, listOf(food, salary), TxType.EXPENSE)
        assertEquals(1, parts.size)
        assertEquals(180_000L, parts[0].amount)
        assertEquals(0, Ledger.byCategory(txs, listOf(food), TxType.EXPENSE, from = 100).size)
    }
}
