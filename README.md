# Cukup

Aplikasi keuangan pribadi Android untuk Gen Z Indonesia. Semua data ada di HP, tanpa internet.

Cukup punya dua bagian yang terpisah:

| | Catatan | Rencana |
|---|---|---|
| Isinya | Dompet (tunai, bank, e-wallet, paylater) dan saldonya. Uang masuk, keluar, dan pindah. | Pembagian persen (mis. 50/30/20), batas belanja, target tabungan. |
| Mengubah saldo? | Ya | **Tidak.** Hanya membandingkan batas dengan catatan. |

```
domain/  Kotlin murni: pembagian persen, siklus gajian, saldo pos, pembaca notifikasi, penebak pos (+ unit test)
app/     Android: Compose, Room, Hilt, Glance widget, pembaca notifikasi, kunci biometrik
docs/    BRIEF.md (spesifikasi) · BRAND.md (desain)
```

## Pasang di HP tanpa kabel

Setiap push ke GitHub otomatis membangun APK di **Releases**. Langkahnya ada di [`docs/UPDATE-NIRKABEL.md`](docs/UPDATE-NIRKABEL.md).

## Pasang lewat laptop

Klik dua kali **`jalankan.bat`** lalu pilih:

1. **Pasang ke HP** — sambungkan HP dengan kabel USB (aktifkan *USB debugging*), aplikasi dibangun dan dipasang.
2. **Buat APK saja** — hasilnya `Cukup-debug.apk` di folder ini; kirim ke HP dan pasang manual.
3. **Tes** — menjalankan semua unit test.
4. **Emulator** — menyalakan HP virtual di komputer dan memasang aplikasinya.

### Mengaktifkan USB debugging di HP
Setelan → Tentang ponsel → ketuk **Nomor build** 7× → kembali → Opsi pengembang → aktifkan **USB debugging**.
Saat HP disambungkan, izinkan komputer ini.

## Untuk developer

Kebutuhan (sudah terpasang di komputer ini): JDK 17 (Temurin), Android SDK di `D:\Android\Sdk`.

```bash
./gradlew :domain:test          # unit test logika inti
./gradlew :app:assembleDebug    # APK debug
./gradlew :app:installDebug     # pasang ke HP/emulator yang tersambung
```

> Di Windows ini Gradle butuh `JAVA_TOOL_OPTIONS=-Djdk.net.unixdomain.tmpdir=D:/Android/tmp`
> (sudah diatur di `jalankan.bat`). Tanpa itu muncul *"Unable to establish loopback connection"*.

## Status

v0.6 — lihat `docs/BRIEF.md` untuk cakupan, koreksi terhadap dokumen riset, dan rencana fase berikutnya.
