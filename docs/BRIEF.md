# Cukup — Brief Produk & Spesifikasi MVP

Versi 0.2 · 30 September 2026 · Status: MVP sedang dibangun
Sumber awal: *Riset Framework dan Workflow Aplikasi Keuangan Android Gen Z Berbasis AI.docx*. Dokumen ini menggantikannya sebagai acuan kerja.

---

## 1. Ringkasan

**Cukup** adalah aplikasi keuangan pribadi Android untuk Gen Z Indonesia. Janjinya: *tahu uangmu masih cukup atau tidak, tanpa mencatat semuanya sendiri.*

| Prinsip | Artinya |
|---|---|
| **Uang dipilah ke pos** | Setiap pemasukan dibagi otomatis ke pos-pos sesuai persentase. Pembagian itu tercatat. |
| **Catat tanpa capek** | Transaksi e-wallet & m-banking terbaca dari notifikasi. Input manual cukup nominal + pos. |
| **Satu angka yang penting** | Beranda menjawab: *aman dipakai hari ini berapa?* |
| **Privat secara default** | Semua data di HP. Tanpa akun, tanpa server, tanpa iklan. |
| **Membimbing, bukan menghakimi** | Nada tenang. Tidak ada merah menyala. |

---

## 2. Konsep anggaran: Pos berbasis persentase

Anggaran **tidak** memakai nominal tetap. Pengguna membuat **pos** (amplop) sendiri, masing-masing dengan persentase. Total semua pos = **100%**.

```
Gaji masuk Rp4.000.000
 ├─ Makan & harian   30%  → Rp1.200.000
 ├─ Kos & tagihan    25%  → Rp1.000.000
 ├─ Nongkrong        15%  → Rp  600.000
 ├─ Cicilan          10%  → Rp  400.000
 └─ Tabungan         20%  → Rp  800.000
```

Aturan:
1. **Setiap pemasukan** (gaji, freelance, transfer masuk) dibagi ke semua pos sesuai persentase *saat itu*. Hasilnya disimpan sebagai **alokasi** — jadi kalau persentase diubah bulan depan, riwayat lama tidak berubah.
2. Pemasukan bisa juga **langsung ke satu pos** (mis. kado ulang tahun → Tabungan) atau dibagi manual.
3. **Setiap pengeluaran** mengambil dari satu pos. Saldo pos = alokasi masuk − pengeluaran ± pindahan.
4. **Pindah antar pos** tercatat (mis. Tabungan → Makan saat darurat).
5. Pembulatan: pembagian memakai metode *largest remainder* sehingga jumlah alokasi selalu tepat sama dengan pemasukan, tidak ada rupiah hilang.
6. Setiap pos punya **jenis**: *Pakai* (dihitung di "aman dipakai"), *Simpan* (tabungan/dana darurat, tidak dihitung), *Cicilan* (untuk hutang/paylater).
7. Preset awal (bisa diubah bebas):
   - **Seimbang 50/30/20** — Kebutuhan 50 · Keinginan 30 · Tabungan 20 (panduan umum literasi keuangan OJK).
   - **Lunasi hutang** — Kebutuhan 40 · Cicilan 30 · Tabungan 20 · Keinginan 10.
   - **Anak kos** — Makan 35 · Kos & tagihan 30 · Transport 10 · Nongkrong 10 · Tabungan 15.

---

## 3. Koreksi terhadap dokumen riset

| Klaim di riset | Kenyataan | Keputusan |
|---|---|---|
| SNAP BI untuk menarik mutasi rekening | SNAP dipakai antar lembaga berizin BI. Aplikasi perorangan tidak bisa mengakses data rekening nasabah tanpa lisensi + kerja sama tiap bank. | **Ditunda (Fase 3).** Diganti pembacaan notifikasi m-banking; impor CSV mutasi di Fase 2. |
| Vico `ComposedChartEntryModel`, `PieChartHost` | API Vico 1.x (sudah dihapus). Vico 2.x tidak punya pie chart. | Vico 2 untuk grafik harian; cincin pos digambar sendiri dengan Compose Canvas. |
| Gemini Nano sebagai mesin utama | Hanya di sebagian kecil HP flagship. | **Rule engine lokal = jalur utama** yang belajar dari koreksi. Gemini Nano opsional (Fase 2). |
| 40-30-20-10 "standar OJK" | Sumbernya konten Lemon8. | Jadi preset "Lunasi hutang", tidak diklaim standar OJK. |
| Glance → Wear OS & Android Auto | Wear memakai Tiles. | Widget layar utama saja. |
| NotificationListener tanpa catatan kebijakan | Google Play mewajibkan pengungkapan jelas. | Layar penjelasan sebelum meminta izin; fitur opsional. |

