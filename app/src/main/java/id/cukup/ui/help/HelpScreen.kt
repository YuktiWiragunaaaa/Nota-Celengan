package id.cukup.ui.help

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import id.cukup.ui.components.Bullet
import id.cukup.ui.components.CardShape
import id.cukup.ui.components.Eyebrow
import id.cukup.ui.components.Gutter
import id.cukup.ui.components.SectionHeader
import id.cukup.ui.components.TopBar
import id.cukup.ui.theme.Type
import id.cukup.ui.theme.colors

/** Panduan singkat: apa bedanya Catatan dan Rencana, dan apa efek tiap tombol. */
@Composable
fun HelpScreen(onBack: () -> Unit) {
    val c = colors
    Column(Modifier.fillMaxSize().background(c.paper).systemBarsPadding()) {
        TopBar("Cara pakai Cukup", onBack)
        Column(Modifier.verticalScroll(rememberScrollState()).padding(bottom = 32.dp)) {
            Text(
                "Cukup punya dua bagian yang terpisah. Catatan berisi uang yang sungguhan ada. Rencana berisi batas yang kamu buat sendiri.",
                style = Type.statement, color = c.ink, modifier = Modifier.padding(horizontal = Gutter),
            )
            Row(Modifier.padding(Gutter).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Side(
                    "📒 CATATAN", "Apa yang terjadi",
                    listOf("Dompet & saldonya", "Uang masuk / keluar", "Pindah antar dompet", "Mengubah saldo"),
                    Modifier.weight(1f),
                )
                Side(
                    "🎯 RENCANA", "Apa yang kamu mau",
                    listOf("Pembagian 50/30/20", "Batas belanja", "Target tabungan", "Tidak mengubah saldo"),
                    Modifier.weight(1f),
                )
            }

            SectionHeader("1. Dompet")
            Para("Dompet adalah tempat uangmu berada: tunai, rekening bank, GoPay, OVO, dan seterusnya. Isi saldonya sekali di awal.")
            Para("\"Uangmu sekarang\" di halaman Catatan = jumlah saldo semua dompet (paylater tidak dihitung karena itu hutang).")
            Para("Kalau angkanya beda dengan aplikasi bank, buka dompetnya lalu ketuk Samakan saldo.")

            SectionHeader("2. Tiga tombol")
            Effect("Keluar", "Saldo dompet yang dipilih berkurang. Kategorinya menentukan grafik \"Keluar ke mana?\".")
            Effect("Masuk", "Saldo dompet yang dipilih bertambah. Tidak dibagi ke mana-mana.")
            Effect("Pindah", "Uang pindah dari satu dompet ke dompet lain, misalnya top up GoPay dari BCA atau tarik tunai. Total uangmu tetap sama.")
            Para("Sebelum menyimpan, kotak \"Dampaknya\" selalu menunjukkan saldo sebelum → sesudah.")

            SectionHeader("3. Kategori")
            Para("Kategori (Makan, Transport, Gaji, dan lainnya) cuma label supaya kamu tahu uang habis ke mana. Bisa diubah di Setelan › Kategori.")

            SectionHeader("4. Rencana (opsional)")
            Para("Rencana membagi uang masuk menjadi beberapa pos, misalnya 50/30/20:")
            Column(Modifier.padding(horizontal = Gutter).fillMaxWidth().clip(CardShape).background(c.card).padding(14.dp)) {
                Text("Gaji Rp4.000.000", style = Type.strong, color = c.ink)
                Bullet("Kebutuhan 50% → batas belanja Rp2.000.000")
                Bullet("Keinginan 30% → batas belanja Rp1.200.000")
                Bullet("Tabungan 20% → target sisihan Rp800.000")
            }
            Spacer(Modifier.height(8.dp))
            Para("Pembagian ini tidak memindahkan uang. Uangnya tetap di dompet. Tiap kamu mencatat uang keluar, Cukup mengurangi sisa batas pos yang sesuai kategori itu (Makan → Kebutuhan, Jajan → Keinginan).")
            Para("\"Uang yang dibagi\" bisa angka tetap, uang masuk periode lalu (contoh: belanja maksimal 50% dari gaji minggu kemarin), atau uang masuk periode ini.")
            Para("\"Aman per hari\" = sisa batas belanja dibagi sisa hari sampai gajian.")
            Para("Pos Tabungan terisi saat kamu Pindah uang ke dompet berjenis Tabungan.")

            SectionHeader("5. Target tabungan")
            Para("Buat target (mis. HP baru Rp3 jt). Terkumpulnya bisa diisi sendiri, atau mengikuti saldo satu dompet khusus. Cukup memperkirakan kapan tercapai dari rencana tabunganmu.")

            SectionHeader("6. Batas & peringatan")
            Effect("Batas sekali belanja", "Per transaksi. Kalau satu kali belanja di atas angka ini, Cukup tanya dulu.")
            Effect("Peringatan rencana", "Untuk total periode. Muncul saat 80% batas belanja terpakai, dan saat lewat.")

            SectionHeader("7. Catat otomatis")
            Para("Kalau izin baca notifikasi dinyalakan, pembayaran dari e-wallet dan m-banking terbaca otomatis dan masuk \"Perlu dicek\" (ikon lonceng). Saldo baru berubah setelah kamu simpan.")
        }
    }
}

@Composable
private fun Side(title: String, subtitle: String, items: List<String>, modifier: Modifier) {
    val c = colors
    Column(modifier.clip(CardShape).background(c.card).padding(14.dp)) {
        Eyebrow(title, color = c.accent)
        Text(subtitle, style = Type.strong, color = c.ink)
        Spacer(Modifier.height(6.dp))
        items.forEach { Text("• $it", style = Type.bodySmall, color = c.mute, modifier = Modifier.padding(vertical = 2.dp)) }
    }
}

@Composable
private fun Para(text: String) {
    Text(text, style = Type.body, color = colors.mute, modifier = Modifier.padding(horizontal = Gutter, vertical = 4.dp))
}

@Composable
private fun Effect(name: String, effect: String) {
    val c = colors
    Column(Modifier.padding(horizontal = Gutter, vertical = 6.dp)) {
        Text(name, style = Type.strong, color = c.ink)
        Text(effect, style = Type.body, color = c.mute)
    }
}
