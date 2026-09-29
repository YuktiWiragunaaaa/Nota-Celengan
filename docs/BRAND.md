# Cukup — Brand & Design System

Versi 0.1 · 30 September 2026 · Arah: **classic editorial**, keluarga yang sama dengan Lintas Waktu.
Sumber kebenaran: kode di `app/src/main/java/id/cukup/ui/theme/Theme.kt` dan `ui/components/`.

---

## 1. Identitas

| | |
|---|---|
| Nama | **Cukup** |
| Makna | "enough". Bukan aplikasi yang membuat kaya, tapi yang membuat **tenang** karena tahu batasnya. |
| Janji | *Uangmu, dipilah.* |
| Nada | Tenang, hangat, tidak menghakimi. Kalimat pendek. Satu frasa italic. Tidak ada tanda seru, tidak ada merah menyala. |
| Ikon | Cincin terbuka (pos hampir penuh) + satu titik, putih di atas arang. |

Contoh kalimat:
- ✓ "Sisa 20% belum punya pos."   ✗ "ERROR: Total harus 100%!"
- ✓ "Semua sudah *beres.*"   ✗ "Yay! Tidak ada notifikasi 🎉"

---

## 2. Warna

Sengaja **tanpa warna aksen**. Warna hanya muncul untuk membedakan pos dan menandai status.

| Token | Terang | Gelap | Dipakai untuk |
|---|---|---|---|
| `paper` | `#FBFAF7` | `#171614` | Latar |
| `ink` | `#1F1E1C` | `#F3F1EC` | Teks utama, tombol isi, tombol + |
| `mute` | `#77756F` | `#B9B6AE` | Body, label |
| `faint` | `#AEACA5` | `#7D7A73` | Placeholder, nominal 0 |
| `line` | `#E6E4DE` | `#34322E` | Garis 1px, bingkai |
| `dark` | `#2A2926` | `#0F0E0D` | **Satu** blok gelap per layar (kotak "Perlu dicek") |
| `caution` | `#9A6B2F` oker | `#D1A263` | Pos terpakai ≥ 80%, kemungkinan duplikat |
| `over` | `#A3402F` bata | `#D9826F` | Pos lewat 100%, saldo minus, hutang |

**Warna pos** (berurutan, tanah & foto analog): arang · tanah liat `#A0674B` · zaitun `#6F7355` · pasir `#C4A77D` · batu `#66737D` · mawar kering `#B48780` · kayu `#8C7A5B`.

---

## 3. Tipografi

| Peran | Font | Ukuran |
|---|---|---|
| Angka utama ("aman dipakai") | Instrument Serif | 52 |
| Judul layar | Instrument Serif, satu kata italic | 36 |
| Judul kecil / nominal baris | Instrument Serif | 20–26 |
| Pernyataan | Instrument Serif *Italic* | 20 |
| Body | Inter 400 | 15 |
| Kecil | Inter 400 | 13 |
| Eyebrow / tombol | Inter 500, KAPITAL, tracking 0.16em | 11 |

Nominal selalu `Rp1.250.000` (tanpa spasi, titik ribuan). Ringkas: `1,2 jt`, `45 rb`.

---

## 4. Komponen

| Komponen | Aturan |
|---|---|
| Tombol utama | Kotak isi `ink`, radius 2dp, label eyebrow |
| Tombol sekunder | Bingkai garis 1px `ink` |
| Pilihan (chip) | Kapsul bergaris `line`; terpilih = isi `ink` |
| Cincin pos | Segmen = persentase pos; bagian pekat = sisa uang, bagian pudar = sudah terpakai |
| Batang pakai | 3dp, warna pos → oker (≥80%) → bata (≥100%) |
| Pemisah | Garis 1px `line`, bukan kartu berbayang |
| Navigasi | Empat tab teks + tombol **+** bundar di tengah |
| Gerak | Fade antar layar; cincin & batang beranimasi 0,7–0,9 detik |