---

## 4. Cakupan

### Fase 1 — MVP (dibangun sekarang)
1. **Onboarding**: nama panggilan → tanggal gajian → pilih preset pos → sesuaikan persentase.
2. **Beranda**: aman dipakai hari ini, total uang, daftar pos (saldo + bar terpakai), transaksi terbaru, kotak "Perlu dicek".
3. **Catat**: keypad besar; Keluar / Masuk / Pindah; pilih pos; catatan. Masuk → pratinjau pembagian ke pos.
4. **Pos**: atur nama, emoji, jenis, persentase (penanda total harus 100%). Detail pos = riwayat alokasi & pengeluarannya.
5. **Deteksi otomatis**: NotificationListenerService untuk GoPay, OVO, DANA, ShopeePay, LinkAja, BCA, Mandiri (Livin'), BRImo, BNI, Jago, SeaBank, blu, Kredivo, Akulaku, SPayLater. Masuk ke **Perlu dicek**; satu ketuk untuk konfirmasi.
6. **Pos otomatis**: rule engine kata kunci merchant → jenis pos; koreksi pengguna diingat per merchant.
7. **Paylater**: belanja paylater dicatat sebagai hutang (tidak mengurangi pos sekarang); pembayaran cicilan mengurangi pos Cicilan dan sisa hutang.
8. **Riwayat**: grafik pengeluaran harian siklus ini (Vico), daftar per hari, filter pos.
9. **Widget** (Glance): aman dipakai hari ini + tombol catat.
10. **Keamanan**: kunci biometrik opsional; `allowBackup=false`; tanpa izin internet.

### Fase 2
Gemini Nano · impor CSV mutasi · ekspor CSV · target tabungan · SQLCipher · pengingat langganan.

### Fase 3
Backend + mitra agregator berizin · sinkron multi-perangkat.

---

## 5. Model data (Room)

| Tabel | Kolom |
|---|---|
| `pockets` | id, name, emoji, percent (0–100), kind (SPEND/SAVE/DEBT), sortOrder, archived |
| `transactions` | id, type (INCOME/EXPENSE/MOVE), amount (Long rupiah), pocketId (sumber utk EXPENSE/MOVE; null utk INCOME dibagi), toPocketId (MOVE / INCOME satu pos), merchant, note, occurredAt, source (MANUAL/NOTIFICATION), sourceApp, status (CONFIRMED/PENDING/DISMISSED), isPaylater, fingerprint (unik) |
| `allocations` | id, transactionId, pocketId, amount, percentAtTime |
| `merchant_rules` | merchantKey (PK), pocketId, updatedAt |

Pengaturan (DataStore): nama, tanggal gajian, biometrik, onboarding selesai.

**Saldo pos** = Σ alokasi + Σ pindahan masuk − Σ pengeluaran (non-paylater, CONFIRMED) − Σ pindahan keluar.
**Hutang paylater** = Σ pengeluaran paylater − Σ pengeluaran dari pos jenis DEBT.
**Aman dipakai hari ini** = Σ saldo pos jenis *Pakai* ÷ sisa hari sampai gajian berikutnya (min 1).
**Anti-duplikat**: fingerprint = sourceApp + nominal + menit + merchant ternormalisasi.

---

## 6. Arsitektur

Clean Architecture + MVI (state tunggal, intent, efek sekali jalan), offline-first, Room → Flow sebagai satu sumber kebenaran.

```
domain/   Modul Kotlin murni: model, rumus (Allocator, Cycle, Balances), NotificationParser, MerchantClassifier. Diuji unit.
app/      Android: data (Room, DataStore, repository), ui (tema, layar MVI), notif listener, widget Glance, Hilt.
```

Stack: Kotlin 2.2 · Compose Material 3 · Hilt · Room · DataStore · Glance · Vico 2 · Biometric · JUnit.
