package id.cukup.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import id.cukup.domain.Allocator
import id.cukup.domain.Pocket
import id.cukup.domain.Rupiah
import id.cukup.ui.theme.Type
import id.cukup.ui.theme.colors

/** Pratinjau pembagian pemasukan ke pos-pos sesuai persentase. */
@Composable
fun SplitPreview(amount: Long, pockets: List<Pocket>, modifier: Modifier = Modifier) {
    val c = colors
    val parts = Allocator.split(amount, pockets)
    Column(modifier) {
        parts.forEachIndexed { i, (p, v) ->
            Row(
                Modifier.fillMaxWidth().padding(horizontal = Gutter, vertical = 9.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                PocketDot(c.pocket(i))
                Spacer(Modifier.width(12.dp))
                Text("${p.emoji} ${p.name}", style = Type.body, color = c.ink, modifier = Modifier.weight(1f))
                Text("${p.percent}%", style = Type.bodySmall, color = c.faint, modifier = Modifier.padding(end = 14.dp))
                Text(Rupiah.format(v), style = Type.amount, color = if (v > 0) c.ink else c.faint)
            }
        }
    }
}
