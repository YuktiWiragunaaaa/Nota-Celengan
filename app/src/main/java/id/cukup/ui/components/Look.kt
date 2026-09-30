package id.cukup.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import id.cukup.domain.Account
import id.cukup.domain.AccountKind
import id.cukup.domain.Category
import id.cukup.domain.PlanPos
import id.cukup.ui.theme.colors

@Composable
fun colorOf(c: Category?): Color = if (c == null) colors.faint else colors.of(c.color, c.sortOrder)

@Composable
fun colorOf(a: Account?): Color = if (a == null) colors.faint else colors.of(a.color, a.sortOrder + 3)

@Composable
fun colorOf(p: PlanPos): Color = colors.of(p.color, p.sortOrder)

/** Nama jenis dompet dalam bahasa sehari-hari. */
fun AccountKind.label(): String = when (this) {
    AccountKind.CASH -> "Tunai"
    AccountKind.BANK -> "Rekening bank"
    AccountKind.EWALLET -> "E-wallet"
    AccountKind.SAVINGS -> "Tabungan"
    AccountKind.PAYLATER -> "Paylater / kartu kredit"
}
